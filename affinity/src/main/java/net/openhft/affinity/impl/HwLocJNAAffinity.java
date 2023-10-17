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

import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import net.openhft.affinity.CpuLayout;
import net.openhft.affinity.IAffinity;
import net.openhft.affinity.ICpuInfo;
import net.openhft.affinity.IDefaultLayoutAffinity;
import net.openhft.affinity.impl.LayoutEntities.Cache;
import net.openhft.affinity.impl.LayoutEntities.Core;
import net.openhft.affinity.impl.LayoutEntities.NumaNode;
import net.openhft.affinity.impl.LayoutEntities.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public enum HwLocJNAAffinity implements IAffinity, IDefaultLayoutAffinity {

    INSTANCE {
        @Override
        public CpuLayout getDefaultLayout() {
            if (DefaultLayoutAR.get() == null) {
                HwLocCpuLayout l = null;
//                try {
                    l = getCpuLayout();
                    DefaultLayoutAR.compareAndSet(null, l);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
            return DefaultLayoutAR.get();
        }
    };

    public static HwLocCpuLayout getCpuLayout() {
        String[] command = {"lstopo-no-graphics", "-v", "--no-io"};
        try {
            Process hwLocProcess = Runtime.getRuntime().exec(command);
            InputStream input = hwLocProcess.getInputStream();
            return parseHwLocOutput(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static HwLocCpuLayout parseHwLocOutput(InputStream input) {
        BufferedReader br = new BufferedReader(new InputStreamReader(input));
        String line;
        NumaNode numaNode = null;
        List<NumaNode> numaNodes = new ArrayList<>();
        Socket socket = null;
        List<Socket> sockets = new ArrayList<>();
        Cache l3Cache = null;
        Cache l2Cache = null;
        Cache l1ICache = null;
        Cache l1DCache = null;
        Cache l1Cache = null;
        Core core = null;
        List<Core> cores = new ArrayList<>();
        HwLocCpuInfo lCPU = null;
        List<ICpuInfo> cpuInfos = new ArrayList<>();
        try {
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("Machine")) {
                    continue;
                }
                if (trimmed.startsWith("depth 0:")) {
                    break;
                }
                int lID = parseId(trimmed, "L");
                if (trimmed.startsWith("NUMANode")) {
                    numaNode = new NumaNode(lID);
                    numaNodes.add(numaNode);
                }
                if (trimmed.startsWith("Package")) {
                    socket = new Socket(lID);
                    if (numaNode != null) {
                        socket.setNode(numaNode);
                    }
                    sockets.add(socket);
                }
                if (trimmed.startsWith("L3")) {
                    l3Cache = parseCache(trimmed);
                }
                if (trimmed.startsWith("L2")) {
                    l2Cache = parseCache(trimmed);
                }
                if (trimmed.startsWith("L1")) {
                    l1Cache = parseCache(trimmed);
                    if (l1Cache.getType() == Cache.CacheType.DATA) {
                        l1DCache = l1Cache;
                    }
                    if (l1Cache.getType() == Cache.CacheType.INSTRUCTION) {
                        l1ICache = l1Cache;
                    }
                }
                if (trimmed.startsWith("Core")) {
                    core = new Core(lID);
                    core.setSocket(socket);
                    cores.add(core);
                }
                if (trimmed.startsWith("PU")) {
                    int pID = parseId(trimmed, "P");
                    lCPU = new HwLocCpuInfo(lID, pID, core, l1ICache, l1DCache, l2Cache, l3Cache, socket, numaNode);
                    cpuInfos.add(lCPU);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new HwLocCpuLayout(cpuInfos, numaNodes, sockets, cores);
    }

    private static Cache parseCache(String s) {
        // Level
        byte level = Byte.valueOf(s.substring(1, 2));
        int lID = parseId(s, "L");
        // Size (assuming this is before linesize!)
        String sizePrefix = "size=";
        int sizeIndex = s.indexOf(sizePrefix);
        if (sizeIndex == -1) {
            throw new IllegalArgumentException("need " + sizePrefix + "<number> in " + s);
        }
        String sizeSuffix = "KB";
        int kbIndex = s.indexOf(sizeSuffix);
        String kbSizePart = s.substring(sizeIndex + sizePrefix.length(), kbIndex);
        int kbSize = Integer.valueOf(kbSizePart);
        // Associativity
        String assocPrefix = "ways=";
        int assocIndex = s.indexOf(assocPrefix);
        byte assoc = 0;
        if (assocIndex >= 0) {
            int startOfNumber = assocIndex + assocPrefix.length();
            int spaceIndex = s.indexOf(" ", startOfNumber);
            assoc = Byte.valueOf(s.substring(startOfNumber, spaceIndex));
        }
        // lineSize
        String lineSizePrefix = "linesize=";
        int lineSizeIndex = s.indexOf(lineSizePrefix);
        if (lineSizeIndex == -1) {
            throw new IllegalArgumentException("need " + lineSizePrefix + "<number> in " + s);
        }
        int startOfNumber = lineSizeIndex + lineSizePrefix.length();
        int spaceIndex = s.indexOf(" ", startOfNumber);
        short lineSize = Short.valueOf(s.substring(startOfNumber, spaceIndex));

        final Cache.CacheType type;
        // Type
        if (level == 1) {
            switch (s.charAt(2)) {
                case 'd':
                    type = Cache.CacheType.DATA;
                    break;
                case 'i':
                    type = Cache.CacheType.INSTRUCTION;
                    break;
                default:
                    type = Cache.CacheType.UNIFIED;
            }
        } else {
            type = Cache.CacheType.UNIFIED;
        }
        Cache cache = new Cache(lID, kbSize * 1024, level, lineSize, assoc, type);
        return cache;
    }

    private static int parseId(String s, String lorP) {
        String sharp = lorP + "#";
        if (s == null) {
            throw new IllegalArgumentException("need " + sharp + "<number> in " + s);
        }
        int sharpIndex = s.indexOf(sharp);
        if (sharpIndex == -1) {
            throw new IllegalArgumentException("need " + sharp + "<number> in " + s);
        }
        int startOfNumber = sharpIndex + sharp.length();
        int spaceIndex = s.indexOf(" ", startOfNumber);
        if (spaceIndex < 0) {
            spaceIndex = s.indexOf(")", startOfNumber);
        }
        int id = Integer.valueOf(s.substring(startOfNumber, spaceIndex));
        return id;
    }


    private static final Logger LOGGER = LoggerFactory.getLogger(HwLocJNAAffinity.class);
    public static final boolean LOADED;

    @Override
    public BitSet getAffinity() {
        final LinuxHelper.cpu_set_t cpuset = LinuxHelper.sched_getaffinity();

        BitSet ret = new BitSet(LinuxHelper.cpu_set_t.__CPU_SETSIZE);
        int i = 0;
        for (NativeLong nl : cpuset.__bits) {
            for (int j = 0; j < Long.SIZE; j++) {
                ret.set(i++, ((nl.longValue() >>> j) & 1) != 0);
            }
        }
        return ret;
    }

    @Override
    public void setAffinity(final BitSet affinity) {
        LinuxHelper.sched_setaffinity(affinity);
    }

    @Override
    public int getCpu() {
        return LinuxHelper.sched_getcpu();
    }

    private static final int PROCESS_ID;

    static {
        int pid = -1;
        try {
            pid = LinuxHelper.getpid();
        } catch (Exception ignored) {
        }
        PROCESS_ID = pid;
    }

    @Override
    public int getProcessId() {
        return PROCESS_ID;
    }

    private static final int SYS_gettid = Platform.is64Bit() ? 186 : 224;
    private static final Object[] NO_ARGS = {};
    private final ThreadLocal<Integer> THREAD_ID = new ThreadLocal<>();

    @Override
    public int getThreadId() {
        Integer tid = THREAD_ID.get();
        if (tid == null) {
            THREAD_ID.set(tid = LinuxHelper.syscall(SYS_gettid, NO_ARGS));
        }
        return tid;
    }

    static {
        boolean loaded = false;
        try {
            INSTANCE.getAffinity();
            loaded = INSTANCE.getDefaultLayout() != null; // if no layout, try another Affinity Impl
        } catch (UnsatisfiedLinkError e) {
            LOGGER.warn("Unable to load jna library {}", e);
        }
        LOADED = loaded;
    }
}
