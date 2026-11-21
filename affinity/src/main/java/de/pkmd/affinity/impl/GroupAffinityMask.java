/*
 * Copyright 2019-2026 Ralf Helbing
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

/**
 * used for Windows API
 * Created by rhelbing on 31.03.17.
 */
public class GroupAffinityMask implements Comparable<GroupAffinityMask> {

	final int groupId;
	final long mask;

	public GroupAffinityMask(int groupId, long mask) {
		this.groupId = groupId;
		this.mask = mask;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		GroupAffinityMask that = (GroupAffinityMask) o;

		if (groupId != that.groupId) {
			return false;
		}
		return mask == that.mask;
	}

	@Override
	public int hashCode() {
		int result = groupId;
		result = 31 * result + (int) (mask ^ (mask >>> 32));
		return result;
	}

	@Override
	public int compareTo(GroupAffinityMask o) {
		int res = Integer.compare(groupId, o.groupId);
		if (res != 0) {
			return res;
		}
		return Long.compare(mask, o.mask);
	}

	public long getMask() {
		return mask;
	}

	public int getGroupId() {
		return groupId;
	}

	public String toString() {
		return groupId + "/0x" + Long.toHexString(mask);
	}
}
