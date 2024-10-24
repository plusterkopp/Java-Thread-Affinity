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

import net.openhft.affinity.Affinity;
import net.openhft.affinity.CpuLayout;
import net.openhft.affinity.IAffinity;
import net.openhft.affinity.IDefaultLayoutAffinity;
import net.openhft.affinity.impl.LayoutEntities.Core;
import net.openhft.affinity.impl.LayoutEntities.Socket;
import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

import static junit.framework.Assert.assertEquals;

/**
 * @author peter.lawrey
 */
public class VanillaCpuLayoutTest {

	@Test
	public void testFromCpuInfoOthers() throws IOException {
		{
			final InputStream is = getClass().getClassLoader().getResourceAsStream("amd64.dual.core.cpuinfo");
			VanillaCpuLayout vcl = VanillaCpuLayout.fromCpuInfo(is);
			assertEquals("0: CpuInfo{socketId=0, coreId=0, threadId=0}\n" +
					"1: CpuInfo{socketId=0, coreId=1, threadId=0}\n", vcl.toString());
		}
		{
			final InputStream is = getClass().getClassLoader().getResourceAsStream("dual.xeon.cpuinfo");
			VanillaCpuLayout vcl = VanillaCpuLayout.fromCpuInfo(is);
			assertEquals("0: CpuInfo{socketId=0, coreId=0, threadId=0}\n" +
					"1: CpuInfo{socketId=0, coreId=0, threadId=1}\n" +
					"2: CpuInfo{socketId=3, coreId=3, threadId=0}\n" +
					"3: CpuInfo{socketId=3, coreId=3, threadId=1}\n", vcl.toString());
		}
		{
			final InputStream is = getClass().getClassLoader().getResourceAsStream("dual.E5405.cpuinfo");
			VanillaCpuLayout vcl = VanillaCpuLayout.fromCpuInfo(is);
			assertEquals("0: CpuInfo{socketId=0, coreId=0, threadId=0}\n" +
					"1: CpuInfo{socketId=0, coreId=1, threadId=0}\n" +
					"2: CpuInfo{socketId=0, coreId=2, threadId=0}\n" +
					"3: CpuInfo{socketId=0, coreId=3, threadId=0}\n" +
					"4: CpuInfo{socketId=1, coreId=4, threadId=0}\n" +
					"5: CpuInfo{socketId=1, coreId=5, threadId=0}\n" +
					"6: CpuInfo{socketId=1, coreId=6, threadId=0}\n" +
					"7: CpuInfo{socketId=1, coreId=7, threadId=0}\n", vcl.toString());
		}
	}

	@Test
	public void testNoIDs() throws IOException {
		final InputStream noids = getClass().getClassLoader().getResourceAsStream("q6600.vm.cpuinfo");
		VanillaCpuLayout vcl = VanillaCpuLayout.fromCpuInfo(noids);
		assertEquals("0: CpuInfo{socketId=0, coreId=0, threadId=0}\n" +
				"1: CpuInfo{socketId=0, coreId=1, threadId=0}\n" +
				"2: CpuInfo{socketId=0, coreId=2, threadId=0}\n" +
				"3: CpuInfo{socketId=0, coreId=3, threadId=0}\n", vcl.toString());
	}

	@Test
	public void testFromProperties() throws IOException {
		final InputStream i7 = getClass().getClassLoader().getResourceAsStream("i7.properties");
		VanillaCpuLayout vcl = VanillaCpuLayout.fromProperties(i7);
		assertEquals("0: CpuInfo{socketId=0, coreId=0, threadId=0}\n" +
				"1: CpuInfo{socketId=0, coreId=1, threadId=0}\n" +
				"2: CpuInfo{socketId=0, coreId=2, threadId=0}\n" +
				"3: CpuInfo{socketId=0, coreId=3, threadId=0}\n" +
				"4: CpuInfo{socketId=0, coreId=0, threadId=1}\n" +
				"5: CpuInfo{socketId=0, coreId=1, threadId=1}\n" +
				"6: CpuInfo{socketId=0, coreId=2, threadId=1}\n" +
				"7: CpuInfo{socketId=0, coreId=3, threadId=1}\n", vcl.toString());
	}

	@Test
	public void testBitmasks() {
		// test if each core mask is wholly contained in its sockets mask
		IAffinity aff = Affinity.getAffinityImpl();
		Assume.assumeTrue("not running for " + aff.getClass(), aff instanceof IDefaultLayoutAffinity);
		IDefaultLayoutAffinity idl = (IDefaultLayoutAffinity) aff;
		CpuLayout cpuLayout = idl.getDefaultLayout();
		Assume.assumeTrue("not running if no default layout ", cpuLayout != null);
		Assume.assumeTrue("not VanillaCpuLayout for " + cpuLayout, cpuLayout instanceof VanillaCpuLayout);
		VanillaCpuLayout vanilla = (VanillaCpuLayout) cpuLayout;
		for (Core core : vanilla.cores) {
			BitSet coreMask = core.getBitSetMask();
			Assume.assumeTrue("not running if no bitmask (group layout?", coreMask != null);
			BitSet intersection = (BitSet) core.getBitSetMask().clone();
			Socket socket = core.getSocket();
			Assume.assumeTrue("core " + core + " must have socket", socket != null);
			BitSet socketMask = (BitSet) socket.getBitSetMask().clone();
			intersection.and(socketMask);  // expect: all bits remain set
			// if there are bits missing, show them here
			BitSet missing = (BitSet) core.getBitSetMask().clone();
			missing.andNot(intersection);
			assertEquals("not wholly contained: " + coreMask + " in " + socketMask + ", missing: " + missing, intersection, coreMask);
		}
	}


}
