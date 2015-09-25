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

	public static CpuLayout fromSysInfo(WindowsJNAAffinity.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] sysInfo) {
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

	    int id = 0;
	    int bits = 0;
	    for ( Group g : groups) {
	        g.id = id++;
	        bits += Long.bitCount( g.mask.mask);
	    }
	    List<CpuInfo> cpuInfos = createInfoList( bits);

	    for ( Group g : groups) {
	        g.setEntityIds(cpuInfos, pos -> cpuInfos.get(pos).groupdId = g.id);
	    }
	    id = 0;
	    for ( NumaNode n : nodes) {
	        n.id = id++;
	        n.setEntityIds( cpuInfos, pos -> cpuInfos.get( pos).numaId = n.id);
	    }
	    id = 0;
	    for ( Package p : packages) {
	        p.id = id++;
	        p.setEntityIds( cpuInfos, pos -> cpuInfos.get( pos).numaId = p.id);
	    }
	    id = 0;
	    for ( Core c : cores) {
	        c.id = id++;
	        c.setEntityIds( cpuInfos, pos -> cpuInfos.get( pos).coreId = c.id);
	    }
	    return cpuInfos;
	}

	public static List<CpuInfo> createInfoList( int size) {
		List<WindowsCpuLayout.CpuInfo> cpuInfos0 = new ArrayList<>( size);
		for ( int i = 0;  i < size;  i++) {
			final CpuInfo cpuInfo = new CpuInfo();
			cpuInfo.threadId = i;
			cpuInfos0.add( cpuInfo);
		}
		return Collections.unmodifiableList( cpuInfos0);
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
		return cpuDetailsFull.get( cpuId).groupdId;
	}

	@Override
	public int numaId(int cpuId) {
		return cpuDetailsFull.get( cpuId).numaId;
	}

	static class CpuInfo extends VanillaCpuLayout.CpuInfo {
		int numaId = 0;
		int groupdId = 0;

		CpuInfo() {}

		CpuInfo(int socketId, int coreId, int threadId) {
			this( socketId, coreId, threadId, 0, 0);
		}

		CpuInfo(int socketId, int coreId, int threadId, int numaId, int groupId) {
			super( socketId, coreId, threadId);
			this.numaId = numaId;
			this.groupdId = groupId;
		}

		@NotNull
		@Override
		public String toString() {
			return "CpuInfo{" +
					"socketId=" + socketId +
					", coreId=" + coreId +
					", threadId=" + threadId +
					", numaId=" + numaId +
					", groupId=" + groupdId +
					'}';
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CpuInfo cpuInfo = (CpuInfo) o;

			if (groupdId != cpuInfo.groupdId) return false;
			if (numaId != cpuInfo.numaId) return false;
			if (coreId != cpuInfo.coreId) return false;
			if (socketId != cpuInfo.socketId) return false;
			return threadId == cpuInfo.threadId;

		}

		@Override
		public int hashCode() {
			int result = groupdId;
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
	            if ( info.groupdId == id) {
	            }
	        }
	    }

	}
}
