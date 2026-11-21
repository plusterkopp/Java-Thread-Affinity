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

import de.pkmd.affinity.Affinity;
import de.pkmd.affinity.CpuLayout;
import de.pkmd.affinity.IAffinity;
import de.pkmd.affinity.IDefaultLayoutAffinity;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.BitSet;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HwLocJNAAffinityTest extends AbstractAffinityImplTest {
	@BeforeClass
	public static void checkJniLibraryPresent() {
		Assume.assumeTrue("linux".equalsIgnoreCase(System.getProperty("os.name")));
	}

	@Override
	public IAffinity getImpl() {
		return Affinity.getAffinityImpl();
	}

	@Test
	public void testDefaultLayout() {
		IAffinity impl = getImpl();
		Assume.assumeTrue("no HwLocJNAAffinity", impl instanceof HwLocJNAAffinity);
		HwLocJNAAffinity lAff = (HwLocJNAAffinity) impl;
		CpuLayout layout = lAff.getDefaultLayout();
		assertNotNull("no cpu layout", layout);
		System.out.println(layout);
	}

	@Test
	public void getAffinityReturnsValuePreviouslySet() {
		final IAffinity impl = getImpl();
		final BitSet cmask = impl.getAffinity();
		System.out.println("current mask: " + cmask);
		final int cores = CORES;
		for (int core = 0; core < cores; core++) {
			final BitSet mask = new BitSet();
			mask.set(core, true);
			impl.setAffinity(mask);
			final BitSet ret_mask = impl.getAffinity();
			assertEquals(mask, ret_mask);
		}
	}

	@Test
	public void getAffinityReturnsValuePreviouslySetRandom() {
		final IAffinity impl = getImpl();
		final BitSet cmask = impl.getAffinity();
		if (impl instanceof IDefaultLayoutAffinity) {
			IDefaultLayoutAffinity idla = (IDefaultLayoutAffinity) impl;
			CpuLayout layout = idla.getDefaultLayout();
			int nCPUs = layout.cpus();
			Random rnd = new Random();
			System.out.println("current mask: " + cmask);
			for (int i = 0; i < 1000; i++) {
				final BitSet mask = new BitSet();
				for (int index = 0; index < nCPUs; index++) {
					if (rnd.nextBoolean()) {
						mask.set(index);
					}
				}
				if (mask.isEmpty()) {   // avoid EINVAL
					continue;
				}
//				System.out.print("mask → " + mask);
				impl.setAffinity(mask);
				final BitSet ret_mask = impl.getAffinity();
//				System.out.println(" … " + ret_mask);
				assertEquals(mask, ret_mask);
			}
		}
	}


}
