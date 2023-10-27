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

import net.openhft.affinity.*;
import net.openhft.affinity.impl.LayoutEntities.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

/**
 * @author peter.lawrey
 */
public class VanillaCpuLayout implements CpuLayout {
    public static final int MAX_CPUS_SUPPORTED = 64;

    @NotNull
    protected final List<ICpuInfo> cpuDetails;
    private final int sockets;
    private final int coresPerSocket;
    private final int threadsPerCore;

    public List<Socket> packages;
    public List<Core> cores;


    VanillaCpuLayout(@NotNull List<ICpuInfo> cpuDetails) {
        this.cpuDetails = cpuDetails;
        SortedSet<Integer> sockets = new TreeSet<Integer>(),
                coresSet = new TreeSet<Integer>(),
                threads = new TreeSet<Integer>();
        for (ICpuInfo cpuDetail : cpuDetails) {
            sockets.add(cpuDetail.getSocketId());
            coresSet.add((cpuDetail.getSocketId() << 16) + cpuDetail.getCoreId());
            threads.add(cpuDetail.getThreadId());
        }
        this.sockets = sockets.size();
        this.coresPerSocket = coresSet.size() / sockets.size();
        this.threadsPerCore = threads.size();

        packages = createSocketsList( cpuDetails);
        cores = createCoreList( cpuDetails, packages); // Collections.unmodifiableList( new ArrayList<Core>( coreSet));

        if (cpuDetails.size() != sockets() * coresPerSocket() * threadsPerCore()) {
            StringBuilder error = new StringBuilder();
            error.append("cpuDetails.size= ").append(cpuDetails.size())
                    .append(" != sockets: ").append(sockets())
                    .append(" * coresPerSocket: ").append(coresPerSocket())
                    .append(" * threadsPerCore: ").append(threadsPerCore()).append('\n');
            for (ICpuInfo detail : cpuDetails) {
                error.append(detail).append('\n');
            }
            throw new AssertionError(error);
        }
    }

    private List<Socket> createSocketsList(List<ICpuInfo> cpuDetails) {

        SortedSet   result = new TreeSet<Socket>();

        Set<Integer> remainingSockets = cpuDetails.stream()
		        .map( cpuDetail -> cpuDetail.getSocketId())
		        .collect(Collectors.toSet());
		// for every socket, set all the bits of corresponding CPUs in the socket mask
        for ( int socketID: remainingSockets) {
            BitSet  mask = new BitSet( cpuDetails.size());
            for ( int i = 0;  i < cpuDetails.size();  i++) {
                ICpuInfo  cpuInfo = cpuDetails.get( i);
	            if ( cpuInfo.getSocketId() == socketID) {
		            mask.set( i);
                }
            }
			Socket  socket = new Socket( mask);
            socket.setId( socketID);
            result.add( socket);
        }
        return Collections.unmodifiableList( new ArrayList<Socket>( result));
    }

    private List<Core> createCoreList(List<ICpuInfo> cpuDetails, List<Socket> sockets) {

        SortedSet   result = new TreeSet<Core>();

        // gather elements to process
        Set<Core> remainingCores = new HashSet<>();
	    for ( int i = 0;  i < cpuDetails.size();  i++) {
            ICpuInfo  cpuInfo = cpuDetails.get( i);
		    Core core = new Core((BitSet) null);
		    core.setId( cpuInfo.getCoreId());
		    Optional<Socket> socketO = sockets.stream().filter(s -> s.getId() == cpuInfo.getSocketId()).findFirst();
		    if (! socketO.isPresent()) {
			    System.err.println( "can not find socket for " + cpuInfo + " in list " + sockets);
		    }
		    core.setSocket( socketO.orElse( null));
			remainingCores.add( core);
	    }
        // for every core, set all the bits of corresponding CPUs in the core mask
        for ( Core remCore: remainingCores) {
            BitSet  mask = new BitSet( cpuDetails.size());
            for ( int i = 0;  i < cpuDetails.size();  i++) {
                ICpuInfo  cpuInfo = cpuDetails.get( i);
                if ( cpuInfo.getCoreId() == remCore.getId()
		                && cpuInfo.getSocketId() == remCore.getSocket().getId()) {
	                mask.set(i);
                }
            }
            Core core = new Core( mask);
            core.setId( remCore.getId());
            core.setSocket( remCore.getSocket());
            result.add( core);
        }
        return Collections.unmodifiableList( new ArrayList<Core>( result));
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
        List<ICpuInfo> cpuDetails = new ArrayList<>();
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
        List<ICpuInfo> cpuDetails = new ArrayList<>();
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
            ICpuInfo cpuDetail = cpuDetails.get(i);
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

	public ICpuInfo getCPUInfo( int index) {
    	return cpuDetails.get( index);
	}
}
