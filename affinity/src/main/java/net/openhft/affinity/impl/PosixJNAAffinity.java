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
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import net.openhft.affinity.IAffinity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Implementation of {@link IAffinity} based on JNA call of
 * sched_setaffinity(3)/sched_getaffinity(3) from 'c' library. Applicable for most
 * linux/unix platforms
 * <p>
 * TODO Support assignment to core 64 and above
 *
 * @author peter.lawrey
 * @author BegemoT
 */
public enum PosixJNAAffinity implements IAffinity {
	INSTANCE;
	public static final boolean LOADED;
	private static final Logger LOGGER = LoggerFactory.getLogger(PosixJNAAffinity.class);
	private static final String LIBRARY_NAME = Platform.isWindows() ? "msvcrt" : "c";

	@Override
	public BitSet getAffinity() {
		final CLibrary lib = CLibrary.INSTANCE;
		final int procs = Runtime.getRuntime().availableProcessors();

		final int cpuSetSizeInLongs = (procs + 63) / 64;
		final int cpuSetSizeInBytes = cpuSetSizeInLongs * 8;
		final Memory cpusetArray = new Memory(cpuSetSizeInBytes);
		final PointerByReference cpuset = new PointerByReference(cpusetArray);
		try {
			final int ret = lib.sched_getaffinity(0, cpuSetSizeInBytes, cpuset);
			if (ret < 0) {
				throw new IllegalStateException("sched_getaffinity((" + cpuSetSizeInBytes + ") , &(" + cpusetArray + ") ) return " + ret);
			}
			ByteBuffer buff = cpusetArray.getByteBuffer(0, cpuSetSizeInBytes);
			return BitSet.valueOf(buff.array());
		} catch (LastErrorException e) {
			if (e.getErrorCode() != 22) {
				throw new IllegalStateException("sched_getaffinity((" + cpuSetSizeInBytes + ") , &(" + cpusetArray + ") ) errorNo=" + e.getErrorCode(), e);
			}
		}

		// fall back to the old method
		final IntByReference cpuset32 = new IntByReference(0);
		try {
			final int ret = lib.sched_getaffinity(0, Integer.SIZE / 8, cpuset32);
			if (ret < 0) {
				throw new IllegalStateException("sched_getaffinity((" + Integer.SIZE / 8 + ") , &(" + cpuset32 + ") ) return " + ret);
			}
			long[] longs = new long[1];
			longs[0] = cpuset32.getValue() & 0xFFFFFFFFL;
			return BitSet.valueOf(longs);
		} catch (LastErrorException e) {
			throw new IllegalStateException("sched_getaffinity((" + Integer.SIZE / 8 + ") , &(" + cpuset32 + ") ) errorNo=" + e.getErrorCode(), e);
		}
	}

	@Override
	public void setAffinity(final BitSet affinity) {
		int procs = Runtime.getRuntime().availableProcessors();
		if (affinity.isEmpty()) {
			throw new IllegalArgumentException("Cannot set zero affinity");
		}

		final CLibrary lib = CLibrary.INSTANCE;
		byte[] buff = affinity.toByteArray();
		final int cpuSetSizeInBytes = buff.length;
		final Memory cpusetArray = new Memory(cpuSetSizeInBytes);
		try {
			cpusetArray.write(0, buff, 0, buff.length);
			final int ret = lib.sched_setaffinity(0, cpuSetSizeInBytes, new PointerByReference(cpusetArray));
			if (ret < 0) {
				throw new IllegalStateException("sched_setaffinity((" + cpuSetSizeInBytes + ") , &(" + affinity + ") ) return " + ret);
			}
		} catch (LastErrorException e) {
			if (e.getErrorCode() != 22 || !Arrays.equals(buff, cpusetArray.getByteArray(0, cpuSetSizeInBytes))) {
				throw new IllegalStateException("sched_setaffinity((" + cpuSetSizeInBytes + ") , &(" + affinity + ") ) errorNo=" + e.getErrorCode(), e);
			}
		}

		final int value = (int) affinity.toLongArray()[0];
		if (value == 0) {
			throw new IllegalArgumentException("Cannot set zero affinity");
		}
		final IntByReference cpuset32 = new IntByReference(0);
		cpuset32.setValue(value);
		try {
			final int ret = lib.sched_setaffinity(0, Integer.SIZE / 8, cpuset32);
			if (ret < 0)
				throw new IllegalStateException("sched_setaffinity((" + Integer.SIZE / 8 + ") , &(" + Integer.toHexString(cpuset32.getValue()) + ") ) return " + ret);
		} catch (LastErrorException e) {
			throw new IllegalStateException("sched_setaffinity((" + Integer.SIZE / 8 + ") , &(" + Integer.toHexString(cpuset32.getValue()) + ") ) errorNo=" + e.getErrorCode(), e);
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
		} catch (UnsatisfiedLinkError ule) {
			try {
				final IntByReference cpu = new IntByReference();
				final IntByReference node = new IntByReference();
				final int ret = lib.syscall(318, cpu, node, null);
				if (ret != 0) {
					throw new IllegalStateException("getcpu( ) return " + ret);
				}

				return cpu.getValue();
			} catch (LastErrorException lee) {
				throw new IllegalStateException("getcpu( ) errorNo=" + lee.getErrorCode(), lee);
			}
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

		int getcpu(final IntByReference cpu,
		           final IntByReference node,
		           final PointerType tcache) throws LastErrorException;

		int getpid() throws LastErrorException;

		int syscall(int number, Object... args) throws LastErrorException;
	}

	static {
		boolean loaded = false;
		try {
			INSTANCE.getAffinity();
			loaded = true;
		} catch (UnsatisfiedLinkError e) {
			LOGGER.warn("Unable to load jna library {}", e);
		}
		LOADED = loaded;
	}
}
