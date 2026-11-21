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

/**
 * Created by rhelbing on 31.03.17.
 */
public class NumaNode extends LayoutEntity {

	protected NumaNode(GroupAffinityMask m) {
		super(m);
	}

	public NumaNode(int index, long mask) {
		super(index, mask);
	}

	public NumaNode(int id) {
		super(id);
	}

	public String toString() {
		return "N " + super.toString();
	}

	public String getLocation() {
		if (groupAffinityMask != null) {
			return groupAffinityMask.getGroupId() + "/" + getId();
		}
		return String.valueOf(getId());
	}

	public String getTypeName() {
		return "N";
	}

	@Override
	public ELayoutEntityType getEntityType() {
		return ELayoutEntityType.NumaNode;
	}

}
