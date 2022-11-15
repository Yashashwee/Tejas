package pipeline;

import javax.management.openmbean.OpenType;

import generic.OperationType;

/**
 * given an operation type, what is the functional unit required?
 */
public class OpTypeToFUTypeMapping {
	
	public static FunctionalUnitType getFUType(OperationType opType)
	{
		if(opType == null)
			return FunctionalUnitType.inValid; 
		switch(opType)
		{
			case integerALU	:	{
									return FunctionalUnitType.integerALU;
								}
			case integerMul	:	{
									return FunctionalUnitType.integerMul;
								}
			case integerDiv	:	{
									return FunctionalUnitType.integerDiv;
								}
			case floatALU	:	{
									return FunctionalUnitType.floatALU;
								}
			case floatMul	:	{
									return FunctionalUnitType.floatMul;
								}
			case floatDiv	:	{
									return FunctionalUnitType.floatDiv;
								}
			case load		:	{
									return FunctionalUnitType.memory;
								}
			case store		:	{
									return FunctionalUnitType.memory;
								}
			case jump		:	{
									return FunctionalUnitType.jump;
								}
			case branch		:	{
									return FunctionalUnitType.jump;
								}
			case mov		:	{
									return FunctionalUnitType.integerALU;
								}
			case xchg		:	{
									return FunctionalUnitType.integerALU;
								}
			case clflush	:	{
									return FunctionalUnitType.memory;
								}
			case syscall	:	{
									return FunctionalUnitType.memory;
			}
			case mfence 	:	{
				// System.out.println("Mfence in functype");
									return FunctionalUnitType.memory;
			}
			default			:	{
									return FunctionalUnitType.inValid;
								}
		}
	}

}