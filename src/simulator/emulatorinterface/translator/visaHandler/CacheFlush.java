package emulatorinterface.translator.visaHandler;

import emulatorinterface.DynamicInstructionBuffer;
import generic.Instruction;
import generic.InstructionTable;

public class CacheFlush implements DynamicInstructionHandler 
{
	public int handle(int microOpIndex, 
			Instruction microOp, DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		
			//System.out.println(microOp.toString());
			return ++microOpIndex;
	}
}
