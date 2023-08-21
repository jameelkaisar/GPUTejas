/*
/*****************************************************************************
				GPUTejas Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2014] [Indian Institute of Technology, Delhi]
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

	Contributors:  Seep Goel, Geetika Malhotra, Harinder Pal
*****************************************************************************/ 
package generic;

import java.io.Serializable;



@SuppressWarnings("serial")
public class Instruction implements Serializable
{
	public OperationType type;	
	public Long MemoryAddresses[];
	
	private long ciscProgramCounter;
	public int blockID;
	
	public Instruction()
	{
		this.MemoryAddresses = null;
	}
	
	public void clear()
	{
		this.type = null;
		this.MemoryAddresses = null;
	}
	
	public Instruction(OperationType type, Long MemoryAddresses[])
	
	{
		this.type = type;
		this.MemoryAddresses = MemoryAddresses;
	}
	
	private void set(OperationType type)
	{
		this.type = type;
		this.MemoryAddresses = null;
	}
	
	private void set(OperationType type, Long MemoryAddresses[])
	{
		this.type = type;
		this.MemoryAddresses = MemoryAddresses;
	}
	
	//all properties of sourceInstruction is copied to the current instruction
	public void copy(Instruction sourceInstruction)
	{
		this.type=sourceInstruction.type;

		this.ciscProgramCounter = sourceInstruction.ciscProgramCounter;
		this.MemoryAddresses = sourceInstruction.MemoryAddresses;
	}
	
	public static Instruction getInvalidInstruction()
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.inValid);
		return ins;
	}
	
	public static Instruction getLoadInstruction(Long MemoryAddresses[])
	{
		
		Instruction ins = new Instruction();
		ins.set(OperationType.load, MemoryAddresses);
		return ins;
	}
	
	public static Instruction getConstantLoadInstruction(Long MemoryAddresses[])
	{
		
		Instruction ins = new Instruction();
		ins.set(OperationType.load_const, MemoryAddresses);
		return ins;
	}
	
	public static Instruction getSharedLoadInstruction(Long MemoryAddresses[])
	{
		
		Instruction ins = new Instruction();
		ins.set(OperationType.load_shared, MemoryAddresses);
		return ins;
	}
	
	
	public static Instruction getStoreInstruction(Long MemoryAddresses[])
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.store, MemoryAddresses);
		return ins;
	}
	
	public static Instruction getConstantStoreInstruction(Long MemoryAddresses[])
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.store_const, MemoryAddresses);
		return ins;
	}
	
	public static Instruction getSharedStoreInstruction(Long MemoryAddresses[])
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.store_shared, MemoryAddresses);
		return ins;
	}
	
	public static Instruction getAddressInstruction()
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.address);
		return ins;
	}
	
	public static Instruction getIntALUInstruction()
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.integerALU);
		return ins;
	}
	
	
	public static Instruction getIntegerMultiplicationInstruction()
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.integerMul);
		return ins;
	}
	
	public static Instruction getIntegerDivisionInstruction()
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.integerDiv);
		return ins;
	}

	public static Instruction getFloatingPointALU()
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.floatALU);
		return ins;
	}
	
	public static Instruction getFloatingPointMultiplication()
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.floatMul);
		return ins;
	}
	
	public static Instruction getFloatingPointDivision()
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.floatDiv);
		return ins;
	}
	
	public static Instruction getPredicateInstruction()
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.predicate);
		return ins;
	}
	
	public static Instruction getBranchInstruction()
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.branch);
		return ins;
	}

	public static Instruction getCallInstruction()
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.call);
		return ins;
	}
	
	public static Instruction getReturnInstruction()
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.Return);
		return ins;
	}
	
	public static Instruction getExitInstruction()
	{
		Instruction ins = new Instruction();
		ins.set(OperationType.exit);
		return ins;
	}

	
	public long getCISCProgramCounter()
	{
		return ciscProgramCounter;
	}
	
	public void setCISCProgramCounter(long programCounter) 
	{
		this.ciscProgramCounter = programCounter;
	}
	
	public OperationType getOperationType()
	{
		return type;
	}
	
	public void setOperationType(OperationType operationType)
	{
		this.type = operationType;
	}
	

	public int getBlockID() {
		return blockID;
	}
	
	public void setBlockID(int b) {
		blockID=b;
	}

	public Long[] getMemoryAddresses() {
		return MemoryAddresses;
	}

	public void setMemoryAddresses(Long[] MemoryAddresses) {
		this.MemoryAddresses = MemoryAddresses;
	}

	
	/**
	 * strInstruction method returns the instruction information in a string.
	 * @return String describing the instruction
	 */
	public String toString()
	{
		return 
		(
			String.format("%-20s", "IP = " + Long.toHexString(ciscProgramCounter)) +
			String.format("%-20s", "Op = " + type) 
		);
	}

	

}