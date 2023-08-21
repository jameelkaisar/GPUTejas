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
package config;


import generic.GpuType;
import generic.MultiPortingType;
import generic.PortType;

import java.io.File;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import memorysystem.Cache;
import memorysystem.Cache.CacheType;
import memorysystem.Cache.CoherenceType;

import org.w3c.dom.*;



public class XMLParser 
{
	private static Document doc;

	public static void parse(String fileName) 
	{ 
		try 
		{
			File file = new File(fileName);
			DocumentBuilderFactory DBFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder DBuilder = DBFactory.newDocumentBuilder();
			doc = DBuilder.parse(file);
			doc.getDocumentElement().normalize();
		
			setSimulationParameters();
			setSystemParameters();
			setLatencyParameters();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
 	}
			
	private static void setLatencyParameters()
	{
		NodeList nodeLst = doc.getElementsByTagName("OperationLatency");
		Node latencyNode = nodeLst.item(0);
		Element latencyElmnt = (Element) latencyNode;
		OperationLatencyConfig.loadLatency= Integer.parseInt(getImmediateString("load", latencyElmnt));
		OperationLatencyConfig.storeLatency= Integer.parseInt(getImmediateString("store", latencyElmnt));
		OperationLatencyConfig.addressLatency= Integer.parseInt(getImmediateString("address", latencyElmnt));
		OperationLatencyConfig.intALULatency= Integer.parseInt(getImmediateString("intALU", latencyElmnt));
		OperationLatencyConfig.intMULLatency= Integer.parseInt(getImmediateString("intMUL", latencyElmnt));
		OperationLatencyConfig.intDIVLatency= Integer.parseInt(getImmediateString("intDIV", latencyElmnt));
		OperationLatencyConfig.floatALULatency= Integer.parseInt(getImmediateString("floatALU", latencyElmnt));
		OperationLatencyConfig.floatMULLatency= Integer.parseInt(getImmediateString("floatMUL", latencyElmnt));
		OperationLatencyConfig.floatDIVLatency= Integer.parseInt(getImmediateString("floatDIV", latencyElmnt));
		OperationLatencyConfig.predicateLatency= Integer.parseInt(getImmediateString("predicate", latencyElmnt));
		OperationLatencyConfig.branchLatency= Integer.parseInt(getImmediateString("branch", latencyElmnt));
		OperationLatencyConfig.callLatency= Integer.parseInt(getImmediateString("call", latencyElmnt));
		OperationLatencyConfig.returnLatency= Integer.parseInt(getImmediateString("return", latencyElmnt));
		OperationLatencyConfig.exitLatency= Integer.parseInt(getImmediateString("exit", latencyElmnt));
				
	}
	
	private static void setSimulationParameters()
	{
		NodeList nodeLst = doc.getElementsByTagName("Simulation");
		Node simulationNode = nodeLst.item(0);
		Element simulationElmnt = (Element) simulationNode;
		SimulationConfig.MaxNumJavaThreads= Integer.parseInt(getImmediateString("MaxNumJavaThreads", simulationElmnt));
		SimulationConfig.MaxNumBlocksPerJavaThread=Integer.parseInt(getImmediateString("MaxNumBlocksPerJavaThread", simulationElmnt));
		if(getImmediateString("GPUType", simulationElmnt).compareTo("Tesla") == 0)			
		{
			SimulationConfig.GPUType = GpuType.Tesla; 
		}
		SimulationConfig.ThreadsPerCTA=Integer.parseInt(getImmediateString("ThreadsPerCTA", simulationElmnt));	
	}
	
	private static void setSystemParameters()
	{
		SystemConfig.declaredCaches = new Hashtable<String, CacheConfig>(); //Declare the hash table for declared caches
		
		NodeList nodeLst = doc.getElementsByTagName("System");
		Node systemNode = nodeLst.item(0);
		Element systemElmnt = (Element) systemNode;
		
		SystemConfig.NoOfTPC = Integer.parseInt(getImmediateString("NoOfTPC", systemElmnt));
		SystemConfig.mainMemoryLatency = Integer.parseInt(getImmediateString("MainMemoryLatency", systemElmnt));
		SystemConfig.mainMemoryFrequency = Long.parseLong(getImmediateString("MainMemoryFrequency", systemElmnt));
		SystemConfig.mainMemPortType = setPortType(getImmediateString("MainMemoryPortType", systemElmnt));
		SystemConfig.mainMemoryAccessPorts = Integer.parseInt(getImmediateString("MainMemoryAccessPorts", systemElmnt));
		SystemConfig.mainMemoryPortOccupancy = Integer.parseInt(getImmediateString("MainMemoryPortOccupancy", systemElmnt));
		
		SystemConfig.cacheBusLatency = Integer.parseInt(getImmediateString("CacheBusLatency", systemElmnt));
		SystemConfig.tpc = new TpcConfig[SystemConfig.NoOfTPC];		
		
		//Set tpc parameters
		for(int i =0; i<SystemConfig.NoOfTPC ; i++)
		{
			SystemConfig.tpc[i] = new TpcConfig();
			TpcConfig tpc = SystemConfig.tpc[i];
			NodeList tpcLst = systemElmnt.getElementsByTagName("TPC");
			Element tpcElmnt = (Element) tpcLst.item(0);
			setTpcProperties(tpc, tpcElmnt);
		}
		
		//Code for remaining Cache configurations
		NodeList cacheLst = systemElmnt.getElementsByTagName("Cache");
		for (int i = 0; i < cacheLst.getLength(); i++)
		{
			Element cacheElmnt = (Element) cacheLst.item(i);
			String cacheName = cacheElmnt.getAttribute("name");
			
			if (!(SystemConfig.declaredCaches.containsKey(cacheName)))	//If the identically named cache is not already present
			{
				CacheConfig newCacheConfigEntry = new CacheConfig();
				newCacheConfigEntry.levelFromTop = Cache.CacheType.Lower;
				String cacheType = cacheElmnt.getAttribute("type");
				Element cacheTypeElmnt = searchLibraryForItem(cacheType);
				setCacheProperties(cacheTypeElmnt, newCacheConfigEntry);
				newCacheConfigEntry.nextLevel = cacheElmnt.getAttribute("nextLevel");
				SystemConfig.declaredCaches.put(cacheName, newCacheConfigEntry);
			}
		}	
	}

	@SuppressWarnings("static-access")
	private static void setTpcProperties(TpcConfig tpc, Element tpcElmnt) {
		
		tpc.NoOfSM = Integer.parseInt(getImmediateString("NoOfSM", tpcElmnt));
		tpc.sm = new SmConfig[TpcConfig.NoOfSM];
		//Set sm parameters
		for (int i = 0; i < TpcConfig.NoOfSM; i++)
		{
			TpcConfig.sm[i] = new SmConfig();
			SmConfig sm = TpcConfig.sm[i]; //To be locally used for assignments
			NodeList smLst = tpcElmnt.getElementsByTagName("SM");
			Element smElmnt = (Element) smLst.item(0);
			setSmProperties(sm, smElmnt);
		}
	}
	
	@SuppressWarnings("static-access")
	private static void setSmProperties(SmConfig sm, Element smElmnt) {
		sm.frequency= Integer.parseInt(getImmediateString("Frequency", smElmnt));
		sm.NoOfWarpSchedulers = Integer.parseInt(getImmediateString("NoOfWarpSchedulers", smElmnt));
		sm.NoOfSP = Integer.parseInt(getImmediateString("NoOfSP", smElmnt));
		sm.WarpSize = Integer.parseInt(getImmediateString("WarpSize", smElmnt));
		sm.sp = new SpConfig[SmConfig.NoOfSP];
		
		
		//Code for instruction cache configurations for each core
		NodeList iCacheList = smElmnt.getElementsByTagName("iCache");
		Element iCacheElmnt = (Element) iCacheList.item(0);
		String cacheType = iCacheElmnt.getAttribute("type");
		Element typeElmnt = searchLibraryForItem(cacheType);
		sm.iCache.levelFromTop = CacheType.iCache;
		setCacheProperties(typeElmnt, sm.iCache);
		sm.iCache.nextLevel = iCacheElmnt.getAttribute("nextLevel");
		
		
		//Code for constant cache configurations for each core
		NodeList constantCacheList = smElmnt.getElementsByTagName("constantCache");
		Element constantCacheElmnt = (Element) constantCacheList.item(0);
		cacheType = constantCacheElmnt.getAttribute("type");
		typeElmnt = searchLibraryForItem(cacheType);
		sm.constantCache.levelFromTop = CacheType.constantCache;
		setCacheProperties(typeElmnt, sm.constantCache);
		sm.constantCache.nextLevel = constantCacheElmnt.getAttribute("nextLevel");
				
				
		//Code for shared cache configurations for each core
		NodeList sharedCacheList = smElmnt.getElementsByTagName("sharedCache");
		Element sharedCacheElmnt = (Element) sharedCacheList.item(0);
		cacheType = sharedCacheElmnt.getAttribute("type");
		typeElmnt = searchLibraryForItem(cacheType);
		sm.sharedCache.levelFromTop = CacheType.sharedCache;
		setCacheProperties(typeElmnt, sm.sharedCache);
		sm.sharedCache.nextLevel = sharedCacheElmnt.getAttribute("nextLevel");
			
		//Set sp parameters
		for (int i = 0; i < SmConfig.NoOfSP; i++)
		{
			SmConfig.sp[i] = new SpConfig();
			SpConfig sp = SmConfig.sp[i]; //To be locally used for assignments
			NodeList spLst = smElmnt.getElementsByTagName("SP");
			Element spElmnt = (Element) spLst.item(0);
			setSpProperties(sp, spElmnt);
		}
		
	}

	@SuppressWarnings("static-access")
	private static void setSpProperties(SpConfig sp, Element spElmnt) {
		// TODO Auto-generated method stub
		sp.NoOfThreadsSupported = Integer.parseInt(getImmediateString("NoOfThreadsSupported", spElmnt));
	}

	private static String getImmediateString(String tagName, Element parent) // Get the immediate string value of a particular tag name under a particular parent tag
	{
		NodeList nodeLst = parent.getElementsByTagName(tagName);
		if (nodeLst.item(0) == null)
		{
			System.err.println("XML Configuration error : Item \"" + tagName + "\" not found inside the \"" + parent.getTagName() + "\" tag in the configuration file!!");
			System.exit(1);
		}
	    Element NodeElmnt = (Element) nodeLst.item(0);
	    NodeList resultNode = NodeElmnt.getChildNodes();
	    return ((Node) resultNode.item(0)).getNodeValue();
	}
	
	private static PortType setPortType(String inputStr)
	{
		PortType result = null;
		if (inputStr.equalsIgnoreCase("UL"))
			result = PortType.Unlimited;
		else if (inputStr.equalsIgnoreCase("FCFS"))
			result = PortType.FirstComeFirstServe;
		else if (inputStr.equalsIgnoreCase("PR"))
			result = PortType.PriorityBased;
		else
		{
			System.err.println("XML Configuration error : Invalid Port Type type specified");
			System.exit(1);
		}
		return result;
	}
	
	private static MultiPortingType setMultiPortingType(String inputStr)
	{
		MultiPortingType result = null;
		if (inputStr.equalsIgnoreCase("G"))
			result = MultiPortingType.GENUINE;
		else if (inputStr.equalsIgnoreCase("B"))
			result = MultiPortingType.BANKED;
		else
		{
			System.err.println("XML Configuration error : Invalid Multiporting type specified");
			System.exit(1);
		}
		return result;
	}

	private static Element searchLibraryForItem(String tagName)	//Searches the <Library> section for a given tag name and returns it in Element form
	{															// Used mainly for cache types
		NodeList nodeLst = doc.getElementsByTagName("Library");
		Element libraryElmnt = (Element) nodeLst.item(0);
		NodeList libItemLst = libraryElmnt.getElementsByTagName(tagName);
		
		if (libItemLst.item(0) == null) //Item not found
		{
			System.err.println("XML Configuration error : Item type \"" + tagName + "\" not found in library section in the configuration file!!");
			System.exit(1);
		}
		
		if (libItemLst.item(1) != null) //Item found more than once
		{
			System.err.println("XML Configuration error : More than one definitions of item type \"" + tagName + "\" found in library section in the configuration file!!");
			System.exit(1);
		}
		
		Element resultElmnt = (Element) libItemLst.item(0);
		return resultElmnt;
	}
	
	private static void setCacheProperties(Element CacheType, CacheConfig cache)
	{
		String tempStr = getImmediateString("WriteMode", CacheType);
		if (tempStr.equalsIgnoreCase("WB"))
			cache.writePolicy = CacheConfig.WritePolicy.WRITE_BACK;
		else if (tempStr.equalsIgnoreCase("WT"))
			cache.writePolicy = CacheConfig.WritePolicy.WRITE_THROUGH;
		else
		{
			System.err.println("XML Configuration error : Invalid Write Mode (please enter WB for write-back or WT for write-through)");
			System.exit(1);
		}
		
		
		cache.blockSize = Integer.parseInt(getImmediateString("BlockSize", CacheType));
		cache.assoc = Integer.parseInt(getImmediateString("Associativity", CacheType));
		cache.size = Integer.parseInt(getImmediateString("Size", CacheType));
		cache.latency = Integer.parseInt(getImmediateString("Latency", CacheType));
		cache.portType = setPortType(getImmediateString("PortType", CacheType));
		cache.accessPorts = Integer.parseInt(getImmediateString("AccessPorts", CacheType));
		cache.portOccupancy = Integer.parseInt(getImmediateString("PortOccupancy", CacheType));
		cache.multiportType = setMultiPortingType(getImmediateString("MultiPortingType", CacheType));
		cache.mshrSize = Integer.parseInt(getImmediateString("MSHRSize", CacheType));
				
		tempStr = getImmediateString("Coherence", CacheType);
		if (tempStr.equalsIgnoreCase("N"))
			cache.coherence = CoherenceType.None;
		else if (tempStr.equalsIgnoreCase("S"))
			cache.coherence = CoherenceType.Snoopy;
		else if (tempStr.equalsIgnoreCase("D"))
			cache.coherence = CoherenceType.Directory;
		else
		{
			System.err.println("XML Configuration error : Invalid value of 'Coherence' (please enter 'S', D' or 'N')");
			System.exit(1);
		}
		cache.numberOfBuses = Integer.parseInt(getImmediateString("NumBuses", CacheType));
		cache.busOccupancy = Integer.parseInt(getImmediateString("BusOccupancy", CacheType));
				
	tempStr = getImmediateString("LastLevel", CacheType);
		if (tempStr.equalsIgnoreCase("Y"))
			cache.isLastLevel = true;
		else if (tempStr.equalsIgnoreCase("N"))
			cache.isLastLevel = false;
		else
		{
			System.err.println("XML Configuration error : Invalid value of 'isLastLevel' (please enter 'Y' for yes or 'N' for no)");
			System.exit(1);
		}
		
	}
	
}