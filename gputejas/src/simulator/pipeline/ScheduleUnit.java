package pipeline;

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



import main.ArchitecturalComponent;
import config.OperationLatencyConfig;


import generic.Event;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.Instruction;

import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SM;
import generic.SimulationElement;

public class ScheduleUnit extends SimulationElement{
	
	SM sm;
	GPUExecutionEngine containingExecutionEngine;
	
	public GenericCircularQueue<Instruction> inputToPipeline;
	StageLatch_MII scheduleExecuteLatch;
	
	Instruction fetchedInstruction;
	boolean fetchedInstructionStatus;

	public ScheduleUnit(SM sm, EventQueue eventQueue, GPUExecutionEngine execEngine) {
		super(PortType.Unlimited, -1, -1, eventQueue, -1, -1);
		this.sm = sm;
		this.containingExecutionEngine = execEngine;
		this. scheduleExecuteLatch = execEngine.getScheduleExecuteLatch();
		this.fetchedInstruction = null;
		this.fetchedInstructionStatus = false;
	}

	public void fetchInstruction(int i)
	
	{
		if(inputToPipeline.isEmpty())
		{
			return;
		}
		
		
		if(this.fetchedInstruction != null)
		{
			return;
		}
		Instruction newInstruction = inputToPipeline.pollFirst();
		if(newInstruction.getOperationType() == OperationType.inValid) {
			this.fetchedInstruction = newInstruction;
			this.fetchedInstructionStatus = true;
		}
		else
		{
			this.fetchedInstruction =  newInstruction;
			
			containingExecutionEngine.gpuMemorySystem.issueRequestToInstrCache(newInstruction.getCISCProgramCounter(), i);
		}
	}
	public void performSchedule(GPUpipeline inorderPipeline, int k)
	{
		if(scheduleExecuteLatch.isFull() == true)
		{
			return;
		}
		if(!this.fetchedInstructionStatus)
		{
			containingExecutionEngine.incrementInstructionMemStall(1); 
		}
		else
		{
			Instruction newInstruction = this.fetchedInstruction;
			this.fetchedInstruction = null;
			this.fetchedInstructionStatus = false;
			
			long latency;
			if(newInstruction==null || newInstruction.getOperationType() == null) {
				this.fetchedInstruction = null;
				this.fetchedInstructionStatus = false;
				fetchInstruction(k);
				return;
			}
			switch(newInstruction.getOperationType())
			{
				case load:
					latency = OperationLatencyConfig.loadLatency;
					break;
					
				case load_const:
					for(int i =0; i < newInstruction.MemoryAddresses.length; i++)
					{
						boolean memReqIssued = false;
						try{
							if(newInstruction.MemoryAddresses[i] == null)
							{
								break;
							}
							else
							{
								memReqIssued = containingExecutionEngine.gpuMemorySystem.issueRequestToConstantCache(
								RequestType.Cache_Read,
								newInstruction.MemoryAddresses[i], k);
								if(!memReqIssued)
								{
									System.out.println("Memory request not issuesd");
									System.exit(0);
									
								}
								
							}
						}
							
							
						catch(Exception e)
						{
							e.printStackTrace();
							System.exit(0);
							return;
						}
						containingExecutionEngine.pendingLoads++;
						main.Main.runners[Integer.parseInt(Thread.currentThread().getName())].mem_flag = true;
					}
					latency = Long.MAX_VALUE;
					break;				
				case load_shared:
					for(int i =0; i < newInstruction.MemoryAddresses.length; i++)
					{
						boolean memReqIssued = false;
						try{
							if(newInstruction.MemoryAddresses[i] == null)
							{
								break;
							}
							else
							{
								memReqIssued = containingExecutionEngine.gpuMemorySystem.issueRequestToSharedCache(
								RequestType.Cache_Read,
								newInstruction.MemoryAddresses[i], k);
								if(!memReqIssued)
								{
									System.out.println("Memory request not issuesd");
									System.exit(0);
									
								}
							}
						}
						catch(Exception e)
						{
							e.printStackTrace();
							System.exit(0);
							return;
						}
						containingExecutionEngine.pendingLoads++;
						main.Main.runners[Integer.parseInt(Thread.currentThread().getName())].mem_flag = true;
					}
					latency = Long.MAX_VALUE;
					break;
				case store:
					latency = OperationLatencyConfig.storeLatency;
					break;
				case store_const:
					for(int i =0; i < newInstruction.MemoryAddresses.length; i++)
					{
						boolean memReqIssued = false;
						try{
							if(newInstruction.MemoryAddresses[i] == null)
							{
								break;
							}
							else
							{
								memReqIssued = containingExecutionEngine.gpuMemorySystem.issueRequestToConstantCache(
								RequestType.Cache_Write,
								newInstruction.MemoryAddresses[i], k);
								if(!memReqIssued)
								{
									System.out.println("Memory request not issuesd");
									System.exit(0);
									
								}
							}
						}
						catch(Exception e)
						{e.printStackTrace();
							System.exit(0);
							return;
						}
					}
					latency = OperationLatencyConfig.storeLatency;
					break;
				case store_shared:
					for(int i =0; i < newInstruction.MemoryAddresses.length; i++)
					{
						boolean memReqIssued = false;
						try{
							if(newInstruction.MemoryAddresses[i] == null)
							{
								break;
							}
							else
							{
								memReqIssued = containingExecutionEngine.gpuMemorySystem.issueRequestToSharedCache(
								RequestType.Cache_Write,
								newInstruction.MemoryAddresses[i], k);
								if(!memReqIssued)
								{
									System.out.println("Memory request not issuesd");
									System.exit(0);
									
								}
							}
						}
						catch(Exception e)
						{
							e.printStackTrace();
							System.exit(0);
							return;
						}
					}
					latency = OperationLatencyConfig.storeLatency;
					break;
				case address:
					latency = OperationLatencyConfig.addressLatency;
					break;
				case integerALU:
					latency = OperationLatencyConfig.intALULatency;
					break;
				case integerMul:
					latency = OperationLatencyConfig.intMULLatency;
					break;
				case integerDiv:
					latency = OperationLatencyConfig.intDIVLatency;
					break;
				case floatALU:
					latency = OperationLatencyConfig.floatALULatency;
					break;
				case floatMul:
					latency = OperationLatencyConfig.floatMULLatency;
					break;
				case floatDiv:
					latency = OperationLatencyConfig.floatDIVLatency;
					break;
				case predicate:
					latency = OperationLatencyConfig.predicateLatency;
					break;
				case branch:
					latency = OperationLatencyConfig.branchLatency;
					break;
				case call:
					latency = OperationLatencyConfig.callLatency;
					break;
				case Return:
					latency = OperationLatencyConfig.returnLatency;
					break;
				case exit:
					latency = OperationLatencyConfig.exitLatency;
					break;
				default:
					latency = 1;
					break;
			}
			this.scheduleExecuteLatch.add(newInstruction, ArchitecturalComponent.getCores()[this.containingExecutionEngine.containingCore.getTPC_number()][this.containingExecutionEngine.containingCore.getSM_number()].clock.getCurrentTime() + latency);
		}
		fetchInstruction(k);
	}
	public GenericCircularQueue<Instruction> getInputToPipeline(){
		return this.inputToPipeline;
	}
	public void setInputToPipeline(GenericCircularQueue<Instruction> inpList){
		this.inputToPipeline = inpList;
	}
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
	}

	public void processCompletionOfMemRequest(long requestedAddress) {
			if(this.fetchedInstruction != null && 
					this.fetchedInstruction.getCISCProgramCounter() == requestedAddress && 
					this.fetchedInstructionStatus==false){
				this.fetchedInstructionStatus=true;
			}
		}

}
