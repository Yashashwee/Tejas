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

	Contributors:   Sunil Kumar :: Shraddha Gupta
*****************************************************************************/

package emulatorinterface.translator.RiscV.instruction;


import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.RiscV.registers.Registers;
import emulatorinterface.translator.RiscV.registers.TempRegisterNum;
import generic.Instruction;
import generic.Operand;
import generic.OperandType;
import generic.InstructionList;

public class FloatingPointLoad implements RiscVStaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
	{
		//the check for floating point register does not returns true if we replace this statement by true each instruction is handled properly
		if((operand1!=null && operand1.getOperandType()==OperandType.floatRegister) && 
				(operand2.isMemoryOperand()) && 
				operand3==null)
		{
			// Push operation at present requires just a single move operation
			//may be some more work needs to be done
			if(operand2.getMemoryLocationSecondOperand()==null)
			{
				//If b is invalid operand, operand1 = a
				instructionArrayList.appendInstruction(Instruction.getMoveInstruction(operand1, 
						operand2.getMemoryLocationFirstOperand()));
			}
			else
			{
				//operand1=a+b
				instructionArrayList.appendInstruction(Instruction.getIntALUInstruction(operand2.getMemoryLocationFirstOperand(),
						operand2.getMemoryLocationSecondOperand(), operand1));
			}
		}
		
		else
		{
			misc.Error.invalidOperation("Floating Point Load", operand1, operand2, operand3);
		}
	}
}