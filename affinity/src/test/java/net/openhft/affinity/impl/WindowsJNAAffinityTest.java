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

import com.sun.jna.platform.win32.WinNT;
import net.openhft.affinity.Affinity;
import net.openhft.affinity.IAffinity;
import net.openhft.affinity.impl.LayoutEntities.Cache;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;

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
		System.out.println("Nodes: " + numaNodes);
	}

	@Test
	public void testSet0() {
		long[] longMask = new long[1];
		longMask[0] = 1;
		BitSet mask = BitSet.valueOf(longMask);
		WinImpl.setThreadAffinity(mask);
		BitSet after = WinImpl.getAffinity();
		assertEquals("Returned mask " + after + " does not match " + mask, mask, after);
	}

	@Test
	public void testShow() {
		testRelationType("all relations", WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationAll);
		testRelationType("cores", WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore);
		testRelationType("packages", WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage);
		testRelationType("caches", WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache);
		testRelationType("getNodes", WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode);
		testRelationType("groups", WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup);
	}

	void testRelationType(String name, int typeID) {
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
		} catch (Exception e) {
			e.printStackTrace(System.out);
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
		WindowsCpuLayout l = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		l.visitCpus(info1 -> {
			l.visitCpus(info2 -> {
				if (info1 != info2) {
					if (info1.getGroupId() == info2.getGroupId()) {
						BitSet mask1 = WindowsJNAAffinity.asBitSet(info1.getMask());
						BitSet mask2 = WindowsJNAAffinity.asBitSet(info2.getMask());
						assertFalse("Masks for " + info1 + " and " + info2 + " must not intersect", mask1.intersects(mask2));
					}
				}
			});
		});
	}

	@Test
	public void testCpuInfoMaskCardinality() {
		WindowsCpuLayout l = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		l.visitCpus(info1 -> {
			BitSet mask1 = WindowsJNAAffinity.asBitSet(info1.getMask());
			assertEquals("Mask Cardinality for " + info1 + " not " + 1, 1, mask1.cardinality());
		});
	}

	@Test
	public void testCpuInfoSwitch() {
		WindowsCpuLayout cpuLayout = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		int index = WinImpl.getCpu();
		WindowsCpuInfo current = cpuLayout.lCpu(index);
		System.out.println("running on #" + index + ": " + current + " enc " + System.getProperty("file.encoding"));
		NumberFormat nf = DecimalFormat.getNumberInstance();
		nf.setGroupingUsed(true);
		nf.setMaximumFractionDigits(3);
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
		byte[] cpuA = new byte[1];
		WindowsJNAAffinity.PROCESSOR_NUMBER.ByReference pNum = new WindowsJNAAffinity.PROCESSOR_NUMBER.ByReference();

		WindowsCpuLayout cpuLayout = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		cpuLayout.visitCpus(info1 -> {
			WinImpl.setGroupAffinity(info1.getGroupId(), info1.getMask());
			WinImpl.getCurrentCpuInfo(groupA, cpuA);
			WinImpl.getCurrentProcessorNumber(pNum);
			System.out.println("direct: " + groupA[0] + "/" + cpuA[0]);
			System.out.println("struct: " + pNum.group + "/" + pNum.number);
			Assert.assertEquals("group mismatch", groupA[0], pNum.group.shortValue());
			Assert.assertEquals("cpuid mismatch", cpuA[0], pNum.number.byteValue());
		});
		System.out.flush();
	}

	@Test
	public void basicSetAffinity() {
		WindowsCpuLayout cpuLayout = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		int nCPUs = cpuLayout.cpus();
		Assume.assumeTrue(nCPUs <= 64);
		for (int cpu = 0; cpu < nCPUs; cpu++) {
			BitSet affinity = new BitSet(nCPUs);
			affinity.set(cpu);
			WinImpl.setAffinity(affinity);
			BitSet affNow = WinImpl.getAffinity();
			System.out.println("cpu: " + cpu + " mask: " + affinity + " → " + affNow);
			Assert.assertEquals("affinity not set", affinity, affNow);
		}
	}

	@Test
	public void basicSetAffinityG() {
		WindowsCpuLayout cpuLayout = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		int nCPUs = cpuLayout.cpus();
		for (int cpu = 0; cpu < nCPUs; cpu++) {
			WindowsCpuInfo info = cpuLayout.lCpu(cpu);
			GroupAffinityMask newGAM = new GroupAffinityMask(info.getGroupId(), info.getMask());
			try {
				GroupAffinityMask previousGAM = WinImpl.setGroupAffinity(info.getGroupId(), info.getMask());
				System.out.println("cpu: " + cpu + " gam: " + newGAM);
			} catch (IllegalStateException ise) {
				Assert.fail("group affinity not set to gam: " + newGAM);
			}
		}
	}

	@Test
	public void basicSetAffinityApic() {
		WindowsCpuLayout cpuLayout = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		int nCPUs = cpuLayout.cpus();
		for (int expectedCPU = 0; expectedCPU < nCPUs; expectedCPU++) {
			WinImpl.setAffinity(expectedCPU);
			int cpuid = Affinity.getCpu();
			assertEquals("expectedCPU not set", expectedCPU, cpuid);
		}
	}

	@Test
	public void listCaches() {
		WindowsCpuLayout cpuLayout = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		int nCPUs = cpuLayout.cpus();
		for (int cpu = 0; cpu < nCPUs; cpu++) {
			WindowsCpuInfo info = cpuLayout.lCpu(cpu);
			Cache l1 = cpuLayout.getCache(cpu, 1);
			Cache l2 = cpuLayout.getCache(cpu, 2);
			Cache l3 = cpuLayout.getCache(cpu, 3);
			System.out.print("cpu: " + info);
			if (l1 != null) {
				System.out.print(" L1: " + (l1.getSize() >> 10) + " kB, "
						+ "line " + l1.getLineSize() + " B, "
						+ "assoc " + (l1.getAssociativity() < 255
						? (l1.getAssociativity() + "x")
						: "full") + ", "
						+ "type " + l1.getType()
						+ " on cores " + Arrays.toString(l1.getCores())
						+ ",   ");
			}
			if (l2 != null) {
				System.out.print("L2: " + (l2.getSize() >> 10) + " kB, "
						+ "line " + l2.getLineSize() + " B, "
						+ "assoc " + (l2.getAssociativity() < 255
						? (l2.getAssociativity() + "x")
						: "full") + ", "
						+ "type " + l2.getType()
						+ " on cores " + Arrays.toString(l2.getCores())
						+ ",   ");
			}
			if (l3 != null) {
				System.out.print("L3: " + (l3.getSize() >> 10) + " kB, "
						+ "line " + l3.getLineSize() + " B, "
						+ "assoc " + (l3.getAssociativity() < 255
						? (l3.getAssociativity() + "x")
						: "full") + ", "
						+ "type " + l3.getType()
						+ " on cores " + Arrays.toString(l3.getCores())
						+ ",   ");
			}
			System.out.println();
		}
	}

	@Test
	public void listCacheCoreRelations() {
		WindowsCpuLayout cpuLayout = (WindowsCpuLayout) WinImpl.getDefaultLayout();
		cpuLayout.getCaches().forEach(cache -> {
			System.out.println("Cache " + cache + " on " + cache.getLocation());
		});
	}
}
