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
import generic.SP;
import generic.Event;
import generic.EventQueue;
import generic.RequestType;
import memorysystem.AddressCarryingEvent;
import memorysystem.Cache;
import memorysystem.SPMemorySystem;

public class GPUMemorySystem extends SPMemorySystem {
	
	GPUExecutionEngine containingExecEngine;
	public int numOfLoads=0;
	public long numOfStores;
	
	public GPUMemorySystem(SP sp, Cache cnCache, Cache shCache, Cache dtCache)
	{
		super(sp, cnCache, shCache, dtCache);
		sp.getExecEngine().setCoreMemorySystem(this);
		containingExecEngine = (GPUExecutionEngine)sp.getExecEngine();
	}

	@SuppressWarnings("unused")
	public void issueRequestToInstrCache(long address)
	{
		GPUpipeline pipeline = (GPUpipeline)sp.getPipelineInterface();

		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getSP().getEventQueue(),
				 iCache.getLatencyDelay(), this, iCache, RequestType.Cache_Read, address, sp.getTPC_number(), sp.getSM_number(), sp.getSP_number());

		AddressCarryingEvent clone = (AddressCarryingEvent) addressEvent.clone();
		boolean isAddedinLowerMshr = this.iCache.addEvent(clone);
		if(!isAddedinLowerMshr)
		{
			misc.Error.showErrorAndExit("Unable to add event to iCache's MSHR !!" + 
					"\nevent = " + addressEvent + 
					"\niCache = " + this.iCache);
		}
		
	}
	
	@SuppressWarnings("unused")
	@Override
	public boolean issueRequestToConstantCache(RequestType requestType, long address) throws Exception{
		GPUpipeline inorderPipeline = (GPUpipeline)sp.getPipelineInterface();
		
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getSP().getEventQueue(),
																	 constantCache.getLatencyDelay(),
																	 this, 
																	 constantCache,
																	 requestType, 
																	 address,
																	 sp.getTPC_number(),
																	 sp.getSM_number(),
																	 sp.getSP_number());
		
		AddressCarryingEvent clone = (AddressCarryingEvent) addressEvent.clone();
		boolean isAddedinLowerMshr = this.constantCache.addEvent(clone);
		if(!isAddedinLowerMshr)
		{
			misc.Error.showErrorAndExit("Unable to add event to constant Cache's MSHR !!" + 
					"\nevent = " + addressEvent + 
					"\niCache = " + this.iCache);
		}
		
		containingExecEngine.updateNoOfMemRequests(1);
		if(requestType == RequestType.Cache_Read)
		{
			containingExecEngine.updateNoOfLd(1);
		}
		else if(requestType == RequestType.Cache_Write)
		{
			containingExecEngine.updateNoOfSt(1);
		}
		
		return true;
	}

	@SuppressWarnings("unused")
	@Override
	public boolean issueRequestToSharedCache(RequestType requestType, long address) {
		GPUpipeline inorderPipeline = (GPUpipeline)sp.getPipelineInterface();
		
		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getSP().getEventQueue(),
																	 sharedCache.getLatencyDelay(),
																	 this, sharedCache, requestType, address,
																	 sp.getTPC_number(), sp.getSM_number(), sp.getSP_number());
		
		AddressCarryingEvent clone = (AddressCarryingEvent) addressEvent.clone();
		boolean isAddedinLowerMshr = this.sharedCache.addEvent(clone);
		if(!isAddedinLowerMshr)
		{
			misc.Error.showErrorAndExit("Unable to add event to shared Cache's MSHR !!" + 
					"\nevent = " + addressEvent + 
					"\niCache = " + this.iCache);
		}
		
		containingExecEngine.updateNoOfMemRequests(1);
		if(requestType == RequestType.Cache_Read)
		{
			containingExecEngine.updateNoOfLd(1);
		}
		else if(requestType == RequestType.Cache_Write)
		{
			containingExecEngine.updateNoOfSt(1);
		}
		
		return true;
	}
	
	@SuppressWarnings("unused")
	public boolean issueRequestToDataCache(RequestType requestType, long address)
	{
		GPUpipeline inorderPipeline = (GPUpipeline)sp.getPipelineInterface();

		AddressCarryingEvent addressEvent = new AddressCarryingEvent(getSP().getEventQueue(),
																	 dCache.getLatencyDelay(),
																	 this, dCache, requestType, address,
																	 sp.getTPC_number(), sp.getSM_number(), sp.getSP_number());
		
		AddressCarryingEvent clone = (AddressCarryingEvent) addressEvent.clone();
		boolean isAddedinLowerMshr = this.dCache.addEvent(clone);
		if(!isAddedinLowerMshr)
		{
			misc.Error.showErrorAndExit("Unable to add event to shared Cache's MSHR !!" + 
					"\nevent = " + addressEvent + 
					"\niCache = " + this.dCache);
		}
		
		containingExecEngine.updateNoOfMemRequests(1);
		if(requestType == RequestType.Cache_Read)
		{
			containingExecEngine.updateNoOfLd(1);
		}
		else if(requestType == RequestType.Cache_Write)
		{
			containingExecEngine.updateNoOfSt(1);
		}
		
		return true;
	}
	
	@Override
	public void handleEvent(EventQueue eventQ, Event event) {
		
		//handle memory response
		AddressCarryingEvent memResponse = (AddressCarryingEvent) event;
		long address = memResponse.getAddress();
		
		if(memResponse.getRequestingElement() == iCache)
		{
			containingExecEngine.getScheduleUnit().processCompletionOfMemRequest(address);
		}
		else if(memResponse.getRequestingElement() == constantCache)
		{
			containingExecEngine.getExecuteUnit().processCompletionOfMemRequest(address);
		}
		
		else if(memResponse.getRequestingElement() == sharedCache)
		{
			containingExecEngine.getExecuteUnit().processCompletionOfMemRequest(address);
		}
		
		else if (memResponse.getRequestingElement() == dCache)
		{
			containingExecEngine.getExecuteUnit().processCompletionOfMemRequest(address);
		}
		
		else
		{
			System.out.println("mem response received by inordercoreMemSys from unkown object : " + memResponse.getRequestingElement());
		}
	}
	
}
