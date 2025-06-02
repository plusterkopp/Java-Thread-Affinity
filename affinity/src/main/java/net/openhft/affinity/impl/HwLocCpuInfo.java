package net.openhft.affinity.impl;

import net.openhft.affinity.INumaCpuInfo;
import net.openhft.affinity.impl.LayoutEntities.*;

import java.util.BitSet;

public class HwLocCpuInfo extends ApicCpuInfo implements INumaCpuInfo {

	NumaNode node;
	private Cache l1DCache;
	private Cache l1ICache;

	private Cache l2Cache;
	private Cache l3Cache;

	public HwLocCpuInfo(int id, int apicID, Core core, Cache l1ICache, Cache l1DCache, Cache l2Cache, Cache l3Cache, Socket socket, NumaNode numaNode) {
		setThreadId(id);
		setApicId(apicID);
		setNode(numaNode);
		setSocketId(socket.getId());
		setCoreId(core.getId());

		this.l1ICache = l1ICache;
		this.l1DCache = l1DCache;
		this.l2Cache = l2Cache;
		this.l3Cache = l3Cache;

		// set bitmasks in entities to contain my apidID
		LayoutEntity[] entities = {core, socket, numaNode, l1DCache, l1ICache, l2Cache, l3Cache};
		for (LayoutEntity entity : entities) {
			if (entity == null) {
				continue;
			}
			BitSet bitMask = entity.getBitSetMask();
			bitMask.set(apicID);
		}
	}

	@Override
	public int getNodeId() {
		return node.getId();
	}

	@Override
	public void setNode(NumaNode n) {
		node = n;

	}

	public Cache getL1DCache() {
		return l1DCache;
	}

	public NumaNode getNode() {
		return node;
	}

	public Cache getL1ICache() {
		return l1ICache;
	}

	public Cache getL2Cache() {
		return l2Cache;
	}

	public Cache getL3Cache() {
		return l3Cache;
	}

	@Override
	public String toString() {
		return "HwLocCpuInfo{" +
				"node=" + node +
				", l1DCache=" + l1DCache +
				", l1ICache=" + l1ICache +
				", l2Cache=" + l2Cache +
				", l3Cache=" + l3Cache +
				"} " + super.toString();
	}
}
