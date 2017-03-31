package net.openhft.affinity.impl.LayoutEntities;

import net.openhft.affinity.impl.WindowsCpuLayout;

/**
 * used for Windows API
 * Created by rhelbing on 31.03.17.
 */
public class GroupAffinityMask implements Comparable<GroupAffinityMask> {

    final int groupId;
    final long mask;

    GroupAffinityMask(int groupId, long mask) {
        this.groupId = groupId;
        this.mask = mask;
    }

    @Override
    public int compareTo(GroupAffinityMask o) {
        int res = Integer.compare(groupId, o.groupId);
        if (res != 0) {
            return res;
        }
        return Long.compare(mask, o.mask);
    }

    public long getMask() {
        return mask;
    }

    public int getGroupId() {
        return groupId;
    }
}
