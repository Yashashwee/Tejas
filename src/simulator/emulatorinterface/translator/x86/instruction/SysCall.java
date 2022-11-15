package emulatorinterface.translator.x86.instruction;

import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.x86.operand.OperandTranslator;
import emulatorinterface.translator.x86.registers.Registers;
import emulatorinterface.translator.x86.registers.TempRegisterNum;
import generic.Instruction;
import generic.Operand;
import generic.InstructionList;
import main.ArchitecturalComponent;
import memorysystem.Cache;
import memorysystem.MESI;
import emulatorinterface.translator.x86.operand.OperandTranslator;
public class SysCall implements X86StaticInstructionHandler {
    public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
                    {
                        if(operand1==null && operand2==null && operand3==null)
                        {
                            //System.out.println("encountered sys call ");
                            instructionArrayList.appendInstruction(Instruction.getSyscallInstruction());
                            
                        }
                        else
                            misc.Error.invalidOperation("Clflush", operand1, operand2, operand3);
                    }
}
