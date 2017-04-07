package net.openhft.affinity;

import net.openhft.affinity.impl.LayoutEntities.Group;

/**
 * extend CpuLayout
 * Created by plusterkopp on 17.09.2015.
 */
public interface GroupedCpuLayout {

	int groupId(int cpuId);

	int groups();

	Iterable<? extends Group> getGroups();
}
