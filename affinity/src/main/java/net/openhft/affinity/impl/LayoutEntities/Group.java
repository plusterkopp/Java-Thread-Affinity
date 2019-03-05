package net.openhft.affinity.impl.LayoutEntities;

import net.openhft.affinity.impl.GroupAffinityMask;

/**
 * Created by rhelbing on 31.03.17.
 */
public class Group extends LayoutEntity {

    protected Group(GroupAffinityMask m) {
        super(m);
    }

    public Group(int index, long mask) {
        super(index, mask);
    }

    public String getLocation() {
        return "" + getId();
    }


}
