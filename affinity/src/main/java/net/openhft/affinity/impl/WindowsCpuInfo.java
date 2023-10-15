package net.openhft.affinity.impl;

import net.openhft.affinity.*;
import net.openhft.affinity.impl.LayoutEntities.NumaNode;
import org.jetbrains.annotations.*;

/**
 * add numaId, groupId
 */
class WindowsCpuInfo extends VanillaCpuInfo implements IGroupCpuInfo, INumaCpuInfo {
	private NumaNode node = null;
	private int groupId = 0;
	private long mask = 0;

	WindowsCpuInfo() {}

	@NotNull
	@Override
	public String toString() {
		return "CpuInfo{" +
				" s" + getSocketId() +
				" c" + getCoreId() +
				" t" + getThreadId() +
				" n" + getNodeId() +
				" g" + groupId +
				" m" + WindowsJNAAffinity.asBitSet(mask) +
				'}';
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		WindowsCpuInfo cpuInfo = (WindowsCpuInfo) o;

		if (groupId != cpuInfo.groupId) return false;
		if (getNodeId() != cpuInfo.getNodeId()) return false;
		if (getCoreId() != cpuInfo.getCoreId()) return false;
		if (getSocketId() != cpuInfo.getSocketId()) return false;
		return getThreadId() == cpuInfo.getThreadId();

	}

	@Override
	public int hashCode() {
		int result = groupId;
		result = 31 * result + getNodeId();
		result = 31 * result + getSocketId();
		result = 31 * result + getCoreId();
		result = 71 * result + getThreadId();
		return result;
	}

	public void setMask(long mask) {
		this.mask = mask;
	}


	public int getNodeId() {
		return node.getId();
	}

	public void setNode(NumaNode n) {
		node = n;
	}

	public int getGroupId() {
		return groupId;
	}

	@Override
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public long getMask() {
		return mask;
	}
}
