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

package net.openhft.affinity;

/**
 * Implementation interface
 *
 * @author cheremin
 * @since 29.12.11,  20:14
 */
public interface IAffinity {
    /**
     * @return returns affinity mask for current thread, or -1 if unknown
     */
    long getAffinity();

    /**
     * @param affinity sets affinity mask of current thread to specified value
     */
    void setAffinity(final long affinity);

    /**
     * @return the current cpu id, or -1 if unknown.
     */
    int getCpu();

    /**
     * @return the process id of the current process.
     */
    int getProcessId();

    /**
     * @return the thread id of the current thread or -1 is not available.
     */
    int getThreadId();

	/** @return a platform-independent default CpuLayout or null, if there really is none */
	CpuLayout getDefaultLayout();
}
