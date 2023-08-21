package main;
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


import config.SystemConfig;
import config.TpcConfig;
import memorysystem.SMMemorySystem;
import memorysystem.MemorySystem;
import generic.SM;


public class ArchitecturalComponent {

	private static SM[][] cores;
	public static SM[][] initCores()
	{
		System.out.println("initializing cores...");
		
		
		SM[][] sms = new SM[SystemConfig.NoOfTPC][TpcConfig.NoOfSM];
		for (int i=0; i<SystemConfig.NoOfTPC; i++)
		{
			for(int j =0; j<TpcConfig.NoOfSM; j++)
			{
				sms[i][j] = new SM(i, j);
			}
		
		}
		return sms;
	}

	public static SM[][] getCores() {
		return cores;
	}

	public static void setCores(SM[][] cores) {
		ArchitecturalComponent.cores = cores;
	}

	public static long getNoOfInstsExecuted()
	{
		long noOfInstsExecuted = 0;
		for(int i = 0; i < ArchitecturalComponent.getCores().length; i++)
		{
			for(int j =0 ; j<ArchitecturalComponent.getCores()[i].length ; j++ )
			{
				noOfInstsExecuted += ArchitecturalComponent.getCores()[i][j].getNoOfInstructionsExecuted();
			}
			
		}
		return noOfInstsExecuted;
	}

		
	private static SMMemorySystem[][] coreMemSysArray;
	public static SMMemorySystem[][] getCoreMemSysArray()
	{
		return coreMemSysArray;
	}

	public static void initMemorySystem(SM[][] sms) {
		coreMemSysArray = MemorySystem.initializeMemSys(ArchitecturalComponent.getCores());		
	}
}
