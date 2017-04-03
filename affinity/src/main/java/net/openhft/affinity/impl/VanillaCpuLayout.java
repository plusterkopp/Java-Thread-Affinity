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

import net.openhft.affinity.CpuLayout;
import net.openhft.affinity.ICpuInfo;
import net.openhft.affinity.impl.LayoutEntities.Core;
import net.openhft.affinity.impl.LayoutEntities.Socket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.function.Function;

import static java.lang.Integer.parseInt;

/**
 * @author peter.lawrey
 */
public class VanillaCpuLayout implements CpuLayout {
    public static final int MAX_CPUS_SUPPORTED = 64;

    @NotNull
    private final List<VanillaCpuInfo> cpuDetails;
    private final int sockets;
    private final int coresPerSocket;
    private final int threadsPerCore;

    public List<Socket> packages;
    public List<Core> cores;


    VanillaCpuLayout(@NotNull List<VanillaCpuInfo> cpuDetails) {
        this.cpuDetails = cpuDetails;
        SortedSet<Integer> sockets = new TreeSet<Integer>(),
                coresSet = new TreeSet<Integer>(),
                threads = new TreeSet<Integer>();
        for (VanillaCpuInfo cpuDetail : cpuDetails) {
            sockets.add(cpuDetail.getSocketId());
            coresSet.add((cpuDetail.getSocketId() << 16) + cpuDetail.getCoreId());
            threads.add(cpuDetail.getThreadId());
        }
        this.sockets = sockets.size();
        this.coresPerSocket = coresSet.size() / sockets.size();
        this.threadsPerCore = threads.size();

        packages = createSocketsList( cpuDetails);
        cores = createCoreList( cpuDetails); // Collections.unmodifiableList( new ArrayList<Core>( coreSet));

        if (cpuDetails.size() != sockets() * coresPerSocket() * threadsPerCore()) {
            StringBuilder error = new StringBuilder();
            error.append("cpuDetails.size= ").append(cpuDetails.size())
                    .append(" != sockets: ").append(sockets())
                    .append(" * coresPerSocket: ").append(coresPerSocket())
                    .append(" * threadsPerCore: ").append(threadsPerCore()).append('\n');
            for (VanillaCpuInfo detail : cpuDetails) {
                error.append(detail).append('\n');
            }
            throw new AssertionError(error);
        }
    }

    private List<Socket> createSocketsList(List<VanillaCpuInfo> cpuDetails) {

        SortedSet   result = new TreeSet<Socket>();

        // gather elements to process
        Set<Integer> remainingCPUIDs = new HashSet<>();
        Set<Integer> remainingSockets = new HashSet<>();
        fillSets( remainingCPUIDs, remainingSockets, cpuDetails, cpuInfo -> cpuInfo.getSocketId());
		// for every socket, set all the bits of corresponding CPUs in the socket mask
        for ( int socketID: remainingSockets) {
            BitSet  mask = new BitSet( cpuDetails.size());
            for ( int i = 0;  i < cpuDetails.size();  i++) {
                VanillaCpuInfo  cpuInfo = cpuDetails.get( i);
                if ( cpuInfo.getSocketId() == socketID) {
                    mask.set( i);
                }
            }
			Socket  socket = new Socket( mask);
            result.add( socket);
        }
        return Collections.unmodifiableList( new ArrayList<Socket>( result));
    }

    private List<Core> createCoreList(List<VanillaCpuInfo> cpuDetails) {

        SortedSet   result = new TreeSet<Core>();

        // gather elements to process
        Set<Integer> remainingCPUIDs = new HashSet<>();
        Set<Integer> remainingCores = new HashSet<>();
        fillSets( remainingCPUIDs, remainingCores, cpuDetails, cpuInfo -> cpuInfo.getCoreId());
        // for every socket, set all the bits of corresponding CPUs in the socket mask
        for ( int coreID: remainingCores) {
            BitSet  mask = new BitSet( cpuDetails.size());
            for ( int i = 0;  i < cpuDetails.size();  i++) {
                VanillaCpuInfo  cpuInfo = cpuDetails.get( i);
                if ( cpuInfo.getCoreId() == coreID) {
                    mask.set( i);
                }
            }
            Core  core = new Core( mask);
            result.add( core);
        }
        return Collections.unmodifiableList( new ArrayList<Core>( result));
    }

    private void fillSets(Set<Integer> remainingCPUIDs, Set<Integer> remainingSockets, List<VanillaCpuInfo> cpuDetails, Function<ICpuInfo, Integer> idMapper) {
        for ( int   i = 0;  i < cpuDetails.size() - 1;  i++) {
            remainingCPUIDs.add( i);
            VanillaCpuInfo  cpuInfo = cpuDetails.get( i);
            int id = idMapper.apply( cpuInfo);
            remainingSockets.add( id);
        }
    }

    @NotNull
    public static VanillaCpuLayout fromProperties(String fileName) throws IOException {
        return fromProperties(openFile(fileName));
    }

    @NotNull
    public static VanillaCpuLayout fromProperties(InputStream is) throws IOException {
        Properties prop = new Properties();
        prop.load(is);
        return fromProperties(prop);
    }

    @NotNull
    public static VanillaCpuLayout fromProperties(@NotNull Properties prop) {
        List<VanillaCpuInfo> cpuDetails = new ArrayList<VanillaCpuInfo>();
        for (int i = 0; i < MAX_CPUS_SUPPORTED; i++) {
            String line = prop.getProperty("" + i);
            if (line == null) break;
            String[] word = line.trim().split(" *, *");
            VanillaCpuInfo details = new VanillaCpuInfo(parseInt(word[0]),
                    parseInt(word[1]), parseInt(word[2]));
            cpuDetails.add(details);
        }
        return new VanillaCpuLayout(cpuDetails);
    }

    @NotNull
    public static VanillaCpuLayout fromCpuInfo() throws IOException {
        return fromCpuInfo("/proc/cpuinfo");
    }

    @NotNull
    public static VanillaCpuLayout fromCpuInfo(String filename) throws IOException {
        return fromCpuInfo(openFile(filename));
    }

    private static InputStream openFile(String filename) throws FileNotFoundException {
        try {
            return new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
            if (is == null)
                throw e;
            return is;
        }
    }

    @NotNull
    public static VanillaCpuLayout fromCpuInfo(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String line;
        List<VanillaCpuInfo> cpuDetails = new ArrayList<VanillaCpuInfo>();
        VanillaCpuInfo details = new VanillaCpuInfo();
        Map<String, Integer> threadCount = new LinkedHashMap<String, Integer>();

        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) {
                String key = details.getSocketId() + "," + details.getCoreId();
                Integer count = threadCount.get(key);
                if (count == null)
                    threadCount.put(key, count = 1);
                else
                    threadCount.put(key, count += 1);
                details.setThreadId(count - 1);
                cpuDetails.add(details);
                details = new VanillaCpuInfo();
                details.setCoreId(cpuDetails.size());
                continue;
            }
            String[] words = line.split("\\s*:\\s*", 2);
            if (words[0].equals("physical id"))
                details.setSocketId(parseInt(words[1]));
            else if (words[0].equals("core id"))
                details.setCoreId(parseInt(words[1]));
        }
        return new VanillaCpuLayout(cpuDetails);
    }

    @Override
    public int cpus() {
        return cpuDetails.size();
    }

    public int sockets() {
        return sockets;
    }

    public int coresPerSocket() {
        return coresPerSocket;
    }

    @Override
    public int threadsPerCore() {
        return threadsPerCore;
    }

    @Override
    public int socketId(int cpuId) {
        return cpuDetails.get(cpuId).getSocketId();
    }

    @Override
    public int coreId(int cpuId) {
        return cpuDetails.get(cpuId).getCoreId();
    }

    @Override
    public int threadId(int cpuId) {
        return cpuDetails.get(cpuId).getThreadId();
    }

    @NotNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, cpuDetailsSize = cpuDetails.size(); i < cpuDetailsSize; i++) {
            VanillaCpuInfo cpuDetail = cpuDetails.get(i);
            sb.append(i).append(": ").append(cpuDetail).append('\n');
        }
        return sb.toString();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VanillaCpuLayout that = (VanillaCpuLayout) o;

        if (coresPerSocket != that.coresPerSocket) return false;
        if (sockets != that.sockets) return false;
        if (threadsPerCore != that.threadsPerCore) return false;
        return cpuDetails.equals(that.cpuDetails);

    }

    @Override
    public int hashCode() {
        int result = cpuDetails.hashCode();
        result = 31 * result + sockets;
        result = 31 * result + coresPerSocket;
        result = 31 * result + threadsPerCore;
        return result;
    }


}
