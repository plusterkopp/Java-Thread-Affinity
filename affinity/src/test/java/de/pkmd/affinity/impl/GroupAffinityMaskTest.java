/*
 * Copyright 2026 Ralf Helbing
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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GroupAffinityMaskTest {

	@Test
	public void testOrderFollowsCpuNumbers() {
		// one 2-thread core per mask, as in a full group of 64 logical CPUs
		TreeSet<GroupAffinityMask> cores = new TreeSet<>();
		for (int cpu = 62; cpu >= 0; cpu -= 2) {
			cores.add(new GroupAffinityMask(0, 3L << cpu));
		}
		List<GroupAffinityMask> sorted = new ArrayList<>(cores);
		assertEquals(32, sorted.size());
		assertEquals("first core holds cpus 0 and 1", 3L, sorted.get(0).getMask());
		assertEquals("last core holds cpus 62 and 63", 3L << 62, sorted.get(31).getMask());
	}

	@Test
	public void testGroupBeforeMask() {
		GroupAffinityMask highInGroup0 = new GroupAffinityMask(0, 1L << 63);
		GroupAffinityMask lowInGroup1 = new GroupAffinityMask(1, 1L);
		assertTrue(highInGroup0.compareTo(lowInGroup1) < 0);
		assertTrue(lowInGroup1.compareTo(highInGroup0) > 0);
		assertEquals(0, highInGroup0.compareTo(new GroupAffinityMask(0, 1L << 63)));
	}
}
