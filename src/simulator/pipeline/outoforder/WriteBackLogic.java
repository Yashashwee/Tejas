package pipeline.outoforder;

import java.io.FileWriter;
import java.io.IOException;

import pipeline.ExecutionEngine;
import config.EnergyConfig;
import config.SimulationConfig;
import generic.Core;
import generic.Event;
import generic.EventQueue;
import generic.GlobalClock;
import generic.Operand;
import generic.OperandType;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SimulationElement;
import memorysystem.AddressCarryingEvent;
import generic.Instruction;

public class WriteBackLogic extends SimulationElement {
	
	Core core;
	OutOrderExecutionEngine execEngine;
	ReorderBuffer ROB;
	FetchLogic fetchLogic;
	MemInstBuffer memInstBuffer;
	// ExecutionEngine execEngine;
	long fencedPC = 6035635;
	
	public WriteBackLogic(Core core, OutOrderExecutionEngine execEngine)
	{
		super(PortType.Unlimited, -1, -1, -1, -1);
		this.core = core;
		this.execEngine = execEngine;
		fetchLogic = execEngine.getFetcher();
		// memInstBuffer = execEngine.getMemInstBuffer();
		ROB = execEngine.getReorderBuffer();
	}

	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
	}
	
	public void performWriteBack()
	{
		if(execEngine.isToStall5() == true /*pipeline stalled due to branch mis-prediction*/
				|| ROB.head == -1 /*ROB empty*/)
		{
			return;
		}
		
		
		int i = ROB.head;
		int noWritten = 0;

		ReorderBufferEntry[] buffer = ROB.getROB();
		if(i!=ROB.tail)
		{
			if ( buffer[i].getInstruction().getCISCProgramCounter()==execEngine.getMfencePC())

			{
				// memInstBuffer.dump();
				// memInstBuffer.resetBuffer();
				execEngine.setFetchStall(false);
				// System.out.println("In wb");
				// System.out.println(buffer[i].getInstruction().getCISCProgramCounter());
			}
		}
		do
		{
			noWritten++;
			
			if(buffer[i].getExecuted() == true &&
					buffer[i].isWriteBackDone() == false)
			{
				buffer[i].setWriteBackDone1(true);
				buffer[i].setWriteBackDone2(true);
				
				/*
				 * aiding decoded instructions that are not yet in the IW.
				 * (see WakeUpLogic.java for detailed explanation)
				 * the below code is part of the solution, the remainder is at the wake-up stage
				 */
				if(buffer[i].getInstruction().getOperationType() == OperationType.load)
				{
					WakeUpLogic.wakeUpLogic(core, buffer[i].getInstruction().getDestinationOperand().getOperandType(), buffer[i].getPhysicalDestinationRegister(), buffer[i].getThreadID(), (buffer[i].pos + 1)%ROB.MaxROBSize);
				}
				// if(buffer[i].getInstruction().getOperationType()==OperationType.clflush)
				// 	System.out.println("Yaayy writeback");
				
				//set value valid in register file, and
				//add destination register to list of available physical registers
				if(buffer[i].getInstruction().getDestinationOperand() != null)
				{
					writeToRFAndAddToAvailableList(buffer[i].getInstruction().getDestinationOperand(),
													buffer[i].getPhysicalDestinationRegister());
				}
				else if(buffer[i].getInstruction().getOperationType() == OperationType.xchg)
				{
					writeToRFAndAddToAvailableList(buffer[i].getInstruction().getSourceOperand1(),
													buffer[i].getOperand1PhyReg1());
					
					if(buffer[i].getInstruction().getSourceOperand1().getOperandType() != buffer[i].getInstruction().getSourceOperand2().getOperandType() ||
							buffer[i].getOperand1PhyReg1() != buffer[i].getOperand2PhyReg1())
					{
						writeToRFAndAddToAvailableList(buffer[i].getInstruction().getSourceOperand2(),
													buffer[i].getOperand2PhyReg1());
					}
				}
				else if(buffer[i].getInstruction().getOperationType()==OperationType.clflush)
				{
					Instruction ins1 = buffer[i].getInstruction();
					Long addr = ins1.getSourceOperand1MemValue();
					execEngine.getCoreMemorySystem().getL1Cache().clearLine(addr);
					
				}else if(buffer[i].getInstruction().getOperationType()==OperationType.syscall)
				{
					execEngine.getCoreMemorySystem().getiTLB().flushTlb();
					execEngine.getCoreMemorySystem().getdTLB().flushTlb();
				}

				if(SimulationConfig.debugMode)
				{
					System.out.println("writeback : " + GlobalClock.getCurrentTime()/core.getStepSize() + " : " + buffer[i].getInstruction());
				}
			}
			
			i = (i+1)%ROB.getMaxROBSize();
			
		}while(i != ROB.tail && noWritten < core.getRetireWidth());
	}

	public void setMemInstBuffer(MemInstBuffer memInstBuffer)
	{
		this.memInstBuffer = memInstBuffer;
	}
	
	//set value valid in register file, and
	//add destination register to list of available physical registers
	private void writeToRFAndAddToAvailableList(Operand destOpnd,
												int physicalRegister)
	{
		RenameTable tempRN = null;
		if(destOpnd != null)
		{
			if(destOpnd.isIntegerRegisterOperand())
			{
				tempRN = execEngine.getIntegerRenameTable();
				if(tempRN.getMappingValid(physicalRegister) == false)
				{
					tempRN.addToAvailableList(physicalRegister);
				}
				tempRN.setValueValid(true, physicalRegister);
				execEngine.getIntegerRegisterFile().setValueValid(true, physicalRegister);
			}
			else if(destOpnd.isFloatRegisterOperand())
			{
				tempRN = execEngine.getFloatingPointRenameTable();
				if(tempRN.getMappingValid(physicalRegister) == false)
				{
					tempRN.addToAvailableList(physicalRegister);
				}
				tempRN.setValueValid(true, physicalRegister);
				execEngine.getFloatingPointRegisterFile().setValueValid(true, physicalRegister);
			}
		}
	}

}
