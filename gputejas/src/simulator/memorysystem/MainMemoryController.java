package memorysystem;
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
import generic.EventQueue;
import generic.PortType;
import generic.SimulationElement;
import generic.Event;
import generic.RequestType;

public class MainMemoryController extends SimulationElement
{
	public int numberOfMemoryControllers;
	public int[] mainmemoryControllersLocations;
	long numAccesses;
		
	public MainMemoryController() {
		super(PortType.Unlimited,
				-1, 
				10,
				250);
	}
	
	public MainMemoryController(int[] memoryControllersLocations) 
	{
		super(PortType.Unlimited,
				-1, 
				10,
				250);
		this.numberOfMemoryControllers = memoryControllersLocations.length;
		this.mainmemoryControllersLocations = memoryControllersLocations;
	}
	
	public void handleEvent(EventQueue eventQ, Event event)
	{
		if (event.getRequestType() == RequestType.Main_Mem_Read)
		{
			event.getRequestingElement().getPort().put(
						event.update(
								eventQ,
								2,//wire delay from main memory to cache
								null,
								event.getRequestingElement(),
								RequestType.Mem_Response));
		}
		else if (event.getRequestType() == RequestType.Main_Mem_Write)
		{
			//Just to tell the requesting things that the write is completed
		}
		
		incrementNumAccesses();
	}
	
	void incrementNumAccesses()
	{
		numAccesses += 1;
	}
}
