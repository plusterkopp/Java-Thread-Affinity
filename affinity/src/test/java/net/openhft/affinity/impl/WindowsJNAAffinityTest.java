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
import org.junit.*;

import java.text.*;
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
		Map<Integer, BitSet> numaNodes = WinImpl.getNumaNodes();
		assertTrue("Must have at least one node", numaNodes.size() > 0);
		System.out.println( "Nodes: " + numaNodes);
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
		Map<Integer, BitSet> groups = WinImpl.getNumaNodes();
		int cpuCount = 0;
		for (BitSet mask : groups.values()) {
			cpuCount += mask.cardinality();
		}
		assertEquals("should have " + cpuCount + " cpuInfos, found only " + l.cpus(), cpuCount, l.cpus());
		for (int i = 0; i < cpuCount; i++) {
			WindowsCpuInfo info = l.lCpu(i);
			System.out.println("cpu " + i + ": " + info);
		}
	}

	@Test
	public void testCpuInfoMasks() {
		WindowsCpuLayout    l = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		l.visitCpus(info1 -> {
			l.visitCpus(info2 -> {
				if (info1 != info2) {
					if (info1.getGroupId() == info2.getGroupId()) {
						BitSet mask1 = Affinity.asBitSet(info1.getMask());
						BitSet mask2 = Affinity.asBitSet(info2.getMask());
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
			BitSet mask1 = Affinity.asBitSet(info1.getMask());
			assertEquals("Mask Cardinality for " + info1 + " not " + 1, 1, mask1.cardinality());
		});
	}

	@Test
	public void testCpuInfoSwitch() {
		WindowsCpuLayout    cpuLayout = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		int index = WinImpl.getCpu();
		WindowsCpuInfo current = cpuLayout.lCpu(index);
		System.out.println("running on #" + index + ": " + current);
		NumberFormat    nf = DecimalFormat.getNumberInstance();
		nf.setGroupingUsed( true);
		nf.setMaximumFractionDigits( 3);
		cpuLayout.visitCpus(info1 -> {
			long switched = System.nanoTime();
			WinImpl.setGroupAffinity(info1.getGroupId(), info1.getMask());
			int i = WinImpl.getCpu();
			WindowsCpuInfo curr = cpuLayout.lCpu(i);
			assertEquals("running on " + curr + ", not " + info1, info1, curr);
			long since = System.nanoTime() - switched;
			System.out.println("time: " + nf.format(since * 1e-3) + " µs, running on " + curr);
		});
		System.out.flush();
	}

	@Test
	public void testJNADirect() {
		short[] groupA = new short[1];
		byte[]  cpuA = new byte[1];
		WindowsJNAAffinity.PROCESSOR_NUMBER.ByReference pNum = new WindowsJNAAffinity.PROCESSOR_NUMBER.ByReference();

		WindowsCpuLayout    cpuLayout = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		cpuLayout.visitCpus(info1 -> {
			WinImpl.setGroupAffinity( info1.getGroupId(), info1.getMask());
			WinImpl.getCurrentCpuInfo(groupA, cpuA);
			WinImpl.getCurrentProcessorNumber(pNum);
			System.out.println("direct: " + groupA[0] + "/" + cpuA[0]);
			System.out.println("struct: " + pNum.group + "/" + pNum.number);
			Assert.assertEquals("group mismatch", groupA[0], pNum.group.shortValue());
			Assert.assertEquals("cpuid mismatch", cpuA[0], pNum.number.byteValue());
		});
		System.out.flush();
	}

}
