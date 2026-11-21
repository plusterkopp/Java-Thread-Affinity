/*
 * Copyright 2016 higherfrequencytrading.com
 * Copyright 2023-2026 Ralf Helbing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.pkmd.affinity.impl;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static junit.framework.Assert.assertEquals;

/**
 * @author peter.lawrey
 */
public class ApicCpuLayoutTest {

	@Test
	public void testFromCpuInfoI7() throws IOException {
		final InputStream i7 = getClass().getClassLoader().getResourceAsStream("i7.cpuinfo");
		ApicCpuLayout vcl = ApicCpuLayout.fromCpuInfo(i7);
		assertEquals("0: CpuInfo{apicId=0, socketId=0, coreId=0, threadId=0}\n" +
				"1: CpuInfo{apicId=2, socketId=0, coreId=1, threadId=0}\n" +
				"2: CpuInfo{apicId=4, socketId=0, coreId=2, threadId=0}\n" +
				"3: CpuInfo{apicId=6, socketId=0, coreId=3, threadId=0}\n" +
				"4: CpuInfo{apicId=1, socketId=0, coreId=0, threadId=1}\n" +
				"5: CpuInfo{apicId=3, socketId=0, coreId=1, threadId=1}\n" +
				"6: CpuInfo{apicId=5, socketId=0, coreId=2, threadId=1}\n" +
				"7: CpuInfo{apicId=7, socketId=0, coreId=3, threadId=1}\n", vcl.toString());
	}

	@Test
	public void testFromCpuInfoOthers() throws IOException {
		{
			final InputStream is = getClass().getClassLoader().getResourceAsStream("core.duo.cpuinfo");
			ApicCpuLayout vcl = ApicCpuLayout.fromCpuInfo(is);
			assertEquals("0: CpuInfo{apicId=0, socketId=0, coreId=0, threadId=0}\n" +
					"1: CpuInfo{apicId=1, socketId=0, coreId=1, threadId=0}\n", vcl.toString());
		}
		{
			final InputStream is = getClass().getClassLoader().getResourceAsStream("amd64.quad.core.cpuinfo");
			ApicCpuLayout vcl = ApicCpuLayout.fromCpuInfo(is);
			assertEquals("0: CpuInfo{apicId=0, socketId=0, coreId=0, threadId=0}\n" +
					"1: CpuInfo{apicId=1, socketId=0, coreId=1, threadId=0}\n" +
					"2: CpuInfo{apicId=2, socketId=0, coreId=2, threadId=0}\n" +
					"3: CpuInfo{apicId=3, socketId=0, coreId=3, threadId=0}\n", vcl.toString());
		}
		{
			final InputStream is = getClass().getClassLoader().getResourceAsStream("i3.cpuinfo");
			ApicCpuLayout vcl = ApicCpuLayout.fromCpuInfo(is);
			assertEquals("0: CpuInfo{apicId=0, socketId=0, coreId=0, threadId=0}\n" +
					"1: CpuInfo{apicId=4, socketId=0, coreId=2, threadId=0}\n" +
					"2: CpuInfo{apicId=1, socketId=0, coreId=0, threadId=1}\n" +
					"3: CpuInfo{apicId=5, socketId=0, coreId=2, threadId=1}\n", vcl.toString());
		}
		{
			final InputStream is = getClass().getClassLoader().getResourceAsStream("q6600.noht.cpuinfo");
			ApicCpuLayout vcl = ApicCpuLayout.fromCpuInfo(is);
			assertEquals("0: CpuInfo{apicId=0, socketId=0, coreId=0, threadId=0}\n" +
					"1: CpuInfo{apicId=2, socketId=0, coreId=2, threadId=0}\n" +
					"2: CpuInfo{apicId=1, socketId=0, coreId=1, threadId=0}\n" +
					"3: CpuInfo{apicId=3, socketId=0, coreId=3, threadId=0}\n", vcl.toString());
		}
	}

}
