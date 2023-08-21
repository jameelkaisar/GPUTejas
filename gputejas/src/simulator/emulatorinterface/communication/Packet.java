package emulatorinterface.communication;

import java.io.Serializable;

import config.SimulationConfig;
import emulatorinterface.translator.x86.instruction.InstructionClass;

@SuppressWarnings("serial")
public class Packet  implements Serializable 
{
	public InstructionClass insClass;
	public Integer ip;
	public Long MemoryAddresses[];
	
	public Packet () 
	{
		MemoryAddresses = new Long[SimulationConfig.ThreadsPerCTA];
	}

	
	public Packet(Integer ip, InstructionClass iClass) 
	{
		this.insClass=iClass;
		this.ip = ip;
	}
	
	public Packet(Integer ip, InstructionClass iClass, Long[] MemoryAddresses) 
	{
		MemoryAddresses = new Long[SimulationConfig.ThreadsPerCTA];
		this.insClass=iClass;
		
		this.ip = ip;
		
		this.MemoryAddresses = MemoryAddresses;
	}
	
	public void set(InstructionClass iClass, Integer ip,  Long[] MemoryAddresses){
		this.insClass=iClass;
		this.ip = ip;
		
		this.MemoryAddresses = MemoryAddresses;
	}
	
	public void set(Packet p) {
		this.insClass=p.insClass;
		this.ip = p.ip;
		this.MemoryAddresses = p.MemoryAddresses;
	}	
}