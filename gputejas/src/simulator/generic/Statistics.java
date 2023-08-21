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

//import emulatorinterface.translator.qemuTranslationCache.TranslatedInstructionCache;
import memorysystem.MemorySystem;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import main.ArchitecturalComponent;
import main.Main;
import memorysystem.Cache;
import config.EnergyConfig;

import config.SimulationConfig;
import config.SmConfig;
import config.SpConfig;
import config.SystemConfig;
import config.TpcConfig;
import config.SystemConfig.Interconnect;
import dram.MainMemoryDRAMController;
import memorysystem.directory.*;
import net.NOC.CONNECTIONTYPE;

public class Statistics {
	
	
	static FileWriter outputFileWriter;
	
	static float[] weightsArray;
	static int currentSlice;
	static int repetitions = Math.max(1, SimulationConfig.ThreadsPerCTA / SpConfig.NoOfThreadsSupported);
	
	static String benchmark;
	
	static long[][] spCycles;
	public static void printSystemConfig()
	{
		//read config.xml and write to output file
		try
		{
			outputFileWriter.write("[Configuration]\n");
			outputFileWriter.write("\n");
			outputFileWriter.write("EmulatorType: OCELOT\n");
			
			outputFileWriter.write("Schedule: " + (new Date()).toString() + "\n");
			
			if (SystemConfig.interconnect == Interconnect.Noc)
			{
				if(SystemConfig.nocConfig.ConnType == CONNECTIONTYPE.OPTICAL)
				{
					outputFileWriter.write("\n\nNOC Technology\t\t=\t" + SystemConfig.nocConfig.ConnType + "\n");
					outputFileWriter.write("\n\nLaser ON\t\t=\t" +ArchitecturalComponent.laseron +"\n");
				}
				else
				{
					outputFileWriter.write("\n\nNOC Technology\t\t=\t" + SystemConfig.nocConfig.ConnType + "\n");
					outputFileWriter.write("\n\nNOC Topology\t\t=\t" + SystemConfig.nocConfig.topology + "\n");
					outputFileWriter.write("NOC Routing Algorithm\t=\t" + SystemConfig.nocConfig.rAlgo + "\n");		
				}
			}
//					
			if(SystemConfig.memControllerToUse==true){							
			outputFileWriter.write("\n[RAM statistics]\n\n");
			long totalReadAndWrite= 0L;
			long totalReadRank= 0L;
			long totalWriteRank= 0L;
			long totalReadTransactions[][];
			long totalWriteTransactions[][];
			double avgLatency;
			long maxCoreCycles = 0;
			

			for(int k=0; k < SystemConfig.mainMemoryConfig.numChans; k++)
			{
				outputFileWriter.write("For channel " + k + ":\n");
				avgLatency = ArchitecturalComponent.getMainMemoryDRAMController(null,k).getAverageLatency();
				outputFileWriter.write("Average Read Latency: " + avgLatency + " cycles = " + (avgLatency/SmConfig.frequency* 1000) + " ns\n");
				totalReadTransactions = ArchitecturalComponent.getMainMemoryDRAMController(null,k).getTotalReadTransactions();
				totalWriteTransactions = ArchitecturalComponent.getMainMemoryDRAMController(null,k).getTotalWriteTransactions();
				totalReadAndWrite=0L;
//		
				for(int i=0;i<SystemConfig.mainMemoryConfig.numRanks;i++){
					
					outputFileWriter.write("\t Rank "+(i+1)+"\n");
					
					for(int j=0;j<SystemConfig.mainMemoryConfig.numBanks;j++){
						outputFileWriter.write("\t\t Bank "+(j+1)+" :: ");
						outputFileWriter.write(" Reads : " +totalReadTransactions[i][j] + " | Writes: "+totalWriteTransactions[i][j] +"\n\n");
						totalReadAndWrite += totalReadTransactions[i][j] + totalWriteTransactions[i][j];
						totalReadRank += totalReadTransactions[i][j];
						totalWriteRank += totalWriteTransactions[i][j];
						}

					outputFileWriter.write("\t Total Reads: " + totalReadRank);
					outputFileWriter.write("\t Total Writes: " + totalWriteRank + "\n");
					totalReadRank = 0L;
					totalWriteRank = 0L;

				}
				outputFileWriter.write("\nTotal Reads and Writes: " + (totalReadAndWrite*64) + " Bytes\n");
			//	outputFileWriter.write("Total Bandwidth: "+ (totalReadAndWrite*64)/(double)(maxCoreCycles/SystemConfig.mainMemoryConfig.sm_ram_ratio * SystemConfig.mainMemoryConfig.tCK)/(1024*1024*1024) * 1000000000 + " GB/s\n");
				outputFileWriter.write("\n");
			}
		
			}
		outputFileWriter.write("\n");
			
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	synchronized public static void calculateCyclesKernel(int javaTid)
	{
		try{
			/*int lastIndex = javaTid % 16;
			for(int i = 0; i < Main.runners[javaTid].TOTALBLOCKS ; i++)
			{      
				spCycles[Main.runners[javaTid].ipcBase.kernelExecuted][lastIndex] += Main.runners[javaTid].blockState[i].tot_cycles;
				lastIndex = ( lastIndex + SimulationConfig.MaxNumJavaThreads ) % 16;
			}*/
			for(int i = 0; i < Main.runners[javaTid].TOTALBLOCKS ; i++)
			{
				spCycles[Main.runners[javaTid].ipcBase.kernelExecuted][Main.runners[javaTid].ipcBase.coreid] += Main.runners[javaTid].blockState[i].tot_cycles;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		
	}

	public static void printCycleStatistics()
	{
		long total_cycles = 0;
		for(int i = 0; i < Main.totalNumKernels ; i++)
		{
			long kernelCycles = 0;
			//for(int j =0; j < 16; j++)
			for(int j =0; j < Main.totalNumCores; j++)
			{
				if(spCycles[i][j] > kernelCycles)
				{
					kernelCycles = spCycles[i][j];
				}
			}
			total_cycles += kernelCycles;
		}
		System.out.println("****************************************************************************");
		System.out.println("TOTAL INSTRUCTIONS = " + total_ins * repetitions);
		System.out.println("TOTAL CYCLES = " + total_cycles);
		System.out.println("TOTAL INSTRUCTIONS PER CYCLE = " + (double)(total_ins)/total_cycles);
		System.out.println("****************************************************************************");
		
		try {
			outputFileWriter.write("****************************************************************************\n");
			outputFileWriter.write("TOTAL INSTRUCTIONS = " + total_ins * repetitions + "\n");
			outputFileWriter.write("TOTAL CYCLES = " + total_cycles + "\n");
			outputFileWriter.write("TOTAL INSTRUCTIONS PER CYCLE = " + (double)(total_ins)/total_cycles + "\n");
			outputFileWriter.write("**************************************************************************** \n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	static void printEnergyStatistics()
	{
		EnergyConfig totalEnergy = new EnergyConfig(0, 0);
		SP[][][] sps = ArchitecturalComponent.getCores();
		try {
			
			// Cores
			outputFileWriter.write("\n\n[ComponentName LeakageEnergy DynamicEnergy TotalEnergy NumDynamicAccesses] : \n");

			EnergyConfig coreEnergy = new EnergyConfig(0, 0);

			for(int i = 0; i< SystemConfig.NoOfTPC; i++)
			{
				for(int j = 0; j < TpcConfig.NoOfSM; j++)
				{
					SP core=sps[i][j][0];
					coreEnergy.add(core.calculateAndPrintSharedCacheEnergy(outputFileWriter, "core["+i+"]["+j+"]"));
					for(int k = 0; k < SmConfig.NoOfSP; k++)
					{
						core=sps[i][j][k];
						coreEnergy.add(core.calculateAndPrintEnergy(outputFileWriter, "core["+i+"]["+j+"]["+k+"]"));
					}	
				}
			}
			outputFileWriter.write("\n\n");
			coreEnergy.printEnergyStats(outputFileWriter, "coreEnergy.total");
			totalEnergy.add(coreEnergy);
			
			outputFileWriter.write("\n\n");
			
			// Shared Cache
			EnergyConfig sharedCacheEnergy = new EnergyConfig(0, 0);
			for (Cache cache : ArchitecturalComponent.getSharedCacheList()) {
				if (cache.cacheName.equals("L2")) //"L2[cachebank.size]" is the cache being used
					continue;
				sharedCacheEnergy.add(cache.calculateAndPrintEnergy(outputFileWriter, cache.cacheName));
			}
			
			outputFileWriter.write("\n\n");
			sharedCacheEnergy.printEnergyStats(outputFileWriter, "sharedCacheEnergy.total");
			totalEnergy.add(sharedCacheEnergy);
						
			// Main Memory
			EnergyConfig mainMemoryEnergy = new EnergyConfig(0, 0);						
			int memControllerId = 0;
			outputFileWriter.write("\n\n");
			for(MainMemoryDRAMController memController : ArchitecturalComponent.memoryControllers) {
				String name = "MainMemoryDRAMController[" + memControllerId + "]";
				memControllerId++;
				mainMemoryEnergy.add(memController.calculateAndPrintEnergy(outputFileWriter, name));
			}
			
			outputFileWriter.write("\n\n");
			mainMemoryEnergy.printEnergyStats(outputFileWriter, "mainMemoryControllerEnergy.total");
			totalEnergy.add(mainMemoryEnergy);
			
			// Coherence
//			EnergyConfig coherenceEnergy = new EnergyConfig(0, 0);						
//			int coherenceId = 0;
			// TODO Directory Cache Power Estimation
//			for(Coherence coherence : ArchitecturalComponent.coherences) {
//				String name = "Coherence[" + coherenceId + "]";
//				coherenceId++;
//				coherenceEnergy.add(coherence.calculateAndPrintEnergy(outputFileWriter, name));
//			}
//			
//			outputFileWriter.write("\n\n");
//			coherenceEnergy.printEnergyStats(outputFileWriter, "coherenceEnergy.total");
//			totalEnergy.add(coherenceEnergy);
			
			// Interconnect
			EnergyConfig interconnectEnergy = new EnergyConfig(0, 0);
			interconnectEnergy.add(ArchitecturalComponent.getInterConnect().calculateAndPrintEnergy(outputFileWriter, "Interconnect"));
			totalEnergy.add(interconnectEnergy);
			
			outputFileWriter.write("\n\n");
			totalEnergy.printEnergyStats(outputFileWriter, "TotalEnergy");
			
			
			}
		catch (Exception e) {
			System.err.println("error in printing stats + \nexception = " + e);
			e.printStackTrace();
		}
	}
	
	public static void printMemorySystemStatistics()
	{
	
		try
		{
			outputFileWriter.write("\n");
			outputFileWriter.write("[Memory System Statistics]\n");
			outputFileWriter.write("\n");
			
			outputFileWriter.write("[Per core statistics]\n");
			outputFileWriter.write("\n");
			
			long tot_icache_access = 0;
			long tot_icache_misses= 0;
			
			
			long tot_constantcache_access = 0;
			long tot_constantcache_misses = 0;
			
			
			long tot_sharedcache_access = 0;
			long tot_sharedcache_misses = 0;
			

			long tot_dcache_access = 0;
			long tot_dcache_misses = 0;
			
			Cache L2 = MemorySystem.cacheNameMappings.get("L2[0]");
			long tot_L2cache_access = L2.noOfAccesses;
			long tot_L2cache_misses = L2.misses;
			
			
			//printing cache details 
			SP[][][] sps = new SP[SystemConfig.NoOfTPC][TpcConfig.NoOfSM][SmConfig.NoOfSP];
			sps = ArchitecturalComponent.getCores();
			for(int i =0 ; i< SystemConfig.NoOfTPC; i++)
			{
				for(int j = 0; j < TpcConfig.NoOfSM; j++)
				{
					
					if (SimulationConfig.GPUType == GpuType.Ampere) {
						outputFileWriter.write("SM : "+j+" of TPC : "+i+"\n");
						outputFileWriter.write("L1Cache details"+"\n");
						outputFileWriter.write("NO OF REQUESTS " +sps[i][j][0].getExecEngine().gpuMemorySystem.getDataCache().noOfRequests+"\n");
						tot_dcache_access += sps[i][j][0].getExecEngine().gpuMemorySystem.getDataCache().noOfRequests;
						outputFileWriter.write("NO OF HITS " +sps[i][j][0].getExecEngine().gpuMemorySystem.getDataCache().hits+"\n");
						outputFileWriter.write("NO OF MISSES " +sps[i][j][0].getExecEngine().gpuMemorySystem.getDataCache().misses+"\n");
						tot_dcache_misses += sps[i][j][0].getExecEngine().gpuMemorySystem.getDataCache().misses;
					}
					
					outputFileWriter.write("constantCache details"+"\n");
					outputFileWriter.write("NO OF REQUESTS " +sps[i][j][0].getExecEngine().gpuMemorySystem.getConstantCache().noOfRequests+"\n");
					tot_constantcache_access += sps[i][j][0].getExecEngine().gpuMemorySystem.getConstantCache().noOfRequests;
					outputFileWriter.write("NO OF HITS " +sps[i][j][0].getExecEngine().gpuMemorySystem.getConstantCache().hits+"\n");
					outputFileWriter.write("NO OF MISSES " +sps[i][j][0].getExecEngine().gpuMemorySystem.getConstantCache().misses+"\n");
					tot_constantcache_misses += sps[i][j][0].getExecEngine().gpuMemorySystem.getConstantCache().misses;
					
					outputFileWriter.write("sharedCache details"+"\n");
					outputFileWriter.write("NO OF REQUESTS " +sps[i][j][0].getExecEngine().gpuMemorySystem.getSharedCache().noOfRequests+"\n");
					tot_sharedcache_access += sps[i][j][0].getExecEngine().gpuMemorySystem.getSharedCache().noOfRequests;
					outputFileWriter.write("NO OF HITS " +sps[i][j][0].getExecEngine().gpuMemorySystem.getSharedCache().hits+"\n");
					outputFileWriter.write("NO OF MISSES " +sps[i][j][0].getExecEngine().gpuMemorySystem.getSharedCache().misses+"\n\n");
					tot_sharedcache_misses = sps[i][j][0].getExecEngine().gpuMemorySystem.getSharedCache().misses ;
				
					for(int k = 0; k < SmConfig.NoOfSP; k++)
					{
						if(sps[i][j][k].getExecEngine().gpuMemorySystem.getiCache().noOfRequests == 0)
						{
							outputFileWriter.write("iCacheRequests are zero for SM "+j+" of TPC "+i+"\n");
//							continue;
						}
						outputFileWriter.write("SP : "+k+" of SM : "+j+" of TPC : "+i+"\n");
						outputFileWriter.write("iCache details"+"\n");
						outputFileWriter.write("NO OF REQUESTS " +sps[i][j][k].getExecEngine().gpuMemorySystem.getiCache().noOfRequests+"\n");
						tot_icache_access += sps[i][j][k].getExecEngine().gpuMemorySystem.getiCache().noOfRequests;
						outputFileWriter.write("NO OF HITS " +sps[i][j][k].getExecEngine().gpuMemorySystem.getiCache().hits+"\n");
						outputFileWriter.write("NO OF MISSES " +sps[i][j][k].getExecEngine().gpuMemorySystem.getiCache().misses+"\n");
						tot_icache_misses += sps[i][j][k].getExecEngine().gpuMemorySystem.getiCache().misses;
						
						if (SimulationConfig.GPUType != GpuType.Ampere) {
							outputFileWriter.write("dCache details"+"\n");
							outputFileWriter.write("NO OF REQUESTS " +sps[i][j][0].getExecEngine().gpuMemorySystem.getDataCache().noOfRequests+"\n");
							tot_dcache_access += sps[i][j][0].getExecEngine().gpuMemorySystem.getDataCache().noOfRequests;
							outputFileWriter.write("NO OF HITS " +sps[i][j][0].getExecEngine().gpuMemorySystem.getDataCache().hits+"\n");
							outputFileWriter.write("NO OF MISSES " +sps[i][j][0].getExecEngine().gpuMemorySystem.getDataCache().misses+"\n");
							tot_dcache_misses += sps[i][j][0].getExecEngine().gpuMemorySystem.getDataCache().misses;
						}
						
						outputFileWriter.write("\n");
					}
				}
			}
			outputFileWriter.write("****************************************************************************\n");
			outputFileWriter.write("Cache details\n");
			outputFileWriter.write("Total Instruction Cache Access " + tot_icache_access +"\n");
			outputFileWriter.write("Total Instruction Cache Misses " + tot_icache_misses+"\n");
			outputFileWriter.write("\n");
			
			if (SimulationConfig.GPUType == GpuType.Ampere) {
				outputFileWriter.write("Total L1 Cache Access " + tot_dcache_access + "\n");
				outputFileWriter.write("Total L1 Cache Misses " +  tot_dcache_misses + "\n");
				outputFileWriter.write("\n");
			} else {
				outputFileWriter.write("Total Data Cache Access " + tot_dcache_access + "\n");
				outputFileWriter.write("Total Data Cache Misses " +  tot_dcache_misses + "\n");
				outputFileWriter.write("\n");
			}
			
			outputFileWriter.write("Total L2 Cache Access " + tot_L2cache_access + "\n");
			outputFileWriter.write("Total L2 Cache Misses " +  tot_L2cache_misses + "\n");
			outputFileWriter.write("\n");
			
			outputFileWriter.write("Total Constant Cache Access " + tot_constantcache_access + "\n");
			outputFileWriter.write("Total Constant Cache Misses " +  tot_constantcache_misses + "\n");
			outputFileWriter.write("\n");
			
			outputFileWriter.write("Total Shared Cache Access " + tot_sharedcache_access + "\n");
			outputFileWriter.write("Total Shared Cache Misses " +  tot_sharedcache_misses + "\n");
			outputFileWriter.write("\n");
			outputFileWriter.write("****************************************************************************\n");
		
			
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	private static long simulationTime;

	
	static long total_ins = 0;
	public static void printSimulationTime()
	{
		//print time taken by simulator
		long seconds = simulationTime/1000;
		long minutes = seconds/60;
		seconds = seconds%60;
		
		
		
		for(int i =0; i <SimulationConfig.MaxNumJavaThreads;i++)
		{
			total_ins += Main.runners[i].ipcBase.ins;
		}
		
		try
		{
			outputFileWriter.write("\n");
			outputFileWriter.write("[Simulator Time]\n");
			
			outputFileWriter.write("Time Taken\t\t=\t" + minutes + " : " + seconds + " minutes\n");
			outputFileWriter.write("Total Instructions executed : "+total_ins * repetitions);
			
			outputFileWriter.write("\n");
			outputFileWriter.write("Instructions per Second\t=\t" + 
					(double)(total_ins * repetitions)/simulationTime + " KIPS\t\t");
						
			System.out.println("\n\nInstructions per Second\t=\t" + 
					(double)(total_ins * repetitions)/simulationTime + " KIPS\t\t");
			
			outputFileWriter.write("\n");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void initStatistics()
	{		
		spCycles = new long[Main.totalNumKernels][Main.totalNumCores]; //[16];
		for(int i = 0 ; i< Main.totalNumKernels ; i++)
		{
			//spCycles[i] = new long[16]; 
			//for(int j = 0; j < 16; j++)
			spCycles[i] = new long[Main.totalNumCores]; 
			for(int j = 0; j < Main.totalNumCores; j++)
			{
				spCycles[i][j] = 0;
			}
		}
	}	
		
	public static void openStream()
	{
		if(SimulationConfig.outputFileName == null)
		{
			SimulationConfig.outputFileName = "default";
		}
		
		try {
			File outputFile = new File(SimulationConfig.outputFileName);
			
			if(outputFile.exists()) {
				
				// rename the previous output file
				Date lastModifiedDate = new Date(outputFile.lastModified());
				File backupFile = new File(SimulationConfig.outputFileName + "_" + lastModifiedDate.toString());
				if(!outputFile.renameTo(backupFile)) {
					System.err.println("error in creating a backup of your previous output file !!\n");
				}
				
				// again point to the new file
				outputFile = new File(SimulationConfig.outputFileName);
			}
			
			outputFileWriter = new FileWriter(outputFile);
			
			
		} catch (IOException e) {
			
			StringBuilder sb = new StringBuilder();
			sb.append("DEFAULT_");
		    Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
			sb.append(sdf.format(cal.getTime()));
			try
			{
				outputFileWriter = new FileWriter(sb.toString());
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}
			System.out.println("unable to create specified output file");
			System.out.println("statistics written to " + sb.toString());
		}
	}
	
	public static void closeStream()
	{
		try
		{
			outputFileWriter.close();
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}


	public static void setSimulationTime(long simulationTime) {
		Statistics.simulationTime = simulationTime;
	}

	public static void setExecutable(String executableFile) {
		Statistics.benchmark = executableFile;
	}

	public static void printAllStatistics(String benchmarkName, 
			long startTime, long endTime) {
	
		Statistics.setExecutable(benchmarkName);
		Statistics.setSimulationTime(endTime - startTime);

		Statistics.openStream();
		Statistics.printSystemConfig();
		Statistics.printSimulationTime();
		Statistics.printCycleStatistics();
		Statistics.printMemorySystemStatistics();
		Statistics.printEnergyStatistics();
				
		
		Statistics.closeStream();
	}	
	
	static long totalNucaBankAccesses;
	
	public static String nocTopology;
	public static String nocRoutingAlgo;
	public static int hopcount=0;
		
	static float averageHopLength;
	static int maxHopLength;
	static int minHopLength;
	
	static long numInsWorkingSetHits[];
	static long numInsWorkingSetMisses[];
	static long maxInsWorkingSetSize[];
	static long minInsWorkingSetSize[];
	static long totalInsWorkingSetSize[];
	static long numInsWorkingSetsNoted[];
	
	static long numDataWorkingSetHits[];
	static long numDataWorkingSetMisses[];
	static long maxDataWorkingSetSize[];
	static long minDataWorkingSetSize[];
	static long totalDataWorkingSetSize[];
	static long numDataWorkingSetsNoted[];
	
	public static String formatDouble(double d)
	{
		return String.format("%.4f", d);
	}
	
}
