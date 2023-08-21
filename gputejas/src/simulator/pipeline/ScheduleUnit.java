package pipeline;

///
///
//				GPUTejas Simulator
//------------------------------------------------------------------------------------------------------------
//
//   Copyright [2014] [Indian Institute of Technology, Delhi]
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//------------------------------------------------------------------------------------------------------------
//
//	Contributors:  Seep Goel, Geetika Malhotra, Harinder Pal
/// 

import java.io.FileWriter;
import java.io.IOException;

import main.ArchitecturalComponent;
import config.CacheConfig.WritePolicy;
import config.EnergyConfig;
import config.OperationEnergyConfig;
import config.OperationLatencyConfig;
import config.SimulationConfig;
import config.SmConfig;
import config.SpConfig;
import generic.Event;
import generic.EventQueue;
import generic.GenericCircularQueue;
import generic.GpuType;
import generic.Instruction;
import generic.OperationType;
import generic.PortType;
import generic.RequestType;
import generic.SP;
import generic.SimulationElement;
import pipeline.StageLatch_MII;


public class ScheduleUnit extends SimulationElement{
	
	SP sp;
	GPUExecutionEngine containingExecutionEngine;
	
	public GenericCircularQueue<Instruction> inputToPipeline;
	public GenericCircularQueue<Instruction> completeWarpPipeline;
	public StageLatch_MII scheduleExecuteLatch;
	Instruction fetchedInstruction;
	boolean fetchedInstructionStatus;
	static int repetitions = Math.max(1, SimulationConfig.ThreadsPerCTA / SpConfig.NoOfThreadsSupported); //ocelot generates traces for {ThreadsPerCTA} number of threads. timings need to account for the number of repetitions required to actually execute all of them.
///////////////////////////////////////////////////////////////////////
	int loadaccess;
	int storeaccess;
	int addressaccess;
	int intALUaccess;
	int intMULaccess;
	int intDIVaccess;
	int floatALUaccess;
	int floatMULaccess;
	int floatDIVaccess;
	int predicateaccess;
	int branchaccess;
	int callaccess;
	int returnaccess;
	int exitaccess;
	long numAccesses;
	//////////////////////////////////////////////////
	public ScheduleUnit(SP sp, EventQueue eventQueue, GPUExecutionEngine execEngine) {
		super(PortType.Unlimited, -1, -1, eventQueue, -1, -1);
		this.sp = sp;
		this.containingExecutionEngine = execEngine;
		this.scheduleExecuteLatch = execEngine.getScheduleExecuteLatch();
		this.fetchedInstruction = null;
		this.fetchedInstructionStatus = false;
		
		/* class variables don't require initialisation to 0
		loadaccess=0;
		storeaccess=0;
		addressaccess=0;
		intALUaccess=0;
		intMULaccess=0;
		intDIVaccess=0;
		floatALUaccess=0;
		floatMULaccess=0;
		floatDIVaccess=0;
		predicateaccess=0;
		branchaccess=0;
		callaccess=0;
		returnaccess=0;
		exitaccess=0;
		numAccesses = 0;*/
		
	}

	public void fetchInstruction()
	{
		if(this.fetchedInstruction != null)
		{
			return;
		}
		
		Instruction newInstruction = completeWarpPipeline.pollFirst();
		if(newInstruction!=null){
			if(newInstruction.getOperationType() == OperationType.inValid) {
				this.fetchedInstruction = newInstruction;
				this.fetchedInstructionStatus = true;
			}
			else
			{
				this.fetchedInstruction =  newInstruction;
				this.fetchedInstructionStatus = true;
				containingExecutionEngine.gpuMemorySystem.issueRequestToInstrCache(newInstruction.getCISCProgramCounter());
			}
		}
	}
	
	public void performSchedule(GPUpipeline inorderPipeline)
	{
		if(scheduleExecuteLatch.isFull() == true)
		{
			System.out.println("is Full");
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
			long latency = 0;
			if(newInstruction==null || newInstruction.getOperationType() == null) {
				this.fetchedInstruction = null;
				this.fetchedInstructionStatus = false;
				fetchInstruction();
				return;
			}

			switch(newInstruction.getOperationType())
			{
				case load:
					loadaccess += repetitions;
					if (SimulationConfig.GPUType == GpuType.Ampere)
						latency = SmConfig.L1Cache.latency;
					else
						latency = SpConfig.dCache.latency;
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
								memReqIssued = containingExecutionEngine.gpuMemorySystem.issueRequestToDataCache(
								RequestType.Cache_Read,	newInstruction.MemoryAddresses[i]);
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
					break;
					
				case load_const:
					loadaccess += repetitions;
					latency = SmConfig.constantCache.latency;
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
								RequestType.Cache_Read,	newInstruction.MemoryAddresses[i]);
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
					break;				
				case load_shared:
					loadaccess += repetitions;
					latency = SmConfig.sharedCache.latency;
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
								newInstruction.MemoryAddresses[i]);
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
					break;
				case store:
					storeaccess += repetitions;
					if (SimulationConfig.GPUType == GpuType.Ampere && SmConfig.L1Cache.writePolicy == WritePolicy.WRITE_BACK)
						latency = SmConfig.L1Cache.latency;
					else if (SpConfig.dCache.writePolicy == WritePolicy.WRITE_BACK)
						latency = SpConfig.dCache.latency;
					else
						latency = OperationLatencyConfig.storeLatency;
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
								memReqIssued = containingExecutionEngine.gpuMemorySystem.issueRequestToDataCache(
								RequestType.Cache_Write,
								newInstruction.MemoryAddresses[i]);
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
					break;
				case store_const:
					storeaccess += repetitions;
					if (SmConfig.constantCache.writePolicy == WritePolicy.WRITE_BACK)
						latency = SmConfig.constantCache.latency;
					else
						latency = OperationLatencyConfig.storeLatency;
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
								newInstruction.MemoryAddresses[i]);
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
					storeaccess += repetitions;
					if (SmConfig.sharedCache.writePolicy == WritePolicy.WRITE_BACK)
						latency = SmConfig.sharedCache.latency;
					else
						latency = OperationLatencyConfig.storeLatency;
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
								newInstruction.MemoryAddresses[i]);
								//System.out.println("Memory Request Issued");
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
					break;
				case address:
					addressaccess += repetitions;
					latency = OperationLatencyConfig.addressLatency;
					break;
				case integerALU:
					intALUaccess += repetitions;
					latency = OperationLatencyConfig.intALULatency;
					break;
				case integerMul:
					intMULaccess += repetitions;
					latency = OperationLatencyConfig.intMULLatency;
					break;
				case integerDiv:
					intDIVaccess += repetitions;
					latency = OperationLatencyConfig.intDIVLatency;
					break;
				case floatALU:
					floatALUaccess += repetitions;
					latency = OperationLatencyConfig.floatALULatency;
					break;
				case floatMul:
					floatMULaccess += repetitions;
					latency = OperationLatencyConfig.floatMULLatency;
					break;
				case floatDiv:
					floatDIVaccess += repetitions;
					latency = OperationLatencyConfig.floatDIVLatency;
					break;
				case predicate:
					predicateaccess += repetitions;
					latency = OperationLatencyConfig.predicateLatency;
					break;
				case branch:
					branchaccess += repetitions;
					latency = OperationLatencyConfig.branchLatency;
					break;
				case call:
					callaccess += repetitions;
					latency = OperationLatencyConfig.callLatency;
					break;
				case Return:
					returnaccess += repetitions;
					latency = OperationLatencyConfig.returnLatency;
					break;
				case exit:
					exitaccess += repetitions;
					latency = OperationLatencyConfig.exitLatency;
					break;
				default:
					latency = 1;
					break;
			}
			numAccesses += repetitions;
			this.scheduleExecuteLatch.add(newInstruction, ArchitecturalComponent.getCores()[this.containingExecutionEngine.containingCore.getTPC_number()][this.containingExecutionEngine.containingCore.getSM_number()][this.containingExecutionEngine.containingCore.getSP_number()].clock.getCurrentTime() + latency*repetitions);
		}
		fetchInstruction();
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

	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{ 
		EnergyConfig totalfupower = new EnergyConfig(0, 0);
		
		// Actually Latch Decode Power 
		EnergyConfig decodepower = new EnergyConfig(containingExecutionEngine.getContainingCore().getDecodePower(), numAccesses);
		decodepower.printEnergyStats(outputFileWriter, componentName+".decoder");
		totalfupower.add(decodepower);
		
		double energy = loadaccess * OperationEnergyConfig.loadEnergy + 
					storeaccess * OperationEnergyConfig.storeEnergy + 
					addressaccess * OperationEnergyConfig.addressEnergy +
					intALUaccess * OperationEnergyConfig.intALUEnergy +
					intMULaccess * OperationEnergyConfig.intMULEnergy +
					intDIVaccess * OperationEnergyConfig.intDIVEnergy +
					floatALUaccess * OperationEnergyConfig.floatALUEnergy +
					floatMULaccess * OperationEnergyConfig.floatMULEnergy +
					floatDIVaccess * OperationEnergyConfig.floatDIVEnergy +
					predicateaccess * OperationEnergyConfig.predicateEnergy +
					branchaccess * OperationEnergyConfig.branchEnergy +
					callaccess * OperationEnergyConfig.callEnergy +
					returnaccess * OperationEnergyConfig.returnEnergy +
					exitaccess * OperationEnergyConfig.exitEnergy;
		EnergyConfig alupower = new EnergyConfig(0,energy);
		alupower.printEnergyStats(outputFileWriter, componentName+".totalalu");
		totalfupower.add(alupower);
		
		return totalfupower;
	}
	
}
