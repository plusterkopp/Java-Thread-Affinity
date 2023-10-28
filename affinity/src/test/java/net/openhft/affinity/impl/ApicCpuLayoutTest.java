/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.affinity.impl;

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
