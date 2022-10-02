package emulatorinterface.translator.visaHandler;

import emulatorinterface.DynamicInstructionBuffer;
import generic.Instruction;
import generic.InstructionTable;

public class SysCall implements DynamicInstructionHandler 
{
	public int handle(int microOpIndex, 
			Instruction microOp, DynamicInstructionBuffer dynamicInstructionBuffer) 
	{
		//nothing to be done in such cases
		//System.out.println("Syscall visa ");
		return ++microOpIndex;
	}
}
