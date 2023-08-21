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
import memorysystem.SMMemorySystem;
import generic.GenericCircularQueue;
import generic.Instruction;
import generic.SM;

public class GPUExecutionEngine extends ExecutionEngine{

	SM sm;
	private ScheduleUnit scheduleUnit;
	private ExecuteUnit executeUnit;
	public boolean executionComplete;
	StageLatch_MII scheduleExecuteLatch;
	
	public int pendingLoads;
	
	public GPUMemorySystem gpuMemorySystem;
	
	private long noOfMemRequests;
	private long noOfLd;
	private long noOfSt;

	public GPUExecutionEngine(SM sm) {
		super(sm);
		this.sm = sm;
		
		pendingLoads = 0;
		//in GPU only one instruction goes to the next stage at a time
		scheduleExecuteLatch = new StageLatch_MII(1,sm);
		executeUnit=new ExecuteUnit(sm, sm.eventQueue, this);
		scheduleUnit=new ScheduleUnit(sm, sm.getEventQueue(), this);
		executionComplete = false;
		//in GPU only one instruction goes to the next stage at a time

	}
	
	public void setCoreMemorySystem(SMMemorySystem smMemorySystem) {
		this.coreMemorySystem = smMemorySystem;
		this.gpuMemorySystem = (GPUMemorySystem)smMemorySystem;
	}
	@Override
	public void setInputToPipeline(GenericCircularQueue<Instruction>[] inpList) {
		
		scheduleUnit.setInputToPipeline(inpList[0]);
		
	}

	public StageLatch_MII getScheduleExecuteLatch(){
		return this.scheduleExecuteLatch;
	}
	
	public ScheduleUnit getScheduleUnit(){
		return this.scheduleUnit;
	}
	public ExecuteUnit getExecuteUnit(){
		return this.executeUnit;
	}
	
	public void setScheduleUnit(ScheduleUnit _scheduleUnit){
		this.scheduleUnit = _scheduleUnit;
	}
	public void setExecuteUnit(ExecuteUnit _executeUnit){
		this.executeUnit = _executeUnit;
	}

	public void setExecutionComplete(boolean execComplete){
		this.executionComplete=execComplete;
		if (execComplete == true)
		{
			sm.setCoreCyclesTaken(ArchitecturalComponent.getCores()[sm.getTPC_number()][sm.getSM_number()].clock.getCurrentTime()/sm.getStepSize());
		}
	}
	
	public boolean getExecutionComplete(){
		return this.executionComplete;
	}
	public void setTimingStatistics()
	{
	

	}
	
	public void updateNoOfLd(int i) {
		this.noOfLd += i;
	}

	public void updateNoOfMemRequests(int i) {
		this.noOfMemRequests += i;
	}

	public void updateNoOfSt(int i) {
		this.noOfSt += i;
	}
	
	public long getNoOfSt() {
		return noOfSt;
	}

	public long getNoOfLd() {
		return noOfLd;
	}

	public long getNoOfMemRequests() {
		return noOfMemRequests;
	}
}
