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

package emulatorinterface;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.util.Hashtable;

import config.SimulationConfig;

import main.ArchitecturalComponent;
import main.Main;
import pipeline.GPUpipeline;
import emulatorinterface.communication.Packet;
import emulatorinterface.communication.filePacket.SimplerFilePacket;
import emulatorinterface.translator.x86.instruction.InstructionClass;
import emulatorinterface.translator.x86.objparser.ObjParser;
import generic.*;


import jsr166y.Phaser;

/* MaxNumThreads threads are created from this class. Each thread
 * continuously keeps reading from the shared memory segment according
 * to its index(taken care in the jni C file).
 */
public class SimplerRunnableThread implements Runnable {
	
	int currBlock = 0;

	public long pipe_time = 0;
	public long mem_time = 0;
	public long barier_wait=0;
	public long phaser_wait=0;
	public boolean mem_flag = false;
	
	public int javaTid;
	public ObjParser myParser;
	public int TOTALBLOCKS, blocksExecuted=0;
	FileInputStream fos = null;
	ObjectInputStream is = null;
	
	public Hashtable<Long, InstructionClass> kernelInstructionsTable;
	public void initialize() throws IOException
	{
		currBlock = 0;
		blocksExecuted=0;
		epochCount=0;
		TOTALBLOCKS=0;
		if(ipcBase.kernelLeft())
		{	
			TOTALBLOCKS= Main.totalBlocks[ipcBase.kernelExecuted] / SimulationConfig.MaxNumJavaThreads;
			if(javaTid < (Main.totalBlocks[ipcBase.kernelExecuted] % SimulationConfig.MaxNumJavaThreads))
			{
				TOTALBLOCKS++; 
			}
		}
		if(TOTALBLOCKS==0)
		{
			return;
		}
		blockState = new BlockState[TOTALBLOCKS];
		inputToPipeline.clear();
		kernelInstructionsTable=main.Main.kernelInstructionsTables[ipcBase.kernelExecuted];
		ipcBase.openNextFile(ipcBase.kernelExecuted);
		
		
		maxCoreAssign = 0; 
		for(int i=0; i<TOTALBLOCKS; i++)
		{
			blockState[i] = new BlockState();
		}

	}
	

	int maxCoreAssign = 0;      //the maximum core id assigned 	
	public BlockState[] blockState;

	GenericCircularQueue<Instruction> inputToPipeline;
	GPUpipeline pipelineInterfaces;
	SM assignedSM ;
	int assignedSP;
	public SimplerFilePacket ipcBase;

	void iFinished() {
			epochEnd.arriveAndDeregister();
	}


    final int ipcCount = 1000;
	
	public void run() {	
			try {
				main.Main.runners[javaTid].initialize();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			ipcBase.javaThreadStarted = true;
		boolean blockEndSeen=false;
		while(ipcBase.kernelLeft())
		{
			if(TOTALBLOCKS==0){
				ipcBase.kernelExecuted++;
				try {
					main.Main.runners[javaTid].initialize();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			}
			
			// create pool for emulator packets
			CircularPacketQueue fromEmulator = new CircularPacketQueue(ipcCount);
			boolean allover = false;
			BlockState threadParam;
			
			Packet pnew = new Packet();
			while(true)
			{
				threadParam = blockState[currBlock];
				int numReads = 0;

				if(threadParam.isFirstPacket)
				{
					pipelineInterfaces.setBlockId(currBlock);
					if(pipelineInterfaces.isExecutionComplete() == true)
					{
						pipelineInterfaces.setExecutionComplete(false);
					}
				}
				// add outstanding micro-operations to input to pipeline
				if (threadParam.outstandingMicroOps.isEmpty() == false) {
					if(threadParam.outstandingMicroOps.size()<inputToPipeline.spaceLeft()) {
						while(threadParam.outstandingMicroOps.isEmpty() == false) {
							Instruction tmp=threadParam.outstandingMicroOps.pollFirst();
							tmp.setBlockID(currBlock);
							inputToPipeline.enqueue(tmp);
						}
					} else {
						// there is no space in pipelineBuffer. So don't fetch any more instructions
						continue;
					}
				}
				
				numReads = ipcBase.fetchManyPackets(this, currBlock, fromEmulator, threadParam);
				if (fromEmulator.size() == 0) {
					continue;
				}
				
				// update the number of read packets
				threadParam.totalRead += numReads;
				
				// If java thread itself is terminated then break out from this
				// for loop. also update the variable all-over so that I can
				// break from the outer while loop also.
				if (ipcBase.javaThreadTermination == true) {
					allover = true;
					break;
				}
				
				threadParam.checkStarted();
				InstructionClass type;
				// Process all the packets read from the communication channel
				while(fromEmulator.isEmpty() == false) {
					pnew = fromEmulator.dequeue();
					
					type=pnew.insClass;
					if(type==InstructionClass.TYPE_BLOCK_END)
					{
						Instruction tmp=Instruction.getInvalidInstruction();
						tmp.setBlockID(currBlock);
						this.inputToPipeline.enqueue(tmp);
						threadParam.finished=true;
						threadParam.isFirstPacket=true;
						blockEndSeen=true;	
					}
					if(type==InstructionClass.TYPE_KERNEL_END)
					{
						System.out.println("EOF from Java "+javaTid+" with totblock :"+TOTALBLOCKS);
						continue;
						
						
					}
					
					boolean ret = processPacket(threadParam, pnew, currBlock, assignedSP);
					if(ret==false) {
						// There is not enough space in pipeline buffer. 
						// So don't process any more packets.
						break;
					}
				}
				runPipelines(assignedSP);
				if(blockEndSeen)
				{
					currBlock=(currBlock+1);
					if(currBlock==TOTALBLOCKS){allover=true;currBlock--;}
					blockEndSeen=false;
				}
				if (ipcBase.javaThreadTermination == true) {  //check if java thread is finished
					allover = true;
					break;
				}
				if(allover) {
					ipcBase.javaThreadTermination = true;
					break;
			    }
			}
			finishAllPipelines(assignedSP);
			Statistics.calculateCyclesKernel(javaTid);
			ipcBase.kernelExecuted++;
			if(ipcBase.kernelExecuted % 10 == 0)
				System.out.println("Simulated " + ipcBase.kernelExecuted + " kernels on " + javaTid);
			ipcBase.javaThreadTermination = false;
			
			blocksExecuted+=currBlock+1;
			
			
				try {
					main.Main.runners[javaTid].initialize();
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
		ipcBase.javaThreadTermination = true;
		iFinished();
		ipcBase.free.release();
		return;
	}
	

	public Thread t;
    public Phaser kernelEnd, epochEnd;
	// initialise a reader thread with the correct thread id and the buffer to
	// write the results in.
	@SuppressWarnings("unchecked")
	public SimplerRunnableThread(String threadName, int javaTid, SimplerFilePacket ipcBase, 
			SM sm, Phaser epochEnd) {

		this.ipcBase = ipcBase;		
		this.javaTid = javaTid;
	    myParser = new ObjParser();
		this.epochEnd=epochEnd;
		System.out.println("--  starting java thread"+this.javaTid);
		inputToPipeline = new GenericCircularQueue<Instruction>(Instruction.class, 4000000);
		assignedSM=sm;
		assignedSP=0;
		pipelineInterfaces=assignedSM.getPipelineInterface(assignedSP);
		pipelineInterfaces.setcoreStepSize(assignedSM.getStepSize());
		GenericCircularQueue<Instruction>[] toBeSet =(GenericCircularQueue<Instruction>[])Array.newInstance(GenericCircularQueue.class, 1);
		toBeSet[0] = inputToPipeline;
		pipelineInterfaces.setInputToPipeline(toBeSet);
		if(pipelineInterfaces.isExecutionComplete() == true)
		{
			pipelineInterfaces.setExecutionComplete(false);
		}
		// Special case must be made for RunnableFromFile
		if(this.ipcBase != null) {
			t=(new Thread(this, threadName));
		}

	}

	protected void runPipelines(int assignedSP) {
		int minN = Integer.MAX_VALUE;
		 if(maxCoreAssign>0) {
			
			int n = inputToPipeline.size(); 
					
			if (n < minN && n != 0)
				minN = n;
		}
		minN = (minN == Integer.MAX_VALUE) ? 0 : minN;

		for (int i1 = 0; i1 < minN; i1++) {
					pipelineInterfaces.oneCycleOperation(assignedSP);
					AddToSetAndIncrementClock();
			
		}
		
		}


		

	
	private void AddToSetAndIncrementClock() {

		assignedSM.clock.incrementClock();
		blockState[currBlock].tot_cycles++;
		
		epochCount++;
	if (epochCount % main.Main.SynchClockDomainsCycles ==0) {
		
		int phase = epochEnd.getPhase(); 
		if (phase==previousPhase) {
			long g=System.currentTimeMillis();
			epochEnd.awaitAdvance(phase);
		phaser_wait+=(System.currentTimeMillis()-g);
			}
			
			previousPhase = epochEnd.arrive();
			
			
		}

		
	}

	public int epochCount,previousPhase=-1;
	@SuppressWarnings("unused")
	protected boolean processPacket(BlockState thread, Packet pnew, int Blocktid, int assigned_SP) {

		
		boolean isSpaceInPipelineBuffer = true;
		
		int GlobalId = javaTid * TOTALBLOCKS + Blocktid;
	
		if (thread.isFirstPacket) 
		{
			
			this.pipelineInterfaces.getCore().getExecEngine(assigned_SP).setExecutionComplete(false);
			
			if(Blocktid>=maxCoreAssign)
				maxCoreAssign = Blocktid+1;

			thread.packetList.add(pnew);
			thread.isFirstPacket=false;
			return isSpaceInPipelineBuffer;
			
		}
		else 
		{
			
			int oldLength = inputToPipeline.size();
			
			long numHandledInsn = 0;
			int numMicroOpsBefore = thread.outstandingMicroOps.size();
			
			myParser.fuseInstruction( thread.packetList.get(0).ip, 
				thread.packetList, thread.outstandingMicroOps);
			int numMicroOpsAfter = thread.outstandingMicroOps.size();

			if(numMicroOpsAfter>numMicroOpsBefore) {
				numHandledInsn = 1;
			} else {
				numHandledInsn = 0;
				
			}

			for(int i=numMicroOpsBefore; i<numMicroOpsAfter; i++) {
				if(i==numMicroOpsBefore) {
					thread.outstandingMicroOps.peek(i).setCISCProgramCounter(thread.packetList.get(0).ip);
				} else {
					thread.outstandingMicroOps.peek(i).setCISCProgramCounter(-1);
				}
				
			}
			
			// Either add all outstanding micro-ops or none.
			if(thread.outstandingMicroOps.size()<this.inputToPipeline.spaceLeft()) {
				// add outstanding micro-operations to input to pipeline
				while(thread.outstandingMicroOps.isEmpty() == false) {
					Instruction tmp=thread.outstandingMicroOps.dequeue();
					tmp.setBlockID(Blocktid);
					this.inputToPipeline.enqueue(tmp);
				}
			} else {
				isSpaceInPipelineBuffer = false;
			}
				

			
			thread.packetList.clear();
			thread.packetList.add(pnew);
			int newLength = inputToPipeline.size();
		}

		return isSpaceInPipelineBuffer;
	}

	
	
	@SuppressWarnings("unused")
	public void finishAllPipelines(int assigned_SP) {

		boolean queueComplete;    //queueComplete is true when all cores have completed
		while(true)
		{
			
			int count=0;
			queueComplete = true;        
			
			if(maxCoreAssign>0) {
				
				
						pipelineInterfaces.oneCycleOperation(assigned_SP);
						AddToSetAndIncrementClock();
						ArchitecturalComponent.getCores()[pipelineInterfaces.getCore().getTPC_number()][pipelineInterfaces.getCore().getSM_number()].clock.incrementClock();
				
				
			}
			if(inputToPipeline.size()==0)
			{
				break;
			}
			
			
		}	
		
	}
}