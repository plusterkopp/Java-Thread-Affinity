package net.openhft.affinity;

import net.openhft.affinity.impl.LayoutEntities.NumaNode;

import java.util.List;

/**
 * extend CpuLayout
 * Created by plusterkopp on 17.09.2015.
 */
public interface NumaCpuLayout {

	int numaNodeId(int cpuId);

	int numaNodes();

	List<? extends NumaNode> getNodes();
}
