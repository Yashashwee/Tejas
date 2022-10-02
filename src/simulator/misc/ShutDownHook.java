package misc;

import generic.Statistics;
import generic.TranslatorStatistics;
import main.Main;
import config.SystemConfig;

public class ShutDownHook extends Thread {

  	public static long endTime;
	@Override
	public void run()
	{
		// 
		// if(Main.printStatisticsOnAsynchronousTermination == false) {
		// 	// There is no need to write the statistics when tejas is used as a front end for collecting traces
		// 	Runtime.getRuntime().halt(0);
		// }
		//Thread.currentThread().dumpStack();
		try {
			Main.getEmulator().forceKill();
		} finally {
			

			if(Main.statFileWritten == false)
			{
				System.out.println("shut down");
				// for (int i=0; i<SystemConfig.maxNumJavaThreads; i++) {
				//  long dataRead = 0;
				//  for (int j = 0; j < Main.runners[i].EMUTHREADS; j++) {
				//     dataRead += Main.runners[i].emulatorThreadState[j].totalRead;
				//  }
				//  TranslatorStatistics.setDataRead(dataRead, i);
				//  TranslatorStatistics.setNoOfMicroOps(Main.runners[i].noOfMicroOps, i);
				// }
				endTime = System.currentTimeMillis();
				Statistics.printAllStatistics(Main.getEmulatorFile(), Main.startTime, endTime);
				// Statistics.printAllStatistics(Main.getEmulatorFile(), -1, -1);
			}
			//Runtime.getRuntime().runFinalization();
			Runtime.getRuntime().halt(0);
		}
	}
}
