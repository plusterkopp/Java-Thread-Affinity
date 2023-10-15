package net.openhft.affinity;

import net.openhft.affinity.impl.LayoutEntities.Cache;

import java.util.List;

public interface CacheCpuLayout {

    /**
     * @param cpuId
     * @return size in bytes
     */
    long l1CacheSize(int cpuId);
    /**
     * @param cpuId
     * @return size in bytes
     */
    long l2CacheSize(int cpuId);
    /**
     * @param cpuId
     * @return size in bytes
     */
    long l3CacheSize(int cpuId);

    /**
     * @param cpuId
     * @return size in bytes
     */
    long l1CacheLineSize(int cpuId);
    /**
     * @param cpuId
     * @return size in bytes
     */
    long l2CacheLineSize(int cpuId);
    /**
     * @param cpuId
     * @return size in bytes
     */
    long l3CacheLineSize(int cpuId);

    /**
     * @param cpuId
     * @return Associativity, or 0 if fully associative
     */
    byte l1Associativity(int cpuId);
    /**
     * @param cpuId
     * @return Associativity, or 0 if fully associative
     */
    byte l2Associativity(int cpuId);
    /**
     * @param cpuId
     * @return Associativity, or 0 if fully associative
     */
    byte l3Associativity(int cpuId);

    Cache.CacheType l1Type(int cpuId);
    Cache.CacheType l2Type(int cpuId);
    Cache.CacheType l3Type(int cpuId);

    Cache getCache( int cpuId, int level);

    List<Cache> getCaches();

    List<Cache> getCaches( int cpuId);
}
