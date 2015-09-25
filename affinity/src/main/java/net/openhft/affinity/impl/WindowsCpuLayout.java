package net.openhft.affinity.impl;

import com.sun.jna.platform.win32.*;
import net.openhft.affinity.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.*;

/**
 * Provides another method to define a layout using a simple string.
 * Created by ralf h on 23.01.2014.
 */
public class WindowsCpuLayout extends VanillaCpuLayout implements NumaCpuLayout, GroupedCpuLayout {

	private final List<CpuInfo> cpuDetailsFull;

	WindowsCpuLayout(@NotNull List<CpuInfo> cpuDetails) {
		super( toVanillaDetails( cpuDetails));
		cpuDetailsFull = cpuDetails;
	}

	static private List<VanillaCpuLayout.CpuInfo> toVanillaDetails( List<CpuInfo> details) {
		List<VanillaCpuLayout.CpuInfo> vanillaDetails = new ArrayList<>( details.size());
		for (CpuInfo fullInfo : details) {
			VanillaCpuLayout.CpuInfo vi = new VanillaCpuLayout.CpuInfo( fullInfo.socketId, fullInfo.coreId, fullInfo.threadId);
			vanillaDetails.add( vi);
		}
		return vanillaDetails;
	}

	public static WindowsCpuLayout fromSysInfo(WindowsJNAAffinity.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] sysInfo) {
		List<WindowsCpuLayout.CpuInfo> cpuInfos = WindowsCpuLayout.asCpuInfos( sysInfo);
		return new WindowsCpuLayout( cpuInfos);
	}

	static List<CpuInfo> asCpuInfos(WindowsJNAAffinity.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] lpis) {
	    SortedSet<Group> groups = new TreeSet<>();
	    SortedSet<NumaNode> nodes = new TreeSet<>();
	    SortedSet<Package> packages = new TreeSet<>();
	    SortedSet<Core> cores = new TreeSet<>();

	    // gather LayoutEntities
	    for ( WindowsJNAAffinity.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX lpi : lpis) {
	        switch ( lpi.relationShip) {
	            case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup:
	                groups.addAll( lpi.asGroups()); break;
	            case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage:
	                packages.add(lpi.asPackage()); break;
	            case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode:
	                nodes.add(lpi.asNumaNode()); break;
	            case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore:
	                cores.add(lpi.asCore()); break;
	            default:    // ignore Caches
	        }
	    }

		// create and set up CpuInfos
	    int id = 0;
	    int bits = 0;
	    for ( Group g : groups) {
	        g.id = id++;
	        bits += Long.bitCount( g.mask.mask);
	    }
	    List<CpuInfo> cpuInfos = createInfoList( bits);

		try {
			for (Group g : groups) {
				g.setEntityIds(cpuInfos, pos -> cpuInfos.get(pos).groupId = g.id);
			}
		} catch ( Exception e) {
			throw e;
		}
	    id = 0;
	    for ( NumaNode n : nodes) {
	        n.id = id++;
	        n.setEntityIds( cpuInfos, pos -> cpuInfos.get( pos).numaId = n.id);
	    }
	    id = 0;
	    for ( Package p : packages) {
	        p.id = id++;
	        p.setEntityIds( cpuInfos, pos -> cpuInfos.get( pos).socketId = p.id);
	    }
	    id = 0;
	    for ( Core c : cores) {
	        c.id = id++;
	        c.setEntityIds( cpuInfos, pos -> cpuInfos.get( pos).coreId = c.id);
	    }
		List<Core> coreList = new ArrayList<>( cores);
		// set threadIds to be relative to coreId
		for ( int i = 0;  i < cpuInfos.size();  i++) {
			CpuInfo cpuInfo = cpuInfos.get( i);
			if ( i == 0) {  // first one is 0
				cpuInfo.threadId = 0;
			} else {    // if last info share the same core, increment our threadId over the last one
				CpuInfo prev = cpuInfos.get( i-1);
				if ( prev.coreId == cpuInfo.coreId) {
					cpuInfo.threadId = prev.threadId + 1;
				} else {    // if new core, start threadId at 0
					cpuInfo.threadId = 0;
				}
			}
			// starting from the mask of the corresponding core, retain the set bit at position threadId and clear all others, remember the resulting mask for this cpuInfo
			Core c = coreList.get( cpuInfo.coreId);
			BitSet coreMask = WindowsJNAAffinity.asBitSet(c.mask.mask);
			int setBitCount = 0;
			int p;
			for ( p = coreMask.nextSetBit( 0);  p >= 0;  p = coreMask.nextSetBit( p+1)) {
				if ( setBitCount != cpuInfo.threadId) {
					coreMask.clear( p);
				}
				setBitCount++;
			}
			long[]  maskRet = coreMask.toLongArray();
			try {
				cpuInfo.mask = maskRet[0];
			} catch ( Exception e) {
				throw e;
			}
		}
	    return cpuInfos;
	}

	/**
	 * @param size
	 * @return unmodifiable List of size CpuInfo
	 */
	@NotNull
	private static List<CpuInfo> createInfoList(int size) {
		List<WindowsCpuLayout.CpuInfo> cpuInfos = new ArrayList<>( size);
		for ( int i = 0;  i < size;  i++) {
			final CpuInfo cpuInfo = new CpuInfo();
			cpuInfo.threadId = i;
			cpuInfos.add(cpuInfo);
		}
		return Collections.unmodifiableList( cpuInfos);
	}

	/**
	 * To be compatible with {@link VanillaCpuLayout}, the address as s/c/t for the sequence of logical CPUs for a dual-socket, four-core, two-thread config is 0/0/0,
	 * 0/1/0 ... 0/3/0, 1/0/0 ... 1/3/0, ... 0/0/1 ... 1/3/1 which is (hopyfully) the same enumeration sequence as in
	 * cpuinfo files.
	 *
	 * @param desc
	 *            String "#sockets/#coresPerSocket/#threadsPerCore"
	 * @return a layout with s*c*t cpus
	 */
	@NotNull
	public static WindowsCpuLayout fromCpuDesc( String desc) {
		List<CpuInfo> cpuDetails = new ArrayList<CpuInfo>();
		String[]	descParts = desc.split( "\\s*/\\s*", 3);
		int	sockets = Integer.parseInt( descParts[ 0]);
		int	coresPerSocket = Integer.parseInt( descParts[ 1]);
		int	threadsPerCore = Integer.parseInt( descParts[ 2]);

		for ( int t = 0;  t < threadsPerCore;  t++) {
			for ( int s = 0;  s < sockets;  s++) {
				for ( int c = 0;  c < coresPerSocket;  c++) {
					CpuInfo	info = new CpuInfo( s,  c,  t);
					cpuDetails.add( info);
				}
			}
		}
		return new WindowsCpuLayout( cpuDetails);
	}

	@Override
	public int groupId(int cpuId) {
		return cpuDetailsFull.get( cpuId).groupId;
	}

	@Override
	public int numaId(int cpuId) {
		return cpuDetailsFull.get( cpuId).numaId;
	}

	public int findCpuInfo(int groupId, int lCPUInGroup) {
		if ( lCPUInGroup > 63) {
			return -1;
		}
		BitSet bs = new BitSet( 64);
		bs.set( lCPUInGroup);
		long[]    maskA = bs.toLongArray();
		for ( int i = 0;  i < cpuDetailsFull.size();  i++) {
			CpuInfo cpuInfo = cpuDetailsFull.get( i);
			if ( cpuInfo.groupId != groupId) {
				continue;
			}
			if ( cpuInfo.mask != maskA[ 0]) {
				continue;
			}
			return i;
		}
		return -1;
	}

	public CpuInfo lCpu(int index) {
		return cpuDetailsFull.get( index);
	}

	public void visitCpus( Consumer<CpuInfo> c) {
		for ( CpuInfo info : cpuDetailsFull) {
			c.accept( info);
		}
	}

	/**
	 * add numaId, groupId
	 */
	static class CpuInfo extends VanillaCpuLayout.CpuInfo {
		int numaId = 0;
		int groupId = 0;
		long mask = 0;

		CpuInfo() {}

		CpuInfo(int socketId, int coreId, int threadId) {
			this( socketId, coreId, threadId, 0, 0);
		}

		CpuInfo(int socketId, int coreId, int threadId, int numaId, int groupId) {
			super( socketId, coreId, threadId);
			this.numaId = numaId;
			this.groupId = groupId;
		}

		@NotNull
		@Override
		public String toString() {
			return "CpuInfo{" +
					"socketId=" + socketId +
					", coreId=" + coreId +
					", threadId=" + threadId +
					", numaId=" + numaId +
					", groupId=" + groupId +
					", mask=" + WindowsJNAAffinity.asBitSet( mask) +
					'}';
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CpuInfo cpuInfo = (CpuInfo) o;

			if (groupId != cpuInfo.groupId) return false;
			if (numaId != cpuInfo.numaId) return false;
			if (coreId != cpuInfo.coreId) return false;
			if (socketId != cpuInfo.socketId) return false;
			return threadId == cpuInfo.threadId;

		}

		@Override
		public int hashCode() {
			int result = groupId;
			result = 31 * result + numaId;
			result = 31 * result + socketId;
			result = 31 * result + coreId;
			result = 71 * result + threadId;
			return result;
		}
	}

	static class GroupAffinityMask implements Comparable<GroupAffinityMask> {
	    final int groupId;
	    final long    mask;

	    GroupAffinityMask(int groupId, long mask) {
	        this.groupId = groupId;
	        this.mask = mask;
	    }

	    @Override
	    public int compareTo( GroupAffinityMask o) {
	        int res = Integer.compare( groupId, o.groupId);
	        if ( res != 0) {
	            return res;
	        }
	        return Long.compare( mask, o.mask);
	    }
	}

	abstract static class LayoutEntity implements Comparable<LayoutEntity> {
	    final GroupAffinityMask mask;
	    int id;

	    protected LayoutEntity( int id, long mask) {
	        this( new GroupAffinityMask( id, mask));
	    }

	    protected LayoutEntity( GroupAffinityMask m) {
	        mask = m;
	    }

	    void setEntityIds( List<CpuInfo> cpuInfos, IntConsumer c) {
	        BitSet bs = WindowsJNAAffinity.asBitSet(mask.mask);
	        bs.stream().forEach( c);
	    }	    @Override
	    public int compareTo(LayoutEntity o) {
	        return mask.compareTo( o.mask);
	    }


	}

	static class Core extends LayoutEntity {
	    int[]   cpuIds;

	    protected Core(GroupAffinityMask m) {
	        super(m);
	    }

	    public Core(int index, long mask) {
	        super( index, mask);
	    }
	}

	static class Package extends LayoutEntity {
	    int[]   coreIds;

	    protected Package(GroupAffinityMask m) {
	        super(m);
	    }

	    public Package(int index, long mask) {
	        super( index, mask);
	    }
	}

	static class NumaNode extends LayoutEntity {
	    int[]   packageIds;

	    protected NumaNode(GroupAffinityMask m) {
	        super(m);
	    }

	    public NumaNode(int index, long mask) {
	        super( index, mask);
	    }
	}

	static class Group extends LayoutEntity {
	    int[]   cpuIds;

	    protected Group(GroupAffinityMask m) {
	        super(m);
	    }

	    public Group(int index, long mask) {
	        super( index, mask);
	    }

	    void setEntityIds( List<CpuInfo> cpuInfos, IntConsumer c) {
	        super.setEntityIds(cpuInfos, c);
	        for ( int i = 0;  i < cpuInfos.size();  i++) {
	            CpuInfo info = cpuInfos.get( i);
	            if ( info.groupId == id) {
	            }
	        }
	    }
	}
}
