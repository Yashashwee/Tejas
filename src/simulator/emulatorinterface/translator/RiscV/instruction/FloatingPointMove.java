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
import generic.InstructionList;
import generic.OperandType;

public class FloatingPointMove implements RiscVStaticInstructionHandler 
{
	public void handle(long instructionPointer, 
			Operand operand1, Operand operand2, Operand operand3,
			InstructionList instructionArrayList,
			TempRegisterNum tempRegisterNum) 
					throws InvalidInstructionException
	{
		if((operand1!=null && operand1.getOperandType()==OperandType.floatRegister) && 
			(operand2!=null && operand2.getOperandType()==OperandType.floatRegister ) && 
			operand3==null)
		{
			instructionArrayList.appendInstruction(Instruction.getMoveInstruction(operand1, operand2));
		}
		
		else if((operand1!=null && operand1.getOperandType()==OperandType.floatRegister) && 
			(operand2!=null && operand2.getOperandType()==OperandType.memory ) && 
			operand3==null)
		{
			// Load from memory to integer register directly
			instructionArrayList.appendInstruction(Instruction.getLoadInstruction(operand2, operand1));
		}
		
		
		else
		{
			misc.Error.invalidOperation("Convert Integer to Floating point", operand1, operand2, operand3);
		}
	}
}