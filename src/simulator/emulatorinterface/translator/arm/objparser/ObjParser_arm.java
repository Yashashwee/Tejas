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

	Contributors:  Ismi Abidi
 *****************************************************************************/

package emulatorinterface.translator.arm.objparser;

import emulatorinterface.DynamicInstructionBuffer;
import emulatorinterface.EmulatorPacketList;
import emulatorinterface.communication.Encoding;
import emulatorinterface.communication.Packet;
import emulatorinterface.translator.InvalidInstructionException;
import emulatorinterface.translator.qemuTranslationCache.TranslatedInstructionCache;
import emulatorinterface.translator.visaHandler.DynamicInstructionHandler;
import emulatorinterface.translator.visaHandler.VisaHandlerSelector;
import emulatorinterface.translator.arm.instruction.InstructionClass;
import emulatorinterface.translator.arm.instruction.InstructionClassTable;
import emulatorinterface.translator.arm.instruction.StaticInstructionHandler;
import emulatorinterface.translator.arm.operand.OperandTranslator;
import emulatorinterface.translator.arm.registers.Registers;
import emulatorinterface.translator.arm.registers.TempRegisterNum;
import generic.GenericCircularQueue;
import generic.Instruction;
import generic.InstructionList;
import generic.InstructionTable;
import generic.Operand;
import generic.OperationType;
import generic.Statistics;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import config.EmulatorConfig;
import config.EmulatorType;
import main.CustomObjectPool;
import main.Main;
import misc.Error;
import misc.Numbers;

/**
 * Objparser class contains methods to parse a static executable file and to
 * determine the information of dynamic instructions. The x86 assembly code
 * generated by objdump utility is first parsed to obtain operation, source
 * operands and destination operand. The x86 CISC operations are broken down
 * into corresponding simpler micro-operations which follow load-store
 * architecture. We store the linear address of the instruction and the
 * corresponding micro-operations in a hash-table for faster access to
 * instruction information later on..
 * 
 * @author Ismi
 */
public class ObjParser_arm 
{
	//private static InstructionTable ciscIPtoRiscIP = null;
	private static InstructionList staticMicroOpList = null;
	private static InstructionList threadMicroOpsList[] = null;
	
	public static void initializeThreadMicroOpsList(int maxApplicationThreads) {
		threadMicroOpsList = new InstructionList[maxApplicationThreads];
		
		for(int i=0; i<maxApplicationThreads; i++) {
			threadMicroOpsList[i] = new InstructionList(10000);
		}
	}
	
	private static DynamicInstructionBuffer[] staticDynamicInstructionBuffers;
	public static void initializeDynamicInstructionBuffer(int maxApplicationThreads) {
		staticDynamicInstructionBuffers = new DynamicInstructionBuffer[maxApplicationThreads];
		
		for(int i=0; i<maxApplicationThreads; i++) {
			staticDynamicInstructionBuffers[i] = new DynamicInstructionBuffer();
		}
	}
	
	private static Instruction staticLoadMicroOp, staticStoreMicroOp, staticBranchMicroOp;
	public static void initializeControlMicroOps() {
		// Load from immediate memory location to a MSR(load_reg)
		Operand loadLocation = Operand.getMemoryOperand(Operand.getImmediateOperand(), null);
		Operand loadRegister = Operand.getIntegerRegister(Registers.encodeRegister("load_reg"));
		staticLoadMicroOp = Instruction.getLoadInstruction(loadLocation, loadRegister);
		
		// Store from MSR(load_reg) to immediate memory location 
		Operand storeLocation = Operand.getMemoryOperand(Operand.getImmediateOperand(), null);
		Operand storeRegister = Operand.getIntegerRegister(Registers.encodeRegister("store_reg"));
		staticStoreMicroOp = Instruction.getStoreInstruction(storeLocation, storeRegister);
		
		// Branch address
		Operand branchAddress = Operand.getImmediateOperand();
		staticBranchMicroOp = Instruction.getBranchInstruction(branchAddress);
	}
	
	

	private static int riscifyInstruction(
			long instructionPointer, String instructionPrefix, String operation, 
			String operand1Str, String operand2Str, String operand3Str, 
			InstructionList instructionList) 
	{
//		if(instructionPointer==4363909) {
//			System.err.println("ip=" + instructionPointer + "\tprefix=" + instructionPrefix + 
//					"\top=" + operation + "\top1=" + operand1Str + "\top2=" + operand2Str + "\top3=" + operand3Str);
//		}
		
		int microOpsIndexBefore = instructionList.length();
		Operand operand1 = null, operand2 = null, operand3 = null;

		
		// System.out.println("instructionList size before = " + microOpsIndexBefore);
		
		try
		{
			//Determine the instruction class for this instruction
			InstructionClass instructionClass;
			instructionClass = InstructionClassTable.getInstructionClass(operation);

			// Obtain a handler for this instruction
			StaticInstructionHandler handler;
			handler = InstructionClassTable.getInstructionClassHandler(instructionClass);
			//if(handler==null)System.out.println(operation+" "+operand1+" "+operand2+" "+operand3);
			// Handle the instruction
			if(handler!=null) {
				// Simplify the operands
	
				TempRegisterNum tempRegisterNum = new TempRegisterNum();
				/*
				operand1 = OperandTranslator.simplifyOperand(operand1Str, instructionList, tempRegisterNum);
				operand2 = OperandTranslator.simplifyOperand(operand2Str, instructionList, tempRegisterNum);
				operand3 = OperandTranslator.simplifyOperand(operand3Str, instructionList, tempRegisterNum);
				
				handler.handle(instructionPointer, operand1, operand2, operand3, instructionList, tempRegisterNum);
				*/
				
				
				
				if((instructionClass==InstructionClass.LOAD_BLOCK)||(instructionClass==InstructionClass.STORE_BLOCK))
				 {
					
				 	operand1 = OperandTranslator.simplifyOperand(operand1Str, instructionList, tempRegisterNum);
				 	Operand[] operandBlock=OperandTranslator.SimplifyBlockOperand(operand2Str, instructionList, tempRegisterNum);
				 	operand3=null;
				 	Operand newOperand;
				 	newOperand=operand1;
				 	//System.out.println(operation+" "+operand1+" "+operand2Str+" "+operandBlock.length);
				 	for(int i=0;i<operandBlock.length;i++)
					{
					
						Operand operand1Location = Operand.getMemoryOperand(newOperand, null);
						//if(instructionClass==instructionClass.LoadBlock)
						handler.handle(instructionPointer, operandBlock[i], operand1Location, operand3, instructionList, tempRegisterNum);
						//if((instructionClass==instructionClass.StoreBlock)
						//Store.handle(instructionPointer, operand1Location, operandBlock[i], operand3, instructionList, tempRegisterNum);
						instructionList.appendInstruction(Instruction.getIntALUInstruction(newOperand, Operand.getImmediateOperand(), newOperand));
					}
				}
				else if((instructionClass==InstructionClass.POP)||(instructionClass==InstructionClass.PUSH))
				{
					
						Operand[] operandBlock=OperandTranslator.SimplifyBlockOperand(operand1Str, instructionList, tempRegisterNum);
						operand2=null;operand3=null;
						//System.out.println(operation+" "+operand1Str+" "+operandBlock.length);
					// Create stack-pointer and [stack-pointer]
					Operand stackPointer = Registers.getStackPointer();
					for(int i=0;i<operandBlock.length;i++)
					{
						Operand stackPointerLocation = Operand.getMemoryOperand(stackPointer, null);
						//if(instructionClass==instructionClass.Pop)
						handler.handle(instructionPointer,operandBlock[i],stackPointerLocation,  operand3, instructionList, tempRegisterNum);
						//if((instructionClass==instructionClass.Push)
						//Store.handle(instructionPointer, operand1Location, operandBlock[i], operand3, instructionList, tempRegisterNum);
						instructionList.appendInstruction(Instruction.getIntALUInstruction(stackPointer, Operand.getImmediateOperand(), stackPointer));
					}
				}
				else{
					//System.out.println(operation+" "+operand1Str+" "+operand2Str+" "+operand3Str);
					operand1 = OperandTranslator.simplifyOperand(operand1Str, instructionList, tempRegisterNum);
					operand2 = OperandTranslator.simplifyOperand(operand2Str, instructionList, tempRegisterNum);
					operand3 = OperandTranslator.simplifyOperand(operand3Str, instructionList, tempRegisterNum);
				
					handler.handle(instructionPointer, operand1, operand2, operand3, instructionList, tempRegisterNum);
				}
				
				
				//now set the ip of all converted instructions to instructionPointer
				for(int i=microOpsIndexBefore; i<instructionList.length(); i++)
				{
					instructionList.setCISCProgramCounter(i, instructionPointer);
				}
			} else {
				throw new InvalidInstructionException("", false);
			}			
		} catch(Exception inInstrEx) {
			/*
			 * microOps created for this instruction are not valid 
			 * since the translation of the instruction did not 
			 * complete its execution.
			 */
			//inInstrEx.printStackTrace();
			//System.out.println(operation+" "+ operand1Str+" "+operand2Str+" "+operand3Str);//System.exit(0);
			
			while(instructionList.getListSize() != microOpsIndexBefore) {
				instructionList.removeLastInstr(operand1, operand2, operand3);
			}
		}
		
		return (instructionList.length()-microOpsIndexBefore);
	}
	
	

	
	// return index of null character for a byte array
	private static int len(byte[] asmBytes) {
		int i=0;
		for(;asmBytes[i]!=0 && i<asmBytes.length;i++);
		return i;
	}
	
	// searches character ch in asmByes. If not-found return -1, else return index of ch
	private static int indexOf(byte[] asmBytes, char ch, int offset, int len) {
		for(int i=offset; i<len(asmBytes); i++) {
			if(asmBytes[i]==ch) {
				return i;
			}
		}
		
		return -1;
	}
	
	private static String concatenateStringArray(String[] strArray) {
		String concatenatedString = new String(strArray[0] + strArray[1] + strArray[2] + strArray[3]);
		return concatenatedString;
	}
	// for  a line of assembly code, this would return the
	// linear address, operation, operand1,operand2, operand3
	private static String[] tokenizeQemuAssemblyCode(byte[] asmBytes) {
		
		String assemblyTokens[] = new String[5];
		int previousPointer, currentPointer;
		previousPointer = currentPointer = 0;
		 
		// --------------operation ---------------------------------- 
		currentPointer = indexOf(asmBytes, ' ', previousPointer, 120);
		
				
		if(currentPointer==-1) {
			assemblyTokens[0] = null;
			assemblyTokens[1] = new String(asmBytes, 0, len(asmBytes)); // only operation field is present
			assemblyTokens[2] = assemblyTokens[3] = assemblyTokens[4] = null;
			return assemblyTokens;
		}
		else {
			assemblyTokens[0] = null;
			assemblyTokens[1] = new String(asmBytes, previousPointer, (currentPointer-previousPointer));
			currentPointer++; previousPointer = currentPointer;
		}
				
		// --------------------- operand1, operand2, operand3 --------------------------------
		if(previousPointer==len(asmBytes)) {
			assemblyTokens[2] = assemblyTokens[3] = assemblyTokens[4] = null;
			return assemblyTokens;
		}
		
		currentPointer = indexOf(asmBytes, '{', previousPointer, 120);//search for push or pop reg list
		
		if(currentPointer==previousPointer) {
			assemblyTokens[3] = assemblyTokens[4] = null;
			assemblyTokens[2] = new String(asmBytes, previousPointer, len(asmBytes)-previousPointer);
			return assemblyTokens;
		} 
		else{
		      currentPointer = indexOf(asmBytes, ',', previousPointer, 120);
		      if(currentPointer==-1) {
			  assemblyTokens[3] = assemblyTokens[4] = null;
			  assemblyTokens[2] = new String(asmBytes, previousPointer, len(asmBytes)-previousPointer);
			
	              }
	              else {// Two operands
			      assemblyTokens[2] = new String(asmBytes, previousPointer, (currentPointer-previousPointer));
			      
			      currentPointer+=2; previousPointer=currentPointer;
			      //System.out.println("cur "+currentPointer+"prev "+previousPointer); //System.exit(0);
			    if(previousPointer==len(asmBytes)) {
				assemblyTokens[3] = assemblyTokens[4] = null;
			    } 
			    else {
				currentPointer = indexOf(asmBytes, '{', previousPointer, 120); //reg-list for ldm or stm instruction 
				
				if(currentPointer!=-1) {
					assemblyTokens[4] = null;
					assemblyTokens[3] = new String(asmBytes, previousPointer, len(asmBytes)-previousPointer);
					
					//return assemblyTokens;
				}
				else {
					currentPointer = indexOf(asmBytes, '[', previousPointer, 120);
					//System.out.println("cur1 "+currentPointer+"prev "+previousPointer); System.exit(0);
					if(currentPointer==previousPointer) {
					   // previousPointer=currentPointer;
					  //assemblyTokens[4] = null;
					    currentPointer = indexOf(asmBytes, ']', previousPointer, 120);
					    if(currentPointer!=-1)
					    {
						assemblyTokens[3] = new String(asmBytes, previousPointer, (currentPointer-previousPointer+1));
						//System.out.println("Token3= "+assemblyTokens[3]);
						currentPointer+=1; previousPointer=currentPointer;
					    }
					    else misc.Error.showErrorAndExit("This is not possible !!!");
					    currentPointer = indexOf(asmBytes, ',', previousPointer, 120);
					     
					    if(currentPointer!=-1)
					    {
						currentPointer+=2; previousPointer=currentPointer;
						assemblyTokens[4] = new String(asmBytes, previousPointer, len(asmBytes)-previousPointer);
						//System.out.println("Token4= "+assemblyTokens[4]);
						return assemblyTokens;
					    } 
					    else {
					    	assemblyTokens[4] =null;
					    	return assemblyTokens;
					    }
					//return assemblyTokens;
					}
					else { 
 						currentPointer = indexOf(asmBytes, ',', previousPointer, 120);
 						if(currentPointer==-1) {
						      assemblyTokens[3] = new String(asmBytes, previousPointer, len(asmBytes)-previousPointer); 
						      assemblyTokens[4] = null;
						      //System.out.println("Token3 "+assemblyTokens[3]);
						      return assemblyTokens;
						}
						assemblyTokens[3] = new String(asmBytes, previousPointer, (currentPointer-previousPointer));
						//System.out.println("Token3 "+assemblyTokens[3]);
						currentPointer+=2; previousPointer=currentPointer;
					}
					if(previousPointer==len(asmBytes)) {
					  assemblyTokens[4] = null;
					  return assemblyTokens;
					}
					
					assemblyTokens[4] = new String(asmBytes, previousPointer, len(asmBytes)-previousPointer);
					//System.out.println("Token4"+assemblyTokens[4]);
					//return assemblyTokens;
				}
			    }
		     }
		}
		/*for(int i=1;i<5;i++)
		{
		    if(assemblyTokens[i]!=null)
		    {
		    assemblyTokens[i]=assemblyTokens[i].replaceAll("#","");
		    System.out.println("Token "+assemblyTokens[i]);
		    }
		}*///System.exit(0);
		return assemblyTokens;
	}
		
	private static boolean removeInstructionFromTail(GenericCircularQueue<Instruction> inputToPipeline, long instructionPointer, int previousSize) {
		
		if(inputToPipeline.size()<previousSize) {
			misc.Error.showErrorAndExit("This is not possible !!!");
		}
		
		while(inputToPipeline.size()>previousSize) {
			Instruction ins = inputToPipeline.pop();
			CustomObjectPool.getInstructionPool().returnObject(ins);
		}
		
		return false;
	}

	/*
	 * This function fuses the statically translated micro-ops with the information received from the emulator.
	 * New micro-ops are added to the circular buffer(argument). Finally it returns the number of CISC instructions it could 
	 * translate.
	 */
	public static void fuseInstruction(
			int tidApp, long startInstructionPointer,
			EmulatorPacketList arrayListPacket, GenericCircularQueue<Instruction> inputToPipeline)
	{
		int prevLengthOfInputToPipeLine = inputToPipeline.size();

		//System.out.println("ip = " + startInstructionPointer + "\t" + Long.toHexString(startInstructionPointer));
		
		// Create a dynamic instruction buffer for all control packets
		DynamicInstructionBuffer dynamicInstructionBuffer = staticDynamicInstructionBuffers[tidApp];
		dynamicInstructionBuffer.configurePackets(arrayListPacket);
		
		InstructionList assemblyPacketList = null;
		
		int numSourceIns = 1;
		int microOpIndex = -1;
		String asmText=null;
		boolean removedFromTail = false;		
		// Riscify the assembly packets
		if(EmulatorConfig.emulatorType==EmulatorType.none) {
			assemblyPacketList = threadMicroOpsList[tidApp];
			threadMicroOpsList[tidApp].clear();
			
			//This is a bug(at least in case of caching): assemblyPacketList = threadMicroOpsList[tidApp]; 
			Packet p = arrayListPacket.get(0);
			
			if(p.value==Encoding.ASSEMBLY) {
				byte asmBytes[] = CustomObjectPool.getCustomAsmCharPool().dequeue(tidApp);
				String assemblyTokens[] = tokenizeQemuAssemblyCode(asmBytes);
				
				//String asmText = concatenateStringArray(assemblyTokens);
				asmText = concatenateStringArray(assemblyTokens);
				//check if present in translated-instruction cache
				if(TranslatedInstructionCache.isPresent(asmText)) {
					assemblyPacketList = TranslatedInstructionCache.getInstructionList(asmText);
					
					for(int j=0; j<assemblyPacketList.length(); j++) {
						assemblyPacketList.setCISCProgramCounter(j, p.ip);
					}
					
				} else {
					// System.out.println(i + " : " + assemblyLine);
					long instructionPointer = p.ip;
					String instructionPrefix, operation, operand1, operand2, operand3;
					instructionPrefix = assemblyTokens[0]; operation = assemblyTokens[1];
					operand1 = assemblyTokens[2]; operand2 = assemblyTokens[3]; operand3 = assemblyTokens[4];
					
					riscifyInstruction( instructionPointer, 
						instructionPrefix, operation, 
						operand1, operand2, operand3, 
						assemblyPacketList);
					
					//Add to translated-instruction cache
					TranslatedInstructionCache.add(asmText, assemblyPacketList);
				}
			} else {
				return;
				//misc.Error.showErrorAndExit("First packet to fuse instruction must be assembly packet !!");
			}
			
			microOpIndex = 0;
			
		} else if (EmulatorConfig.emulatorType==EmulatorType.pin) {
			
		}
		
		Instruction staticMicroOp, dynamicMicroOp;
		DynamicInstructionHandler dynamicInstructionHandler;
				
		// main translate loop.
		while(true)
		{
			staticMicroOp = assemblyPacketList.get(microOpIndex); 
			if(staticMicroOp==null || staticMicroOp.getCISCProgramCounter() != startInstructionPointer) {
				break;
			}
			
			dynamicInstructionHandler = VisaHandlerSelector.selectHandler(staticMicroOp.getOperationType());
			dynamicMicroOp = getDynamicMicroOp(staticMicroOp);
			microOpIndex = dynamicInstructionHandler.handle(microOpIndex, dynamicMicroOp, dynamicInstructionBuffer);
			
			if(microOpIndex==-1) {
				//System.out.println(asmText);
				// I was unable to fuse certain micro-ops of this instruction. 
				// So, I must remove any previously 
				// computed micro-ops from the buffer
				//System.out.println("111"+dynamicMicroOp.getOperationType()+" "+staticMicroOp.getOperationType());
				CustomObjectPool.getInstructionPool().returnObject(dynamicMicroOp);
				removeInstructionFromTail(inputToPipeline, staticMicroOp.getCISCProgramCounter(), prevLengthOfInputToPipeLine);
				removedFromTail = true;
				numSourceIns = 0;
				break;
			} else {
				//System.out.println(asmText);
				//System.out.println("222"+dynamicMicroOp.getOperationType()+" "+staticMicroOp.getOperationType());
				inputToPipeline.enqueue(dynamicMicroOp); //append microOp
			}
		}
		
		if((removedFromTail == true )&& inputToPipeline.size()!=prevLengthOfInputToPipeLine) {
			System.err.println("\ncurrentSize = " + inputToPipeline.size());
			System.err.println("previousSize = " + prevLengthOfInputToPipeLine);
			misc.Error.showErrorAndExit("");
		}
		
		// If we have failed to read any information from dynamicInstructionBuffer before, then read it now.
		if(dynamicInstructionBuffer.missedInformation()) {
			//This instruction could not be translated. However, if there are some 
			//load/store/branch operations in this instruction, they must be pushed 
			//to pipeline.
			flushDynamicInformationPackets(startInstructionPointer, dynamicInstructionBuffer, inputToPipeline);
		}
		
		/* clear the dynamicInstructionBuffer */		
		// dynamicInstructionBuffer.clearBuffer();
		//System.out.println(inputToPipeline);
		//return numSourceIns;
	}
	
	//Some instructions are not translated by the translator. However, if there are some 
	//load/store/branch operations in this instruction, they must be pushed 
	//to pipeline.
	private static void flushDynamicInformationPackets(
			long instructionPointer,
			DynamicInstructionBuffer dynamicInstructionBuffer,
			GenericCircularQueue<Instruction> inputToPipeline) 
	{
		Instruction dynamicMicroOp;
		DynamicInstructionHandler dynamicInstructionHandler;
		
		// load information
		for(int i=dynamicInstructionBuffer.getMemReadCount(); i<dynamicInstructionBuffer.getMemReadSize(); i++)
		{
			dynamicMicroOp = CustomObjectPool.getInstructionPool().borrowObject();
			dynamicMicroOp.copy(staticLoadMicroOp);
			dynamicInstructionHandler = VisaHandlerSelector.selectHandler(dynamicMicroOp.getOperationType());
			dynamicInstructionHandler.handle(0, dynamicMicroOp, dynamicInstructionBuffer);
			inputToPipeline.enqueue(dynamicMicroOp);
		}
		
		// store information
		for(int i=dynamicInstructionBuffer.getMemWriteCount(); i<dynamicInstructionBuffer.getMemWriteSize(); i++)
		{
			dynamicMicroOp = CustomObjectPool.getInstructionPool().borrowObject();
			dynamicMicroOp.copy(staticStoreMicroOp);
			dynamicInstructionHandler = VisaHandlerSelector.selectHandler(dynamicMicroOp.getOperationType());
			dynamicInstructionHandler.handle(0, dynamicMicroOp, dynamicInstructionBuffer);
			inputToPipeline.enqueue(dynamicMicroOp);
		}
			
		// branch information. This must be performed strictly after memory operations.
		if(dynamicInstructionBuffer.isBranchInformationReadNeeded())
		{
			dynamicMicroOp = CustomObjectPool.getInstructionPool().borrowObject();
			dynamicMicroOp.copy(staticBranchMicroOp);
			dynamicInstructionHandler = VisaHandlerSelector.selectHandler(dynamicMicroOp.getOperationType());
			dynamicInstructionHandler.handle(0, dynamicMicroOp, dynamicInstructionBuffer);
			inputToPipeline.enqueue(dynamicMicroOp);
		}
	}

	private static Instruction getDynamicMicroOp(Instruction staticMicroOp) {
		
		Instruction dynamicMicroOp = null;
		
		if(EmulatorConfig.emulatorType==EmulatorType.pin) {
			dynamicMicroOp = CustomObjectPool.getInstructionPool().borrowObject();
			dynamicMicroOp.copy(staticMicroOp);
		} else if(EmulatorConfig.emulatorType==EmulatorType.none) {
			// This will ensure that the packet is returned to instruction pool
			dynamicMicroOp = staticMicroOp;
		}
		
		return dynamicMicroOp;
	}
}
