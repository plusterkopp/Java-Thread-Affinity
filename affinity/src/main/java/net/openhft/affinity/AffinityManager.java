package net.openhft.affinity;

import net.openhft.affinity.impl.*;
import net.openhft.affinity.impl.LayoutEntities.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.function.*;


/**
 * Created by rhelbing on 07.10.2015.
 */
public class AffinityManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(AffinityManager.class);
	public static final AffinityManager INSTANCE = new AffinityManager();

	final private CpuLayout   cpuLayout;

	private AffinityManager() {
		cpuLayout = getLayout();
	}

	private CpuLayout getLayout() {
		CpuLayout layout = new NoCpuLayout(Runtime.getRuntime().availableProcessors());
		IAffinity impl = Affinity.getAffinityImpl();
		if (impl instanceof IDefaultLayoutAffinity) {
			IDefaultLayoutAffinity dla = (IDefaultLayoutAffinity) impl;
			layout = dla.getDefaultLayout();
		} else {
			if (new File("/proc/cpuinfo").exists()) {
				try {
					layout = VanillaCpuLayout.fromCpuInfo();
				} catch (Exception e) {
					LOGGER.warn("Unable to load /proc/cpuinfo", e);
				}
			}
		}
		return layout;
	}

	/**
	 * try to bind the current thread to a socket
	 * @param socketId
	 * @return true if the current cpu after the call is one the desired socket, or false
	 */
	public boolean bindToSocket( int socketId) {
		// implement for Windows case first, generalize when interfaces become available for VanillaLayout, too
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			try {
				Socket socket = w.packages.get(socketId);
				return bindToSocket( socket);
			} catch ( IndexOutOfBoundsException e) {
				return false;
			}
		}
		return false;
	}

	public boolean bindToSocket( Socket socket) {
		// implement for Windows case first, generalize when interfaces become available for VanillaLayout, too
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			socket.bind();
			int cpuId = Affinity.getCpu();
			if (cpuLayout.socketId(cpuId) == socket.getId()) {
				return true;
			}
		}
		return false;
	}

	public boolean bindToCore(int coreId) {
		// implement for Windows case first, generalize when interfaces become available for VanillaLayout, too
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			try {
				Core core = w.cores.get(coreId);
				return bindToCore(core);
			} catch ( IndexOutOfBoundsException e) {
				return false;
			}
		}
		return false;
	}

	public boolean bindToCore(Core core) {
		// implement for Windows case first, generalize when interfaces become available for VanillaLayout, too
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			core.bind();
			int	cpuId = Affinity.getCpu();
			if ( cpuLayout.coreId(cpuId) == core.getId()) {
				return true;
			}
		}
		return false;
	}

	public boolean bindToGroup(int id) {
		// implement for Windows case first, generalize when interfaces become available for VanillaLayout, too
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			try {
				Group entity = w.groups.get( id);
				return bindToGroup(entity);
			} catch ( IndexOutOfBoundsException e) {
				return false;
			}
		}
		return false;
	}

	public boolean bindToGroup(Group entity) {
		// implement for Windows case first, generalize when interfaces become available for VanillaLayout, too
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			entity.bind();
			int	cpuId = Affinity.getCpu();
			if ( w.groupId(cpuId) == entity.getId()) {
				return true;
			}
		}
		return false;
	}

	public boolean bindToNode(int id) {
		// implement for Windows case first, generalize when interfaces become available for VanillaLayout, too
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			try {
				NumaNode node = w.nodes.get( id);
				return bindToNode(node);
			} catch ( IndexOutOfBoundsException e) {
				return false;
			}
		}
		return false;
	}

	public boolean bindToNode(NumaNode node) {
		// implement for Windows case first, generalize when interfaces become available for VanillaLayout, too
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			node.bind();
			int	cpuId = Affinity.getCpu();
			if ( w.numaNodeId(cpuId) == node.getId()) {
				return true;
			}
		}
		return false;
	}

	public void visitEntities( Consumer<LayoutEntity> visitor) {
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			w.groups.forEach( visitor);
			w.nodes.forEach( visitor);
			w.packages.forEach( visitor);
			w.cores.forEach( visitor);
		}
	}

	public void unregisterFromOthers(LayoutEntity current, Thread t) {
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			visitEntities( e -> {
				if ( e != current) {
					e.unregister(t);
				}
			});
		}
	}

	public int getNumSockets() {
		return cpuLayout.sockets();
	}

	public Socket getSocket(int i) {
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout wcpul = (WindowsCpuLayout) cpuLayout;
			return wcpul.packages.get( i);
		}
		return null;
	}

	public Core getCore(int i) {
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout wcpul = (WindowsCpuLayout) cpuLayout;
			return wcpul.cores.get( i);
		}
		return null;
	}

	public NumaNode getNode(int i) {
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout wcpul = (WindowsCpuLayout) cpuLayout;
			return wcpul.nodes.get( i);
		}
		return null;
	}

	public List<LayoutEntity> getBoundTo(Thread thread) {
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout wcpul = (WindowsCpuLayout) cpuLayout;
			List<LayoutEntity> result = new ArrayList<>(1);
			Consumer<LayoutEntity> addIfHasThread = (entity) -> {
				if ( entity.getThreads().contains( thread)) {
					result.add( entity);
				}
			};
			visitEntities(addIfHasThread);
			return result;
		}
		return Collections.EMPTY_LIST;

	}



}
