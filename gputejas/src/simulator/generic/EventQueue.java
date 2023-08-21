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

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

import main.ArchitecturalComponent;
import dram.MainMemoryDRAMController;

import main.ArchitecturalComponent;
import memorysystem.Cache;
import memorysystem.MemorySystem;

public class EventQueue
{
	private PriorityBlockingQueue<Event> priorityQueue;
	int tpcId, smId, spId;
	public EventQueue(int tpcId, int smId, int spId)
	{
		this.tpcId=tpcId;
		this.smId=smId;
		this.spId=spId;
		priorityQueue = new PriorityBlockingQueue<Event>(1, new EventComparator());
	}
	
	public void addEvent(Event event)
	{
		if(priorityQueue.add(event) == false) {
			Event newEvent = event.clone();
			priorityQueue.add(newEvent);
		}
		/*System.out.println("---");
		System.out.println(event.getRequestType());
		System.out.println(event.getRequestingElement());
		System.out.println(event.getProcessingElement());*/
	}
	
	public void processEvents()
	{
		long currentClockTime = ArchitecturalComponent.getCores()[tpcId][smId][spId].clock.getCurrentTime();
		try {
			while(!priorityQueue.isEmpty())
			{
				long eventTime = priorityQueue.peek().getEventTime();
				if (eventTime <= currentClockTime)
				{
					Event event = priorityQueue.remove();
					event.getProcessingElement().handleEvent(this, event);
				}
				else
				{
					break;
				}
			}
		} catch (Exception e) {

		}
	}
	
	public boolean isEmpty()
	{
		return priorityQueue.isEmpty();
	}
	
	public void clear()
	{
		priorityQueue.clear();
	}
	
	public void dump()
	{
		System.out.println("------------------------------------------------------------------------------------");
		System.out.println("event queue size = " + priorityQueue.size());
		Iterator<Event> iterator = priorityQueue.iterator();
		while(iterator.hasNext())
		{
			Event event = iterator.next();			
			event.dump();
		}
		System.out.println("------------------------------------------------------------------------------------");
	}
}