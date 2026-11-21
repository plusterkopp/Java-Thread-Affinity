/*
 * Copyright 2017-2026 Ralf Helbing
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

package de.pkmd.affinity.impl.LayoutEntities;

import de.pkmd.affinity.impl.GroupAffinityMask;

import java.util.BitSet;

/**
 * Created by rhelbing on 31.03.17.
 */
public class Group extends LayoutEntity {

	protected Group(GroupAffinityMask m) {
		super(m);
	}

	public Group(int index, long mask) {
		super(index, mask);
	}

	public String getLocation() {
		return "" + getId();
	}

	public int getMSB() {
		BitSet bitSet = getBitSetMask();
		if (bitSet != null) {
			return bitSet.length();
		}
		GroupAffinityMask gam = getGroupMask();
		if (gam != null) {
			long mask = gam.getMask();
			return Long.SIZE - Long.numberOfLeadingZeros(mask);
		}
		return 0;
	}

	public String getTypeName() {
		return "G";
	}

	@Override
	public ELayoutEntityType getEntityType() {
		return ELayoutEntityType.Group;
	}

}
