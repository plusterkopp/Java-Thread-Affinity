package net.openhft.affinity.impl;

import com.sun.jna.platform.win32.*;
import net.openhft.affinity.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Provides another method to define a layout using a simple string.
 * Created by ralf h on 23.01.2014.
 */
public class WindowsCpuLayout extends VanillaCpuLayout implements NumaCpuLayout, GroupedCpuLayout {

	private final List<WindowsCpuInfo> cpuDetailsFull;

	SortedSet<AffinityManager.Group> groupSet;
	SortedSet<AffinityManager.NumaNode> nodeSet;
	SortedSet<AffinityManager.Socket> packageSet;
	SortedSet<AffinityManager.Core> coreSet;

	static private List<VanillaCpuInfo> toVanillaDetails( List<ICpuInfo> details) {
		List<VanillaCpuInfo> vanillaDetails = new ArrayList<>( details.size());
		for (ICpuInfo fullInfo : details) {
			VanillaCpuInfo vi = new VanillaCpuInfo(fullInfo.getSocketId(), fullInfo.getCoreId(), fullInfo.getThreadId());
			vanillaDetails.add( vi);
		}
		return vanillaDetails;
	}

	WindowsCpuLayout(@NotNull List<ICpuInfo> cpuDetails, SortedSet<AffinityManager.Group> groups, SortedSet<AffinityManager.NumaNode> nodes, SortedSet<AffinityManager.Socket> packages, SortedSet<AffinityManager.Core> cores) {
		super( toVanillaDetails( cpuDetails));
		cpuDetailsFull = new ArrayList<>( cpuDetails.size());
		for ( ICpuInfo info: cpuDetails) {
			if ( info instanceof WindowsCpuInfo) {
				cpuDetailsFull.add((WindowsCpuInfo) info);
			}
		}
		groupSet = groups;
		nodeSet = nodes;
		packageSet = packages;
		groupSet = groups;
	}

	public static WindowsCpuLayout fromSysInfo(WindowsJNAAffinity.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] sysInfo) {

		SortedSet<AffinityManager.Group> groups = new TreeSet<>();
		SortedSet<AffinityManager.NumaNode> nodes = new TreeSet<>();
		SortedSet<AffinityManager.Socket> packages = new TreeSet<>();
		SortedSet<AffinityManager.Core> cores = new TreeSet<>();
		List<ICpuInfo> cpuInfos = WindowsCpuLayout.asCpuInfos( sysInfo, groups, nodes, packages, cores);
		WindowsCpuLayout result = new WindowsCpuLayout(cpuInfos, groups, nodes, packages, cores);
		return result;
	}

	static List<ICpuInfo> asCpuInfos(WindowsJNAAffinity.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] lpis,
	                                 SortedSet<AffinityManager.Group> groups, SortedSet<AffinityManager.NumaNode> nodes,
	                                 SortedSet<AffinityManager.Socket> sockets, SortedSet<AffinityManager.Core> cores) {

	    // gather LayoutEntities and set their group affinity mask
	    for ( WindowsJNAAffinity.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX lpi : lpis) {
	        switch ( lpi.relationShip) {
	            case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup:
	                groups.addAll( lpi.asGroups()); break;
	            case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage:
	                sockets.add(lpi.asPackage()); break;
	            case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode:
	                nodes.add(lpi.asNumaNode()); break;
	            case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore:
	                cores.add(lpi.asCore()); break;
	            default:    // ignore Caches
	        }
	    }

		// create and set up CpuInfos with group id and group-relative mask
	    int id = 0;
	    int bits = 0;
		// first, sum the sizes of each group as given in the cardinality of their bit mask
	    for ( AffinityManager.Group g : groups) {
	        g.setId( id++);
	        bits += Long.bitCount( g.getGroupMask().getMask());
	    }
		// create all CpuInfos, set their fields later
	    List<ICpuInfo> cpuInfos = createInfoList( bits);

		// Field GroupID
		// for each group with size S, set the groupID for the next S cpuInfos
		int cpuID = 0;
		for (AffinityManager.Group g : groups) {
			BitSet  bs = Affinity.asBitSet( g.getGroupMask().getMask());
			int[] positions = bs.stream().toArray();
			for ( int pos : positions) {
				final ICpuInfo cpuInfo = cpuInfos.get( cpuID);
				if ( cpuInfo instanceof IGroupCpuInfo) {
					IGroupCpuInfo gi = (IGroupCpuInfo) cpuInfo;
					gi.setGroupId( g.getId());
				}
				cpuID++;
			}
		}

		// Numa Node ID
	    id = 0;
	    for ( AffinityManager.NumaNode n : nodes) {
	        n.setId(id++);
		    final int nodeGroupId = n.getGroupMask().getGroupId();
	        n.setEntityIds( cpuInfos, pos -> {
		        final ICpuInfo cpuInfo = cpuInfos.get(pos);
		        // skip if wrong group
		        if ( cpuInfo instanceof IGroupCpuInfo) {
			        IGroupCpuInfo gi = (IGroupCpuInfo) cpuInfo;
			        if ( gi.getGroupId() != nodeGroupId) {
				        return;
			        }
		        }
		        if ( cpuInfo instanceof INumaCpuInfo) {
			        INumaCpuInfo ni = (INumaCpuInfo) cpuInfo;
			        ni.setNodeId( n.getId());
		        }
	        });
	    }

		// Socket ID
		AffinityManager.NumaNode[]  nodeArr = nodes.toArray( new AffinityManager.NumaNode[ nodes.size()]);
	    id = 0;
	    for ( AffinityManager.Socket s : sockets) {
	        s.setId(id++);
		    final int socketGroupId = s.getGroupMask().getGroupId();
	        s.setEntityIds( cpuInfos, pos -> {
		        final ICpuInfo cpuInfo = cpuInfos.get(pos);
		        // skip if wrong group
		        if ( cpuInfo instanceof IGroupCpuInfo) {
			        IGroupCpuInfo gi = (IGroupCpuInfo) cpuInfo;
			        if ( gi.getGroupId() != socketGroupId) {
				        return;
			        }
		        }
		        cpuInfo.setSocketId(s.getId());
		        if ( cpuInfo instanceof INumaCpuInfo) {
			        INumaCpuInfo ni = (INumaCpuInfo) cpuInfo;
			        s.setNode( nodeArr[ ni.getNodeId()]);
		        }
	        });
	    }

		// Core ID
		AffinityManager.Socket[]  socketArr = nodes.toArray( new AffinityManager.Socket[ sockets.size()]);
		id = 0;
	    for ( AffinityManager.Core c : cores) {
	        c.setId(id++);
		    final int coreGroupId = c.getGroupMask().getGroupId();
	        c.setEntityIds( cpuInfos, pos -> {
		        final ICpuInfo cpuInfo = cpuInfos.get(pos);
		        // skip if wrong group
		        if ( cpuInfo instanceof IGroupCpuInfo) {
			        IGroupCpuInfo gi = (IGroupCpuInfo) cpuInfo;
			        if ( gi.getGroupId() != coreGroupId) {
				        return;
			        }
		        }
		        c.setSocket( socketArr[ cpuInfo.getSocketId()]);
		        cpuInfo.setCoreId(c.getId());
	        });
	    }

		// Thread IDs and masks
		List<AffinityManager.Core> coreList = new ArrayList<>( cores);
		// set threadIds to be relative to coreId
		for ( int i = 0;  i < cpuInfos.size();  i++) {
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
				AffinityManager.Core c = coreList.get(cpuInfo.getCoreId());
				BitSet coreMask = Affinity.asBitSet(c.getGroupMask().getMask());
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
	 * @param size
	 * @return unmodifiable List of size CpuInfo
	 */
	@NotNull
	private static List<ICpuInfo> createInfoList(int size) {
		List<ICpuInfo> cpuInfos = new ArrayList<>( size);
		for ( int i = 0;  i < size;  i++) {
			final ICpuInfo cpuInfo = new WindowsCpuInfo();
			cpuInfo.setThreadId( i);
			cpuInfos.add(cpuInfo);
		}
		return Collections.unmodifiableList( cpuInfos);
	}

	@Override
	public int groupId(int cpuId) {
		return cpuDetailsFull.get(cpuId).getGroupId();
	}

	@Override
	public int groups() {
		return groupSet.size();
	}

	@Override
	public int numaNodeId(int cpuId) {
		return cpuDetailsFull.get(cpuId).getNodeId();
	}

	@Override
	public int numaNodes() {
		return nodeSet.size();
	}

	public int findCpuInfo(int groupId, int lCPUInGroup) {
		if ( lCPUInGroup > 63) {
			return -1;
		}
		BitSet bs = new BitSet( 64);
		bs.set( lCPUInGroup);
		long[]    maskA = bs.toLongArray();
		for ( int i = 0;  i < cpuDetailsFull.size();  i++) {
			WindowsCpuInfo cpuInfo = cpuDetailsFull.get( i);
			if ( cpuInfo.getGroupId() != groupId) {
				continue;
			}
			if ( cpuInfo.getMask() != maskA[ 0]) {
				continue;
			}
			return i;
		}
		return -1;
	}

	public WindowsCpuInfo lCpu(int index) {
		return cpuDetailsFull.get( index);
	}

	public void visitCpus( Consumer<WindowsCpuInfo> c) {
		for ( WindowsCpuInfo info : cpuDetailsFull) {
			c.accept( info);
		}
	}

}
