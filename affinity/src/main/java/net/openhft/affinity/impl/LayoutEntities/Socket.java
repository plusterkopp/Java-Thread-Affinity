package net.openhft.affinity.impl.LayoutEntities;

import net.openhft.affinity.impl.GroupAffinityMask;

import java.util.BitSet;

/**
 * Created by rhelbing on 31.03.17.
 */
public class Socket extends LayoutEntity {
    private NumaNode node;

    protected Socket(GroupAffinityMask m) {
        super(m);
    }

    public Socket(BitSet bitmask) {
        super( bitmask);
    }

    public Socket(int index, long mask) {
        super(index, mask);
    }


    public void setNode(NumaNode node) {
        this.node = node;
    }

    public NumaNode getNode() {
        return node;
    }

    public String toString() {
        return "S " + super.toString() + ( node == null ? "" : " on " + getNode());
    }

    public String getLocation() {
        return ( node == null ? "" : node.getLocation() + "/") + getId();
    }

}
