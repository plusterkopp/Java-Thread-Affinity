package net.openhft.affinity;

import net.openhft.affinity.impl.LayoutEntities.*;
import net.openhft.affinity.impl.*;
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
	 * @param socketId id of socket
	 * @return true if the current cpu after the call is one the desired socket, or false
	 */
	public boolean bindToSocket( int socketId) {
		if ( cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout v = (VanillaCpuLayout) cpuLayout;
			try {
				Socket socket = v.packages.get(socketId);
				return bindToSocket( socket);
			} catch ( IndexOutOfBoundsException e) {
				return false;
			}
		}
		return false;
	}

	public boolean bindToSocket( Socket socket) {
		if ( cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout w = (VanillaCpuLayout) cpuLayout;
			socket.bind();
			int cpuId = Affinity.getCpu();
			final int currentSocketId = cpuLayout.socketId(cpuId);
			if (currentSocketId == socket.getId()) {
				return true;
			}
			ICpuInfo current = w.getCPUInfo( cpuId);
			BitSet  desired = (BitSet) socket.getBitMask().clone();
			Socket currentSocket = w.packages.stream()
				.filter( s -> s.getId() == currentSocketId)
				.findFirst()
				.get();
			BitSet  ofCurrentSocket = (BitSet) currentSocket.getBitMask().clone();
			ofCurrentSocket.and( desired);
			System.err.print( "can not bind: " + socket + ", bound to " + currentSocket + " masks intersect at " + ofCurrentSocket) ;
		}
		return false;
	}

	public boolean bindToCore(int coreId) {
		if ( cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout w = (VanillaCpuLayout) cpuLayout;
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
		if ( cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout w = (VanillaCpuLayout) cpuLayout;
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
		if ( cpuLayout instanceof NumaCpuLayout) {
			NumaCpuLayout w = (NumaCpuLayout) cpuLayout;
			try {
				NumaNode node = w.getNodes().get( id);
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

	public boolean bindToCache(int cacheId) {
		if ( cpuLayout instanceof CacheCpuLayout) {
			CacheCpuLayout l = (CacheCpuLayout) cpuLayout;
			try {
				Cache cache = l.getCaches().get(cacheId);
				return bindToCache( cache);
			} catch ( IndexOutOfBoundsException e) {
				return false;
			}
		}
		return false;
	}

	public boolean bindToCache(Cache cache) {
		if ( cpuLayout instanceof CacheCpuLayout) {
			CacheCpuLayout l = (CacheCpuLayout) cpuLayout;
			cache.bind();
			int	cpuId = Affinity.getCpu();
			List<Cache> caches = l.getCaches(cpuId);
			if ( ! caches.contains( cache)) {
				Core core = ( ( WindowsCpuLayout) cpuLayout).cores.get( cpuLayout.coreId( cpuId));
				return false;
			}
			return true;
		}
		return false;
	}

	public void visitEntities( Consumer<LayoutEntity> visitor) {
		if ( cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout v = (VanillaCpuLayout) cpuLayout;
			if (cpuLayout instanceof WindowsCpuLayout) {
				WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
				w.groups.forEach( visitor);
				w.nodes.forEach( visitor);
			}
			v.packages.forEach( visitor);
			v.cores.forEach( visitor);
		}
		if ( cpuLayout instanceof CacheCpuLayout) {
			CacheCpuLayout l = (CacheCpuLayout) cpuLayout;
			List<Cache> caches = l.getCaches();
			caches.forEach( visitor);
		}
	}

	public void unregisterFromOthers(LayoutEntity current, Thread t) {
		if ( cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout w = (VanillaCpuLayout) cpuLayout;
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
		if ( cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout vcpul = (VanillaCpuLayout) cpuLayout;
			return vcpul.packages.get( i);
		}
		return null;
	}

	public Core getCore(int i) {
		if ( cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout vcpul = (VanillaCpuLayout) cpuLayout;
			return vcpul.cores.get( i);
		}
		return null;
	}

	public NumaNode getNode(int i) {
		if ( cpuLayout instanceof NumaCpuLayout) {
			NumaCpuLayout nl = (NumaCpuLayout) cpuLayout;
			return nl.getNodes().get( i);
		}
		return null;
	}

	public List<LayoutEntity> getBoundTo(Thread thread) {
		if ( cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout wcpul = (VanillaCpuLayout) cpuLayout;
			List<LayoutEntity> result = new ArrayList<>(1);
			Consumer<LayoutEntity> addIfHasThread = (entity) -> {
				List<Thread> entityThreads = entity.getThreads();
				if ( entityThreads.contains( thread)) {
					result.add( entity);
				}
			};
			visitEntities(addIfHasThread);
			return result;
		}
		return Collections.emptyList();

	}


	public void dumpLayout() {
		SortedSet<LayoutEntity> sortedEntities = new TreeSet<LayoutEntity>( ( a, b) -> {
			GroupAffinityMask gamA = a.getGroupMask();
			GroupAffinityMask gamB = b.getGroupMask();
			if ( gamA.getGroupId() != gamB.getGroupId()) {
				return Integer.compare( gamA.getGroupId(), gamB.getGroupId());
			}
			if ( gamA.getMask() != gamB.getMask()) {
				return -Long.compare( gamA.getMask(), gamB.getMask());
			}
			if ( ( a instanceof Cache) && ( b instanceof Cache)) {
				Cache ca = (Cache) a;
				Cache cb = (Cache) b;
				return Integer.compare( ca.getLevel(), cb.getLevel());
			}
			if (a instanceof Core) {
				return -1;
			}
			if (b instanceof Core) {
				return 1;
			}
			return Integer.compare( Objects.hashCode( a), Objects.hashCode( b));
		});
		visitEntities( e -> sortedEntities.add( e));
		sortedEntities.forEach( e -> System.out.println( e.getClass().getSimpleName() + ": " + e));
	}
}
