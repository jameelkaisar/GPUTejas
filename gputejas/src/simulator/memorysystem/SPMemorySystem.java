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
package memorysystem;

import generic.CachePullEvent;
import generic.GpuType;
import generic.PortType;
import generic.SimulationElement;
import generic.SP;
import generic.RequestType;
import config.SystemConfig;
import config.CacheConfig;
import config.SimulationConfig;

public abstract class SPMemorySystem extends SimulationElement
{
	protected int tpc_id;
	protected int sm_id;
	protected int sp_id;
	protected SP sp;
	protected Cache iCache;
	protected Cache constantCache;
	protected Cache sharedCache;
	protected Cache dCache;
	
	protected long numInstructionSetChunksNoted = 0;
	protected long numDataSetChunksNoted = 0;
	
	@SuppressWarnings("static-access")
	protected SPMemorySystem(SP sp, Cache cnCache, Cache shCache, Cache dtCache)
	{
		super(PortType.Unlimited, -1, -1, sp.getEventQueue(), -1, -1);
		this.setSP(sp);
		this.tpc_id = sp.getTPC_number();
		this.sm_id = sp.getSM_number();
		this.sp_id = sp.getSP_number();
		
		// Keep all cache names unique to ensure they don't overwrite each other in the cache mapping hashtable.
		
		CacheConfig cacheParameterObj;
		cacheParameterObj = SystemConfig.tpc[tpc_id].sm[sm_id].sp[sp_id].iCache;
		iCache = new Cache("iCache["+tpc_id+"]["+sm_id+"]["+sp_id+"]", this.sm_id, cacheParameterObj, this);
		iCache.setComInterface(sp.getComInterface());
		this.sp.getEventQueue().addEvent(
									new CachePullEvent(
											this.sp.getEventQueue(),
											0,
											iCache,
											iCache,
											RequestType.PerformPulls,
											this.tpc_id,
											this.sm_id,
											this.sp_id));
		
		if (cnCache == null) {
			cacheParameterObj = SystemConfig.tpc[tpc_id].sm[sm_id].constantCache;
			constantCache = new Cache("constantCache["+tpc_id+"]["+sm_id+"]", this.sm_id, cacheParameterObj, this);
			constantCache.setComInterface(sp.getComInterface());
			this.sp.getEventQueue().addEvent(
										new CachePullEvent(
												this.sp.getEventQueue(),
												0,
												constantCache,
												constantCache,
												RequestType.PerformPulls,
												this.tpc_id,
												this.sm_id,
												0));
		} else {
			constantCache = cnCache;
		}
				
		
		if (shCache == null) {
			cacheParameterObj = SystemConfig.tpc[tpc_id].sm[sm_id].sharedCache;
			sharedCache = new Cache("sharedCache["+tpc_id+"]["+sm_id+"]", this.sm_id, cacheParameterObj, this);
			sharedCache.setComInterface(sp.getComInterface());
			this.sp.getEventQueue().addEvent(
										new CachePullEvent(
												this.sp.getEventQueue(),
												0,
												sharedCache,
												sharedCache,
												RequestType.PerformPulls,
												this.tpc_id,
												this.sm_id,
												0));
		} else {
			sharedCache = shCache;
		}
		
		if (dtCache == null) {
			if (SimulationConfig.GPUType == GpuType.Ampere) {
				cacheParameterObj = SystemConfig.tpc[tpc_id].sm[sm_id].L1Cache;
				dCache = new Cache("L1Cache["+tpc_id+"]["+sm_id+"]", this.sm_id, cacheParameterObj, this);
				dCache.setComInterface(sp.getComInterface());
				this.sp.getEventQueue().addEvent(
										new CachePullEvent(
												this.sp.getEventQueue(),
												0,
												dCache,
												dCache,
												RequestType.PerformPulls,
												this.tpc_id,
												this.sm_id,
												0));
			} else {
				cacheParameterObj = SystemConfig.tpc[tpc_id].sm[sm_id].sp[sp_id].dCache;
				dCache = new Cache("dCache["+tpc_id+"]["+sm_id+"]["+sp_id+"]", this.sm_id, cacheParameterObj, this);
				dCache.setComInterface(sp.getComInterface());
				this.sp.getEventQueue().addEvent(
										new CachePullEvent(
												this.sp.getEventQueue(),
												0,
												dCache,
												dCache,
												RequestType.PerformPulls,
												this.tpc_id,
												this.sm_id,
												0));
			}
		} else {
			dCache = dtCache;
		}
	}
	
	public abstract void issueRequestToInstrCache(long address);
			
	public abstract boolean issueRequestToConstantCache(RequestType requestType, long address) throws Exception;
	
	public abstract boolean issueRequestToSharedCache(RequestType requestType, long address) throws Exception;
	

	public Cache getiCache() {
		return iCache;
	}
	
	public Cache getConstantCache() {
		return constantCache;
	}
	
	public Cache getSharedCache() {
		return sharedCache;
	}
	
	public Cache getDataCache() {
		return dCache;
	}

	private String tagNameWithSPId(String name) {
		return (name + "[" + this.sp_id + "]");
	}
	
	public void setSP(SP sp) {
		this.sp = sp;
	}

	public SP getSP() {
		return sp;
	}
}
