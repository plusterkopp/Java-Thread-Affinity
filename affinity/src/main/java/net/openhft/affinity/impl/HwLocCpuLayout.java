package net.openhft.affinity.impl;

import net.openhft.affinity.CacheCpuLayout;
import net.openhft.affinity.ICpuInfo;
import net.openhft.affinity.NumaCpuLayout;
import net.openhft.affinity.impl.LayoutEntities.Cache;
import net.openhft.affinity.impl.LayoutEntities.Core;
import net.openhft.affinity.impl.LayoutEntities.NumaNode;
import net.openhft.affinity.impl.LayoutEntities.Socket;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides another method to define a layout using a simple string.
 * Created by ralf h on 23.01.2014.
 */
public class HwLocCpuLayout extends VanillaCpuLayout implements NumaCpuLayout, CacheCpuLayout {

	private final List<HwLocCpuInfo> cpuDetailsFull;
	private final Map<Integer, HwLocCpuInfo> cpuDetailsByApic = new HashMap<>();
	private final List<Cache> caches;
	public List<NumaNode> nodes;

	static private List<ICpuInfo> toVanillaDetails(List<ICpuInfo> details) {
		List<ICpuInfo> vanillaDetails = new ArrayList<>(details.size());
		Map<Integer, Set<Integer>> coresToCPUs = new HashMap<>();
		for (ICpuInfo fullInfo : details) {
			// VanillaCPUInfo wants threadId local to coreId
			Set<Integer> cpuSet = coresToCPUs.computeIfAbsent(fullInfo.getCoreId(), dummy -> new HashSet());
			int cpuSetSize = cpuSet.size();
			cpuSet.add(fullInfo.getThreadId());
			VanillaCpuInfo vi = new VanillaCpuInfo(fullInfo.getSocketId(), fullInfo.getCoreId(), cpuSetSize);
			vanillaDetails.add(vi);
		}
		return vanillaDetails;
	}

	HwLocCpuLayout(@NotNull List<ICpuInfo> cpuDetails, List<NumaNode> nodeList,
	               List<Socket> socketList, List<Core> coreList) {

		super(toVanillaDetails(cpuDetails));

		Set<Cache> cacheSet = new HashSet<>();
		cpuDetailsFull = new ArrayList<>(cpuDetails.size());
		for (ICpuInfo info : cpuDetails) {
			if (info instanceof HwLocCpuInfo) {
				HwLocCpuInfo hwInfo = (HwLocCpuInfo) info;
				cpuDetailsFull.add(hwInfo);
				cacheSet.add(hwInfo.getL1DCache());
				cacheSet.add(hwInfo.getL1ICache());
				cacheSet.add(hwInfo.getL2Cache());
				cacheSet.add(hwInfo.getL3Cache());
				cpuDetailsByApic.put(hwInfo.getApicId(), hwInfo);
			}
		}
		nodes = Collections.unmodifiableList(new ArrayList<>(nodeList));
		packages = Collections.unmodifiableList(new ArrayList<>(socketList));
		cores = Collections.unmodifiableList(new ArrayList<>(coreList));

		caches = Collections.unmodifiableList(new ArrayList<>(cacheSet));
		caches.forEach(cache -> cache.setLayout(this));
	}

	@Override
	public int numaNodeId(int cpuId) {
		return getCPUInfo(cpuId).getNodeId();
	}

	public HwLocCpuInfo lCpu(int index) {
		return cpuDetailsFull.get(index);
	}

	public void visitCpus(Consumer<HwLocCpuInfo> c) {
		for (HwLocCpuInfo info : cpuDetailsFull) {
			c.accept(info);
		}
	}

	@Override
	public int numaNodes() {
		return nodes.size();
	}

	@Override
	public List<? extends NumaNode> getNodes() {
		return nodes;
	}

	Stream<Cache> cachesIntersecting(int cpuId) {
		return caches.stream().
				filter(cache -> cache.getBitMask().get(cpuId));
	}

	/**
	 * we only want one hit, therefore ignore Instruction Cache
	 *
	 * @param cpuId  cpuId
	 * @param level  cache level
	 * @param getter getter
	 * @return usually some sort of size
	 */
	private long getCacheInfo(int cpuId, int level, Function<Cache, Long> getter) {
		long[] retValA = {-1};
		cachesIntersecting(cpuId)
				.filter(cache -> cache.getLevel() == level)
				.filter(cache -> cache.getType() != Cache.CacheType.INSTRUCTION)
				.findFirst()
				.ifPresent(cache -> retValA[0] = getter.apply(cache));
		return retValA[0];
	}

	private byte getCacheInfoB(int cpuId, int level, Function<Cache, Byte> getter) {
		byte[] retValA = {-1};
		cachesIntersecting(cpuId)
				.filter(cache -> cache.getLevel() == level)
				.findFirst()
				.ifPresent(cache -> retValA[0] = getter.apply(cache));
		return retValA[0];
	}

	private Cache.CacheType getCacheInfoCT(int cpuId, int level, Function<Cache, Cache.CacheType> getter) {
		Cache.CacheType[] retValA = new Cache.CacheType[1];
		cachesIntersecting(cpuId)
				.filter(cache -> cache.getLevel() == level)
				.findFirst()
				.ifPresent(cache -> retValA[0] = getter.apply(cache));
		return retValA[0];
	}

	@Override
	public Cache getCache(int cpuId, int level) {
		return cachesIntersecting(cpuId)
				.filter(cache -> cache.getLevel() == level)
				.findFirst()
				.orElse(null);
	}

	@Override
	public long l1CacheSize(int cpuId) {
		return getCacheInfo(cpuId, 1, Cache::getSize);
	}

	@Override
	public long l2CacheSize(int cpuId) {
		return getCacheInfo(cpuId, 2, Cache::getSize);
	}

	@Override
	public long l3CacheSize(int cpuId) {
		return getCacheInfo(cpuId, 3, Cache::getSize);
	}

	@Override
	public long l1CacheLineSize(int cpuId) {
		return getCacheInfo(cpuId, 1, Cache::getLineSize);
	}

	@Override
	public long l2CacheLineSize(int cpuId) {
		return getCacheInfo(cpuId, 2, Cache::getLineSize);
	}

	@Override
	public long l3CacheLineSize(int cpuId) {
		return getCacheInfo(cpuId, 3, Cache::getLineSize);
	}

	@Override
	public byte l1Associativity(int cpuId) {
		return getCacheInfoB(cpuId, 1, Cache::getAssociativity);
	}

	@Override
	public byte l2Associativity(int cpuId) {
		return getCacheInfoB(cpuId, 2, Cache::getAssociativity);
	}

	@Override
	public byte l3Associativity(int cpuId) {
		return getCacheInfoB(cpuId, 3, Cache::getAssociativity);
	}

	@Override
	public Cache.CacheType l1Type(int cpuId) {
		return getCacheInfoCT(cpuId, 1, Cache::getType);
	}

	@Override
	public Cache.CacheType l2Type(int cpuId) {
		return getCacheInfoCT(cpuId, 2, Cache::getType);
	}

	@Override
	public Cache.CacheType l3Type(int cpuId) {
		return getCacheInfoCT(cpuId, 3, Cache::getType);
	}

	public List<Cache> getCaches() {
		return caches;
	}

	public List<Cache> getCachesForCore(int cpuId) {
		List<Cache> allCaches = getCaches();
		List<Cache> result = new ArrayList<>();
		int coreId = coreId(cpuId);
		Core core = cores.get(coreId);
		GroupAffinityMask coreGAM = core.getGroupMask();
		allCaches.forEach(cache -> {
			GroupAffinityMask cacheGAM = cache.getGroupMask();
			if (cacheGAM.equals(coreGAM)) {
				result.add(cache);
			}
		});
		return result;
	}

	public List<Cache> getCaches(int cpuId) {
		Stream<Cache> cacheS = cachesIntersecting(cpuId);
		return cacheS.collect(Collectors.toList());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0, cpuDetailsSize = cpuDetailsFull.size(); i < cpuDetailsSize; i++) {
			HwLocCpuInfo cpuDetail = cpuDetailsFull.get(i);
			sb.append(i).append(": ").append(cpuDetail).append('\n');
		}
		return sb.toString();
	}

	@Override
	public int coreId(int cpuId) {
		return getCPUInfo(cpuId).getCoreId();
	}

	@Override
	public int socketId(int cpuId) {
		return getCPUInfo(cpuId).getSocketId();
	}

	public HwLocCpuInfo getCPUInfo(int apicId) {
		return cpuDetailsByApic.get(apicId);
	}


}
