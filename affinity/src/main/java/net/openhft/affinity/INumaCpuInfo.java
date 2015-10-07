package net.openhft.affinity;

/**
 * Created by rhelbing on 07.10.2015.
 */
public interface INumaCpuInfo {
	void setNodeId(int id);

	int getNodeId();
}
