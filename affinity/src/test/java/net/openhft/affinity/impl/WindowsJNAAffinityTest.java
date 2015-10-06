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

import com.sun.jna.platform.win32.*;
import net.openhft.affinity.*;
import oracle.jrockit.jfr.events.*;
import org.junit.*;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author peter.lawrey
 */
public class WindowsJNAAffinityTest extends AbstractAffinityImplTest {

	static private WindowsJNAAffinity WinImpl;

	@BeforeClass
	public static void checkJnaLibraryPresent() {
		final String osName = System.getProperty("os.name");
		final boolean isWin = osName.contains("Win");
		Assume.assumeTrue(isWin);
		Assume.assumeTrue(WindowsJNAAffinity.LOADED);
		IAffinity impl = Affinity.getAffinityImpl();
		Assume.assumeTrue((impl instanceof WindowsJNAAffinity));
		WinImpl = (WindowsJNAAffinity) impl;
	}

    @Override
    public IAffinity getImpl() {
        return Affinity.getAffinityImpl();
    }

	@Test
	public void testGetAffinityGroups() {
		int numCPUs = Runtime.getRuntime().availableProcessors();
		if (numCPUs < 2) {
			return;
		}
		// must find at least one group
		Map<Integer, BitSet> groups = WinImpl.getAffinityGroups();
		assertTrue("Must have at least one group", groups.size() > 0);
		System.out.println( "Groups: " + groups);
	}

	@Test
	public void testSet0() {
		long[]  longMask = new long[ 1];
		longMask[ 0] = 1;
		BitSet mask = BitSet.valueOf(longMask);
		WinImpl.setThreadAffinity(mask);
		BitSet after = WinImpl.getAffinity();
		assertEquals("Returned mask " + after + " does not match " + mask, mask, after);
	}

	@Test
	public void testShow() {
		testRelationType( "all relations", WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationAll);
		 testRelationType( "cores", WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore);
		 testRelationType("packages", WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage);
		 testRelationType( "caches", WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache);
		 testRelationType("nodes", WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode);
		 testRelationType("groups", WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup);
	}

	void testRelationType( String name, int typeID) {
		System.out.println("\n" + name + ": ");
		try {
			WindowsJNAAffinity.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] infos = WinImpl.getLogicalProcessorInformation(typeID);
			for (WindowsJNAAffinity.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX slpi : infos) {
				System.out.println(
//						"at: " + slpi.getPointer() +
						slpi + ", size " + slpi.size);
			}
			System.err.flush();
			System.out.flush();
			for (WindowsJNAAffinity.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX slpi : infos) {
				assertTrue("strange relation type: " + slpi, WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup >= slpi.relationShip);
				assertTrue("strange size: " + slpi, slpi.size() < 500);
			}
		} catch ( Exception e) {
			e.printStackTrace( System.out);
		}
		System.err.flush();
		System.out.flush();
	}

	@Test
	public void testCpuInfosCount() {
		WindowsCpuLayout l = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		Map<Integer, BitSet> groups = WinImpl.getAffinityGroups();
		int cpuCount = 0;
		for (BitSet mask : groups.values()) {
			cpuCount += mask.cardinality();
		}
		assertEquals("should have " + cpuCount + " cpuInfos, found only " + l.cpus(), cpuCount, l.cpus());
		for (int i = 0; i < cpuCount; i++) {
			WindowsCpuLayout.CpuInfo info = l.lCpu(i);
			System.out.println("cpu " + i + ": " + info);
		}
	}

	@Test
	public void testCpuInfoMasks() {
		WindowsCpuLayout    l = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		l.visitCpus(info1 -> {
			l.visitCpus(info2 -> {
				if (info1 != info2) {
					if (info1.groupId == info2.groupId) {
						BitSet mask1 = WinImpl.asBitSet(info1.mask);
						BitSet mask2 = WinImpl.asBitSet(info2.mask);
						assertFalse("Masks for " + info1 + " and " + info2 + " must not intersect", mask1.intersects(mask2));
					}
				}
			});
		});
	}

	@Test
	public void testCpuInfoMaskCardinality() {
		WindowsCpuLayout    l = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		l.visitCpus( info1 -> {
			BitSet mask1 = WinImpl.asBitSet(info1.mask);
			assertEquals("Mask Cardinality for " + info1 + " not " + 1, 1, mask1.cardinality());
		});
	}

	@Test
	public void testCpuInfoSwitch() {
		WindowsCpuLayout    cpuLayout = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		int index = WinImpl.getCpu();
		WindowsCpuLayout.CpuInfo current = cpuLayout.lCpu(index);
		System.out.println( "running on #" + index + ": " + current);
		cpuLayout.visitCpus(info1 -> {
			long switched = System.nanoTime();
			WinImpl.setGroupAffinity(info1.groupId, info1.mask);
			int i = WinImpl.getCpu();
			WindowsCpuLayout.CpuInfo curr = cpuLayout.lCpu(i);
			assertEquals("running on " + curr + ", not " + info1, info1, curr);
			long since = System.nanoTime() - switched;
			System.out.println("time: " + (since * 1e-3) + " µs, running on " + curr);
		});
		System.out.flush();
	}

}
