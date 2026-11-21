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

import de.pkmd.affinity.ICpuInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by rhelbing on 07.10.2015.
 */
public class VanillaCpuInfo implements ICpuInfo {
	protected int socketId;
	/**
	 * local to socketId
	 */
	protected int coreId;
	/**
	 * local to socketId + coreId
	 */
	protected int threadId;

	VanillaCpuInfo() {
	}

	public VanillaCpuInfo(int socketId, int coreId, int threadId) {
		this.socketId = socketId;
		this.coreId = coreId;
		this.threadId = threadId;
	}

	@NotNull
	@Override
	public String toString() {
		return "CpuInfo{" +
				"socketId=" + socketId +
				", coreId=" + coreId +
				", threadId=" + threadId +
				'}';
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		VanillaCpuInfo cpuInfo = (VanillaCpuInfo) o;

		if (coreId != cpuInfo.coreId) {
			return false;
		}
		if (socketId != cpuInfo.socketId) {
			return false;
		}
		return threadId == cpuInfo.threadId;

	}

	@Override
	public int hashCode() {
		int result = socketId;
		result = 31 * result + coreId;
		result = 31 * result + threadId;
		return result;
	}

	@Override
	public int getSocketId() {
		return socketId;
	}

	@Override
	public void setSocketId(int socketId) {
		this.socketId = socketId;
	}

	@Override
	public int getCoreId() {
		return coreId;
	}

	@Override
	public void setCoreId(int coreId) {
		this.coreId = coreId;
	}

	@Override
	public int getThreadId() {
		return threadId;
	}

	@Override
	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}
}
