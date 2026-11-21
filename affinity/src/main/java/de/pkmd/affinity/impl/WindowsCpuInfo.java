/*
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

import de.pkmd.affinity.IGroupCpuInfo;
import de.pkmd.affinity.INumaCpuInfo;
import de.pkmd.affinity.impl.LayoutEntities.NumaNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * add numaId, groupId
 */
class WindowsCpuInfo extends ApicCpuInfo implements IGroupCpuInfo, INumaCpuInfo {
	private NumaNode node = null;
	private int groupId = 0;
	private long mask = 0;

	WindowsCpuInfo() {
	}

	@NotNull
	@Override
	public String toString() {
		return "CpuInfo{" +
				" s" + getSocketId() +
				" c" + getCoreId() +
				" t" + getThreadId() +
				" a" + getApicId() +
				" n" + getNodeId() +
				" g" + groupId +
				" m" + WindowsJNAAffinity.asBitSet(mask) +
				'}';
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		WindowsCpuInfo cpuInfo = (WindowsCpuInfo) o;

		if (groupId != cpuInfo.groupId) return false;
		if (getNodeId() != cpuInfo.getNodeId()) return false;
		if (getCoreId() != cpuInfo.getCoreId()) return false;
		if (getSocketId() != cpuInfo.getSocketId()) return false;
		return getThreadId() == cpuInfo.getThreadId();

	}

	@Override
	public int hashCode() {
		int result = groupId;
		result = 31 * result + getNodeId();
		result = 31 * result + getSocketId();
		result = 31 * result + getCoreId();
		result = 71 * result + getThreadId();
		return result;
	}

	public void setMask(long mask) {
		this.mask = mask;
	}


	public int getNodeId() {
		return node.getId();
	}

	public void setNode(NumaNode n) {
		node = n;
	}

	public int getGroupId() {
		return groupId;
	}

	@Override
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public long getMask() {
		return mask;
	}
}
