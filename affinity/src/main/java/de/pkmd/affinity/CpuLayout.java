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

/**
 * @author peter.lawrey
 */
public interface CpuLayout {
	/**
	 * @return the number of cpus.
	 */
	int cpus();

	int sockets();

	int coresPerSocket();

	int threadsPerCore();

	/**
	 * @param cpuId the logical processor number
	 * @return which socket id this cpu is on.
	 */
	int socketId(int cpuId);

	/**
	 * @param cpuId the logical processor number
	 * @return which core on a socket this cpu is on.
	 */
	int coreId(int cpuId);

	/**
	 * @param cpuId the logical processor number
	 * @return which thread on a core this cpu is on.
	 */
	int threadId(int cpuId);

	/**
	 * not all cores are created equal, therefore, we must allow for different thread count per core
	 *
	 * @param physicalCore
	 * @return numbers of threads that core can accomodate
	 */
	int threadsPerCore(int physicalCore);


//    ICpuInfo getCPUInfo( int cpuId);
}
