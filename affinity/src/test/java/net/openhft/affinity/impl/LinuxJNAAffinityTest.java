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
import net.openhft.affinity.AffinitySupport;
import net.openhft.affinity.CpuLayout;
import net.openhft.affinity.IAffinity;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.BitSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author peter.lawrey
 */
public class LinuxJNAAffinityTest extends AbstractAffinityImplTest {
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
        IAffinity   impl = getImpl();
        Assume.assumeTrue( "no LinuxJANAffinity", impl instanceof LinuxJNAAffinity);
        LinuxJNAAffinity    lAff = (LinuxJNAAffinity) impl;
        CpuLayout   layout = lAff.getDefaultLayout();
        System.out.print( layout);
    }

    @Test
    public void getAffinityReturnsValuePreviouslySet() {
        final IAffinity impl = getImpl();
        final int cores = CORES;
        for (int core = 0; core < cores; core++) {
            final BitSet mask = new BitSet();
            mask.set(core, true);
            impl.setAffinity(mask);
            final BitSet ret_mask = impl.getAffinity();
            assertEquals(mask, ret_mask);
        }
    }


}
