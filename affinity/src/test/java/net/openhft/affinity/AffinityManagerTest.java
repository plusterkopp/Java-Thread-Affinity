package net.openhft.affinity;

import static org.junit.Assert.*;

import net.openhft.affinity.impl.*;
import org.junit.*;

import java.util.*;

public class AffinityManagerTest {

	static private IAffinity   impl = null;
	static private WindowsCpuLayout    layout = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		impl = Affinity.getAffinityImpl();
		if ( impl instanceof IDefaultLayoutAffinity) {
			IDefaultLayoutAffinity dla = (IDefaultLayoutAffinity) impl;
			CpuLayout cpuLayout = dla.getDefaultLayout();
			if (cpuLayout instanceof WindowsCpuLayout) {
				layout = (WindowsCpuLayout) cpuLayout;
			}
		}
		Assume.assumeTrue(layout != null);
	}

	@Test
	public void testEntities() {
		// Nodes
		for (AffinityManager.NumaNode  node : layout.nodes) {
			System.out.println( "binding to node " + node);
			boolean success = AffinityManager.INSTANCE.bindToNode(node.getId());
			Assert.assertTrue( "can not bind node " + node.getId(), success);
		}
		// Sockets
		for (AffinityManager.Socket socket : layout.packages) {
			System.out.println( "binding to socket " + socket);
			boolean success = AffinityManager.INSTANCE.bindToSocket( socket.getId());
			Assert.assertTrue( "can not bind socket " + socket.getId(), success);
		}
		// Cores
		for (AffinityManager.Core core: layout.cores) {
			System.out.println( "binding to core " + core);
			boolean success = AffinityManager.INSTANCE.bindToCore(core.getId());
			Assert.assertTrue( "can not bind core " + core.getId(), success);
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
		for (AffinityManager.Group  group : layout.groups) {
			System.out.println("binding to group " + group);
			boolean success = am.bindToGroup( group);
			List<AffinityManager.LayoutEntity> boundTo = am.getBoundTo(Thread.currentThread());
			Assert.assertEquals("too many entities " + boundTo, 1, boundTo.size());
		}
		for (AffinityManager.NumaNode  node : layout.nodes) {
			System.out.println("binding to node " + node);
			boolean success = am.bindToNode(node);
			List<AffinityManager.LayoutEntity> boundTo = am.getBoundTo(Thread.currentThread());
			Assert.assertEquals("too many entities " + boundTo, 1, boundTo.size());
		}
		// Sockets
		for (AffinityManager.Socket socket : layout.packages) {
			System.out.println( "binding to socket " + socket);
			boolean success = am.bindToSocket(socket);
			List<AffinityManager.LayoutEntity> boundTo = am.getBoundTo(Thread.currentThread());
			Assert.assertEquals("too many entities " + boundTo, 1, boundTo.size());
		}
		// Cores
		for (AffinityManager.Core core: layout.cores) {
			System.out.println( "binding to core " + core);
			boolean success = am.bindToCore(core);
			List<AffinityManager.LayoutEntity> boundTo = am.getBoundTo(Thread.currentThread());
			Assert.assertEquals("too many entities " + boundTo, 1, boundTo.size());
		}


	}

}
