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

import java.util.Enumeration;
import java.util.Hashtable;

import pipeline.GPUMemorySystem;
import main.ArchitecturalComponent;
import memorysystem.Cache.CacheType;
import memorysystem.directory.CentralizedDirectoryCache;
import generic.*;
import config.CacheConfig;
import config.SystemConfig;
import config.TpcConfig;


public class MemorySystem
{
	static SM[][] cores;
	static Hashtable<String, Cache> cacheList;
	public static MainMemoryController mainMemoryController;
	public static CentralizedDirectoryCache centralizedDirectory;
	
	public static Hashtable<String, Cache> getCacheList() {
		return cacheList;
	}

	@SuppressWarnings("unused")
	public static SMMemorySystem[][] initializeMemSys(SM[][] sms)
	{
		MemorySystem.cores = sms;
		SMMemorySystem smMemSysArray[][] = new SMMemorySystem[sms.length][sms[0].length];
		
		System.out.println("initializing memory system...");
		CacheConfig cacheParameterObj;
		/*First initialize the L2 and greater caches (to be linked with L1 caches and among themselves)*/
		cacheList = new Hashtable<String, Cache>(); //Declare the hash table for level 2 or greater caches
		boolean flag = false;
		for (Enumeration<String> cacheNameSet = SystemConfig.declaredCaches.keys(); cacheNameSet.hasMoreElements(); )
		{
			String cacheName = cacheNameSet.nextElement();
			
			if (!(cacheList.containsKey(cacheName))) //If not already present
			{
				cacheParameterObj = SystemConfig.declaredCaches.get(cacheName);
				
				//Declare the new cache
				Cache newCache = null;
				newCache = new Cache(cacheParameterObj, null);
				
				
				//Put the newly formed cache into the new list of caches
				cacheList.put(cacheName, newCache);
				
				//add initial cachepull event
				if(newCache.levelFromTop == CacheType.Lower)
				{
					ArchitecturalComponent.getCores()[0][0].getEventQueue().addEvent(
											new CachePullEvent(
													ArchitecturalComponent.getCores()[0][0].getEventQueue(),
													0,
													newCache,
													newCache,
													RequestType.PerformPulls,
													-1, -1));
				}
			}
		}
		
		mainMemoryController = new MainMemoryController();
		//Initialize the core memory systems
		for (int i = 0; i < SystemConfig.NoOfTPC ; i++)
		{
			for(int j =0 ; j<  TpcConfig.NoOfSM; j++)
			{
				int k=0;
				{
					SMMemorySystem smMemSys = null;
					smMemSys = new GPUMemorySystem(sms[i][j], k);
			
					smMemSysArray[i][j] = smMemSys;
				
					//Set the next levels of the instruction cache
					if (smMemSys.iCache.isLastLevel == true) //If this is the last level, don't set anything
					{
						continue;
					}
				
					String nextLevelName = smMemSys.iCache.nextLevelName;
				
					if (nextLevelName.isEmpty())
					{
						System.err.println("Memory system configuration error : The iCache is not last level but the next level is not specified");
						System.exit(1);
					}
					
					if (cacheList.containsKey(nextLevelName)) 
					{
						//Point the cache to its next level
						smMemSys.iCache.nextLevel = cacheList.get(nextLevelName);
						smMemSys.iCache.nextLevel.prevLevel.add(smMemSys.iCache);
					}
					else
					{
						System.err.println("Memory system configuration error : A cache specified as a next level does not exist");
						System.exit(1);
					}
				}
				
			}			
		}
			
			for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
			{
				String cacheName = cacheNameSet.nextElement();
				Cache cacheToSetNextLevel = cacheList.get(cacheName);
					
				if (cacheToSetNextLevel.isLastLevel == true) //If this is the last level, don't set anything
				{
					continue;
				}
				String nextLevelName = cacheToSetNextLevel.nextLevelName;
				
				if (nextLevelName.isEmpty())
				{
					System.err.println("Memory system configuration error : The cache \""+ cacheName +"\" is not last level but the next level is not specified");
					System.exit(1);
				}
				if (cacheName.equals(nextLevelName)) //If the cache is itself given as its next level
				{
					System.err.println("Memory system configuration error : The cache \""+ cacheName +"\" is specified as a next level of itself");
					System.exit(1);
				}
					
				if (cacheList.containsKey(nextLevelName)) 
				{
					//Point the cache to its next level
					cacheToSetNextLevel.nextLevel = cacheList.get(nextLevelName);
					cacheToSetNextLevel.nextLevel.prevLevel.add(cacheToSetNextLevel);
				}
				else
				{
					System.err.println("Memory system configuration error : A cache specified as a next level does not exist");
					System.exit(1);
				}
			}
			
			for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
			{
				String cacheName = cacheNameSet.nextElement();
				Cache cacheToSetConnectedMSHR = cacheList.get(cacheName);
				cacheToSetConnectedMSHR.populateConnectedMSHR();
			}
			
			return smMemSysArray;
		}
		
		
		public static   CentralizedDirectoryCache getDirectoryCache()
		{
			return centralizedDirectory;
		}
		
		public static void printMemSysResults()
		{
			System.out.println("\n Memory System results\n");
			
			System.out.println(" ");
			System.out.println(" Results of other caches");
			
			for (Enumeration<String> cacheNameSet = cacheList.keys(); cacheNameSet.hasMoreElements(); /*Nothing*/)
			{
				String cacheName = cacheNameSet.nextElement();
				Cache cache = cacheList.get(cacheName);
				
				System.out.println(
						cacheName + " Hits : " 
						+ cache.hits 
						+ "\t ; " + cacheName + " misses : " 
						+ cache.misses);
			}
		}
	}
