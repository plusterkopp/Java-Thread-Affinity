/*
 * Copyright 2013 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.affinity.impl;

import com.sun.jna.*;
import com.sun.jna.ptr.*;
import net.openhft.affinity.*;

import java.io.*;
import java.util.logging.*;


/**
 * Implementation of {@link IAffinity} based on JNA call of
 * sched_setaffinity(3)/sched_getaffinity(3) from 'c' library. Applicable for most
 * linux/unix platforms
 * <p/>
 * TODO Support assignment to core 64 and above
 *
 * @author peter.lawrey
 * @author BegemoT
 */
public enum PosixJNAAffinity implements IAffinity {
    INSTANCE;
    public static final boolean LOADED;
    private static final Logger LOGGER = Logger.getLogger(PosixJNAAffinity.class.getName());
    private static final String LIBRARY_NAME = Platform.isWindows() ? "msvcrt" : "c";

    @Override
    public long getAffinity() {
        final CLibrary lib = CLibrary.INSTANCE;
        // TODO where are systems with 64+ cores...
        final LongByReference cpuset = new LongByReference(0L);
        try {
            final int ret = lib.sched_getaffinity(0, Long.SIZE / 8, cpuset);
            if (ret < 0)
                throw new IllegalStateException("sched_getaffinity((" + Long.SIZE / 8 + ") , &(" + cpuset + ") ) return " + ret);
            return cpuset.getValue();
        } catch (LastErrorException e) {
            if (e.getErrorCode() != 22)
                throw new IllegalStateException("sched_getaffinity((" + Long.SIZE / 8 + ") , &(" + cpuset + ") ) errorNo=" + e.getErrorCode(), e);
        }
        final IntByReference cpuset32 = new IntByReference(0);
        try {
            final int ret = lib.sched_getaffinity(0, Integer.SIZE / 8, cpuset32);
            if (ret < 0)
                throw new IllegalStateException("sched_getaffinity((" + Integer.SIZE / 8 + ") , &(" + cpuset32 + ") ) return " + ret);
            return cpuset32.getValue();
        } catch (LastErrorException e) {
            throw new IllegalStateException("sched_getaffinity((" + Integer.SIZE / 8 + ") , &(" + cpuset32 + ") ) errorNo=" + e.getErrorCode(), e);
        }
    }

    @Override
    public void setAffinity(final long affinity) {
        final CLibrary lib = CLibrary.INSTANCE;
        try {
            //fixme: where are systems with more then 64 cores...
            final int ret = lib.sched_setaffinity(0, Long.SIZE / 8, new LongByReference(affinity));
            if (ret < 0) {
                throw new IllegalStateException("sched_setaffinity((" + Long.SIZE / 8 + ") , &(" + affinity + ") ) return " + ret);
            }
        } catch (LastErrorException e) {
            if (e.getErrorCode() != 22)
                throw new IllegalStateException("sched_getaffinity((" + Long.SIZE / 8 + ") , &(" + affinity + ") ) errorNo=" + e.getErrorCode(), e);
        }
        try {
            final int ret = lib.sched_setaffinity(0, Integer.SIZE / 8, new IntByReference((int) affinity));
            if (ret < 0) {
                throw new IllegalStateException("sched_setaffinity((" + Integer.SIZE / 8 + ") , &(" + affinity + ") ) return " + ret);
            }
        } catch (LastErrorException e) {
            throw new IllegalStateException("sched_getaffinity((" + Integer.SIZE / 8 + ") , &(" + affinity + ") ) errorNo=" + e.getErrorCode(), e);
        }
    }

    @Override
    public int getCpu() {
        final CLibrary lib = CLibrary.INSTANCE;
        try {
            final int ret = lib.sched_getcpu();
            if (ret < 0)
                throw new IllegalStateException("sched_getcpu( ) return " + ret);
            return ret;
        } catch (LastErrorException e) {
            throw new IllegalStateException("sched_getcpu( ) errorNo=" + e.getErrorCode(), e);
        }
    }

    private static final int PROCESS_ID;

    @Override
    public int getProcessId() {
        return PROCESS_ID;
    }

    static {
        int processId;
        try {
            processId = CLibrary.INSTANCE.getpid();
        } catch (Exception ignored) {
            processId = -1;
        }
        PROCESS_ID = processId;
    }

    private final ThreadLocal<Integer> THREAD_ID = new ThreadLocal<Integer>();

    @Override
    public int getThreadId() {
        if (ISLINUX) {
            Integer tid = THREAD_ID.get();
            if (tid == null)
                THREAD_ID.set(tid = CLibrary.INSTANCE.syscall(SYS_gettid, NO_ARGS));
            return tid;
        }
        return -1;
    }

	@Override
	public CpuLayout getDefaultLayout() {
		try {
			return VanillaCpuLayout.fromCpuInfo();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "cannot create default CpuLayout " + e);
		}
		return null;
	}

	private static final boolean ISLINUX = "Linux".equals(System.getProperty("os.name"));

    private static final boolean IS64BIT = is64Bit0();

    private static final int SYS_gettid = is64Bit() ? 186 : 224;

    private static final Object[] NO_ARGS = {};

    public static boolean is64Bit() {
        return IS64BIT;
    }

    private static boolean is64Bit0() {
        String systemProp;
        systemProp = System.getProperty("com.ibm.vm.bitmode");
        if (systemProp != null) {
            return "64".equals(systemProp);
        }
        systemProp = System.getProperty("sun.arch.data.model");
        if (systemProp != null) {
            return "64".equals(systemProp);
        }
        systemProp = System.getProperty("java.vm.version");
        return systemProp != null && systemProp.contains("_64");
    }


    /**
     * @author BegemoT
     */
    interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary)
                Native.loadLibrary(LIBRARY_NAME, CLibrary.class);

        int sched_setaffinity(final int pid,
                              final int cpusetsize,
                              final PointerType cpuset) throws LastErrorException;

        int sched_getaffinity(final int pid,
                              final int cpusetsize,
                              final PointerType cpuset) throws LastErrorException;

        int sched_getcpu() throws LastErrorException;

        int getpid() throws LastErrorException;

        int syscall(int number, Object... args) throws LastErrorException;
    }

    static {
        boolean loaded = false;
        try {
            INSTANCE.getAffinity();
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            LOGGER.log(Level.WARNING, "Unable to load jna library " + e);
        }
        LOADED = loaded;
    }
}
