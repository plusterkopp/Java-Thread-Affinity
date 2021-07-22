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
import net.openhft.affinity.impl.LayoutEntities.*;
import org.slf4j.*;

import static com.sun.jna.platform.win32.WinNT.*;
//import static com.sun.jna.platform.win32.BaseTSD.*;

import java.util.*;

/**
 * Implementation of {@link net.openhft.affinity.IAffinity} based on JNA call of
 * sched_SetThreadAffinityMask/GetProcessAffinityMask from Windows 'kernel32' library. Applicable for
 * most windows platforms
 * <p> *
 *
 * @author andre.monteiro
 */
public enum WindowsJNAAffinity implements IAffinity, IGroupAffinity, IDefaultLayoutAffinity {
    INSTANCE {
        @Override
        public CpuLayout getDefaultLayout() {
            if ( DefaultLayoutAR.get() == null) {
                WindowsCpuLayout l = WindowsCpuLayout.fromSysInfo( INSTANCE.getLogicalProcessorInformation());
                DefaultLayoutAR.compareAndSet( null, l);
            }
            return DefaultLayoutAR.get();
        }
    };

    public static final boolean LOADED;
    private static final Logger LOGGER = LoggerFactory.getLogger(WindowsJNAAffinity.class);

    public static native void getCurrentCpuInfo( short[] groupA, byte[] cpuA);

    static {
        boolean loaded = false;
        try {
            INSTANCE.getAffinity();
            loaded = true;
            Native.register( "affinityInfo");
        } catch (UnsatisfiedLinkError e) {
            LOGGER.warn("Unable to load jna library", e);
        }
        LOADED = loaded;
    }

    public static BitSet asBitSet(long mask) {
        long[] longs = new long[1];
        longs[0] = mask;
        return BitSet.valueOf(longs);
    }

    /**
     * @author BegemoT
     */
    private interface LibKernel32 extends Library {
        LibKernel32 INSTANCE = (LibKernel32) Native.loadLibrary("kernel32", LibKernel32.class, W32APIOptions.UNICODE_OPTIONS);

        int GetProcessAffinityMask(final int pid, final PointerType lpProcessAffinityMask, final PointerType lpSystemAffinityMask) throws LastErrorException;

        DWORD_PTR SetThreadAffinityMask( HANDLE thread, final DWORD_PTR lpProcessAffinityMask) throws LastErrorException;

        int GetCurrentThread() throws LastErrorException;

        boolean GetNumaHighestNodeNumber( final PointerType retValHighestNodeNumber) throws LastErrorException;

        boolean GetNumaNodeProcessorMaskEx( short node, GROUP_AFFINITY.ByReference maskRet);

        boolean GetLogicalProcessorInformationEx( int relationShipType, Memory ret, WinDef.DWORDByReference retLength);

        void GetCurrentProcessorNumberEx( PROCESSOR_NUMBER.ByReference resultRef);

        boolean SetThreadGroupAffinity( final HANDLE tid, GROUP_AFFINITY.ByReference gaRefNew, GROUP_AFFINITY.ByReference gaRefOld);
    }


    private final ThreadLocal<Integer> THREAD_ID = new ThreadLocal<>();
    private final ThreadLocal<PROCESSOR_NUMBER.ByReference> ProcNumRef =
            ThreadLocal.withInitial( () -> new PROCESSOR_NUMBER.ByReference());

    /**
     * @author plusterkopp
     */
    private interface LibAffinityInfo extends Library {
        LibAffinityInfo INSTANCE = (LibAffinityInfo) Native.loadLibrary("affinityInfo", LibAffinityInfo.class, W32APIOptions.UNICODE_OPTIONS);

        boolean getSystemRelationShipInfos( PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX_ARR.ByReference retList, IntByReference retLength);
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
            BitSet maskBS = asBitSet(mask.longValue());
            return "g#" + group.intValue() + "/" + maskBS.toString();
        }
    }

    public static class PROCESSOR_RELATIONSHIP extends Structure {
        private static final int MaxGroupCount = 1;
        public byte flags;
        // public byte    efficiencyClass;
        public byte    reserved[] = new byte[21];
        public short groupCount;
        public GROUP_AFFINITY[]  groupMasks = new GROUP_AFFINITY[ MaxGroupCount];

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
            return "Active:" + activeProcCount + "/" + maximumProcCount + ", mask: " + asBitSet(activeProessorMask);
        }

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
            sb.append( "groups active: ").append( activeGroupCount).append(" of ").append( maximumGroupCount);
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

        public List<Group> asGroups() {
            List<Group> groups = new ArrayList<>();
            for ( int i = 0;  i < _u.group.activeGroupCount.intValue();  i++) {
                PROCESSOR_GROUP_INFO groupInfo = _u.group.groupInfos[ i];
                Group g = new Group( i, groupInfo.activeProessorMask);
                groups.add( g);
            }
            return groups;
        }

        public Socket asPackage() {
            GROUP_AFFINITY aff = _u.processor.getGroupAffinities()[0];
            return new Socket( aff.group.intValue(), aff.mask.longValue());
        }

        public NumaNode asNumaNode() {
            GROUP_AFFINITY aff = _u.numaNode.groupMask;
            return new NumaNode( aff.group.intValue(), aff.mask.longValue());
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] { "relationShip", "size", "_u"});
        }

        public Core asCore() {
            GROUP_AFFINITY aff = _u.processor.getGroupAffinities()[0];
            return new Core( aff.group.intValue(), aff.mask.longValue());
        }

        public Cache asCache() {
            GROUP_AFFINITY aff = _u.cache.groupMask;
            Cache cache = new Cache( aff.group.intValue(), aff.mask.longValue());
            cache.setType( _u.cache.type);
            cache.setAssociativity( _u.cache.associativity);
            cache.setSize( _u.cache.cacheSize);
            cache.setLevel( _u.cache.level);
            cache.setLineSize( _u.cache.lineSize);
            return cache;
        }

        @Override
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

    public static class PROCESSOR_NUMBER  extends Structure {
        public WinDef.WORD group;
        public WinDef.BYTE number;
        public WinDef.BYTE reserved;


        public static class ByReference extends PROCESSOR_NUMBER implements Structure.ByReference {}

        @Override
        protected List getFieldOrder() {
            return Arrays.asList( "group", "number", "reserved");
        }

        public String toString() {
            return "g#" + group + "/" + number;
        }
    }

    @Override
    public int getCpu() {
        short gr[] = new short[1];
        byte pr[] = new byte[1];
        getCurrentCpuInfo( gr, pr);
        CpuLayout    cpuLayout = INSTANCE.getDefaultLayout();
        if ( cpuLayout instanceof WindowsCpuLayout) {
            WindowsCpuLayout    wLayout = (WindowsCpuLayout) cpuLayout;
            int index = wLayout.findCpuInfo( gr[0], pr[0]);
            return index;
        }
        return -1;
    }

    public int getTid() {
        Kernel32 lib = Kernel32.INSTANCE;

        try {
            return lib.GetCurrentThreadId(); // GetCurrentThread();
        } catch (LastErrorException e) {
            throw new IllegalStateException("GetCurrentThread( ) errorNo=" + e.getErrorCode(), e);
        }
    }

    @Override
    public int getThreadId() {
        Integer tid = THREAD_ID.get();
        if (tid == null)
            THREAD_ID.set(tid = Kernel32.INSTANCE.GetCurrentThreadId());
        return tid;
    }

    @Override
    public int getProcessId() {
        return Kernel32.INSTANCE.GetCurrentProcessId();
    }

    public BitSet setThreadAffinity(final BitSet affinity) {
        DWORD_PTR aff;
        long[] longs = affinity.toLongArray();
        switch (longs.length)
        {
            case 0:
                aff = new DWORD_PTR( 0); // new WinDef.DWORDLONG(0);
                break;
            case 1:
                aff = new DWORD_PTR(longs[0]);// new WinDef.DWORDLONG(longs[0]);
                break;
            default:
                throw new IllegalArgumentException("Windows API does not support more than 64 CPUs for thread affinity");
        }

        WinNT.HANDLE threadHandle = Kernel32.INSTANCE.GetCurrentThread();
        try
        {
            DWORD_PTR affBefore = LibKernel32.INSTANCE.SetThreadAffinityMask(threadHandle, aff);
            return asBitSet(affBefore.longValue());
        }
        catch (LastErrorException e)
        {
            throw new IllegalStateException("SetThreadAffinityMask((" + threadHandle + ") , &(" + affinity + ") ) errorNo=" + e.getErrorCode(), e);
        }
    }

    @Override
    public void setAffinity(final BitSet affinity) {
        BitSet  sysAff = getSystemAffinity();
//        if ( sysAff != null && ! sysAff.intersects( affinity)) {
//            LOGGER.error( "can not set affinity " + affinity + " when system affinity mask is only " + sysAff);
//            return;
//        }
        setThreadAffinity( affinity);
        BitSet affNow = getAffinity();
        if ( ! affNow.equals( affinity)) {
            LOGGER.warn("did not set affinity " + affinity + " current affinity is " + affNow);
        }
    }

    public GroupAffinityMask setGroupAffinity(int groupId, long mask) {
        GROUP_AFFINITY.ByReference  garef = new GROUP_AFFINITY.ByReference();
        garef.group.setValue( groupId);
        garef.mask.setValue(mask);

        GROUP_AFFINITY.ByReference  garefprev = new GROUP_AFFINITY.ByReference();
        HANDLE threadHandle = Kernel32.INSTANCE.GetCurrentThread();
        try
        {
            LibKernel32.INSTANCE.SetThreadGroupAffinity( threadHandle, garef, garefprev);
            return new GroupAffinityMask( garefprev.group.intValue(), garefprev.mask.longValue());
        }
        catch (LastErrorException e)
        {
            throw new IllegalStateException("SetThreadGroupAffinity((" + threadHandle + ") , g" + groupId + " m" + mask + ") errorNo=" + e.getErrorCode(), e);
        }
    }

    public BitSet getSystemAffinity() {
        final Kernel32 lib = Kernel32.INSTANCE;
        final ULONG_PTRByReference procAffResult = new ULONG_PTRByReference();
        final ULONG_PTRByReference systemAffResult = new ULONG_PTRByReference();
        try {
            int myPid = Kernel32.INSTANCE.GetCurrentProcessId();
            WinNT.HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, myPid);
            final boolean success = lib.GetProcessAffinityMask(pHandle, procAffResult, systemAffResult);
            // Successful result is positive, according to the docs
            // https://msdn.microsoft.com/en-us/library/windows/desktop/ms683213%28v=vs.85%29.aspx
            if ( ! success) {
                throw new IllegalStateException("GetProcessAffinityMask(( -1 ), &(" + procAffResult + "), &(" + systemAffResult + ") ) return " + success);
            }
            ULONG_PTR value = systemAffResult.getValue();
            return asBitSet( value.longValue());
        }
        catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            e.printStackTrace();
        }
        return new BitSet();
    }

    public BitSet getProcessAffinity() {
        final Kernel32 lib = Kernel32.INSTANCE;
        final ULONG_PTRByReference procAffResult = new ULONG_PTRByReference();
        final ULONG_PTRByReference systemAffResult = new ULONG_PTRByReference();
        try {
            int myPid = Kernel32.INSTANCE.GetCurrentProcessId();
            WinNT.HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, myPid);
            final boolean success = lib.GetProcessAffinityMask(pHandle, procAffResult, systemAffResult);
            // Successful result is positive, according to the docs
            // https://msdn.microsoft.com/en-us/library/windows/desktop/ms683213%28v=vs.85%29.aspx
            if ( ! success) {
                throw new IllegalStateException("GetProcessAffinityMask(( -1 ), &(" + procAffResult + "), &(" + systemAffResult + ") ) return " + success);
            }
            ULONG_PTR value = procAffResult.getValue();
            return asBitSet( value.longValue());
        }
        catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            e.printStackTrace();
        }
        return new BitSet();
    }

    public SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] getLogicalProcessorInformation() {
        return getLogicalProcessorInformation(WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationAll);
    }

    public SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] getLogicalProcessorInformation( int relationType) {
        IntByReference  retCount = new IntByReference();
        PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX_ARR.ByReference retList = new PSYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX_ARR.ByReference();
        LibAffinityInfo.INSTANCE.getSystemRelationShipInfos(retList, retCount);
        SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.ByReference[] allResults = retList.toArray(retCount.getValue());
        List<SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX> filteredList = new ArrayList<>();
        for (SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX.ByReference result : allResults) {
            if ( result.relationShip == relationType || relationType == WinNT.LOGICAL_PROCESSOR_RELATIONSHIP.RelationAll) {
                filteredList.add( result);
            }
        }
        return filteredList.toArray(new SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[filteredList.size()]);
    }

    @Override
    public BitSet getAffinity() {
        BitSet  procAff = getProcessAffinity();
        BitSet  affBefore = setThreadAffinity( procAff);
        setThreadAffinity(affBefore);
        return affBefore;
    }

    public Map<Integer, BitSet> getNumaNodes() {
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
                BitSet bsMask = asBitSet(maskRL);
                result.put( Integer.valueOf( i), bsMask);
            }
        }
        return result;
    }

    public void getCurrentProcessorNumber(PROCESSOR_NUMBER.ByReference pNum) {
//        PROCESSOR_NUMBER.ByReference procNumRef = ProcNumRef.get();
        LibKernel32.INSTANCE.GetCurrentProcessorNumberEx( pNum);
    }



}
