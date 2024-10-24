package net.openhft.affinity.impl;

import com.sun.jna.platform.win32.WinNT;
import net.openhft.affinity.*;
import net.openhft.affinity.impl.LayoutEntities.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Provides another method to define a layout using a simple string.
 * Created by ralf h on 23.01.2014.
 */
public class WindowsCpuLayout extends ApicCpuLayout implements NumaCpuLayout, GroupedCpuLayout, CacheCpuLayout {

	private final List<WindowsCpuInfo> cpuDetailsFull;
	private final Map<Integer, WindowsCpuInfo> cpuDetailsByApic = new HashMap<>();

	public final List<Group> groups;
	private final List<Cache> caches;
	public List<NumaNode> nodes;

	static private List<ICpuInfo> toVanillaDetails(List<ICpuInfo> details) {
		List<ICpuInfo> vanillaDetails = new ArrayList<>(details.size());
		for (ICpuInfo fullInfo : details) {
			VanillaCpuInfo vi = new VanillaCpuInfo(fullInfo.getSocketId(), fullInfo.getCoreId(), fullInfo.getThreadId());
			vanillaDetails.add(vi);
		}
		return vanillaDetails;
	}

	WindowsCpuLayout(@NotNull List<ICpuInfo> cpuDetails, SortedSet<Group> groupSet, SortedSet<NumaNode> nodeSet,
					 SortedSet<Socket> packageSet, SortedSet<Core> coreSet, SortedSet<Cache> cacheSet) {

		super(toVanillaDetails(cpuDetails));
		cpuDetailsFull = new ArrayList<>(cpuDetails.size());
		for (ICpuInfo info : cpuDetails) {
			if (info instanceof WindowsCpuInfo) {
				WindowsCpuInfo wInfo = (WindowsCpuInfo) info;
				cpuDetailsFull.add(wInfo);
				cpuDetailsByApic.put(wInfo.getApicId(), wInfo);
			}
		}
		groups = Collections.unmodifiableList(new ArrayList<>(groupSet));
		nodes = Collections.unmodifiableList(new ArrayList<>(nodeSet));
		packages = Collections.unmodifiableList(new ArrayList<>(packageSet));
		cores = Collections.unmodifiableList(new ArrayList<>(coreSet));
		caches = Collections.unmodifiableList(new ArrayList<>(cacheSet));
		caches.forEach(cache -> cache.setLayout(this));
	}

	public static WindowsCpuLayout fromSysInfo(WindowsJNAAffinity.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] sysInfo) {

		SortedSet<Group> groups = new TreeSet<>();
		SortedSet<NumaNode> nodes = new TreeSet<>();
		SortedSet<Socket> packages = new TreeSet<>();
		SortedSet<Core> cores = new TreeSet<>();
		SortedSet<Cache> caches = new TreeSet<>();
		List<ICpuInfo> cpuInfos = WindowsCpuLayout.asCpuInfos(sysInfo, groups, nodes, packages, cores, caches);
		WindowsCpuLayout result = new WindowsCpuLayout(cpuInfos, groups, nodes, packages, cores, caches);
		return result;
	}

	static List<ICpuInfo> asCpuInfos(WindowsJNAAffinity.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] lpis,
									 SortedSet<Group> groups, SortedSet<NumaNode> nodes,
									 SortedSet<Socket> sockets, SortedSet<Core> cores, SortedSet<Cache> caches) {

		// gather LayoutEntities and set their group affinity mask
		for (WindowsJNAAffinity.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX lpi : lpis) {
			switch (lpi.relationShip) {
				case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup:
					groups.addAll(lpi.asGroups());
					break;
				case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage:
					sockets.add(lpi.asPackage());
					break;
				case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode:
					nodes.add(lpi.asNumaNode());
					break;
				case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore:
					cores.add(lpi.asCore());
					break;
				case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache:
					caches.add(lpi.asCache());
					break;
				default:    // no longer ignore Caches
			}
		}

		// create and set up CpuInfos with group id and group-relative mask
		int id = 0;
		int bits = 0;
		// first, sum the sizes of each group as given in the cardinality of their bit mask
		for (Group g : groups) {
			g.setId(id++);
			bits += Long.bitCount(g.getGroupMask().getMask());
		}
		// create all CpuInfos, set their fields later
		List<ICpuInfo> cpuInfos = createInfoList(bits);

		// Field GroupID
		// for each group with size S, set the groupID for the next S cpuInfos
		int cpuID = 0;
		for (Group g : groups) {
			BitSet bs = WindowsJNAAffinity.asBitSet(g.getGroupMask().getMask());
			int[] positions = bs.stream().toArray();
			for (int ignored : positions) {
				final ICpuInfo cpuInfo = cpuInfos.get(cpuID);
				if (cpuInfo instanceof IGroupCpuInfo) {
					IGroupCpuInfo gi = (IGroupCpuInfo) cpuInfo;
					gi.setGroupId(g.getId());
				}
				cpuID++;
			}
		}

		// Numa Node ID
		id = 0;
		for (NumaNode n : nodes) {
			n.setId(id++);
			final int nodeGroupId = n.getGroupMask().getGroupId();
			n.setEntityIds(cpuInfos, nodeGroupId, pos -> {
				final ICpuInfo cpuInfo = cpuInfos.get(pos);
				if (cpuInfo instanceof INumaCpuInfo) {
					INumaCpuInfo ni = (INumaCpuInfo) cpuInfo;
					ni.setNode(n);
				}
			});
		}

		// Socket ID
		NumaNode[] nodeArr = nodes.toArray(new NumaNode[nodes.size()]);
//		System.out.println( "infos: " + cpuInfos);
		id = 0;
		for (Socket s : sockets) {
			s.setId(id++);
			final int socketGroupId = s.getGroupMask().getGroupId();
			s.setEntityIds(cpuInfos, socketGroupId, pos -> {
				final ICpuInfo cpuInfo = cpuInfos.get(pos);
				cpuInfo.setSocketId(s.getId());
				if (cpuInfo instanceof INumaCpuInfo) {
					INumaCpuInfo ni = (INumaCpuInfo) cpuInfo;
					s.setNode(nodeArr[ni.getNodeId()]);
				}
			});
		}

		// Core ID
		Socket[] socketArr = sockets.toArray(new Socket[sockets.size()]);
		id = 0;
		for (Core c : cores) {
			c.setId(id++);
			final int coreGroupId = c.getGroupMask().getGroupId();
			c.setEntityIds(cpuInfos, coreGroupId, pos -> {
				final ICpuInfo cpuInfo = cpuInfos.get(pos);
				c.setSocket(socketArr[cpuInfo.getSocketId()]);
				cpuInfo.setCoreId(c.getId());
			});
		}

		// Caches
		id = 0;
		for (Cache ca : caches) {
			ca.setId(id++);
		}


		// Thread IDs and masks
		List<Core> coreList = new ArrayList<>(cores);
		// set threadIds to be relative to coreId
		for (int i = 0; i < cpuInfos.size(); i++) {
			ICpuInfo cpuInfo = cpuInfos.get(i);
			if (i == 0) {  // first one is 0
				cpuInfo.setThreadId(0);
			} else {    // if last info share the same core, increment our threadId over the last one
				ICpuInfo prev = cpuInfos.get(i - 1);
				if (prev.getCoreId() == cpuInfo.getCoreId()) {
					cpuInfo.setThreadId(prev.getThreadId() + 1);
				} else {    // if new core, start threadId at 0
					cpuInfo.setThreadId(0);
				}
			}
			if (cpuInfo instanceof WindowsCpuInfo) {
				WindowsCpuInfo windowsCpuInfo = (WindowsCpuInfo) cpuInfo;
				// starting from the mask of the corresponding core, retain the set bit at position threadId and clear all others, remember the resulting mask for this cpuInfo
				Core c = coreList.get(cpuInfo.getCoreId());
				BitSet coreMask = WindowsJNAAffinity.asBitSet(c.getGroupMask().getMask());
				int setBitCount = 0;
				int p;
				for (p = coreMask.nextSetBit(0); p >= 0; p = coreMask.nextSetBit(p + 1)) {
					if (setBitCount != cpuInfo.getThreadId()) {
						coreMask.clear(p);
					}
					setBitCount++;
				}
				long[] maskRet = coreMask.toLongArray();
				try {
					windowsCpuInfo.setMask(maskRet[0]);
				} catch (Exception e) {
					throw e;
				}
			}
		}
		return cpuInfos;
	}

	/**
	 * @param size size
	 * @return unmodifiable List of size CpuInfo
	 */
	@NotNull
	private static List<ICpuInfo> createInfoList(int size) {
		List<WindowsCpuInfo> cpuInfos = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			final WindowsCpuInfo cpuInfo = new WindowsCpuInfo();
			cpuInfo.setThreadId(i);
			cpuInfo.setApicId(i);
			cpuInfos.add(cpuInfo);
		}
		return Collections.unmodifiableList(cpuInfos);
	}

	@Override
	public int groupId(int cpuId) {
		return cpuDetailsFull.get(cpuId).getGroupId();
	}

	@Override
	public int groups() {
		return groups.size();
	}

	@Override
	public List<? extends Group> getGroups() {
		return groups;
	}

	@Override
	public int numaNodeId(int cpuId) {
		return cpuDetailsFull.get(cpuId).getNodeId();
	}

	public int findCpuInfo(int groupId, int lCPUInGroup) {
		if (lCPUInGroup > 63) {
			return -1;
		}
		BitSet bs = new BitSet(64);
		bs.set(lCPUInGroup);
		long[] maskA = bs.toLongArray();
		for (int i = 0; i < cpuDetailsFull.size(); i++) {
			WindowsCpuInfo cpuInfo = cpuDetailsFull.get(i);
			if (cpuInfo.getGroupId() != groupId) {
				continue;
			}
			if (cpuInfo.getMask() != maskA[0]) {
				continue;
			}
			return i;
		}
		return -1;
	}

	public WindowsCpuInfo lCpu(int index) {
		return cpuDetailsFull.get(index);
	}

	public void visitCpus(Consumer<WindowsCpuInfo> c) {
		for (WindowsCpuInfo info : cpuDetailsFull) {
			c.accept(info);
		}
	}

	@Override
	public int numaNodes() {
		return nodes.size();
	}

	@Override
	public List<? extends NumaNode> getNodes() {
		return nodes;
	}

	@Override
	public long mask(int cpuId) {
		WindowsCpuInfo cpuInfo = lCpu(cpuId);
		return cpuInfo.getMask();
	}

	public Stream<Cache> cachesIntersecting(int cpuId) {
		WindowsCpuInfo cpuInfo = lCpu(cpuId);
		return caches.stream().
				filter(cache -> cache.intersects(cpuInfo.getGroupId(), cpuInfo.getMask()));
	}


	public List<Cache> getCaches() {
		return caches;
	}

	public List<Cache> getCachesForCore(int cpuId) {
		List<Cache> allCaches = getCaches();
		List<Cache> result = new ArrayList<>();
		int coreId = coreId(cpuId);
		Core core = cores.get(coreId);
		GroupAffinityMask coreGAM = core.getGroupMask();
		allCaches.forEach(cache -> {
			GroupAffinityMask cacheGAM = cache.getGroupMask();
			if (cacheGAM.equals(coreGAM)) {
				result.add(cache);
			}
		});
		return result;
	}

}
