/**
* Copyright (c) 2011, Regents of the University of Colorado
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package edu.colorado.clear.dependency;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.colorado.clear.dependency.DEPFeat;
import edu.colorado.clear.reader.DEPReader;

/** @author Jinho D. Choi ({@code choijd@colorado.edu}) */
public class DEPFeatTest
{
	@Test
	public void testDEPFeat()
	{
		DEPFeat feat = new DEPFeat();
		assertEquals(DEPReader.BLANK_COLUMN, feat.toString());
		
		feat = new DEPFeat(DEPReader.BLANK_COLUMN);
		assertEquals(DEPReader.BLANK_COLUMN, feat.toString());
		
		feat.add("lst=choi|fst=jinho");
		assertEquals("fst=jinho|lst=choi", feat.toString());
		
		assertEquals("choi" , feat.get("lst"));
		assertEquals("jinho", feat.get("fst"));
		assertEquals(null   , feat.get("mid"));
		
		feat.add(DEPReader.BLANK_COLUMN);
		assertEquals("fst=jinho|lst=choi", feat.toString());
	}
}