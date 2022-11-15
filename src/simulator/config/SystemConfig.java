/*****************************************************************************
				Tejas Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
------------------------------------------------------------------------------------------------------------

	Contributors:  Moksh Upadhyay
*****************************************************************************/
package config;

import java.util.Hashtable;
import java.util.Vector;
import java.io.FileWriter;
import java.io.IOException;

import generic.PortType;

public class SystemConfig 
{
	public static enum Interconnect {
		Bus, Noc
	}

	public static  int NUM_BENCHMARKS;
	
	public static int NoOfCores;
	
	
	public static  String benchmarknames[]; // Number of benchmarks
	public static String numCountThreads[];
	
	public static int maxNumJavaThreads;
	public static int numEmuThreadsPerJavaThread;	
	
	public static CoreConfig[] core; 
	public static Vector<CacheConfig> sharedCacheConfigs=new Vector<CacheConfig>();	

	//added later kush
	public static boolean memControllerToUse;

	public static int mainMemoryLatency;
	public static long mainMemoryFrequency;
	public static PortType mainMemPortType;
	public static int mainMemoryAccessPorts;
	public static int mainMemoryPortOccupancy;
	public static int cacheBusLatency;
	public static String coherenceEnforcingCache;
	public static BusConfig busConfig;
	public static NocConfig nocConfig;
	public static EnergyConfig busEnergy;
	
	public static Interconnect interconnect;
	public static EnergyConfig  mainMemoryControllerPower;
	public static EnergyConfig  globalClockPower;
	
	//FIXME
	//TODO
	//have to do it here since the object is not being created in xml parser yet
	public static MainMemoryConfig mainMemoryConfig; 
	
	public static void printMainMemoryConfig(FileWriter fw)
	{
		try
		{
		fw.write("\n[Main Memory Configuration]\n");
		fw.write("RAM frequency:" + (1/(SystemConfig.mainMemoryConfig.tCK)*1000) + " MHz\n");
		fw.write("Num Channels: " + SystemConfig.mainMemoryConfig.numChans + "\n");
		fw.write("Num Ranks: " + SystemConfig.mainMemoryConfig.numRanks + "\n");
		fw.write("Num Banks: " + SystemConfig.mainMemoryConfig.numBanks + "\n");
		fw.write("Row Buffer Policy: " + SystemConfig.mainMemoryConfig.rowBufferPolicy + "\n");
		fw.write("Scheduling Policy: " + SystemConfig.mainMemoryConfig.schedulingPolicy + "\n");
		fw.write("Queuing Structure: " + SystemConfig.mainMemoryConfig.queuingStructure + "\n");
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
}
