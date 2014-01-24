/* Copyright 2013 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. */

package net.openhft.affinity.impl;

import static junit.framework.Assert.*;

import java.io.*;

import org.junit.*;

/**
 * @author ralf h
 */
@SuppressWarnings( "deprecation")
public class WindowsCpuLayoutTest {

	@Test
	public void testFromCpuDesc_1_4_2() {
		WindowsCpuLayout vcl = WindowsCpuLayout.fromCpuDesc( "1/4/2");
		assertEquals( "0: CpuInfo{socketId=0, coreId=0, threadId=0}\n"
				+ "1: CpuInfo{socketId=0, coreId=1, threadId=0}\n" + "2: CpuInfo{socketId=0, coreId=2, threadId=0}\n"
				+ "3: CpuInfo{socketId=0, coreId=3, threadId=0}\n" + "4: CpuInfo{socketId=0, coreId=0, threadId=1}\n"
				+ "5: CpuInfo{socketId=0, coreId=1, threadId=1}\n" + "6: CpuInfo{socketId=0, coreId=2, threadId=1}\n"
				+ "7: CpuInfo{socketId=0, coreId=3, threadId=1}\n", vcl.toString());
	}

	@Test
	public void testFromCpuInfo_1_1_1() {
		{
			WindowsCpuLayout vcl = WindowsCpuLayout.fromCpuDesc( "1/1/1");
			assertEquals( "0: CpuInfo{socketId=0, coreId=0, threadId=0}\n", vcl.toString());
		}
	}

	@Test
	public void test_4_8_2() {
		WindowsCpuLayout vcl = WindowsCpuLayout.fromCpuDesc( "4/8/2");
		assertEquals( ""
				+ "0: CpuInfo{socketId=0, coreId=0, threadId=0}\n"
				+ "1: CpuInfo{socketId=0, coreId=1, threadId=0}\n"
				+ "2: CpuInfo{socketId=0, coreId=2, threadId=0}\n"
				+ "3: CpuInfo{socketId=0, coreId=3, threadId=0}\n"
				+ "4: CpuInfo{socketId=0, coreId=4, threadId=0}\n"
				+ "5: CpuInfo{socketId=0, coreId=5, threadId=0}\n"
				+ "6: CpuInfo{socketId=0, coreId=6, threadId=0}\n"
				+ "7: CpuInfo{socketId=0, coreId=7, threadId=0}\n"
				+ "8: CpuInfo{socketId=1, coreId=0, threadId=0}\n"
				+ "9: CpuInfo{socketId=1, coreId=1, threadId=0}\n"
				+ "10: CpuInfo{socketId=1, coreId=2, threadId=0}\n"
				+ "11: CpuInfo{socketId=1, coreId=3, threadId=0}\n"
				+ "12: CpuInfo{socketId=1, coreId=4, threadId=0}\n"
				+ "13: CpuInfo{socketId=1, coreId=5, threadId=0}\n"
				+ "14: CpuInfo{socketId=1, coreId=6, threadId=0}\n"
				+ "15: CpuInfo{socketId=1, coreId=7, threadId=0}\n"
				+ "16: CpuInfo{socketId=2, coreId=0, threadId=0}\n"
				+ "17: CpuInfo{socketId=2, coreId=1, threadId=0}\n"
				+ "18: CpuInfo{socketId=2, coreId=2, threadId=0}\n"
				+ "19: CpuInfo{socketId=2, coreId=3, threadId=0}\n"
				+ "20: CpuInfo{socketId=2, coreId=4, threadId=0}\n"
				+ "21: CpuInfo{socketId=2, coreId=5, threadId=0}\n"
				+ "22: CpuInfo{socketId=2, coreId=6, threadId=0}\n"
				+ "23: CpuInfo{socketId=2, coreId=7, threadId=0}\n"
				+ "24: CpuInfo{socketId=3, coreId=0, threadId=0}\n"
				+ "25: CpuInfo{socketId=3, coreId=1, threadId=0}\n"
				+ "26: CpuInfo{socketId=3, coreId=2, threadId=0}\n"
				+ "27: CpuInfo{socketId=3, coreId=3, threadId=0}\n"
				+ "28: CpuInfo{socketId=3, coreId=4, threadId=0}\n"
				+ "29: CpuInfo{socketId=3, coreId=5, threadId=0}\n"
				+ "30: CpuInfo{socketId=3, coreId=6, threadId=0}\n"
				+ "31: CpuInfo{socketId=3, coreId=7, threadId=0}\n"
				+ "32: CpuInfo{socketId=0, coreId=0, threadId=1}\n"
				+ "33: CpuInfo{socketId=0, coreId=1, threadId=1}\n"
				+ "34: CpuInfo{socketId=0, coreId=2, threadId=1}\n"
				+ "35: CpuInfo{socketId=0, coreId=3, threadId=1}\n"
				+ "36: CpuInfo{socketId=0, coreId=4, threadId=1}\n"
				+ "37: CpuInfo{socketId=0, coreId=5, threadId=1}\n"
				+ "38: CpuInfo{socketId=0, coreId=6, threadId=1}\n"
				+ "39: CpuInfo{socketId=0, coreId=7, threadId=1}\n"
				+ "40: CpuInfo{socketId=1, coreId=0, threadId=1}\n"
				+ "41: CpuInfo{socketId=1, coreId=1, threadId=1}\n"
				+ "42: CpuInfo{socketId=1, coreId=2, threadId=1}\n"
				+ "43: CpuInfo{socketId=1, coreId=3, threadId=1}\n"
				+ "44: CpuInfo{socketId=1, coreId=4, threadId=1}\n"
				+ "45: CpuInfo{socketId=1, coreId=5, threadId=1}\n"
				+ "46: CpuInfo{socketId=1, coreId=6, threadId=1}\n"
				+ "47: CpuInfo{socketId=1, coreId=7, threadId=1}\n"
				+ "48: CpuInfo{socketId=2, coreId=0, threadId=1}\n"
				+ "49: CpuInfo{socketId=2, coreId=1, threadId=1}\n"
				+ "50: CpuInfo{socketId=2, coreId=2, threadId=1}\n"
				+ "51: CpuInfo{socketId=2, coreId=3, threadId=1}\n"
				+ "52: CpuInfo{socketId=2, coreId=4, threadId=1}\n"
				+ "53: CpuInfo{socketId=2, coreId=5, threadId=1}\n"
				+ "54: CpuInfo{socketId=2, coreId=6, threadId=1}\n"
				+ "55: CpuInfo{socketId=2, coreId=7, threadId=1}\n"
				+ "56: CpuInfo{socketId=3, coreId=0, threadId=1}\n"
				+ "57: CpuInfo{socketId=3, coreId=1, threadId=1}\n"
				+ "58: CpuInfo{socketId=3, coreId=2, threadId=1}\n"
				+ "59: CpuInfo{socketId=3, coreId=3, threadId=1}\n"
				+ "60: CpuInfo{socketId=3, coreId=4, threadId=1}\n"
				+ "61: CpuInfo{socketId=3, coreId=5, threadId=1}\n"
				+ "62: CpuInfo{socketId=3, coreId=6, threadId=1}\n"
				+ "63: CpuInfo{socketId=3, coreId=7, threadId=1}\n",
				vcl.toString());
	}

	@Test
	public void testFromProperties_4_1_1() throws IOException {
		WindowsCpuLayout vcl = WindowsCpuLayout.fromCpuDesc( "4/1/1");
		assertEquals( ""
				+ "0: CpuInfo{socketId=0, coreId=0, threadId=0}\n"
				+ "1: CpuInfo{socketId=1, coreId=0, threadId=0}\n"
				+ "2: CpuInfo{socketId=2, coreId=0, threadId=0}\n"
				+ "3: CpuInfo{socketId=3, coreId=0, threadId=0}\n", vcl.toString());
	}
}
