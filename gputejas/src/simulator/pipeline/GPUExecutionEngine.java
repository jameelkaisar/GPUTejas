package pipeline;
import pipeline.RF.*;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Vector;

import config.EnergyConfig;
import config.RegConfig;
import config.SimulationConfig;
import config.SmConfig;
import config.SystemConfig;
import config.TpcConfig;
import main.ArchitecturalComponent;
import memorysystem.MemorySystem;
import memorysystem.Mode3MSHR;
import memorysystem.SPMemorySystem;
import generic.GenericCircularQueue;
import generic.GpuType;
import generic.Instruction;
import generic.LocalClockperSp;
import generic.SP;

public class GPUExecutionEngine extends ExecutionEngine{

	private static final int MAX_REG_OPERANDS = 0;
	SP sp;
	private ScheduleUnit scheduleUnit;
	private ExecuteUnit executeUnit;
	private OperandCollector OperandCollector;
	public Scoreboard m_scoreboard;
	public boolean executionComplete;
	StageLatch_MII scheduleExecuteLatch;
    int last_warp_id;
    int pendingLoads;
	public GPUMemorySystem gpuMemorySystem;
	static int[][] SMExecutionCount = new int[SystemConfig.NoOfTPC][TpcConfig.NoOfSM]; 
	
	private long noOfMemRequests;
	private long noOfLd;
	private long noOfSt;
	private long regfile_reads;
	private long regfile_writes;
	private long non_rf_operands;
	public GenericCircularQueue<Warp> WarpTable;
	public GenericCircularQueue<Warp> Warp_write;
	ArrayList<Integer> result;
	public GPUExecutionEngine(SP sp) {
		super(sp);
		this.sp = sp;
		pendingLoads=0;
		//in GPU only one instruction goes to the next stage at a time
		scheduleExecuteLatch = new StageLatch_MII(1,sp);
		executeUnit=new ExecuteUnit(sp, sp.eventQueue, this);
		scheduleUnit=new ScheduleUnit(sp, sp.getEventQueue(), this);
		executionComplete = false;
		OperandCollector= new OperandCollector();
		OperandCollector.init(RegConfig.num_banks,this.sp,this);
		last_warp_id=1;
		result = new ArrayList<Integer>();
	}
	
	public void setCoreMemorySystem(SPMemorySystem spMemorySystem) {
		this.coreMemorySystem = spMemorySystem;
		this.gpuMemorySystem = (GPUMemorySystem)spMemorySystem;
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
	public OperandCollector getOperandCollector(){
		return this.OperandCollector;
	}
	
	public void setScheduleUnit(ScheduleUnit _scheduleUnit){
		this.scheduleUnit = _scheduleUnit;
	}
	
	public void setExecuteUnit(ExecuteUnit _executeUnit){
		this.executeUnit = _executeUnit;
	}
	
	public void setOperandCollector(OperandCollector _OperandCollector){
		this.OperandCollector = _OperandCollector;
	}
	
	public void setExecutionComplete(boolean execComplete){
		this.executionComplete=execComplete;
		if (execComplete == true)
		{
			sp.setCoreCyclesTaken(sp.clock.getCurrentTime()/sp.getStepSize());
		}
	}
	
	public boolean getExecutionComplete(){
		return this.executionComplete;
	}
	
	public void setTimingStatistics()
	{
	

	}

	public int register_bank(int regnum, int wid, int num_banks, int bank_warp_shift)
	{
	   int bank = regnum;
	   if (bank_warp_shift!=0)
	      bank += wid;
	   return bank % num_banks;
	}

	public List<Integer> get_regs_written(Warp inst) {
		result.add(0,inst.dst);
		return result;
	}

	public void incregfile_writes(int active_count) {
		this.setRegfile_writes(this.getRegfile_writes() + active_count);	
	}

	// Increment non Register file operands
	public void incnon_rf_operands(int active_count) {
		this.setNon_rf_operands(this.getNon_rf_operands() + active_count);
	}
	public void incregfile_reads(int active_count) {
		this.setRegfile_reads(this.getRegfile_reads() + active_count);	
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

	public long getRegfile_reads() {
		return regfile_reads;
	}

	public void setRegfile_reads(long regfile_reads) {
		this.regfile_reads = regfile_reads;
	}

	public long getRegfile_writes() {
		return regfile_writes;
	}

	public void setRegfile_writes(long regfile_writes) {
		this.regfile_writes = regfile_writes;
	}

	public long getNon_rf_operands() {
		return non_rf_operands;
	}

	public void setNon_rf_operands(long non_rf_operands) {
		this.non_rf_operands = non_rf_operands;
	}

	public OperandCollector getOpndColl() {
		return getOperandCollector();
	}

	public void writeback()
    {
		// process next instruction that is going to writeback
		if(Warp_write.size() >0)
		{
			Warp warp= Warp_write.pollFirst();
			if(warp!=null)
			{
				OperandCollector.writeback(warp);
				// m_scoreboard.releaseRegister(warp.warp_id(), warp.out);
			}
			// for(int r=0;r<m_next_wb.get_num_write_regs();r++)
			// m_scoreboard.releaseRegister( m_next_wb.warp_id(), m_next_wb.out[r] );
		}
    }
 
	public void WarpTable()
	{	
		while(scheduleUnit.inputToPipeline.size()>0 && !WarpTable.isFull())
		{
		Instruction newInstruction = scheduleUnit.inputToPipeline.pollFirst();
		Warp inst=new Warp(newInstruction,last_warp_id++);
		WarpTable.enqueue(inst);
		}
	}
	
	public void clear()
	{
		SMExecutionCount[sp.getTPC_number()][sp.getSM_number()]++;
		this.WarpTable = null;
		this.Warp_write = null;
		this.m_scoreboard = null;
		this.scheduleUnit.inputToPipeline = null;
		this.scheduleUnit.completeWarpPipeline = null;
		this.OperandCollector.clear();
		this.result.clear();
		
		// these take up bulk of memory
		((Mode3MSHR) this.gpuMemorySystem.getiCache().missStatusHoldingRegister).clear();
		if (SimulationConfig.GPUType == GpuType.Ampere) {
			if (SMExecutionCount[sp.getTPC_number()][sp.getSM_number()] == SmConfig.NoOfSP) {
				((Mode3MSHR) this.gpuMemorySystem.getDataCache().missStatusHoldingRegister).clear();
				((Mode3MSHR) this.gpuMemorySystem.getSharedCache().missStatusHoldingRegister).clear();
				((Mode3MSHR) this.gpuMemorySystem.getConstantCache().missStatusHoldingRegister).clear();
				// clear L2 requests as the event queue used earlier is out of scope
				((Mode3MSHR) MemorySystem.cacheNameMappings.get("L2[0]").missStatusHoldingRegister).clear();
			}
		}
		else {
			((Mode3MSHR) this.gpuMemorySystem.getDataCache().missStatusHoldingRegister).clear();
			if (SMExecutionCount[sp.getTPC_number()][sp.getSM_number()] == SmConfig.NoOfSP) {
				((Mode3MSHR) this.gpuMemorySystem.getSharedCache().missStatusHoldingRegister).clear();
				((Mode3MSHR) this.gpuMemorySystem.getConstantCache().missStatusHoldingRegister).clear();
				((Mode3MSHR) MemorySystem.cacheNameMappings.get("L2[0]").missStatusHoldingRegister).clear();
			}
		}
	}
	
	public void allocate()
	{
		this.WarpTable=new GenericCircularQueue<Warp>(Warp.class, 400000);
		this.Warp_write=new GenericCircularQueue<Warp>(Warp.class, 400000);
		this.m_scoreboard=new Scoreboard(0, 400000);
		this.scheduleUnit.completeWarpPipeline=new GenericCircularQueue<Instruction>(Instruction.class, 4000000);
	}
	
	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		EnergyConfig totalPower = new EnergyConfig(0, 0);
		
		EnergyConfig regFilePower =  getWriteBackUnitIn().calculateAndPrintEnergy(outputFileWriter, componentName + ".regFile");
		totalPower.add(totalPower, regFilePower);
		
		EnergyConfig fuPower =  getExecutionCore().calculateAndPrintEnergy(outputFileWriter, componentName + ".FuncUnit");
		totalPower.add(totalPower, fuPower);
		
		EnergyConfig resultsBroadcastBusPower =  getExecUnitIn().calculateAndPrintEnergy(outputFileWriter, componentName + ".resultsBroadcastBus");
		totalPower.add(totalPower, resultsBroadcastBusPower);
		
		totalPower.printEnergyStats(outputFileWriter, componentName + ".total");
		
		return totalPower;
	}
	
	private OperandCollector getWriteBackUnitIn() {
		return OperandCollector;
	}
	
	private ScheduleUnit getExecutionCore() {
		// both decode and execution power is given here
		return scheduleUnit;
	}
	
	private ExecuteUnit getExecUnitIn() {
		return executeUnit;
	}

}
