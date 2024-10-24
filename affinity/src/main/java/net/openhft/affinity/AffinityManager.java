package net.openhft.affinity;

import net.openhft.affinity.impl.GroupAffinityMask;
import net.openhft.affinity.impl.LayoutEntities.*;
import net.openhft.affinity.impl.NoCpuLayout;
import net.openhft.affinity.impl.VanillaCpuLayout;
import net.openhft.affinity.impl.WindowsCpuLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;


/**
 * Created by rhelbing on 07.10.2015.
 */
public class AffinityManager {

	private static class AffinityManagerHolder {
		static final AffinityManager instance = new AffinityManager();
	}

	// public static final AffinityManager INSTANCE = new AffinityManager();

	public static AffinityManager getInstance() {
		return AffinityManagerHolder.instance;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(AffinityManager.class);

	final private CpuLayout cpuLayout;

	private AffinityManager() {
		cpuLayout = initLayout();
		countEntities();
	}

	private void countEntities() {
		Map<String, Integer> typeToCount = new HashMap<>();
		visitEntities(e -> {
			String typeName = e.getTypeName();
			typeToCount.compute(typeName, (name, count) -> count == null ? 1 : count + 1);
		});
		visitEntities(e -> {
			String typeName = e.getTypeName();
			int count = typeToCount.get(typeName);
			e.setCountInLayout(count);
		});

	}

	// initializing the layout may occasionally throw spurious errors, very hard to reproduce
//	Caused by: java.lang.Error: Invalid memory access
//	at com.sun.jna.Native.read(Native Method) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Pointer.read(Pointer.java:140) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Pointer.readArray(Pointer.java:464) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Pointer.getValue(Pointer.java:450) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.readField(Structure.java:746) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.read(Structure.java:605) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.autoRead(Structure.java:2260) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.conditionalAutoRead(Structure.java:576) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.toArray(Structure.java:1654) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.toArray(Structure.java:1675) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Pointer.readArray(Pointer.java:506) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Pointer.getValue(Pointer.java:450) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.readField(Structure.java:746) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.read(Structure.java:605) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Pointer.getValue(Pointer.java:370) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.readField(Structure.java:746) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Union.readField(Union.java:223) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.read(Structure.java:605) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at net.openhft.affinity.impl.WindowsJNAAffinity$SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.read(WindowsJNAAffinity.java:355) ~[affinity-3.4.jar:?]
//	at net.openhft.affinity.impl.WindowsJNAAffinity$SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.<init>(WindowsJNAAffinity.java:329) ~[affinity-3.4.jar:?]
//	at net.openhft.affinity.impl.WindowsJNAAffinity$SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX$ByReference.<init>(WindowsJNAAffinity.java:421) ~[affinity-3.4.jar:?]
//	at jdk.internal.reflect.GeneratedConstructorAccessor27.newInstance(Unknown Source) ~[?:?]
//	at jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45) ~[?:?]
//	at java.lang.reflect.Constructor.newInstance(Constructor.java:490) ~[?:?]
//	at com.sun.jna.Structure.newInstance(Structure.java:1871) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.updateStructureByReference(Structure.java:703) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Pointer.getValue(Pointer.java:367) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.readField(Structure.java:746) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.read(Structure.java:605) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.autoRead(Structure.java:2260) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.conditionalAutoRead(Structure.java:576) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.toArray(Structure.java:1654) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at com.sun.jna.Structure.toArray(Structure.java:1675) ~[jna-5.12.1.jar:5.12.1 (b0)]
//	at net.openhft.affinity.impl.WindowsJNAAffinity$PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX_ARR.toArray(WindowsJNAAffinity.java:452) ~[affinity-3.4.jar:?]
//	at net.openhft.affinity.impl.WindowsJNAAffinity.getLogicalProcessorInformation(WindowsJNAAffinity.java:634) ~[affinity-3.4.jar:?]
//	at net.openhft.affinity.impl.WindowsJNAAffinity.getLogicalProcessorInformation(WindowsJNAAffinity.java:627) ~[affinity-3.4.jar:?]
//	at net.openhft.affinity.impl.WindowsJNAAffinity$1.getDefaultLayout(WindowsJNAAffinity.java:45) ~[affinity-3.4.jar:?]
//	at net.openhft.affinity.AffinityManager.getLayout(AffinityManager.java:30) ~[affinity-3.4.jar:?]
//	at net.openhft.affinity.AffinityManager.<init>(AffinityManager.java:22) ~[affinity-3.4.jar:?]
//	at net.openhft.affinity.AffinityManager.<clinit>(AffinityManager.java:17) ~[affinity-3.4.jar:?]

	private CpuLayout initLayout() {
		int count = 0;
		CpuLayout fallbackLayout = new NoCpuLayout(Runtime.getRuntime().availableProcessors());
		// try to guard against spurios JNA Errors shown above. In that case, we retry up to 3 times
		while (++count <= 3) {
			try {
				CpuLayout layout = fallbackLayout;
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
			} catch (Throwable t) {
				LOGGER.error("can not initialize layout round " + count, t);
			}
		}
		return fallbackLayout;
	}

	public CpuLayout getLayout() {
		return cpuLayout;
	}

	/**
	 * try to bind the current thread to a socket
	 *
	 * @param socketId id of socket
	 * @return true if the current cpu after the call is one the desired socket, or false
	 */
	public boolean bindToSocket(int socketId) {
		if (cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout v = (VanillaCpuLayout) cpuLayout;
			try {
				Socket socket = v.packages.get(socketId);
				return bindToSocket(socket);
			} catch (IndexOutOfBoundsException e) {
				return false;
			}
		}
		return false;
	}

	public boolean bindToSocket(Socket socket) {
		if (cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout w = (VanillaCpuLayout) cpuLayout;
			socket.bind();
			int cpuId = Affinity.getCpu();
			final int currentSocketId = cpuLayout.socketId(cpuId);
			if (currentSocketId == socket.getId()) {
				return true;
			}
			ICpuInfo current = w.getCPUInfo(cpuId);
			BitSet desired = (BitSet) socket.getBitSetMask().clone();
			Socket currentSocket = w.packages.stream()
					.filter(s -> s.getId() == currentSocketId)
					.findFirst()
					.get();
			BitSet ofCurrentSocket = (BitSet) currentSocket.getBitSetMask().clone();
			ofCurrentSocket.and(desired);
			System.err.print("can not bind: " + socket + ", bound to " + currentSocket + " masks intersect at " + ofCurrentSocket);
		}
		return false;
	}

	public boolean bindToCore(int coreId) {
		if (cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout w = (VanillaCpuLayout) cpuLayout;
			try {
				Core core = w.cores.get(coreId);
				return bindToCore(core);
			} catch (IndexOutOfBoundsException e) {
				return false;
			}
		}
		return false;
	}

	public boolean bindToCore(Core core) {
		if (cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout w = (VanillaCpuLayout) cpuLayout;
			core.bind();
			int cpuId = Affinity.getCpu();
			if (cpuLayout.coreId(cpuId) == core.getId()) {
				return true;
			}
		}
		return false;
	}

	public boolean bindToGroup(int id) {
		// implement for Windows case first, generalize when interfaces become available for VanillaLayout, too
		if (cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			try {
				Group entity = w.groups.get(id);
				return bindToGroup(entity);
			} catch (IndexOutOfBoundsException e) {
				return false;
			}
		}
		return false;
	}

	public boolean bindToGroup(Group entity) {
		// implement for Windows case first, generalize when interfaces become available for VanillaLayout, too
		if (cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			entity.bind();
			int cpuId = Affinity.getCpu();
			if (w.groupId(cpuId) == entity.getId()) {
				return true;
			}
		}
		return false;
	}

//	public boolean bindToNode(int id) {
//		// implement for Windows case first, generalize when interfaces become available for VanillaLayout, too
//		if ( cpuLayout instanceof NumaCpuLayout) {
//			NumaCpuLayout w = (NumaCpuLayout) cpuLayout;
//			try {
//				NumaNode node = w.getNodes().get( id);
//				return bindToNode(node);
//			} catch ( IndexOutOfBoundsException e) {
//				return false;
//			}
//		}
//		return false;
//	}

	public boolean bindToNode(NumaNode node) {
		if (cpuLayout instanceof NumaCpuLayout) {
			NumaCpuLayout layout = (NumaCpuLayout) cpuLayout;
			node.bind();
			int cpuId = Affinity.getCpu();
			if (layout.numaNodeId(cpuId) == node.getId()) {
				return true;
			}
		}
		return false;
	}

//	public boolean bindToCache(int cacheId) {
//		if ( cpuLayout instanceof CacheCpuLayout) {
//			CacheCpuLayout l = (CacheCpuLayout) cpuLayout;
//			try {
//				Cache cache = l.getCaches().get(cacheId);
//				return bindToCache( cache);
//			} catch ( IndexOutOfBoundsException e) {
//				return false;
//			}
//		}
//		return false;
//	}

	public boolean bindToCache(Cache cache) {
		if (cpuLayout instanceof CacheCpuLayout) {
			CacheCpuLayout l = (CacheCpuLayout) cpuLayout;
			cache.bind();
			int cpuId = Affinity.getCpu();
			List<Cache> caches = l.getCaches(cpuId);
			if (!caches.contains(cache)) {
//				Core core = ( ( WindowsCpuLayout) cpuLayout).cores.get( cpuLayout.coreId( cpuId));
				return false;
			}
			return true;
		}
		return false;
	}

	public void visitEntities(Consumer<LayoutEntity> visitor) {
		if (cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout v = (VanillaCpuLayout) cpuLayout;
			if (cpuLayout instanceof GroupedCpuLayout) {
				GroupedCpuLayout gl = (GroupedCpuLayout) cpuLayout;
				gl.getGroups().forEach(visitor);
			}
			if (cpuLayout instanceof NumaCpuLayout) {
				NumaCpuLayout nl = (NumaCpuLayout) cpuLayout;
				nl.getNodes().forEach(visitor);
			}
			v.packages.forEach(visitor);
			v.cores.forEach(visitor);
		}
		if (cpuLayout instanceof CacheCpuLayout) {
			CacheCpuLayout l = (CacheCpuLayout) cpuLayout;
			List<Cache> caches = l.getCaches();
			caches.forEach(visitor);
		}
	}

	public void unregisterFromOthers(LayoutEntity current, Thread t) {
		if (cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout w = (VanillaCpuLayout) cpuLayout;
			visitEntities(e -> {
				if (e != current) {
					e.unregister(t);
				}
			});
		}
	}

	public int getNumSockets() {
		return cpuLayout.sockets();
	}

	public Socket getSocket(int i) {
		if (cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout vcpul = (VanillaCpuLayout) cpuLayout;
			return vcpul.packages.get(i);
		}
		return null;
	}

	public Core getCore(int i) {
		if (cpuLayout instanceof VanillaCpuLayout) {
			VanillaCpuLayout vcpul = (VanillaCpuLayout) cpuLayout;
			return vcpul.cores.get(i);
		}
		return null;
	}

	public NumaNode getNode(int i) {
		if (cpuLayout instanceof NumaCpuLayout) {
			NumaCpuLayout nl = (NumaCpuLayout) cpuLayout;
			return nl.getNodes().get(i);
		}
		return null;
	}

	public List<LayoutEntity> getBoundTo(Thread thread) {
		if (cpuLayout instanceof VanillaCpuLayout) {
			List<LayoutEntity> result = new ArrayList<>(1);
			Consumer<LayoutEntity> addIfHasThread = (entity) -> {
				List<Thread> entityThreads = entity.getThreads();
				if (entityThreads.contains(thread)) {
					result.add(entity);
				}
			};
			visitEntities(addIfHasThread);
			return result;
		}
		return Collections.emptyList();
	}


	public void dumpLayout() {
		StringBuilder sb = new StringBuilder();
		dumpLayout(sb);
		System.out.print(sb);
	}

	public void dumpLayout(StringBuilder sb) {
		SortedSet<LayoutEntity> sortedEntities = new TreeSet<LayoutEntity>((a, b) -> {
			GroupAffinityMask gamA = a.getGroupMask();
			GroupAffinityMask gamB = b.getGroupMask();
			if (gamA != null && gamB != null) {
				if (gamA.getGroupId() != gamB.getGroupId()) {
					return Integer.compare(gamA.getGroupId(), gamB.getGroupId());
				}
				if (gamA.getMask() != gamB.getMask()) {
					return -Long.compareUnsigned(gamA.getMask(), gamB.getMask());
				}
				if ((a instanceof Cache) && (b instanceof Cache)) {
					Cache ca = (Cache) a;
					Cache cb = (Cache) b;
					return Integer.compare(ca.getLevel(), cb.getLevel());
				}
				if (a instanceof Core) {
					return -1;
				}
				if (b instanceof Core) {
					return 1;
				}
			} else {    // should be hierarchical. Linux doesn't assign SMT bits next to each other.
				BitSet bsA = a.getBitSetMask();
				long[] longBitsA = bsA.toLongArray();
				BitSet bsB = b.getBitSetMask();
				long[] longBitsB = bsB.toLongArray();
				for (int i = longBitsA.length - 1; i >= 0; i--) {
					long maskA = longBitsA[i];
					long maskB = longBitsB[i];
					if (maskA != maskB) {
						return -Long.compareUnsigned(maskA, maskB);
					}
					// Caches kennen wir hier nicht :(
				}

			}
			return Integer.compare(Objects.hashCode(a), Objects.hashCode(b));
		});
		visitEntities(e -> sortedEntities.add(e));
		sortedEntities.forEach(e -> {
			sb.append(getLocation(e))
				.append(" ")
				.append(e.getClass().getSimpleName() + ": " + e + "\n");
		});
	}

	public String getLocation(LayoutEntity le) {
		String locationInfo = le.getLocationInfo();
		if ( locationInfo != null) {
			return locationInfo;
		}
		List<LayoutEntity> inEntities = new ArrayList<>(10);
		visitEntities(entity -> {
			// exclude the entity itself and any entity that has only one instance (which then fullyContains everything else anyway and doesn't add information)
			if (entity == le || entity.getCountInLayout() < 2) {
				return;
			}
			// omit L1 caches
			if (entity instanceof Cache) {
				Cache cache = (Cache) entity;
				if (cache.getLevel() == 1) {
					return;
				}
			}
			if (entity.fullyContains(le)) {
				inEntities.add(entity);
			}
		});
		Collections.sort(inEntities, (a, b) -> {
			GroupAffinityMask gamA = a.getGroupMask();
			GroupAffinityMask gamB = b.getGroupMask();
			if (gamA != null && gamB != null) {
				long gamAMask = gamA.getMask();
				long gamBMask = gamB.getMask();
				return Long.compare(Long.bitCount(gamAMask), Long.bitCount(gamBMask));
			}
			// or BitSets
			BitSet bsA = a.getBitSetMask();
			BitSet bsB = b.getBitSetMask();
			return Long.compare(bsA.cardinality(), bsB.cardinality());
		});
		StringBuilder sb = new StringBuilder(100);
		sb.append(le.getTypeName())
			.append("#")
			.append(le.getId())
		;
		inEntities.forEach(e -> {
			sb
				.append("/")
				.append(e.getTypeName())
				.append("#")
				.append(e.getId());
		});

		locationInfo = sb.toString();
		le.setLocationInfo( locationInfo);
		return locationInfo;
	}

}
