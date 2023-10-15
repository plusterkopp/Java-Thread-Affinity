package net.openhft.affinity.impl;

import net.openhft.affinity.ICpuInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by rhelbing on 07.10.2015.
 */
class VanillaCpuInfo implements ICpuInfo {
    private int socketId;
    /** local to socketId */
    private int coreId;
	/** local to socketId + coreId */
    private int threadId;
    private int apicid = -1;    // not set in some use cases

    VanillaCpuInfo() {
    }

    VanillaCpuInfo(int socketId, int coreId, int threadId) {
        this.socketId = socketId;
        this.coreId = coreId;
        this.threadId = threadId;
    }

    VanillaCpuInfo(int socketId, int coreId, int threadId, int apicid) {
        this( socketId, coreId, threadId);
        this.apicid = apicid;
    }

    @NotNull
    @Override
    public String toString() {
        return "CpuInfo{" +
		        ( apicid == -1 ? "" : "apicId=" + apicid + ", ") +
                "socketId=" + socketId +
                ", coreId=" + coreId +
                ", threadId=" + threadId +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VanillaCpuInfo cpuInfo = (VanillaCpuInfo) o;

        if (coreId != cpuInfo.coreId) {
            return false;
        }
        if (socketId != cpuInfo.socketId) {
            return false;
        }
        return threadId == cpuInfo.threadId;

    }

    @Override
    public int hashCode() {
        int result = socketId;
        result = 31 * result + coreId;
        result = 31 * result + threadId;
        result = 31 * result + apicid;
        return result;
    }

    @Override
    public int getSocketId() {
        return socketId;
    }

    @Override
    public void setSocketId(int socketId) {
        this.socketId = socketId;
    }

    @Override
    public int getCoreId() {
        return coreId;
    }

    @Override
    public void setCoreId(int coreId) {
        this.coreId = coreId;
    }

    @Override
    public int getThreadId() {
        return threadId;
    }

    @Override
    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    void setApicId(int apicId) {
        this.apicid = apicId;
    }

    public int getApicId() {
    	return apicid;
    }
}
