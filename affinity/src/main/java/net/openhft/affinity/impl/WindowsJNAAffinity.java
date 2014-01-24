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

import java.util.logging.*;

import net.openhft.affinity.*;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.*;

/**
 * Implementation of {@link net.openhft.affinity.IAffinity} based on JNA call of
 * sched_SetThreadAffinityMask/GetProcessAffinityMask from Windows 'kernel32' library. Applicable for
 * most windows platforms
 * <p> *
 *
 * @author andre.monteiro
 */
public enum WindowsJNAAffinity implements IAffinity {
    INSTANCE;
    public static final boolean LOADED;
    private static final Logger LOGGER = Logger.getLogger(WindowsJNAAffinity.class.getName());

	@Override
	public long getAffinity() {
		long    before = setThreadAffinity( getProcessAffinity());
		setThreadAffinity( before);
		return before;
	}

    public long getProcessAffinity() {
        final CLibrary lib = CLibrary.INSTANCE;
        final LongByReference cpuset1 = new LongByReference(0);
        final LongByReference cpuset2 = new LongByReference(0);
        try {

            final int ret = lib.GetProcessAffinityMask(-1, cpuset1, cpuset2);
            if (ret < 0)
                throw new IllegalStateException("GetProcessAffinityMask(( -1 ), &(" + cpuset1 + "), &(" + cpuset2 + ") ) return " + ret);

            return cpuset1.getValue();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void setAffinity( final long affinity) {
	    setThreadAffinity( affinity);
    }

    public long setThreadAffinity(final long affinity) {
        final CLibrary lib = CLibrary.INSTANCE;

        WinDef.DWORDLONG aff = new WinDef.DWORDLONG(affinity);
	    long    before;
        int pid = getTid();
        try {
            before = lib.SetThreadAffinityMask(pid, aff);
	        return before;
        } catch (LastErrorException e) {
            throw new IllegalStateException("SetThreadAffinityMask((" + pid + ") , &(" + affinity + ") ) errorNo=" + e.getErrorCode(), e);
        }
    }

    public int getTid() {
        final CLibrary lib = CLibrary.INSTANCE;

        try {
            return lib.GetCurrentThread();

        } catch (LastErrorException e) {
            throw new IllegalStateException("GetCurrentThread( ) errorNo=" + e.getErrorCode(), e);
        }
    }


    @Override
    public int getCpu() {
        return -1;
    }

    @Override
    public int getProcessId() {
        return Kernel32.INSTANCE.GetCurrentProcessId();
    }

    @Override
    public int getThreadId() {
        return Kernel32.INSTANCE.GetCurrentThreadId();
    }

	@Override
	/**
	 * look for Property with key "CPUDESC" and use "1/1/1" as default
	 * @see net.openhft.affinity.impl.WindowsCpuLayout
	 * @see net.openhft.affinity.IAffinity#getDefaultLayout()
	 */
	public CpuLayout getDefaultLayout() {
		String  desc = System.getProperty( "CPUDESC", "1/1/1");
		return WindowsCpuLayout.fromCpuDesc( desc);
	}


	/**
     * @author BegemoT
     */
    private interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.loadLibrary("kernel32", CLibrary.class);

        int GetProcessAffinityMask(final int pid, final PointerType lpProcessAffinityMask, final PointerType lpSystemAffinityMask) throws LastErrorException;

        long SetThreadAffinityMask(final int pid, final WinDef.DWORDLONG lpProcessAffinityMask) throws LastErrorException;

        int GetCurrentThread() throws LastErrorException;
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
