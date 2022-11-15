/*****************************************************************************
				Tejas Simulator
------------------------------------------------------------------------------------------------------------

   Copyright [2010] [Indian Institute of Technology, Delhi]
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

	Contributors:  Moksh Upadhyay
*****************************************************************************/
package memorysystem;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import config.EnergyConfig;
import config.SystemConfig;

import generic.*;
import main.ArchitecturalComponent;

public class TLB extends SimulationElement
{
	CoreMemorySystem containingMemSys;
	protected Hashtable<Long, TLBEntry> TLBuffer;
	protected int TLBSize; //Number of entries
	protected double timestamp;
	protected long tlbRequests = 0;
	protected long tlbHits = 0;
	protected long tlbMisses = 0;
	private int memoryPenalty;
	
	private long noOfAccesses = 0;
	
	EnergyConfig power;
		
	public Core getCore() {
		return containingMemSys.getCore();
	}
	
	public int getMemoryPenalty() {
		return memoryPenalty;
	}
	
	//Outstanding Request Table : Stores pageID v/s LSQEntryIndex
	protected Hashtable<Long, ArrayList<LSQEntry>> missStatusHoldingRegister
			= new Hashtable<Long, ArrayList<LSQEntry>>();
	
	//For telling that what addresses are processed this cycle (for BANKED multi-port option)
	protected ArrayList<Long> addressesProcessedThisCycle = new ArrayList<Long>();
	
	//For telling how many requests are processed this cycle (for GENUINELY multi-ported option)
	protected int requestsProcessedThisCycle = 0;
	
	public TLB(PortType portType, int noOfPorts, long occupancy, long latency,
			CoreMemorySystem containingMemSys, int tlbSize, int memoryPenalty,
			EnergyConfig power) 
	{
		super(portType, noOfPorts, occupancy, latency, containingMemSys.getCore().getFrequency());
		
		TLBSize = tlbSize;
		this.timestamp = 0;
		TLBuffer = new Hashtable<Long, TLBEntry>(TLBSize);
		this.containingMemSys =containingMemSys;
		this.memoryPenalty = memoryPenalty;		
		this.power = power;
	}
	
	/**
	 * Removes the page offset bits from the address
	 * @param virtualAddr : Complete virtual address
	 * @return pageID obtained by removing page offset bits from virtual address
	 */
	protected static long getPageID(long virtualAddr)
	{
		long pageID = virtualAddr >>> MemorySystem.PAGE_OFFSET_BITS;
		return pageID;
	}
	public void flushTlb()
	{
		TLBuffer.clear();
	}
	public long getPhyAddr(Long virtualAddr)
	{
		if(this.searchTLBForPhyAddr(virtualAddr)){
			TLBEntry entry=TLBuffer.get(getPageID(virtualAddr));
			return entry.getPhyAddr();
		}
		else
			return -1;
	}
	public boolean searchTLBForPhyAddr(long virtualAddr) //Returns whether the address was already in the TLB or not
	{
		noOfAccesses++;
		
		tlbRequests++;
		timestamp += 1.0; //Increment the timestamp to be set in this search
		boolean isEntryFoundInTLB;
		
		long pageID = getPageID(virtualAddr); //Remove the page offset bits from the address
		TLBEntry entry;
		
		if ((TLBuffer.isEmpty()) || ((entry = TLBuffer.get(pageID)) == null)) //Entry not found in the TLB
		{
			tlbMisses++;
			//Fetch the TLB entry from Main memory through the event TLBAddrSearchEvent
			AddressCarryingEvent addressEvent = new AddressCarryingEvent(getCore().getEventQueue(), memoryPenalty, this, 
					this, RequestType.Tlb_Miss_Response, pageID);
			
			
			this.getPort().put(addressEvent);
									
			//return pageID;
			isEntryFoundInTLB = false;
		}
		else //Entry found in the page table
		{
			tlbHits++;
			entry.setTimestamp(timestamp);
			//return entry.getPhyAddr();
			isEntryFoundInTLB = true;
		}
		return isEntryFoundInTLB;
	}
	
	//Just have to provide the full address.
	//The pageID is calculated within
	private void addTLBEntry(long pageID)
	{
		noOfAccesses++;
		
		long addressForTLB = pageID << MemorySystem.PAGE_OFFSET_BITS;
		
		TLBEntry entry = new TLBEntry();
		entry.setPhyAddr(addressForTLB);
		entry.setTimestamp(timestamp);
		
		if (!(TLBuffer.size() < TLBSize)) // If there is no space in the TLB
		{
			long keyToRemove = searchOldestTimestamp(); //We use LRU replacement
			TLBuffer.remove(keyToRemove);
		}
		TLBuffer.put(pageID, entry);
	}
	
	
	private long searchOldestTimestamp()
	{
		long oldestAddr = 0;
		double minTimestamp = Double.MAX_VALUE;
		for (Enumeration<TLBEntry> entriesEnum = TLBuffer.elements(); entriesEnum.hasMoreElements(); )
		{
			TLBEntry entry = entriesEnum.nextElement();
			if (entry.getTimestamp() < minTimestamp)
			{
				oldestAddr = entry.getPhyAddr();
				minTimestamp = entry.getTimestamp();
			}
		}
		return (getPageID(oldestAddr));
	}
	
	/**
	 * Tells whether the request of current event can be processed in the current cycle (due to device port availability)
	 * @return A boolean value :TRUE if the request can be processed and FALSE otherwise
	 */
/*	protected boolean canServiceRequest()
	{
		//TLB is a Genuinely multi-ported element
		//So if number of requests this cycle has not reached the total number of ports
		if (this.requestsProcessedThisCycle < this.ports)
		{
			requestsProcessedThisCycle++;
			return true;
		}
		else
			return false;
	}*/
	
	/**
	 * Used when a new request is made to a cache and there is a miss.
	 * This adds the request to the outstanding requests buffer of the cache
	 * @param pageID : pageID requested
	 * @return Whether the entry was already there or not
	 */
//	protected boolean addOutstandingRequest(long pageID, LSQEntry lsqEntry)
//	{
//		boolean entryAlreadyThere;
//		
//		if (!/*NOT*/missStatusHoldingRegister.containsKey(pageID))
//		{
//			entryAlreadyThere = false;
//			missStatusHoldingRegister.put(pageID, new ArrayList<LSQEntry>());
//		}
//		else
//			entryAlreadyThere = true;
//		
//		missStatusHoldingRegister.get(pageID).add(lsqEntry);
//		
//		return entryAlreadyThere;
//	}
	
	public long getTlbHits() {
		return tlbHits;
	}
	
	public void setTlbHits(long hits) {
		tlbHits = hits;
	}

	public long getTlbMisses() {
		return tlbMisses;
	}

	public void setTlbMisses(long misses) {
		tlbMisses = misses;
	}
	
	public long getTlbRequests() {
		return tlbRequests;
	}
	
	public void setTlbRequests(long requests) {
		tlbRequests = requests;
	}

	public void handleEvent(EventQueue eventQ, Event event)
	{
		if(event.getRequestType()==RequestType.Tlb_Miss_Response) {
			long pageId = ((AddressCarryingEvent)event).getAddress();
			addTLBEntry(pageId);
		} else {
			misc.Error.showErrorAndExit("Invalid event sent to TLB : " + event);
		}
	}

	public void resetNumAccesses()
	{
	  noOfAccesses = 0;
	}
	
	public EnergyConfig calculateAndPrintEnergy(FileWriter outputFileWriter, String componentName) throws IOException
	{
		EnergyConfig tlbPower = new EnergyConfig(power, noOfAccesses);
		tlbPower.printEnergyStats(outputFileWriter, componentName);
		return tlbPower;
	}
	static FileWriter fw;
	public static void printTLBStatistics(FileWriter fw, int i) throws IOException
	{
		//System.out.println("Inside TLB");
		CoreMemorySystem coreMemSys[]=ArchitecturalComponent.getCoreMemSysArray();
		TLB.fw=fw;
		fw.write("core\t\t=\t" + i + "\n");
		fw.write("Memory Requests\t=\t" + coreMemSys[i].getNumberOfMemoryRequests() + "\n");
		fw.write("Loads\t\t=\t" + coreMemSys[i].getNumberOfLoads() + "\n");
		fw.write("Stores\t\t=\t" + coreMemSys[i].getNumberOfStores() + "\n");
		fw.write("LSQ forwardings\t=\t" + coreMemSys[i].getNumberOfValueForwardings() + "\n");
		printCacheStatistics("iTLB[" + i + "]", coreMemSys[i].getiTLB().getTlbHits(), coreMemSys[i].getiTLB().getTlbMisses());
		printCacheStatistics("dTLB[" + i + "]", coreMemSys[i].getdTLB().getTlbHits(), coreMemSys[i].getdTLB().getTlbMisses());
		
	}
	static void printCacheStatistics(String cacheStr,
			long hits, long misses) throws IOException
	{
		fw.write("\n\n" + cacheStr + " Hits\t=\t" + hits);
		fw.write("\n" + cacheStr + " Misses\t=\t" + misses);
		fw.write("\n" + cacheStr + " Accesses\t=\t" + (hits+misses));
		fw.write("\n" + cacheStr + " Hit-Rate\t=\t" + Statistics.formatDouble((double)hits/(double)(hits+misses)));
		fw.write("\n" + cacheStr + " Miss-Rate\t=\t" + Statistics.formatDouble((double)misses/(double)(hits+misses)));
	}
	public static void resetTLBValues()
	{
		CoreMemorySystem coreMemSys[]=ArchitecturalComponent.getCoreMemSysArray();
		for(int i=0;i<SystemConfig.NoOfCores;i++)
		{
			coreMemSys[i].setNumberOfMemoryRequests(0);
			coreMemSys[i].setNumberOfLoads(0);
			coreMemSys[i].setNumberOfStores(0);
			coreMemSys[i].setNumberOfValueForwardings(0);
			coreMemSys[i].getiTLB().setTlbHits(0);
			coreMemSys[i].getiTLB().setTlbMisses(0);
			coreMemSys[i].getdTLB().setTlbHits(0);
			coreMemSys[i].getdTLB().setTlbMisses(0);
			
		}
		
		
	}
	
}
