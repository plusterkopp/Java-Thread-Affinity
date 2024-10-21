package net.openhft.affinity;

import net.openhft.affinity.impl.LayoutEntities.Cache;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface CacheCpuLayout {

	List<Cache> getCaches();

	default List<Cache> getCaches(int cpuId) {
		Stream<Cache> cacheS = cachesIntersecting(cpuId);
		return cacheS.collect(Collectors.toList());
	}

	List<Cache> getCachesForCore(int cpuId);


	/**
	 * @param cpuId
	 * @return size in bytes
	 */
	default long l1CacheSize(int cpuId) {
		return getCacheInfo(cpuId, 1, Cache::getSize);
	}

	/**
	 * @param cpuId
	 * @return size in bytes
	 */
	default long l2CacheSize(int cpuId) {
		return getCacheInfo(cpuId, 2, Cache::getSize);
	}

	/**
	 * @param cpuId
	 * @return size in bytes
	 */
	default long l3CacheSize(int cpuId) {
		return getCacheInfo(cpuId, 3, Cache::getSize);
	}

	/**
	 * @param cpuId
	 * @return size in bytes
	 */
	default long l1CacheLineSize(int cpuId) {
		return getCacheInfo(cpuId, 1, Cache::getLineSize);
	}

	/**
	 * @param cpuId
	 * @return size in bytes
	 */
	default long l2CacheLineSize(int cpuId) {
		return getCacheInfo(cpuId, 2, Cache::getLineSize);
	}

	/**
	 * @param cpuId
	 * @return size in bytes
	 */
	default long l3CacheLineSize(int cpuId) {
		return getCacheInfo(cpuId, 3, Cache::getLineSize);
	}

	/**
	 * @param cpuId
	 * @return Associativity, or 0 if fully associative
	 */
	default byte l1Associativity(int cpuId) {
		return getCacheInfoB(cpuId, 1, Cache::getAssociativity);
	}

	/**
	 * @param cpuId
	 * @return Associativity, or 0 if fully associative
	 */
	default byte l2Associativity(int cpuId) {
		return getCacheInfoB(cpuId, 2, Cache::getAssociativity);
	}

	/**
	 * @param cpuId
	 * @return Associativity, or 0 if fully associative
	 */
	default byte l3Associativity(int cpuId) {
		return getCacheInfoB(cpuId, 3, Cache::getAssociativity);
	}

	default Cache.CacheType l1Type(int cpuId) {
		return getCacheInfoCT(cpuId, 1, Cache::getType);
	}

	default Cache.CacheType l2Type(int cpuId) {
		return getCacheInfoCT(cpuId, 2, Cache::getType);
	}

	default Cache.CacheType l3Type(int cpuId) {
		return getCacheInfoCT(cpuId, 3, Cache::getType);
	}

	Stream<Cache> cachesIntersecting(int cpuId);

	/**
	 * we only want one hit, therefore ignore Instruction Cache
	 *
	 * @param cpuId  cpuId
	 * @param level  cache level
	 * @param getter getter
	 * @return usually some sort of size
	 */
	default long getCacheInfo(int cpuId, int level, Function<Cache, Long> getter) {
		long[] retValA = {-1};
		cachesIntersecting(cpuId)
				.filter(cache -> cache.getLevel() == level)
				.filter(cache -> cache.getType() != Cache.CacheType.INSTRUCTION)
				.findFirst()
				.ifPresent(cache -> retValA[0] = getter.apply(cache));
		return retValA[0];
	}

	default Cache.CacheType getCacheInfoCT(int cpuId, int level, Function<Cache, Cache.CacheType> getter) {
		Cache.CacheType[] retValA = new Cache.CacheType[1];
		cachesIntersecting(cpuId)
				.filter(cache -> cache.getLevel() == level)
				.findFirst()
				.ifPresent(cache -> retValA[0] = getter.apply(cache));
		return retValA[0];
	}

	default byte getCacheInfoB(int cpuId, int level, Function<Cache, Byte> getter) {
		byte[] retValA = {-1};
		cachesIntersecting(cpuId)
				.filter(cache -> cache.getLevel() == level)
				.findFirst()
				.ifPresent(cache -> retValA[0] = getter.apply(cache));
		return retValA[0];
	}

	default Cache getCache(int cpuId, int level) {
		return cachesIntersecting(cpuId)
				.filter(cache -> cache.getLevel() == level)
				.findFirst()
				.orElse(null);
	}

}
