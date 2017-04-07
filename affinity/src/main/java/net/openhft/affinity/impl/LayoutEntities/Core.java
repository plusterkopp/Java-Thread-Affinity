package net.openhft.affinity.impl.LayoutEntities;

import org.jetbrains.annotations.Nullable;

import java.util.*;

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

    public int hashCode() {
        int socketId = -1;
        if (socket != null) {
            socketId = socket.getId();
        }
        return 1023 * socketId + getId();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Core otherCore = (Core) o;
        if ( getId() != otherCore.getId()) {
            return false;
        }
        Socket otherSocket = otherCore.getSocket();
        if ( ! Objects.equals( otherSocket, getSocket())) {
            return false;
        }
        return true;

    }


}
