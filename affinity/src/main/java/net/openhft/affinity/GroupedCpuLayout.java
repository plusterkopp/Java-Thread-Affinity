package net.openhft.affinity;

/**
 * extend CpuLayout
 * Created by plusterkopp on 17.09.2015.
 */
public interface GroupedCpuLayout {

	int groupId(int cpuId);

	int groups();

}