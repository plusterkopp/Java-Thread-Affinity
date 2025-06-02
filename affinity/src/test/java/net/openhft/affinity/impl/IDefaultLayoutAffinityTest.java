/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.affinity.impl;

import net.openhft.affinity.Affinity;
import net.openhft.affinity.CpuLayout;
import net.openhft.affinity.IAffinity;
import net.openhft.affinity.IDefaultLayoutAffinity;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.BitSet;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IDefaultLayoutAffinityTest extends AbstractAffinityImplTest {

	@Override
	public IAffinity getImpl() {
		return Affinity.getAffinityImpl();
	}

	@Test
	public void testRawData() {
		IAffinity impl = getImpl();
		if ( ! ( impl instanceof IDefaultLayoutAffinity)) {
			System.out.println( "not a default layout affinity: " + impl);
			return;
		}
		IDefaultLayoutAffinity idla = (IDefaultLayoutAffinity) impl;
		System.out.println( "Raw Data: " + idla.getRawData());
	}

}
