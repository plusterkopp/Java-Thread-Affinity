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

package de.pkmd.affinity;

import de.pkmd.affinity.impl.GroupAffinityMask;

/**
 * Implementation interface
 *
 * @author plusterkopp
 * @since 2015-09-17
 */
public interface IGroupAffinity {

	public GroupAffinityMask setGroupAffinity(int groupId, long mask);
}
