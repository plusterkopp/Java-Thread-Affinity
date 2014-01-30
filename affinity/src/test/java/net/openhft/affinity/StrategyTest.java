package net.openhft.affinity;

import static org.junit.Assert.*;
import net.openhft.affinity.impl.*;

import org.junit.*;

public class StrategyTest {

	@Test
	public void testSameCore() {
		String[]	descs = { "1/4/2", "4/8/2", "1/4/1", "8/1/1"};
		for ( String desc : descs) {
			CpuLayout	layout = WindowsCpuLayout.fromCpuDesc( desc);
			testSameCore( layout);
			testSameSocket( layout);
		}
	}

	@Test
	public void testSameSocket() {
		String[]	descs = { "1/4/2", "4/8/2", "1/4/1", "8/1/1"};
		for ( String desc : descs) {
			CpuLayout	layout = WindowsCpuLayout.fromCpuDesc( desc);
			testSameSocket( layout);
		}
	}

	private void testSameCore( CpuLayout layout) {
		System.out.println( "Test SameCore: " + layout);
		AffinityLock.cpuLayout( layout);
		for ( int i = 0;  i < layout.cpus();  i++) {
			for ( int j = 0;  j < layout.cpus();  j++) {
				if ( i != j) {
					AffinityStrategy	strategy = AffinityStrategies.SAME_CORE;
					final boolean sameSocketId = layout.socketId( i) == layout.socketId( j);
					final boolean sameCoreId = layout.coreId( i) == layout.coreId( j) && sameSocketId;
					final boolean matches = strategy.matches( i, j);
					assertTrue( "wrong match/mismatch: " + i + "/" + j, sameCoreId == matches);
				}
			}
		}
	}

	private void testSameSocket( CpuLayout layout) {
		System.out.println( "Test SameSocket: " + layout);
		AffinityLock.cpuLayout( layout);
		for ( int i = 0;  i < layout.cpus();  i++) {
			for ( int j = 0;  j < layout.cpus();  j++) {
				if ( i != j) {
					AffinityStrategy	strategy = AffinityStrategies.SAME_SOCKET;
					final boolean sameSocketId = ( layout.socketId( i) == layout.socketId( j)
							&& layout.coreId( i) != layout.coreId( j));
					final boolean matches = strategy.matches( i, j);
					assertTrue( "wrong match/mismatch: " + i + "/" + j, sameSocketId == matches);
				}
			}
		}
	}


}
