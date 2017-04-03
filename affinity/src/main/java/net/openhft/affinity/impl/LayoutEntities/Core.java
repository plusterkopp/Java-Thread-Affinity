package net.openhft.affinity.impl.LayoutEntities;

import java.util.BitSet;

/**
 * Created by rhelbing on 31.03.17.
 */
public class Core extends LayoutEntity {
    private Socket socket;

    public Core(GroupAffinityMask m) {
        super(m);
    }

    public Core(int group, long mask) {
        super(group, mask);
    }

    public Core(BitSet mask) {
        super( mask);
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public String toString() {
        return "C " + super.toString() + " on " + getSocket();
    }

    public String getLocation() {
        return getSocket().getLocation() + "/" + getId();
    }

}
