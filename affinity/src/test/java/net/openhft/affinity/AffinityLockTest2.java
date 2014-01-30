/*
 * Copyright 2013 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.affinity;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import net.openhft.affinity.impl.*;

import org.junit.*;

/**
 * @author peter.lawrey
 */
public class AffinityLockTest2 {
    @Test
    public void testi7Spec() throws IOException {
    	CpuLayout	i7info = VanillaCpuLayout.fromCpuInfo("i7.cpuinfo");
    	CpuLayout	i7desc = WindowsCpuLayout.fromCpuDesc( "1/4/2");
    	assertEquals( "CPU count mismatch", i7info.cpus(), i7desc.cpus());
    	assertEquals( "Socket count mismatch", i7info.sockets(), i7desc.sockets());
    	assertEquals( "Core count mismatch", i7info.coresPerSocket(), i7desc.coresPerSocket());
    	assertEquals( "Thread count mismatch", i7info.threadsPerCore(), i7desc.threadsPerCore());
    	for ( int cpuID = 0;  cpuID < i7info.cpus();  cpuID++) {
    		assertEquals( "core ID mismatch at cpu " + cpuID, i7info.coreId( cpuID), i7desc.coreId( cpuID));
    		assertEquals( "thread ID mismatch at cpu " + cpuID, i7info.threadId( cpuID), i7desc.threadId( cpuID));
    		assertEquals( "socket ID mismatch at cpu " + cpuID, i7info.socketId( cpuID), i7desc.socketId( cpuID));
    	}
    }

    @Test
    public void testProximity() throws InterruptedException {
    	final CpuLayout defaultLayout = AffinitySupport.getDefaultLayout();
		AffinityLock.cpuLayout( defaultLayout);
    	System.out.println( "\nAcquiring on Same Socket");
    	ArrayList<Thread>	threads = new ArrayList<Thread>();
    	final AtomicReference<AffinityLock> lockHolder = new AtomicReference<AffinityLock>( null);
    	final CountDownLatch	firstLatch = new CountDownLatch( 1);
    	final Thread	first = new Thread( new Runnable() {
			@Override
			public void run() {
				System.out.println( AffinityLock.dumpLocks());
				final AffinityLock al = AffinityLock.acquireLock();
				lockHolder.set( al);
				firstLatch.countDown();
				try {
					Thread.sleep( 10000000);
				} catch ( InterruptedException ie) {
					al.release();
				}
			}
		}, "proximity 0");
    	threads.add( first);
    	first.start();
    	firstLatch.await();
		System.out.println( "first: " + lockHolder.get().dumpLock());
    	int	counter = 1;
    	int maxThreads = AffinityLock.cpuLayout().coresPerSocket() * AffinityLock.cpuLayout().threadsPerCore();
    	if ( AffinityLock.cpuLayout().sockets() < 2) {	// reserve one cpu
    		maxThreads--;
    	}
    	final CountDownLatch	loopLatch = new CountDownLatch( maxThreads - 1);
    	final Map<String, Proximity> proxMap = new ConcurrentHashMap<String, Proximity>();
		while ( threads.size() < maxThreads) {
    		final int	fc = counter++;
        	Thread	t = new Thread( new Runnable() {
    			@Override
    			public void run() {
    				AffinityLock al = lockHolder.get().acquireLock( AffinityStrategies.SAME_SOCKET, AffinityStrategies.SAME_CORE, AffinityStrategies.ANY);
    				final Thread currentThread = Thread.currentThread();
					Proximity p = Proximity.getProximity( first, currentThread);
					proxMap.put( Thread.currentThread().getName(), p);
					if ( ! ( p == Proximity.SAME_SOCKET || p == Proximity.SAME_CORE)) {
						System.err.println( "wrong socket (" + p + "): " + al.dumpLock());
					} else {
						System.out.println( "same socket: " + al.dumpLock());
					}
					loopLatch.countDown();
    				try {
    					Thread.sleep( 10000000);
    				} catch ( InterruptedException ie) {
    					al.release();
    				}
    			}
    		}, "proximity " + fc);
        	t.start();
        	threads.add( t);
    	}
		loopLatch.await();
		for ( Thread thread : threads) {
			thread.interrupt();
		}
		for ( Thread thread : threads) {
			thread.join();
			if ( thread != first) {
				final String name = thread.getName();
				Proximity	p = proxMap.get( name);
				assertTrue( "Proximity failure for: " + name, p == Proximity.SAME_SOCKET || p == Proximity.SAME_CORE);
			}
		}
//		System.out.println( AffinityLock.dumpLocks());
    }

    @Test
    public void testDifferentSocket() {
    	final CpuLayout defaultLayout = AffinitySupport.getDefaultLayout();
		AffinityLock.cpuLayout( defaultLayout);
    	Assume.assumeTrue( "Need more than one socket", defaultLayout.sockets() > 1);
    	System.out.println( "\nAcquiring on Different Sockets");
    	AffinityLock[]	locks = new AffinityLock[ defaultLayout.sockets()];
    	for ( int socket = 0;  socket < defaultLayout.sockets();  socket++) {
    		// build id-list
    		int[]	cpuids = new int[ socket];
    		for ( int i = 0;  i < socket;  i++) {
    			cpuids[ i] = locks[ i].cpuId();
    		}
    		locks[ socket] = AffinityLock.acquireLock( false, AffinityStrategies.DIFFERENT_SOCKET, cpuids);
    		System.out.println( locks[ socket].dumpLock());
    	}
    	System.out.println( "Checking Different Sockets");
    	int	counter = 0;
    	for ( AffinityLock al1 : locks) {
			for ( AffinityLock al2 : locks) {
				if ( al1 != al2) {
					counter ++;
					assertFalse( counter + ": on same socket: " + al1.dumpLock() + " and " + al2.dumpLock(),
							AffinityStrategies.SAME_SOCKET.matches( al1.cpuId(), al2.cpuId()));
				}
			}
		}
    	for ( AffinityLock al1 : locks) {
    		al1.release();
    	}
    }
}
