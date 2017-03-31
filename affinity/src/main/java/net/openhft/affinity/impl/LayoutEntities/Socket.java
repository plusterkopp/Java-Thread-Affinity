package net.openhft.affinity.impl.LayoutEntities;

/**
 * Created by rhelbing on 31.03.17.
 */
public class Socket extends LayoutEntity {
    private NumaNode node;

    protected Socket(GroupAffinityMask m) {
        super(m);
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
        return "S " + super.toString() + " on " + getNode();
    }

    public String getLocation() {
        return getNode().getLocation() + "/" + getId();
    }

}
