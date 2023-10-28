package net.openhft.affinity;

/**
 * Created by rhelbing on 07.10.2015.
 */
public interface ICpuInfo {
	/**
	 * count from 0 to # phys sockets
	 */
	public int getSocketId();

	/**
	 * count over all phys cores
	 */
	public int getCoreId();

	/**
	 * relative to coreId
	 */
	public int getThreadId();

	public void setThreadId(int i);

	public void setSocketId(int socketId);

	public void setCoreId(int coreId);
}
