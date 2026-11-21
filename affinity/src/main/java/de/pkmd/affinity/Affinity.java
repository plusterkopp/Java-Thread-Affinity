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

package de.pkmd.affinity;

import de.pkmd.affinity.impl.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.BitSet;

/**
 * Library to wrap low level JNI or JNA calls.  Can be called without needing to know the actual implementation used.
 *
 * @author peter.lawrey
 */
public enum Affinity {
	;
	private static final Logger LOGGER = LoggerFactory.getLogger(Affinity.class);
	@NotNull
	private static final IAffinity AFFINITY_IMPL = initImpl();
	private static Boolean JNAAvailable;

	private static IAffinity initImpl() {
		String osName = System.getProperty("os.name");
		if (osName.contains("Win") && isWindowsJNAAffinityUsable()) {
			LOGGER.trace("Using Windows JNA-based affinity control implementation");
			return WindowsJNAAffinity.INSTANCE;
		}
		if (osName.startsWith("Linux")) {
			if (isHwLocAffinityUsable()) {
				LOGGER.trace("Using hwloc text-based affinity control implementation");
				return HwLocJNAAffinity.INSTANCE;
			}
			if (isLinuxJNAAffinityUsable()) {
				LOGGER.trace("Using Linux JNA-based affinity control implementation");
				return LinuxJNAAffinity.INSTANCE;
			}
		}
		LOGGER.info("Using dummy affinity control implementation");
		return NullAffinity.INSTANCE;
	}

	public static IAffinity getAffinityImpl() {
		return AFFINITY_IMPL;
	}

	private static boolean isWindowsJNAAffinityUsable() {
		if (isJNAAvailable()) {
			try {
				return WindowsJNAAffinity.LOADED;
			} catch (Throwable t) {
				logThrowable(t, "Windows JNA-based affinity not usable because it failed to load!");
				return false;
			}
		}
		LOGGER.warn("Windows JNA-based affinity not usable due to JNA not being available!");
		return false;
	}

	private static boolean isHwLocAffinityUsable() {
		if (isJNAAvailable()) {
			try {
				return HwLocJNAAffinity.LOADED;
			} catch (Throwable t) {
				logThrowable(t, "HwLoc affinity not usable because it failed to load!");
				return false;
			}
		}
		LOGGER.warn("HwLoc affinity not usable due to JNA not being available!");
		return false;
	}

	private static boolean isLinuxJNAAffinityUsable() {
		if (isJNAAvailable()) {
			try {
				return LinuxJNAAffinity.LOADED;
			} catch (Throwable t) {
				logThrowable(t, "Linux JNA-based affinity not usable because it failed to load!");
				return false;
			}
		}
		LOGGER.warn("Linux JNA-based affinity not usable due to JNA not being available!");
		return false;
	}

	private static void logThrowable(Throwable t, String description) {
		StringWriter sw = new StringWriter();
		sw.append(description);
		sw.append(" Reason: ");
		t.printStackTrace(new PrintWriter(sw));
		LOGGER.warn(sw.toString());
	}

	public static BitSet getAffinity() {
		return AFFINITY_IMPL.getAffinity();
	}

	public static void setAffinity(int cpu) {
		BitSet affinity = new BitSet(Runtime.getRuntime().availableProcessors());
		affinity.set(cpu);
		setAffinity(affinity);
	}

	public static void setAffinity(final BitSet affinity) {
		AFFINITY_IMPL.setAffinity(affinity);
	}

	public static int getCpu() {
		return AFFINITY_IMPL.getCpu();
	}

	public static int getThreadId() {
		return AFFINITY_IMPL.getThreadId();
	}

	public static boolean isJNAAvailable() {
		if (JNAAvailable == null)
			try {
				Class.forName("com.sun.jna.Platform");
				JNAAvailable = true;
			} catch (ClassNotFoundException ignored) {
				JNAAvailable = false;
			}
		return JNAAvailable;
	}

}
