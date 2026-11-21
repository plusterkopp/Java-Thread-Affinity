/*
 * Copyright 2023-2026 Ralf Helbing
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by rhelbing on 07.10.2015.
 */
public class ApicCpuInfo extends VanillaCpuInfo {
	private int apicId = -1;    // not set in some use cases

	ApicCpuInfo() {
	}

	ApicCpuInfo(int socketId, int coreId, int threadId, int apicId) {
		super(socketId, coreId, threadId);
		this.apicId = apicId;
	}

	@NotNull
	@Override
	public String toString() {
		return "CpuInfo{" +
				"apicId=" + apicId +
				", socketId=" + socketId +
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

		ApicCpuInfo cpuInfo = (ApicCpuInfo) o;

		if (coreId != cpuInfo.coreId) {
			return false;
		}
		if (socketId != cpuInfo.socketId) {
			return false;
		}
		if (threadId != cpuInfo.threadId) {
			return false;
		}
		return apicId == cpuInfo.apicId;
	}

	@Override
	public int hashCode() {
		int result = socketId;
		result = 31 * result + coreId;
		result = 31 * result + threadId;
		result = 31 * result + apicId;
		return result;
	}

	void setApicId(int apicId) {
		this.apicId = apicId;
	}

	public int getApicId() {
		return apicId;
	}
}
