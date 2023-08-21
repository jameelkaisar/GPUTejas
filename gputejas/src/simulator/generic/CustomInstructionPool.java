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


public class CustomInstructionPool {
	public GenericCircularBuffer<Instruction> pool;
	
	public CustomInstructionPool(int minPoolSize, int maxPoolSize)
	{
		pool = new GenericCircularBuffer<Instruction>(Instruction.class, minPoolSize, maxPoolSize, true);
	}
	
	public Instruction borrowObject()
	{	
		return new Instruction();
	}
	
	public void returnObject(Instruction arg0)
	{	
	}
	
	public int getNumIdle()
	{
		return pool.size();
	}
}

