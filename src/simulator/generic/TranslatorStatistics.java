package generic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;



import main.ArchitecturalComponent;
import main.Main;


import config.CoreConfig;
import config.EmulatorConfig;
import config.SystemConfig;
import config.SimulationConfig;
import emulatorinterface.communication.IpcBase;
import emulatorinterface.RunnableThread;

public class TranslatorStatistics {
	
	//private static CoreMemorySystem coreMemSys[];
	private static Core cores[];
	
	//Translator Statistics
	
	static long dataRead[];
	static long numHandledCISCInsn[][];
	static long numCISCInsn[][];
	static long noOfMicroOps[][];
	static double staticCoverage;
	static double dynamicCoverage;
	
	
	/*This class prints the Translator Statistics*/
	
	static long totalNumMicroOps = 0;
	static long totalHandledCISCInsn = 0;
	static long totalPINCISCInsn = 0;
	
	public static void resetTranslatorStatistics()
	{
		totalNumMicroOps = 0;
		totalHandledCISCInsn = 0;
		totalPINCISCInsn = 0;
		for(int i=0;i<dataRead.length;i++)
			dataRead[i]=0;
		for(int i=0;i<numHandledCISCInsn.length;i++)
			for (int j=0;j<numHandledCISCInsn[i].length;j++)
				numHandledCISCInsn[i][j]=0;
		for(int i=0;i<numCISCInsn.length;i++)
			for(int j=0;j<numCISCInsn[i].length;j++)
				numCISCInsn[i][j]=0;

		for(int i=0;i<noOfMicroOps.length;i++)
		{
			for(int j=0;j<noOfMicroOps[i].length;j++)
				noOfMicroOps[i][j]=0;
		}
		//staticCoverage=0;
		dynamicCoverage=0;
		for(int i=0;i<SystemConfig.maxNumJavaThreads;i++){
			// RunnableThread.noOfMicroOps[i]=0;
			for(int j = 0; j < Main.runners[i].EMUTHREADS; j++) {
				Main.runners[i].emulatorThreadState[j].totalRead=0;
				Main.runners[i].noOfMicroOps[j] = 0;
			}
		}	
	}
	
	public static long getTotalNumMicroOps()
	{
		return totalNumMicroOps;
	}
	public static long getTotalHandledCISCInsn()
	{
		return totalHandledCISCInsn;
	}
	public static long getTotalPINCISCInsn()
	{
		return totalPINCISCInsn;
	}
	
	public static void initTranslatorStatistics()
	{
		dataRead = new long[SystemConfig.maxNumJavaThreads];
		numHandledCISCInsn = new long[SystemConfig.maxNumJavaThreads][SystemConfig.numEmuThreadsPerJavaThread];
		numCISCInsn = new long[SystemConfig.maxNumJavaThreads][SystemConfig.numEmuThreadsPerJavaThread];
		noOfMicroOps = new long[SystemConfig.maxNumJavaThreads][SystemConfig.numEmuThreadsPerJavaThread];
	}
	
	
	public static void printTranslatorStatistics(FileWriter fw)
	{

		for (int i=0; i<SystemConfig.maxNumJavaThreads; i++) {
			long dataRead = 0;
			for (int j = 0; j < Main.runners[i].EMUTHREADS; j++) {
				dataRead += Main.runners[i].emulatorThreadState[j].totalRead;
				noOfMicroOps[i][j] = Main.runners[i].noOfMicroOps[j];
			}
			setDataRead(dataRead, i);
			// setNoOfMicroOps(Main.runners[i].noOfMicroOps, i);
		}	

		cores = ArchitecturalComponent.getCores();
		totalNumMicroOps = 0;
		totalHandledCISCInsn = 0;
		totalPINCISCInsn = 0;

		for(int i = 0; i < SystemConfig.maxNumJavaThreads; i++)
		{
			for (int j=0; j<IpcBase.getEmuThreadsPerJavaThread(); j++) {
				if(SimulationConfig.pinpointsSimulation == false)
				{
					totalNumMicroOps += noOfMicroOps[i][j];
				}
//				totalNumMicroOps += numCoreInstructions[i];
				totalHandledCISCInsn += numHandledCISCInsn[i][j];
				totalPINCISCInsn += numCISCInsn[i][j];
			}
		}
		
		dynamicCoverage = ((double)totalHandledCISCInsn/(double)totalPINCISCInsn)*(double)100.0;
		
		if(SimulationConfig.pinpointsSimulation == true)
		{
			for(int i = 0; i < SystemConfig.NoOfCores; i++)
			{
				totalNumMicroOps += cores[i].getNoOfInstructionsExecuted();
			}
			totalHandledCISCInsn = 3000000;
		}

	

		
		//for each java thread, print number of instructions provided by PIN and number of instructions forwarded to the pipeline
		try
		{
			fw.write("\n");
			fw.write("[Translator Statistics]\n");
			fw.write("\n");
			
			for(int i = 0; i < SystemConfig.maxNumJavaThreads; i++)
			{
				fw.write("Java thread\t=\t" + i + "\n");
				fw.write("Data Read\t=\t" + dataRead[i] + " bytes\n");
//				fw.write("Number of instructions provided by emulator\t=\t" + numHandledCISCInsn[i] + "\n");
//				fw.write("Number of Micro-Ops\t=\t" + noOfMicroOps[i] + " \n");
//				fw.write("MicroOps/CISC = " + 
//						((double)(numInstructions[i]))/((double)(noOfMicroOps[i])) + "\n");
//				fw.write("\n");
			}
			fw.write("Number of micro-ops\t\t=\t" + totalNumMicroOps + "\n");
			fw.write("Number of handled CISC instructions\t=\t" + totalHandledCISCInsn + "\n");
			fw.write("Number of PIN CISC instructions\t=\t" + totalPINCISCInsn + "\n");
			
			fw.write("Static coverage\t\t=\t" + formatDouble(staticCoverage) + " %\n");
			fw.write("Dynamic Coverage\t=\t" + formatDouble(dynamicCoverage) + " %\n");
			fw.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	public static void setDataRead(long dataRead, int thread) {
		TranslatorStatistics.dataRead[thread] = dataRead;
	}
	
	public static long getNumHandledCISCInsn(int javaThread, int emuThread) {
		return TranslatorStatistics.numHandledCISCInsn[javaThread][emuThread];
	}
	
	public static void setNumHandledCISCInsn(long numInstructions, int javaThread, int emuThread) {
		TranslatorStatistics.numHandledCISCInsn[javaThread][emuThread] = numInstructions;
		PinPointsProcessing.toProcessEndOfSlice(numHandledCISCInsn[javaThread][emuThread]);
	}
	
	public static void setNumCISCInsn(long numInstructions, int javaThread, int emuThread) {
		TranslatorStatistics.numCISCInsn[javaThread][emuThread] = numInstructions;
	}
	
	public static long getNumCISCInsn(int javaTid, int tidEmu) {
		return numCISCInsn[javaTid][tidEmu];
	}

	public static void setNoOfMicroOps(long noOfMicroOps[], int thread) {
		TranslatorStatistics.noOfMicroOps[thread] = noOfMicroOps;
	}
	
	public static void setStaticCoverage(double staticCoverage) {
		TranslatorStatistics.staticCoverage = staticCoverage;
	}
	public static String formatFloat(float f)
	{
		return String.format("%.4f", f);
	}
	
	public static String formatDouble(double d)
	{
		return String.format("%.4f", d);
	}
	
	
}
