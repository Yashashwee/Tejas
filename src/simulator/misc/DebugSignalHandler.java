package misc;

import sun.misc.Signal;
import sun.misc.SignalHandler;
import java.lang.reflect.*;

import generic.Statistics;
import generic.TranslatorStatistics;
import main.Main;
import config.SystemConfig;


public class DebugSignalHandler implements SignalHandler
{
   
   public static void listenTo(String name) {
      Signal signal = new Signal(name);
      Signal.handle(signal, new DebugSignalHandler());
   }
 
   public static long endTime;
   public void handle(Signal signal) {
      endTime = System.currentTimeMillis();
      System.out.println("Printing Intermediate Statistics");
      // for (int i=0; i<SystemConfig.maxNumJavaThreads; i++) {
      //    long dataRead = 0;
      //    for (int j = 0; j < Main.runners[i].EMUTHREADS; j++) {
      //       dataRead += Main.runners[i].emulatorThreadState[j].totalRead;
      //    }
      //    TranslatorStatistics.setDataRead(dataRead, i);
      //    TranslatorStatistics.setNoOfMicroOps(Main.runners[i].noOfMicroOps, i);
      // }

      Statistics.printAllStatistics(Main.getEmulatorFile(), Main.startTime, endTime);
   }
}