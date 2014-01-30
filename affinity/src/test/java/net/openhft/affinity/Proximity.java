package net.openhft.affinity;

/**
 * allows for a quick check if affinity wishes were granted
 * @author rhelbing
 *
 */
public enum Proximity {

	SAME_THREAD,
	SAME_CORE,
	SAME_SOCKET,
	DIFFERENT_SOCKET,
	NONE;

	public static Proximity getProximity( Thread t1, Thread t2) {
		AffinityLock	al1 = AffinityLock.findLockForThread( t1);
		AffinityLock	al2 = AffinityLock.findLockForThread( t2);

		if ( al1 == null || al2 == null) {
			return NONE;
		}

		// this should not happen as one thread can not be assigned to two locks
		final int cpu1 = al1.cpuId();
		final int cpu2 = al2.cpuId();
		if ( cpu1 == cpu2) {
			return SAME_THREAD;
		}

		CpuLayout	l = AffinityLock.cpuLayout();
		if ( l.socketId( cpu1) == l.socketId( cpu2)) {
			if ( l.coreId( cpu1) == l.coreId( cpu2)) {
				return SAME_CORE;	// Core is private to socket, so must check for same socket first
			}
			return SAME_SOCKET;
		}
		return DIFFERENT_SOCKET;	// nothing in common
	}

}
