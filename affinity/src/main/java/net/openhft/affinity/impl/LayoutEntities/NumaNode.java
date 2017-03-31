package net.openhft.affinity.impl.LayoutEntities;

/**
 * Created by rhelbing on 31.03.17.
 */
public class NumaNode extends LayoutEntity {

    protected NumaNode(GroupAffinityMask m) {
        super(m);
    }

    public NumaNode(int index, long mask) {
        super(index, mask);
    }

    public String toString() {
        return "N " + super.toString();
    }

    public String getLocation() {
        if (groupAffinityMask != null) {
            return groupAffinityMask.groupId + "/" + getId();
        }
        return String.valueOf( getId());
    }
}
