/*
 * Copyright 2016 higherfrequencytrading.com
 * Copyright 2015-2026 Ralf Helbing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.pkmd.affinity.impl;

import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import de.pkmd.affinity.CpuLayout;
import de.pkmd.affinity.IAffinity;
import de.pkmd.affinity.IDefaultLayoutAffinity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

public enum LinuxJNAAffinity implements IAffinity, IDefaultLayoutAffinity {
	INSTANCE {
		@Override
		public CpuLayout getDefaultLayout() {
			if (DefaultLayoutAR.get() == null) {
				VanillaCpuLayout l = null;
				try {
					l = VanillaCpuLayout.fromCpuInfo();
					DefaultLayoutAR.compareAndSet(null, l);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return DefaultLayoutAR.get();
		}

		@Override
		public String getRawData() {
			return VanillaCpuLayout.getLastRawData();
		}
	};
	private static final Logger LOGGER = LoggerFactory.getLogger(LinuxJNAAffinity.class);
	public static final boolean LOADED;

	@Override
	public BitSet getAffinity() {
		final LinuxHelper.cpu_set_t cpuset = LinuxHelper.sched_getaffinity();

		BitSet ret = new BitSet(LinuxHelper.cpu_set_t.__CPU_SETSIZE);
		int i = 0;
		for (NativeLong nl : cpuset.__bits) {
			for (int j = 0; j < Long.SIZE; j++)
				ret.set(i++, ((nl.longValue() >>> j) & 1) != 0);
		}
		return ret;
	}


	public BitSet getAffinityOld() {
		final LinuxHelper.cpu_set_t cpuset = LinuxHelper.sched_getaffinity();

		boolean collect = false;
		ArrayList<Byte> bytes = new ArrayList<Byte>();

		ByteBuffer buff = null;
		if (Platform.is64Bit()) {
			buff = ByteBuffer.allocate(2 * Long.SIZE / 8); // reicht für 128 CPUs
		} else {
			buff = ByteBuffer.allocate(Integer.SIZE / 8);
		}

		final NativeLong[] bits = cpuset.__bits;
		for (int i = bits.length - 1; i >= 0; --i) {
			final long longValue = bits[i].longValue();
			if (!collect && longValue != 0) {
				collect = true;
			}

			if (collect) {
				if (Platform.is64Bit()) {
					buff.putLong(longValue);
				} else {
					buff.putInt((int) longValue);
				}

				final byte[] arr = buff.array();
				//for (int j = arr.length - 1; j >= 0; --j)
				for (int j = 0; j < arr.length; j++) {
					bytes.add(arr[j]);
				}
			}
		}

		if (!bytes.isEmpty()) {
			byte[] data = new byte[bytes.size()];
			for (int i = 0; i < bytes.size(); i++) {
				// don't forget to reverse the order of long values
				data[data.length - i - 1] = bytes.get(i);
			}
			return BitSet.valueOf(data);
		} else {
			return new BitSet();
		}
	}

	// TODO: FIXME!!! CHANGE IAffinity TO SUPPORT PLATFORMS WITH 64+ CORES FIXME!!!
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
		if (tid == null)
			THREAD_ID.set(tid = LinuxHelper.syscall(SYS_gettid, NO_ARGS));
		return tid;
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
