package net.openhft.affinity;

import net.openhft.affinity.impl.LayoutEntities.*;
import net.openhft.affinity.impl.VanillaCpuLayout;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class AffinityManagerTest {

	static private IAffinity   impl = null;
	static private VanillaCpuLayout layout = null;

	@BeforeClass
	public static void setUpBeforeClass() {
		impl = Affinity.getAffinityImpl();
		if ( impl instanceof IDefaultLayoutAffinity) {
			IDefaultLayoutAffinity dla = (IDefaultLayoutAffinity) impl;
			CpuLayout cpuLayout = dla.getDefaultLayout();
			if (cpuLayout instanceof VanillaCpuLayout) {
				layout = (VanillaCpuLayout) cpuLayout;
			}
		}
		Assume.assumeTrue( "can not run with " + impl.getClass(), layout != null);
		AffinityManager.getInstance().dumpLayout();
	}

	@Test
	public void testEntities() {
		// Nodes
		if (layout instanceof NumaCpuLayout) {
			NumaCpuLayout nLayout = (NumaCpuLayout) layout;
			for (NumaNode node : nLayout.getNodes()) {
				System.out.println("binding to node " + node);
				boolean success = AffinityManager.getInstance().bindToNode(node);
				if (!success) {
					int currentCpuId = Affinity.getCpu();
					System.err.println("cpu=" + currentCpuId + " after mask " + node.getBitMask() + " actual: " + Affinity.getAffinity());
				}
				Assert.assertTrue("did not bind node " + node, success);
			}
		}
		// Sockets, more than one loop
		for ( int i = 0;  i < 100;  i++) {
			for (Socket socket : layout.packages) {
				if ( i == 0) {
					System.out.println("binding to socket " + socket);
				}
				boolean success = AffinityManager.getInstance().bindToSocket(socket);
				Assert.assertTrue("did not bind socket " + socket + " in round " + i, success);
			}
		}
		// Cores, more than one loop
		for ( int i = 0;  i < 100;  i++) {
			for (Core core : layout.cores) {
				if ( i == 0) {
					System.out.println("binding to core " + core);
				}
				boolean success = AffinityManager.getInstance().bindToCore(core.getId());
				Assert.assertTrue("did not bind core " + core.getId(), success);
			}
		}
		if ( layout instanceof CacheCpuLayout) {
			CacheCpuLayout cacheLayout = (CacheCpuLayout) layout;
			// Caches, more than one loop
			for ( int i = 0;  i < 100;  i++) {
				for (Cache cache : cacheLayout.getCaches()) {
					if ( i == 0) {
						System.out.println("binding to cache " + cache);
					}
					boolean success = AffinityManager.getInstance().bindToCache(cache);
					Assert.assertTrue("did not bind cache " + cache + " (" + i + ")", success);
				}
			}
		}
		// must not bind for other ids
		final int wrongSocketId = layout.sockets() + 1;
		System.out.println( "not binding to socket " + wrongSocketId);
		boolean success = AffinityManager.getInstance().bindToSocket(wrongSocketId);
		Assert.assertFalse( "bound to non-existing socket " + wrongSocketId, success);

	}

	@Test
	public void testBoundTo() {
		// Nodes
		final AffinityManager am = AffinityManager.getInstance();
		if ( layout instanceof GroupedCpuLayout) {
			GroupedCpuLayout gLayout = (GroupedCpuLayout) layout;
			for (Group group : gLayout.getGroups()) {
				System.out.print("binding to group " + group);
				boolean success = am.bindToGroup(group);
				List<LayoutEntity> boundTo = am.getBoundTo(Thread.currentThread());
				System.out.println(" … bound to " + boundTo + " (" + success + ")");
				Assert.assertEquals("too many entities " + boundTo, 1, boundTo.size());
				Assert.assertEquals( "bound to another entity", group, boundTo.get( 0));
			}
		}
		if ( layout instanceof NumaCpuLayout) {
			NumaCpuLayout nLayout = (NumaCpuLayout) layout;
			for (NumaNode node : nLayout.getNodes()) {
				System.out.print("binding to node " + node);
				boolean success = am.bindToNode(node);
				List<LayoutEntity> boundTo = am.getBoundTo(Thread.currentThread());
				System.out.println(" … bound to " + boundTo + " (" + success + ")");
				Assert.assertEquals("too many entities " + boundTo, 1, boundTo.size());
				Assert.assertEquals("bound to another entity", node, boundTo.get(0));
			}
		}
		// Sockets
		for (Socket socket : layout.packages) {
			System.out.print( "binding to socket " + socket);
			boolean success = am.bindToSocket(socket);
			List<LayoutEntity> boundTo = am.getBoundTo(Thread.currentThread());
			System.out.println(" … bound to " + boundTo + " (" + success + ")");
			Assert.assertEquals("too many entities " + boundTo, 1, boundTo.size());
			Assert.assertEquals( "bound to another entity", socket, boundTo.get( 0));
		}
		// Cores
		for (Core core: layout.cores) {
			System.out.print( "binding to core " + core);
			boolean success = am.bindToCore(core);
			List<LayoutEntity> boundTo = am.getBoundTo(Thread.currentThread());
			System.out.println(" … bound to " + boundTo + " (" + success + ")");
			Assert.assertEquals("too many entities " + boundTo, 1, boundTo.size());
			Assert.assertEquals( "bound to another entity", core, boundTo.get( 0));
		}
		// Caches
		if ( layout instanceof CacheCpuLayout) {
			CacheCpuLayout cacheLayout = (CacheCpuLayout) layout;
			for (Cache cache : cacheLayout.getCaches()) {
				System.out.print("binding to cache " + cache);
				boolean success = am.bindToCache(cache);
				List<LayoutEntity> boundTo = am.getBoundTo(Thread.currentThread());
				System.out.println(" … bound to " + boundTo + " (" + success + ")");
				Assert.assertEquals("too many entities " + boundTo, 1, boundTo.size());
				Assert.assertEquals( "bound to another entity", cache, boundTo.get( 0));
			}
		}
	}

}
