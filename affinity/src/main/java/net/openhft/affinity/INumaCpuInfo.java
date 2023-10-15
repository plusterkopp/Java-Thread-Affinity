package net.openhft.affinity;

import net.openhft.affinity.impl.LayoutEntities.NumaNode;

/**
 * Created by rhelbing on 07.10.2015.
 */
public interface INumaCpuInfo {
	void setNode( NumaNode numaNode);

	int getNodeId();
}
