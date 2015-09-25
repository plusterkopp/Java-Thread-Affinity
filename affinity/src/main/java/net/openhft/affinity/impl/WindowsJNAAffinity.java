/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.affinity.impl;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.*;
import com.sun.jna.win32.*;
import net.openhft.affinity.*;
import org.slf4j.*;

import java.util.*;

/**
 * Implementation of {@link net.openhft.affinity.IAffinity} based on JNA call of
 * sched_SetThreadAffinityMask/GetProcessAffinityMask from Windows 'kernel32' library. Applicable for
 * most windows platforms
 * <p> *
 *
 * @author andre.monteiro
 */
public enum WindowsJNAAffinity implements IAffinity, INumaAffinity, IGroupAffinity, IDefaultLayoutAffinity {
    INSTANCE {
        @Override
        public CpuLayout getDefaultLayout() {
            return WindowsCpuLayout.fromSysInfo( getLogicalProcessorInformation());
        }

        @Override
        public int getNumaGroup() {
            return 0;
        }

        @Override
        public int getCpuGroup() {
            return 0;
        }
    };


    public static final boolean LOADED;
    private static final Logger LOGGER = LoggerFactory.getLogger(WindowsJNAAffinity.class);
    private final ThreadLocal<Integer> THREAD_ID = new ThreadLocal<>();

    public SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] getLogicalProcessorInformation() {
        return getLogicalProcessorInformation(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationAll);
    }    public static BitSet asBitSet( long mask) {
        long[] longs = new long[1];
        longs[0] = mask;
        return BitSet.valueOf(longs);
    }

    public SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] getLogicalProcessorInformation( int relationType) {
        IntByReference  retCount = new IntByReference();
        PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX_ARR.ByReference retList = new PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX_ARR.ByReference();
        LibAffinityInfo.INSTANCE.getSystemRelationShipInfos( retList, retCount);
        System.out.flush();
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.ByReference[] result = retList.toArray(retCount.getValue());
        return result;
    }    @Override
    public BitSet getAffinity() {
        BitSet  procAff = getProcessAffinity();
        BitSet  affBefore = setThreadAffinity( procAff);
        setThreadAffinity( affBefore);
        return affBefore;
    }

    public Map<Integer, BitSet>    getAffinityGroups() {
        long    highestNumaNodeNumber;
        LongByReference hnnn = new LongByReference( 0);
        boolean callOK = LibKernel32.INSTANCE.GetNumaHighestNodeNumber( hnnn);
        highestNumaNodeNumber = hnnn.getValue();
        Map<Integer, BitSet> result = new HashMap<Integer, BitSet>((int) highestNumaNodeNumber + 1);
        short groupID = 0;
        GROUP_AFFINITY.ByReference maskRet = new GROUP_AFFINITY.ByReference();
        for ( short i = 0;  i <= highestNumaNodeNumber; i++){
            boolean ok = LibKernel32.INSTANCE.GetNumaNodeProcessorMaskEx( i, maskRet);
            if (ok) {
                long maskRL = maskRet.mask.longValue();
                BitSet bsMask = asBitSet( maskRL);
                result.put( Integer.valueOf( i), bsMask);
            }
        }
        return result;
    }    public BitSet getProcessAffinity() {
        final LibKernel32 lib = LibKernel32.INSTANCE;
        final LongByReference cpuset1 = new LongByReference(0);
        final LongByReference cpuset2 = new LongByReference(0);
        try {

            final int ret = lib.GetProcessAffinityMask(-1, cpuset1, cpuset2);
            // Successful result is positive, according to the docs
            // https://msdn.microsoft.com/en-us/library/windows/desktop/ms683213%28v=vs.85%29.aspx
            if (ret <= 0)
            {
                throw new IllegalStateException("GetProcessAffinityMask(( -1 ), &(" + cpuset1 + "), &(" + cpuset2 + ") ) return " + ret);
            }
            return asBitSet(cpuset1.getValue());
        }
        catch (Exception e)
        {
            LOGGER.error(e.getMessage(), e);
            e.printStackTrace();
        }

        return new BitSet();
    }

    /**
     * @author BegemoT
     */
    private interface LibKernel32 extends Library {
        LibKernel32 INSTANCE = (LibKernel32) Native.loadLibrary("kernel32", LibKernel32.class, W32APIOptions.UNICODE_OPTIONS);

        int GetProcessAffinityMask(final int pid, final PointerType lpProcessAffinityMask, final PointerType lpSystemAffinityMask) throws LastErrorException;

        long SetThreadAffinityMask(final int pid, final WinDef.DWORDLONG lpProcessAffinityMask) throws LastErrorException;

        int GetCurrentThread() throws LastErrorException;

        boolean GetNumaHighestNodeNumber( final PointerType retValHighestNodeNumber) throws LastErrorException;

        boolean GetNumaNodeProcessorMaskEx( short node, GROUP_AFFINITY.ByReference maskRet);

        boolean GetLogicalProcessorInformationEx( int relationShipType, Memory ret, WinDef.DWORDByReference retLength);
    }    public BitSet getSystemAffinity() {
        final LibKernel32 lib = LibKernel32.INSTANCE;
        final LongByReference procAffResult = new LongByReference(0);
        final LongByReference systemAffResult = new LongByReference(0);
        try {

            final int ret = lib.GetProcessAffinityMask(-1, procAffResult, systemAffResult);
            // Successful result is positive, according to the docs
            // https://msdn.microsoft.com/en-us/library/windows/desktop/ms683213%28v=vs.85%29.aspx
            if (ret <= 0)
            {
                throw new IllegalStateException("GetProcessAffinityMask(( -1 ), &(" + procAffResult + "), &(" + systemAffResult + ") ) return " + ret);
            }

            return asBitSet(systemAffResult.getValue());
        }
        catch (Exception e)
        {
            LOGGER.error(e.getMessage(), e);
            e.printStackTrace();
        }

        return new BitSet();
    }

    /**
     * @author plusterkopp
     */
    private interface LibAffinityInfo extends Library {
        LibAffinityInfo INSTANCE = (LibAffinityInfo) Native.loadLibrary("affinityInfo", LibAffinityInfo.class, W32APIOptions.UNICODE_OPTIONS);

        boolean getSystemRelationShipInfos( PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX_ARR.ByReference retList, IntByReference retLength);
    }    @Override
    public void setAffinity(final BitSet affinity) {
        BitSet  sysAff = getSystemAffinity();
        if ( ! sysAff.intersects( affinity)) {
            LOGGER.error( "can not set affinity " + affinity + " when system affinity mask is only " + sysAff);
            return;
        }
        setThreadAffinity( affinity);
        BitSet affNow = getAffinity();
        if ( ! affNow.equals( affinity)) {
            LOGGER.warn("did not set affinity " + affinity + " current affinity is " + affNow);
        }
    }

    public static class GROUP_AFFINITY extends Structure {
        public WinDef.DWORDLONG    mask;
        public WinDef.WORD group;
        public WinDef.WORD[]  reserved = new WinDef.WORD[ 3];   // WORD      Reserved[3];

        public static class ByReference extends GROUP_AFFINITY implements Structure.ByReference {}

        @Override
        protected List getFieldOrder() {
            return Arrays.asList( "mask", "group", "reserved");
        }

        public String toString() {
            BitSet maskBS = asBitSet( mask.longValue());
            return "g#" + group.intValue() + "/" + maskBS.toString();
        }
    }    public BitSet setThreadAffinity(final BitSet affinity) {
        final LibKernel32 lib = LibKernel32.INSTANCE;

        WinDef.DWORDLONG aff;
        long[] longs = affinity.toLongArray();
        switch (longs.length)
        {
            case 0:
                aff = new WinDef.DWORDLONG(0);
                break;
            case 1:
                aff = new WinDef.DWORDLONG(longs[0]);
                break;
            default:
                throw new IllegalArgumentException("Windows API does not support more than 64 CPUs for thread affinity");
        }

        int pid = getTid();
        try
        {
            long affBefore = lib.SetThreadAffinityMask(pid, aff);
            return asBitSet(affBefore);
        }
        catch (LastErrorException e)
        {
            throw new IllegalStateException("SetThreadAffinityMask((" + pid + ") , &(" + affinity + ") ) errorNo=" + e.getErrorCode(), e);
        }
    }

    public static class PROCESSOR_RELATIONSHIP extends Structure {
        private static final int MaxGroupCount = 1;
        public byte flags;
        // public byte    efficiencyClass;
        public byte    reserved[] = new byte[21];
        public short groupCount;
        public GROUP_AFFINITY[]  groupMasks = new GROUP_AFFINITY[ MaxGroupCount];   // max 3 groups für alle LCPUs here (really, only one)

        public static class ByReference extends PROCESSOR_RELATIONSHIP implements Structure.ByReference {}

        @Override
        protected List getFieldOrder() {
            return Arrays.asList( "flags",
                    // "efficiencyClass",
                    "reserved", "groupCount", "groupMasks");
        }

        public GROUP_AFFINITY[] getGroupAffinities() {
//            GROUP_AFFINITY[]    result = (GROUP_AFFINITY[]) groupMasks.toArray( groupCount.intValue());
            int gc = groupCount; // .getValue();
            if ( gc > groupMasks.length) {
                System.out.println( "too many masks: " + gc + "/" + groupMasks.length);
                gc = groupMasks.length;
            }
            GROUP_AFFINITY[]    result = new GROUP_AFFINITY[gc];
            for ( int i = 0;  i < gc; i++) {
                result[i] = groupMasks[i];
            }
            return result;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append( flags == 0 ? "SMT off" : "SMT on");
//             sb.append( efficiencyClass == 0 ? " EC 0" : " EC " + efficiencyClass);
            GROUP_AFFINITY[]    masks = getGroupAffinities();
            if ( masks.length == 1) {
                sb.append( " mask: ").append( masks[0]);
            } else if ( masks.length == 0){
                sb.append( " no mask");
            } else {
                sb.append( " mask ");
                for ( int i = 0;  i < masks.length;  i++) {
                    sb.append( "#").append(i).append(": ").append(masks[i]);
                    if ( i < masks.length-1) {
                        sb.append(", ");
                    }
                }
            }
            return sb.toString();
        }
    }    public int getTid() {
        final LibKernel32 lib = LibKernel32.INSTANCE;

        try {
            return lib.GetCurrentThread();
        } catch (LastErrorException e) {
            throw new IllegalStateException("GetCurrentThread( ) errorNo=" + e.getErrorCode(), e);
        }
    }

    public static class NUMA_NODE_RELATIONSHIP extends Structure {
        public WinDef.DWORD nodeNumber;
        public byte reserved[] = new byte[20];
        public GROUP_AFFINITY  groupMask;

        public static class ByReference extends NUMA_NODE_RELATIONSHIP implements Structure.ByReference {}

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] { "nodeNumber", "reserved", "groupMask"});
        }

        @Override
        public String toString() {
            return "N#" + nodeNumber.intValue() + "/" + groupMask;
        }
    }    @Override
    public int getCpu() {
        return -1;
    }

    public static class PROCESSOR_GROUP_INFO extends Structure {
        public byte maximumProcCount;
        public byte activeProcCount;
        public byte reserved[] = new byte[38];
        public long activeProessorMask;

        public static class ByReference extends PROCESSOR_GROUP_INFO implements Structure.ByReference {}

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] { "maximumProcCount",  "activeProcCount", "reserved", "activeProessorMask"});
        }

        @Override
        public String toString() {
            if ( activeProcCount == maximumProcCount) {
                return "Groups:" + activeProcCount + "/" + asBitSet( activeProessorMask);
            }
            return "Active:" + activeProcCount + "/" + maximumProcCount + "/" + asBitSet( activeProessorMask);
        }

    }    @Override
    public int getProcessId() {
        return Kernel32.INSTANCE.GetCurrentProcessId();
    }

    public static class GROUP_RELATIONSHIP extends Structure {
        final public PROCESSOR_GROUP_INFO[]  groupInfos = new PROCESSOR_GROUP_INFO[ 3];
        public WinDef.WORD maximumGroupCount;
        public WinDef.WORD activeGroupCount;
        public byte reserved[] = new byte[20];

        public static class ByReference extends GROUP_RELATIONSHIP implements Structure.ByReference {}

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] { "maximumGroupCount",  "activeGroupCount", "reserved", "groupInfos"});
        }

        public PROCESSOR_GROUP_INFO[] getGroupInfos() {
            final int gc = activeGroupCount.intValue();
            PROCESSOR_GROUP_INFO[]    result = new PROCESSOR_GROUP_INFO[gc];
            for ( int i = 0;  i < gc;  i++) {
                result[i] = groupInfos[i];
            }
            return result;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (maximumGroupCount == activeGroupCount) {
                sb.append( "group count: ").append( maximumGroupCount);
            } else {
                sb.append( "groups active: ").append( activeGroupCount).append(" of ").append( maximumGroupCount);
            }
            PROCESSOR_GROUP_INFO[]    infos = getGroupInfos();
            if ( infos.length == 1) {
                sb.append( " group info: ").append( infos[0]);
            } else if ( infos.length == 0){
                sb.append( " no group");
            } else {
                sb.append( " group ");
                for ( int i = 0;  i < infos.length;  i++) {
                    sb.append( "#").append(i).append(": ").append(infos[i]);
                    if ( i < infos.length-1) {
                        sb.append(", ");
                    }
                }
            }
            return sb.toString();
        }

    }    @Override
    public int getThreadId() {
        Integer tid = THREAD_ID.get();
        if (tid == null)
            THREAD_ID.set(tid = Kernel32.INSTANCE.GetCurrentThreadId());
        return tid;
    }

    public static class CACHE_RELATIONSHIP extends Structure {
        public byte level;
        public byte associativity;
        public WinDef.WORD lineSize;
        public WinDef.DWORD cacheSize;
        public byte type;
        public byte reserved[] = new byte[20];
        public GROUP_AFFINITY  groupMask;

        public static class ByReference extends CACHE_RELATIONSHIP implements Structure.ByReference {}

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] { "level", "associativity", "lineSize", "cacheSize", "type", "reserved", "groupMask"});
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("L").append(level);
            if (associativity == 0xFF) {
                sb.append( " fully assoc");
            } else {
                sb.append(" ").append(associativity).append("x assoc");
            }
            sb.append( " LSize ").append(lineSize.intValue()).append( " B");
            // B, K, M
            String    unit = " B";
            long    cs = cacheSize.longValue();
            if (cs % 1024 == 0) {
                cs /= 1024;
                unit = " K";
            }
            if (cs % 1024 == 0) {
                cs /= 1024;
                unit = " M";
            }
            sb.append( " Size ").append( cs).append( unit);
            sb.append( " Type ");
            if ( type == WinNT.PROCESSOR_CACHE_TYPE.CacheData) {
                sb.append("Data");
            } else if( type == WinNT.PROCESSOR_CACHE_TYPE.CacheInstruction) {
                sb.append("Instruction");
            } else if( type == WinNT.PROCESSOR_CACHE_TYPE.CacheTrace) {
                sb.append("Trace");
            } else if( type == WinNT.PROCESSOR_CACHE_TYPE.CacheUnified) {
                sb.append("Unified");
            }
            sb.append( " mask: ").append( groupMask);
            return sb.toString();
        }
    }

    public static class SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX extends Structure {
        public int  relationShip;
        public WinDef.DWORD    size;
        public _U   _u;
        public SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX() {
            super();
        }
        public SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX(Pointer memory) {
            super(memory);
            this.read();
        }

        public int size() {
            if ( size != null) {
                return size.intValue();
            }
            return super.size();
        }

        public void read() {
            super.read();
            switch ( relationShip) {
                case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore:
                case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage:
                    _u.setType( PROCESSOR_RELATIONSHIP.class); break;
                case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode:
                    _u.setType( NUMA_NODE_RELATIONSHIP.class); break;
                case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache:
                    _u.setType( CACHE_RELATIONSHIP.class); break;
                case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup:
                    _u.setType( GROUP_RELATIONSHIP.class); break;
            }
            try {
                _u.read();
            } catch ( Throwable t) {
                _u.read();
            }
        }

        public List<WindowsCpuLayout.Group> asGroups() {
            List<WindowsCpuLayout.Group> groups = new ArrayList<>();
            int index = 0;
            for ( PROCESSOR_GROUP_INFO groupInfo : _u.group.groupInfos) {
                WindowsCpuLayout.Group g = new WindowsCpuLayout.Group( index, groupInfo.activeProessorMask);
                groups.add( g);
            }
            return groups;
        }

        public WindowsCpuLayout.Package asPackage() {
            GROUP_AFFINITY aff = _u.processor.getGroupAffinities()[0];
            return new WindowsCpuLayout.Package( aff.group.intValue(), aff.mask.longValue());
        }

        public WindowsCpuLayout.NumaNode asNumaNode() {
            GROUP_AFFINITY aff = _u.numaNode.groupMask;
            return new WindowsCpuLayout.NumaNode( aff.group.intValue(), aff.mask.longValue());
        }        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] { "relationShip", "size", "_u"});
        }

        public WindowsCpuLayout.Core asCore() {
            GROUP_AFFINITY aff = _u.processor.getGroupAffinities()[0];
            return new WindowsCpuLayout.Core( aff.group.intValue(), aff.mask.longValue());
        }        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
//            sb.append( " at: " + this.getPointer());
            switch ( relationShip) {
                case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore:
                    sb.append(" CoreInfo ").append(_u.processor); break;
                case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage:
                    sb.append(" PackageInfo ").append(_u.processor); break;
                case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode:
                    sb.append(" NodeInfo ").append(_u.numaNode); break;
                case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache:
                    sb.append(" CacheInfo ").append(_u.cache); break;
                case WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup:
                    sb.append(" GroupInfo ").append(_u.group); break;
            }
            return sb.toString();
        }

        public static class ByReference extends SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX implements Structure.ByReference {
            public ByReference(Pointer memory) {
                super(memory);
            }
        }

        public static class _U extends Union {
            public PROCESSOR_RELATIONSHIP   processor;
            public NUMA_NODE_RELATIONSHIP   numaNode;
            public CACHE_RELATIONSHIP   cache;
            public GROUP_RELATIONSHIP   group;

            public static class ByValue extends _U implements Union.ByValue {}
        }





    }

    public static class PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX extends Structure {
        public SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.ByReference pSLPIex;
        public PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX() { }

        public static class ByReference extends PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX implements Structure.ByReference {}
        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"pSLPIex"});
        }
    }

    public static class PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX_ARR extends Structure {
        public PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.ByReference ppSLPIex;
        public PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX_ARR() { }

        public static class ByReference extends PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX_ARR implements Structure.ByReference {}
        public SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.ByReference[] toArray(int size) {
            PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.ByReference[] retvalP = (PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.ByReference[]) ppSLPIex.toArray(size);
            SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.ByReference[] retval = new SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.ByReference[ size];
            for ( int i = 0;  i < retvalP.length;  i++) {
                retval[ i] = retvalP[i].pSLPIex;
            }
            return retval;
        }
        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] { "ppSLPIex"});
        }

    }

    static {
        boolean loaded = false;
        try {
            INSTANCE.getAffinity();
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            LOGGER.warn("Unable to load jna library", e);
        }
        LOADED = loaded;
    }




















}
