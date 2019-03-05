package net.openhft.affinity;

import com.sun.jna.platform.win32.WinNT;
import net.openhft.affinity.impl.LayoutEntities.Cache;

import java.util.List;

public interface CacheCpuLayout {

    public enum CacheType {
        DATA, INSTRUCTION, TRACE, UNIFIED;

        public static CacheType getInstance(byte type) {
            if ( type == WinNT.PROCESSOR_CACHE_TYPE.CacheData) {
                return DATA;
            } else if( type == WinNT.PROCESSOR_CACHE_TYPE.CacheInstruction) {
                return INSTRUCTION;
            } else if( type == WinNT.PROCESSOR_CACHE_TYPE.CacheTrace) {
                return TRACE;
            } else if( type == WinNT.PROCESSOR_CACHE_TYPE.CacheUnified) {
                return UNIFIED;
            }
            return null;
        }

        public char shortName() {
            return name().charAt( 0);
        }
    }

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

    CacheType l1Type( int cpuId);
    CacheType l2Type( int cpuId);
    CacheType l3Type( int cpuId);

    Cache getCache( int cpuId, int level);

    List<Cache> getCaches();
}
