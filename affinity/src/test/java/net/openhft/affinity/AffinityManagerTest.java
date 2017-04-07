package net.openhft.affinity;

import net.openhft.affinity.impl.*;
import net.openhft.affinity.impl.LayoutEntities.*;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class AffinityManagerTest {

	static private IAffinity   impl = null;
	static private VanillaCpuLayout layout = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		impl = Affinity.getAffinityImpl();
		if ( impl instanceof IDefaultLayoutAffinity) {
			IDefaultLayoutAffinity dla = (IDefaultLayoutAffinity) impl;
			CpuLayout cpuLayout = dla.getDefaultLayout();
			if (cpuLayout instanceof VanillaCpuLayout) {
				layout = (VanillaCpuLayout) cpuLayout;
			}
		}
		Assume.assumeTrue( "can not run with " + impl.getClass(), layout != null);
	}

	@Test
	public void testEntities() {
		// Nodes
		if (layout instanceof NumaCpuLayout) {
			NumaCpuLayout nLayout = (NumaCpuLayout) layout;
			for (NumaNode node : nLayout.getNodes()) {
				System.out.println( "binding to node " + node);
				boolean success = AffinityManager.INSTANCE.bindToNode(node.getId());
				Assert.assertTrue( "can not bind node " + node.getId(), success);
			}
		}
		// Sockets, more than one loop
		for ( int i = 0;  i < 100;  i++) {
			for (Socket socket : layout.packages) {
				if ( i == 0) {
					System.out.println("binding to socket " + socket);
				}
				boolean success = AffinityManager.INSTANCE.bindToSocket(socket.getId());
				Assert.assertTrue("can not bind socket " + socket.getId(), success);
			}
		}
		// Cores, more than one loop
		for ( int i = 0;  i < 100;  i++) {
			for (Core core : layout.cores) {
				if ( i == 0) {
					System.out.println("binding to core " + core);
				}
				boolean success = AffinityManager.INSTANCE.bindToCore(core.getId());
				Assert.assertTrue("can not bind core " + core.getId(), success);
			}
		}
		// must not bind for other ids
		final int wrongSocketId = layout.sockets() + 1;
		System.out.println( "not binding to socket " + wrongSocketId);
		boolean success = AffinityManager.INSTANCE.bindToSocket(wrongSocketId);
		Assert.assertFalse( "bount to non-existing socket " + wrongSocketId, success);

	}

	@Test
	public void testUnbind() {
		// Nodes
		final AffinityManager am = AffinityManager.INSTANCE;
		if ( layout instanceof GroupedCpuLayout) {
			GroupedCpuLayout gLayout = (GroupedCpuLayout) layout;
			for (Group group : gLayout.getGroups()) {
				System.out.println("binding to group " + group);
				boolean success = am.bindToGroup(group);
				List<LayoutEntity> boundTo = am.getBoundTo(Thread.currentThread());
				Assert.assertEquals("too many entities " + boundTo, 1, boundTo.size());
			}
		}
		if ( layout instanceof NumaCpuLayout) {
			NumaCpuLayout nLayout = (NumaCpuLayout) layout;
			for (NumaNode node : nLayout.getNodes()) {
				System.out.println("binding to node " + node);
				boolean success = am.bindToNode(node);
				List<LayoutEntity> boundTo = am.getBoundTo(Thread.currentThread());
				Assert.assertEquals("too many entities " + boundTo, 1, boundTo.size());
			}
		}
		// Sockets
		for (Socket socket : layout.packages) {
			System.out.println( "binding to socket " + socket);
			boolean success = am.bindToSocket(socket);
			List<LayoutEntity> boundTo = am.getBoundTo(Thread.currentThread());
			Assert.assertEquals("too many entities " + boundTo, 1, boundTo.size());
		}
		// Cores
		for (Core core: layout.cores) {
			System.out.println( "binding to core " + core);
			boolean success = am.bindToCore(core);
			List<LayoutEntity> boundTo = am.getBoundTo(Thread.currentThread());
			Assert.assertEquals("too many entities " + boundTo, 1, boundTo.size());
		}
	}

}
