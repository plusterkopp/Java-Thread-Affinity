package net.openhft.affinity.impl.LayoutEntities;

import net.openhft.affinity.*;
import net.openhft.affinity.impl.GroupAffinityMask;
import net.openhft.affinity.impl.WindowsJNAAffinity;

import java.util.*;
import java.util.function.IntConsumer;

/**
 * Created by rhelbing on 31.03.17.
 */
public abstract class LayoutEntity implements Comparable<LayoutEntity> {
    final Set<Thread> threads = new HashSet<>();
    final GroupAffinityMask groupAffinityMask;  // only for Windows, when set, bitsetMask must be null
    final BitSet    bitsetMask; // if set, groupAffinityMask must be null
    private int id;

    protected LayoutEntity(int groupID, long mask) {
        this(new GroupAffinityMask(groupID, mask));
    }

	protected LayoutEntity(GroupAffinityMask m) {
		groupAffinityMask = m;
		bitsetMask = null;
	}

	protected LayoutEntity(BitSet m) {
		groupAffinityMask = null;
		bitsetMask = m;
	}

	/**
     * using the mask, call the consumer with each index in the bitset that is set and has matching group (assuming infos with same group ID are consecutive)
     *
     * @param cpuInfos
     * @param c
     */
    public void setEntityIds(List<ICpuInfo> cpuInfos, int groupID, IntConsumer c) {
	    BitSet bs = bitsetMask != null ? bitsetMask : WindowsJNAAffinity.asBitSet(groupAffinityMask.getMask());
        // find lowest index in cpuInfos with matching group ID
        int index = 0;
        for (index = 0; index < cpuInfos.size(); index++) {
            ICpuInfo info = cpuInfos.get(index);
            if (info instanceof IGroupCpuInfo) {
                IGroupCpuInfo groupInfo = (IGroupCpuInfo) info;
                final boolean ok = groupInfo.getGroupId() == groupID;
                if (ok) {
                    break;
                }
            } else {    // no groups, assume match
                break;
            }
        }
        int startIndex = index;
        bs.stream().map(pos -> pos + startIndex).forEach(c);
    }

    @Override
    public int compareTo(LayoutEntity o) {
    	// hope this doesn't get called too often
	    if (bitsetMask != null && o.bitsetMask != null) {
	    	String s1 = bitsetMask.toString();
	    	String s2 = o.bitsetMask.toString();
	    	return s1.compareTo( s2);
	    }
	    // I don't expect to compare two Entities with different mask modes
	    return groupAffinityMask.compareTo(o.groupAffinityMask);
    }

    public void setId(int id) {
        this.id = id;
    }

    public GroupAffinityMask getGroupMask() {
        return groupAffinityMask;
    }

    public BitSet getBitMask() {
    	return bitsetMask;
    }

    public int getId() {
        return id;
    }

    public void bind() {
        Thread t = Thread.currentThread();
        IAffinity impl = Affinity.getAffinityImpl();
        if ( groupAffinityMask != null) {
	        if (impl instanceof IGroupAffinity) {
		        IGroupAffinity ga = (IGroupAffinity) impl;
		        ga.setGroupAffinity(groupAffinityMask.getGroupId(), groupAffinityMask.getMask());
	        } else {
		        impl.setAffinity(WindowsJNAAffinity.asBitSet(groupAffinityMask.getMask()));
	        }
        } else {
        	impl.setAffinity( bitsetMask);
        }
        synchronized (threads) {
            threads.add(t);
        }
        AffinityManager.getInstance().unregisterFromOthers(this, t);
    }

    /**
     * create list of threads in my set that are still alive, clear my set and add all living threads from the list, return the list,
     * synchronized on the thread set
     *
     * @return List of alive threads
     */
    public List<Thread> getThreads() {
        synchronized (threads) {
            List<Thread> result = new ArrayList<>(threads.size());
            for (Thread t : threads) {
                if (t.isAlive()) {
                    result.add(t);
                }
            }
            threads.clear();
            threads.addAll(result);
            return result;
        }
    }

    /**
     * @return id/group/maskAsBinary
     */
    @Override
    public String toString() {
	    if (bitsetMask != null) {
	    	StringBuilder sb = new StringBuilder( 300);
	    	sb.append( "ID: ")
				    .append( getId())
				    .append( " M: ");
		    StringBuilder    bitSB = new StringBuilder( 200);
		    long   bits[] = bitsetMask.toLongArray();
		    for (int i = bits.length - 1; i >= 0; i--) {
			    bitSB.append( longMaskToString( bits[ i]))
					    .append(" ");
		    }
		    bitSB.setLength( bitSB.length() - 1);
		    sb.append( bitSB);
		    return sb.toString();
	    }
	    // Windows: using GroupMask instead of BitSet
	    return "ID: " + getId() + " GM: " + groupAffinityMask.getGroupId() + "/" + longMaskToString( groupAffinityMask.getMask());
    }

    String longMaskToString( long l) {
		StringBuilder sb = new StringBuilder( 100);
		sb.append( Long.toBinaryString( l));
		int groupSize = getGroupSize();
	    char[] zeros = new char[ groupSize - sb.length()];
	    Arrays.fill( zeros, '0');
	    sb.insert( 0, zeros);
	    for ( int i = sb.length() - 8;  i > 0;  i -= 8) {
		    sb.insert( i, '.');
	    }
	    String result = sb.toString();
	    result = result.replace( "11111111", "FF");
	    result = result.replace( "00000000", "OO");
	    return result;
    }

    private int getGroupSize() {
        CpuLayout cpuLayout = AffinityManager.getInstance().getLayout();
        if ( cpuLayout instanceof GroupedCpuLayout) {
            GroupedCpuLayout groupLayout = (GroupedCpuLayout) cpuLayout;
            int groupId = groupAffinityMask.getGroupId();
            List<? extends Group> groups = groupLayout.getGroups();
            Group group = groups.get(groupId);
            return group.getMSB();
        }
        return cpuLayout.cpus();
    }

    /**
     * @return String component to describe location of this {@link LayoutEntity}
     */
    public abstract String getLocation();

    public void unregister(Thread t) {
        synchronized (threads) {
            threads.remove(t);
        }
    }

    public boolean intersects(int groupId, long mask) {
        if ( groupId != groupAffinityMask.getGroupId()) {
            return false;
        }
        return ( mask & groupAffinityMask.getMask()) != 0;
    }
}
