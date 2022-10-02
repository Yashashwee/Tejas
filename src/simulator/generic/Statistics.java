package generic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import main.ArchitecturalComponent;
import main.Main;
import memorysystem.Cache;
import memorysystem.TLB;
import memorysystem.CoreMemorySystem;
import memorysystem.MainMemoryController;
import memorysystem.MemorySystem;
import memorysystem.coherence.Coherence;
import memorysystem.nuca.NucaCache;
import memorysystem.nuca.NucaCache.NucaType;
import net.NocInterface;
import net.Router;
import config.CoreConfig;
import config.EmulatorConfig;
import config.EnergyConfig;
import config.SimulationConfig;
import config.SystemConfig;
import config.SystemConfig.Interconnect;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.translator.qemuTranslationCache.TranslatedInstructionCache;

import dram.MainMemoryDRAMController;
import config.MainMemoryConfig;

public class Statistics {
	
	
	static FileWriter outputFileWriter;
	
	static String benchmark;
	public static void printSystemConfig()
	{
		try
		{
			outputFileWriter.write("[Configuration]\n");
			outputFileWriter.write("\n");
			
			outputFileWriter.write("EmulatorType: " + EmulatorConfig.emulatorType + "\n");
			outputFileWriter.write("Benchmark: "+benchmark+"\n");
			outputFileWriter.write("Schedule: " + (new Date()).toString() + "\n");


			//added by harveenk
			if(SystemConfig.memControllerToUse==true){
				SystemConfig.printMainMemoryConfig(outputFileWriter);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	private static CoreMemorySystem coreMemSys[];
	private static Core cores[];
	
	public static void printTimingStatistics()
	{
		try
		{
			Core.printTimingStatistics(outputFileWriter);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	static void resetEnergyStatistics()
	{
			// Cores
			for(Core core : cores) {
				core.resetNumAccesses();
			}
			
			// Shared Cache
			for (Cache cache : ArchitecturalComponent.getSharedCacheList()) {
				cache.resetNumAccesses();
			}

			// Main Memory
			int memControllerId = 0;
			for(MainMemoryDRAMController memController : ArchitecturalComponent.memoryControllers) {
				memControllerId++;
				memController.resetNumAccesses();
			}
			
			// Coherence
			int coherenceId = 0;
			for(Coherence coherence : ArchitecturalComponent.coherences) {
				coherenceId++;
				coherence.resetNumAccesses();
			}
			
			// Interconnect
			ArchitecturalComponent.getInterConnect().resetNumAccesses();			
	}
		
	static void printEnergyStatistics()
	{
		EnergyConfig totalEnergy = new EnergyConfig(0, 0);
		
		try {
			// Cores
			int i = 0;
			
			outputFileWriter.write("\n\n[ LeakageEnergy   DynamicEnergy   TotalEnergy   NumDynamicAcc  ComponentName] : \n");

			EnergyConfig coreEnergy = new EnergyConfig(0, 0);
			i = 0;
			for(Core core : cores) {
				coreEnergy.add(core.calculateAndPrintEnergy(outputFileWriter, "core[" + (i++) + "]"));
			}
			
			outputFileWriter.write("\n\n");
			coreEnergy.printEnergyStats(outputFileWriter, "coreEnergy.total");
			totalEnergy.add(coreEnergy);
			
			outputFileWriter.write("\n\n");
			
			// Shared Cache
			EnergyConfig sharedCacheEnergy = new EnergyConfig(0, 0);
			for (Cache cache : ArchitecturalComponent.getSharedCacheList()) {
				sharedCacheEnergy.add(cache.calculateAndPrintEnergy(outputFileWriter, cache.toString()));
			}
			
			outputFileWriter.write("\n\n");
			sharedCacheEnergy.printEnergyStats(outputFileWriter, "sharedCacheEnergy.total");
			totalEnergy.add(sharedCacheEnergy);
						
			// Main Memory
			EnergyConfig mainMemoryEnergy = new EnergyConfig(0, 0);						
			int memControllerId = 0;
			outputFileWriter.write("\n\n");
			for(MainMemoryDRAMController memController : ArchitecturalComponent.memoryControllers) {
				String name = "MainMemoryDRAMController[" + memControllerId + "]";
				memControllerId++;
				mainMemoryEnergy.add(memController.calculateAndPrintEnergy(outputFileWriter, name));
			}
			
			outputFileWriter.write("\n");
			mainMemoryEnergy.printEnergyStats(outputFileWriter, "mainMemoryControllerEnergy.total");
			totalEnergy.add(mainMemoryEnergy);
			
			// Coherence
			EnergyConfig coherenceEnergy = new EnergyConfig(0, 0);						
			int coherenceId = 0;
			for(Coherence coherence : ArchitecturalComponent.coherences) {
				String name = "Coherence[" + coherenceId + "]";
				coherenceId++;
				coherenceEnergy.add(coherence.calculateAndPrintEnergy(outputFileWriter, name));
			}
			
			outputFileWriter.write("\n\n");
			coherenceEnergy.printEnergyStats(outputFileWriter, "coherenceEnergy.total");
			totalEnergy.add(coherenceEnergy);
			
			// Interconnect
			EnergyConfig interconnectEnergy = new EnergyConfig(0, 0);
			interconnectEnergy.add(ArchitecturalComponent.getInterConnect().calculateAndPrintEnergy(outputFileWriter, "Interconnect"));
			totalEnergy.add(interconnectEnergy);
			
			outputFileWriter.write("\n\n");
			totalEnergy.printEnergyStats(outputFileWriter, "TotalEnergy");
			
		} catch (Exception e) {
			System.err.println("error in printing stats + \nexception = " + e);
			e.printStackTrace();
		}
	}

	//Memory System Statistics
	static long totalNucaBankAccesses;
	
	public static String nocTopology;
	public static String nocRoutingAlgo;
	public static int hopcount=0;
		
	static float averageHopLength;
	static int maxHopLength;
	static int minHopLength;
	
	static long numInsWorkingSetHits[];
	static long numInsWorkingSetMisses[];
	static long maxInsWorkingSetSize[];
	static long minInsWorkingSetSize[];
	static long totalInsWorkingSetSize[];
	static long numInsWorkingSetsNoted[];
	
	static long numDataWorkingSetHits[];
	static long numDataWorkingSetMisses[];
	static long maxDataWorkingSetSize[];
	static long minDataWorkingSetSize[];
	static long totalDataWorkingSetSize[];
	static long numDataWorkingSetsNoted[];
	
	
	public static void printMemorySystemStatistics()
	{
		//for each core, print memory system statistics
		// Main.printStatisticsOnAsynchronousTermination = false;
		try
		{
			outputFileWriter.write("\n");
			outputFileWriter.write("[Memory System Statistics]\n");
			outputFileWriter.write("\n");
			
			outputFileWriter.write("[Per core statistics]\n");
			outputFileWriter.write("\n");
			
			for(int i = 0; i < SystemConfig.NoOfCores; i++)
			{
				if(cores[i].getCoreCyclesTaken()==0){
					outputFileWriter.write("Nothing executed on core "+i+"\n");
					continue;
				}
				TLB.printTLBStatistics(outputFileWriter, i);
				
				for(Cache c : coreMemSys[i].getCoreCacheList()) {
					c.printCacheStats(outputFileWriter);
					addToConsolidatedCacheList(c);
				}
				
				outputFileWriter.write("\n");
			}
			
			outputFileWriter.write("\n\n[Shared Caches]\n\n");
			for (Cache c : ArchitecturalComponent.getSharedCacheList()) {
				c.printCacheStats(outputFileWriter);
				addToConsolidatedCacheList(c);
			}
			
			outputFileWriter.write("\n\n[Consolidated Stats For Caches]\n\n");
			for (Map.Entry<String, Vector<Cache>> entry : consolidatedCacheList.entrySet()) {
				Cache.printConsolidatedCacheStats(outputFileWriter,entry.getKey(), entry.getValue());
			}
			ArchitecturalComponent.nucastats(outputFileWriter);
			
			if (SystemConfig.interconnect == Interconnect.Noc)
			{
				outputFileWriter.write("\n\nNOC Topology\t\t=\t" + SystemConfig.nocConfig.topology + "\n");
				outputFileWriter.write("NOC Routing Algorithm\t=\t" + SystemConfig.nocConfig.rAlgo + "\n");
			}
			if(SimulationConfig.nucaType!=NucaType.NONE)
			{
				/* TODO anuj
				double totalNucaBankPower = (totalNucaBankAccesses*PowerConfig.dcache2Power)/executionTime;
				outputFileWriter.write("Total Nuca Bank Accesses Power\t=\t" + totalNucaBankPower + "\n");
				double totalRouterPower = ((hopcount*(PowerConfig.linkEnergy+PowerConfig.totalRouterEnergy))/executionTime);
				
				
				if (hopcount != 0)
				{
					outputFileWriter.write("Router Hops\t=\t" + hopcount + "\n");
					outputFileWriter.write("Total Router Power\t=\t" + totalRouterPower + "\n");
				}*/
								
				/* TODO anuj
				double totalBufferPower = (Switch.totalBufferAccesses*PowerConfig.bufferEnergy)/executionTime;
				outputFileWriter.write("Total Buffer Accesses\t=\t" + Switch.totalBufferAccesses + "\n");
				if(totalBufferPower!=0)
					outputFileWriter.write("Total Buffer Power\t=\t" + totalBufferPower + "\n");
				outputFileWriter.write("Total NUCA Dynamic Power\t=\t" 
								+ (totalNucaBankPower
								+  totalRouterPower
								+ totalBufferPower) + "\n");
				*/
			}
			
			if(SystemConfig.memControllerToUse==true){							
				outputFileWriter.write("\n[RAM statistics]\n\n");

				MainMemoryDRAMController.printMemoryStats(outputFileWriter,Core.getMaxCoreCycles(),cores[0].getFrequency());
				
			}
			outputFileWriter.write("\n\n");
			
			for(Coherence coherence : ArchitecturalComponent.coherences) {
				coherence.printStatistics(outputFileWriter);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	static Hashtable<String, Vector<Cache>> consolidatedCacheList = 
			new Hashtable<String, Vector<Cache>>();
	
	private static void addToConsolidatedCacheList(Cache c) {
		Vector<Cache> cacheList = consolidatedCacheList.get(c.cacheConfig.cacheName);
		if(cacheList==null) {
			cacheList = new Vector<Cache>();
			cacheList.add(c);
			consolidatedCacheList.put(c.cacheConfig.cacheName, cacheList);
		}  else {
			if(cacheList.contains(c)==true) {
				//misc.Error.showErrorAndExit("This cache has already been added to cache list " + c + " " + cacheList.toString());
			} else {
				cacheList.add(c);
			}
		}
	}

	static void printInsWorkingSetStats() throws IOException {
		long insMaxWorkingSet = Long.MIN_VALUE; 
		long insMinWorkingSet = Long.MAX_VALUE;
		long insTotalWorkingSet = 0, insNumWorkingSetNoted = 0;
		long insWorkingSetHits = 0, insWorkingSetMisses = 0;
		
		outputFileWriter.write("\n\nPer Core Ins Working Set Stats : \n");
		for(int i=0; i<SystemConfig.NoOfCores; i++) {
			if(numInsWorkingSetHits[i]==0 && numInsWorkingSetMisses[i]==0) {
				continue;
			} else {
				// Min, Avg, Max working set of the core
				// Hitrate for the working set
				outputFileWriter.write("\nMinInsWorkingSet[" + i + "]\t=\t" + 
						minInsWorkingSetSize[i]);
				
				if(minInsWorkingSetSize[i]<insMinWorkingSet) {
					insMinWorkingSet = minInsWorkingSetSize[i];
				}
				
				outputFileWriter.write("\nAvgInsWorkingSet[" + i + "]\t=\t" + 
						(float)totalInsWorkingSetSize[i]/(float)numInsWorkingSetsNoted[i]);
				
				insTotalWorkingSet += totalInsWorkingSetSize[i];
				insNumWorkingSetNoted += numInsWorkingSetsNoted[i];
				
				outputFileWriter.write("\nMaxInsWorkingSet[" + i + "]\t=\t" + 
						maxInsWorkingSetSize[i]);
				
				if(maxInsWorkingSetSize[i]>insMaxWorkingSet) {
					insMaxWorkingSet = maxInsWorkingSetSize[i];
				}
				
				outputFileWriter.write("\nInsWorkingSetHitrate[" + i + "]\t=\t" + 
						(float)numInsWorkingSetHits[i]/(float)(numInsWorkingSetHits[i] + numInsWorkingSetMisses[i]));
				
				insWorkingSetHits += numInsWorkingSetHits[i];
				insWorkingSetMisses += numInsWorkingSetMisses[i];
			}
			outputFileWriter.write("\n");
		}
		
		outputFileWriter.write("\n\nTotal Ins Working Set Stats : \n");
		outputFileWriter.write("\nMinInsWorkingSet\t=\t" + insMinWorkingSet);
		outputFileWriter.write("\nAvgInsWorkingSet\t=\t" + formatDouble((float)insTotalWorkingSet/(float)insNumWorkingSetNoted));
		outputFileWriter.write("\nMaxInsWorkingSet\t=\t" + insMaxWorkingSet);
		
		float hitrate = (float)insWorkingSetHits/(float)(insWorkingSetHits+insWorkingSetMisses);
		outputFileWriter.write("\nInsWorkingSetHitrate\t=\t" + formatDouble(hitrate));
		
		outputFileWriter.write("\n\n");
	}
	
	
	static void printDataWorkingSetStats() throws IOException {
		long dataMaxWorkingSet = Long.MIN_VALUE; 
		long dataMinWorkingSet = Long.MAX_VALUE;
		long dataTotalWorkingSet = 0, dataNumWorkingSetNoted = 0;
		long dataWorkingSetHits = 0, dataWorkingSetMisses = 0;
		
		outputFileWriter.write("\n\nPer Core Data Working Set Stats : \n");
		for(int i=0; i<SystemConfig.NoOfCores; i++) {
			if(numDataWorkingSetHits[i]==0 && numDataWorkingSetMisses[i]==0) {
				continue;
			} else {
				// Min, Avg, Max working set of the core
				// Hitrate for the working set
				outputFileWriter.write("\nMinDataWorkingSet[" + i + "]\t=\t" + 
						minDataWorkingSetSize[i]);
				
				if(minDataWorkingSetSize[i]<dataMinWorkingSet) {
					dataMinWorkingSet = minDataWorkingSetSize[i];
				}
				
				outputFileWriter.write("\nAvgDataWorkingSet[" + i + "]\t=\t" + 
						formatDouble((float)totalDataWorkingSetSize[i]/(float)numDataWorkingSetsNoted[i]));
				
				dataTotalWorkingSet += totalDataWorkingSetSize[i];
				dataNumWorkingSetNoted += numDataWorkingSetsNoted[i];
				
				outputFileWriter.write("\nMaxDataWorkingSet[" + i + "]\t=\t" + 
						maxDataWorkingSetSize[i]);
				
				if(maxDataWorkingSetSize[i]>dataMaxWorkingSet) {
					dataMaxWorkingSet = maxDataWorkingSetSize[i];
				}
				
				outputFileWriter.write("\nDataWorkingSetHitrate[" + i + "]\t=\t" + 
						formatDouble((float)numDataWorkingSetHits[i]/(float)(numDataWorkingSetHits[i] + numDataWorkingSetMisses[i])));
				
				dataWorkingSetHits += numDataWorkingSetHits[i];
				dataWorkingSetMisses += numDataWorkingSetMisses[i];
			}
			outputFileWriter.write("\n");
		}
		
		outputFileWriter.write("\n\nTotal Data Working Set Stats : \n");
		outputFileWriter.write("\nMinDataWorkingSet\t=\t" + dataMinWorkingSet);
		outputFileWriter.write("\nAvgDataWorkingSet\t=\t" + (float)dataTotalWorkingSet/(float)dataNumWorkingSetNoted);
		outputFileWriter.write("\nMaxDataWorkingSet\t=\t" + dataMaxWorkingSet);
		
		float hitrate = (float)dataWorkingSetHits/(float)(dataWorkingSetHits+dataWorkingSetMisses);
		outputFileWriter.write("\nDataWorkingSetHitrate\t=\t" + formatDouble(hitrate));
		
		outputFileWriter.write("\n\n");
	}
	
	private static long simulationTime;

	public static void printSimulationTime()
	{
		//print time taken by simulator
		long seconds = simulationTime/1000;
		long minutes = seconds/60;
		seconds = seconds%60;
		try
		{
			outputFileWriter.write("\n");
			outputFileWriter.write("[Simulator Time]\n");
			
			outputFileWriter.write("Time Taken\t\t=\t" + minutes + " : " + seconds + " minutes\n");
			
			outputFileWriter.write("Instructions per Second\t=\t" + 
					formatDouble((double)TranslatorStatistics.getTotalNumMicroOps()/simulationTime) + " KIPS\t\tin terms of micro-ops\n");
			outputFileWriter.write("Instructions per Second\t=\t" + 
					formatDouble((double)TranslatorStatistics.getTotalHandledCISCInsn()/simulationTime) + " KIPS\t\tin terms of CISC instructions\n");
			
			outputFileWriter.write("\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}




	public static void initStatistics()
	{		
		TranslatorStatistics.initTranslatorStatistics();
		
		if(SimulationConfig.collectInsnWorkingSetInfo==true) {
			numInsWorkingSetHits = new long[SystemConfig.NoOfCores];
			numInsWorkingSetMisses = new long[SystemConfig.NoOfCores];
			maxInsWorkingSetSize = new long[SystemConfig.NoOfCores];
			minInsWorkingSetSize = new long[SystemConfig.NoOfCores];
			totalInsWorkingSetSize = new long[SystemConfig.NoOfCores];
			numInsWorkingSetsNoted = new long[SystemConfig.NoOfCores];
		}
		
		if(SimulationConfig.collectDataWorkingSetInfo==true) {
			numDataWorkingSetHits = new long[SystemConfig.NoOfCores];
			numDataWorkingSetMisses = new long[SystemConfig.NoOfCores];
			maxDataWorkingSetSize = new long[SystemConfig.NoOfCores];
			minDataWorkingSetSize = new long[SystemConfig.NoOfCores];
			totalDataWorkingSetSize = new long[SystemConfig.NoOfCores];
			numDataWorkingSetsNoted = new long[SystemConfig.NoOfCores];
		}
		
		if(SimulationConfig.pinpointsSimulation == true)
		{		
			
		}
		
	}	

	public static void openStream()
	{
		if(SimulationConfig.outputFileName == null)
		{
			SimulationConfig.outputFileName = "default";
		}
		
		try {
			File outputFile = new File(SimulationConfig.outputFileName);
			
			if(outputFile.exists()) {
				
				// rename the previous output file
				Date lastModifiedDate = new Date(outputFile.lastModified());
				File backupFile = new File(SimulationConfig.outputFileName + "_" + lastModifiedDate.toString());
				if(!outputFile.renameTo(backupFile)) {
					System.err.println("error in creating a backup of your previous output file !!\n");
				}
				
				// again point to the new file
				outputFile = new File(SimulationConfig.outputFileName);
			}
			
			outputFileWriter = new FileWriter(outputFile);
			
			
		} catch (IOException e) {
			
			StringBuilder sb = new StringBuilder();
			sb.append("DEFAULT_");
		    Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
			sb.append(sdf.format(cal.getTime()));
			try
			{
				outputFileWriter = new FileWriter(sb.toString());
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
			System.out.println("unable to create specified output file");
			System.out.println("statistics written to " + sb.toString());
		}
	}
	
	public static void closeStream()
	{
		try {
			outputFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void setStaticCoverage(double staticCoverage) {
		//Statistics.staticCoverage = staticCoverage;
		TranslatorStatistics.setStaticCoverage(staticCoverage);
	}
	
	// Ins working set
	public static void setMaxInsWorkingSetSize(long workingSetSize, int core) {
		Statistics.maxInsWorkingSetSize[core] = workingSetSize;
	}
	
	public static void setMinInsWorkingSetSize(long workingSetSize, int core) {
		Statistics.minInsWorkingSetSize[core] = workingSetSize;
	}
	
	public static void setTotalInsWorkingSetSize(long workingSetSize, int core) {
		Statistics.totalInsWorkingSetSize[core] = workingSetSize;
	}
	
	public static void setNumInsWorkingSetNoted(long workingSetNoted, int core) {
		Statistics.numInsWorkingSetsNoted[core] = workingSetNoted;
	}
	
	public static void setNumInsWorkingSetHits(long workingSetHits, int core) {
		Statistics.numInsWorkingSetHits[core] = workingSetHits;
	}
	
	public static void setNumInsWorkingSetMisses(long workingSetMisses, int core) {
		Statistics.numInsWorkingSetMisses[core] = workingSetMisses;
	}
	
	
	// Data working set
	public static void setMaxDataWorkingSetSize(long workingSetSize, int core) {
		Statistics.maxDataWorkingSetSize[core] = workingSetSize;
	}
	
	public static void setMinDataWorkingSetSize(long workingSetSize, int core) {
		Statistics.minDataWorkingSetSize[core] = workingSetSize;
	}
	
	public static void setTotalDataWorkingSetSize(long workingSetSize, int core) {
		Statistics.totalDataWorkingSetSize[core] = workingSetSize;
	}
	
	public static void setNumDataWorkingSetNoted(long workingSetNoted, int core) {
		Statistics.numDataWorkingSetsNoted[core] = workingSetNoted;
	}
	
	public static void setNumDataWorkingSetHits(long workingSetHits, int core) {
		Statistics.numDataWorkingSetHits[core] = workingSetHits;
	}
	
	public static void setNumDataWorkingSetMisses(long workingSetMisses, int core) {
		Statistics.numDataWorkingSetMisses[core] = workingSetMisses;
	}
	
	public static void setSimulationTime(long simulationTime) {
		Statistics.simulationTime = simulationTime;
	}

	public static void setExecutable(String executableFile) {
		Statistics.benchmark = executableFile;
	}

	public static void resetAllStatistics() {
		Main.setStartTime();
		cores = ArchitecturalComponent.getCores();
		coreMemSys = ArchitecturalComponent.getCoreMemSysArray();
	
		TranslatorStatistics.resetTranslatorStatistics();
		Core.resetTimingStatistics();
		TLB.resetTLBValues();
		MainMemoryDRAMController.resetMemoryStats();
		for(int i=0;i<SystemConfig.NoOfCores;i++){
			for(Cache cache : coreMemSys[i].getCoreCacheList()){
				cache.resetCacheStats();
			}
		}
		for (Cache c : ArchitecturalComponent.getSharedCacheList()) {
			c.resetCacheStats();
		}

		Statistics.resetnuca();
		Statistics.resetEnergyStatistics();
		
	}
		
	public static void printAllStatistics(String benchmarkName, 
			long startTime, long endTime) {
		//set up statistics module
		cores = ArchitecturalComponent.getCores();
		coreMemSys = ArchitecturalComponent.getCoreMemSysArray();

		Statistics.setExecutable(benchmarkName);
		
		//TODO : NUCA stats not being printed !!
		//printNucaStats();
			
		Statistics.setSimulationTime(endTime - startTime);
		
		//print statistics
		Statistics.openStream();
		Statistics.printSystemConfig();
		TranslatorStatistics.printTranslatorStatistics(outputFileWriter);
		Statistics.printTimingStatistics();
		Statistics.printMemorySystemStatistics();
		
		try {
			if(SimulationConfig.collectInsnWorkingSetInfo) {
				Statistics.printInsWorkingSetStats();
			}
			
			if(SimulationConfig.collectDataWorkingSetInfo) {
				Statistics.printDataWorkingSetStats();
			}
		} catch (IOException e) {
			
		}
		
		Statistics.printSimulationTime();
		Statistics.printEnergyStatistics();
		
		// Qemu translation cache stats
		if(TranslatedInstructionCache.getHitRate()!=-1) {
			try {
				outputFileWriter.write("[Qemu translation cache]\n");
				outputFileWriter.write("Hit-rate = " + (TranslatedInstructionCache.getHitRate() * 100 ) + " %");
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
		Statistics.closeStream();
	}
	
	private static void printNucaStats() {
		for(Cache cache : ArchitecturalComponent.getCacheList())
		{
			if(cache.getClass()!=Cache.class)
			{
//				if (((NucaCache)cache).nucaType != NucaType.NONE )
//				{
//					averageHopLength = ((NucaCache)cache).getAverageHoplength(); 
//					maxHopLength = ((NucaCache)cache).getMaxHopLength();
//					minHopLength = ((NucaCache)cache).getMinHopLength();
//					Statistics.nocTopology = ((NocInterface)((NucaCache)cache).cacheBank.get(0).comInterface).getRouter().topology.name();
//					Statistics.nocRoutingAlgo = ((NocInterface)((NucaCache)cache).cacheBank.get(0).comInterface).getRouter().rAlgo.name();
//					for(int i=0;i< ((NucaCache)cache).cacheRows;i++)
//					{
//						Statistics.hopcount += ((NocInterface)((NucaCache)cache).cacheBank.get(i).comInterface).getRouter().hopCounters; 
//					}
//					if(Statistics.nocTopology.equals("FATTREE") ||
//							Statistics.nocTopology.equals("OMEGA") ||
//							Statistics.nocTopology.equals("BUTTERFLY")) {
//						for(int k = 0 ; k<((NucaCache)cache).noc.intermediateSwitch.size();k++){
//							Statistics.hopcount += ((NucaCache)cache).noc.intermediateSwitch.get(k).hopCounters;
//						}
//					}
//				}
//				Statistics.totalNucaBankAccesses = ((NucaCache)cache).getTotalNucaBankAcesses();
			}
			
		}
		
	}
	
	public static void resetnuca(){
		for(String name : ArchitecturalComponent.nucaList.keySet())
		{
			NucaCache nuca = ArchitecturalComponent.nucaList.get(name);
			nuca.migrations=0;
			nuca.hopCount=0;
		}
	}

	/*public static long getNumCISCInsn(int javaTid, int tidEmu) {
		return numCISCInsn[javaTid][tidEmu];
	}*/

	public static String formatInt(long i)
	{
		return String.format("%15d", i);
	}
	
	public static String formatFloat(float f)
	{
		return String.format("%15.4f", f);
	}
	
	public static String formatDouble(double d)
	{
		return String.format("%15.4f", d);
	}
	
}
