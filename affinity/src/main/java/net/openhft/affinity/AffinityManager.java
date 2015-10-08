package net.openhft.affinity;

import net.openhft.affinity.impl.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.function.*;


/**
 * Created by rhelbing on 07.10.2015.
 */
public class AffinityManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(AffinityManager.class);
	public static final AffinityManager INSTANCE = new AffinityManager();


	public static class GroupAffinityMask implements Comparable<GroupAffinityMask> {
	    final int groupId;
	    final long    mask;

	    GroupAffinityMask(int groupId, long mask) {
	        this.groupId = groupId;
	        this.mask = mask;
	    }

	    @Override
	    public int compareTo( GroupAffinityMask o) {
	        int res = Integer.compare( groupId, o.groupId);
	        if ( res != 0) {
	            return res;
	        }
	        return Long.compare( mask, o.mask);
	    }

		public long getMask() {
			return mask;
		}

		public int getGroupId() {
			return groupId;
		}
	}

	abstract static class LayoutEntity implements Comparable<LayoutEntity> {
		final Set<Thread> threads = new HashSet<>();
	    final GroupAffinityMask mask;
	    private int id;

	    protected LayoutEntity( int id, long mask) {
	        this( new GroupAffinityMask( id, mask));
	    }

	    protected LayoutEntity( GroupAffinityMask m) {
	        mask = m;
	    }

		/**
		 * using the mask, call the consumer with each index in the bitset that is set
		 * @param cpuInfos
		 * @param c
		 */
	    public void setEntityIds( List<ICpuInfo> cpuInfos, IntConsumer c) {
	        BitSet bs = Affinity.asBitSet(mask.mask);
	        bs.stream().forEach( c);
	    }

		@Override
	    public int compareTo(LayoutEntity o) {
	        return mask.compareTo( o.mask);
	    }

		public void setId(int id) {
			this.id = id;
		}

		public GroupAffinityMask getGroupMask() {
			return mask;
		}

		public int getId() {
			return id;
		}

		public void bind() {
			Thread t = Thread.currentThread();
			IAffinity impl = Affinity.getAffinityImpl();
			if ( impl instanceof IGroupAffinity) {
				IGroupAffinity ga = (IGroupAffinity) impl;
				ga.setGroupAffinity( mask.groupId, mask.mask);
			} else {
				impl.setAffinity( Affinity.asBitSet( mask.mask));
			}
			synchronized ( threads) {
				threads.add(t);
			}
			INSTANCE.unregister(this, t);
		}

		/**
		 * create list of threads in my set that are still alive, clear my set and add all living threads from the list, return the list,
		 * synchronized on the thread set
		 * @return List of alive threads
		 */
		public List<Thread> getThreads() {
			synchronized ( threads) {
				List<Thread> result = new ArrayList<>(threads.size());
				for (Thread t : threads) {
					if ( t.isAlive()) {
						result.add( t);
					}
				}
				threads.clear();
				threads.addAll(result);
				return result;
			}
		}

		/**
		 * @return id/group/maskAsBinary
		 */
		@Override
		public String toString() {
			return "" + getId() + "/" + mask.groupId + "/" + Long.toBinaryString( mask.getMask());
		}

		public void unregister(Thread t) {
			synchronized ( threads) {
				threads.remove(t);
			}
		}
	}

	public static class Core extends LayoutEntity {
		private Socket socket;

		protected Core(GroupAffinityMask m) {
	        super(m);
	    }

	    public Core(int group, long mask) {
	        super( group, mask);
	    }

		public void setSocket(Socket socket) {
			this.socket = socket;
		}

		public Socket getSocket() {
			return socket;
		}

		public String toString() {
			return "C " + super.toString() + " on " + getSocket();
		}
	}

	public static class Socket extends LayoutEntity {
		private NumaNode node;

		protected Socket(GroupAffinityMask m) {
	        super(m);
	    }

	    public Socket(int index, long mask) {
	        super( index, mask);
	    }


		public void setNode(NumaNode node) {
			this.node = node;
		}

		public NumaNode getNode() {
			return node;
		}

		public String toString() {
			return "S " + super.toString() + " on " + getNode();
		}

	}

	public static class NumaNode extends LayoutEntity {

	    protected NumaNode(GroupAffinityMask m) {
	        super(m);
	    }

	    public NumaNode(int index, long mask) {
	        super( index, mask);
	    }

		public String toString() {
			return "N " + super.toString();
		}

	}

	public static class Group extends LayoutEntity {

	    protected Group(GroupAffinityMask m) {
	        super(m);
	    }

	    public Group(int index, long mask) {
	        super( index, mask);
	    }

	    public void setEntityIds(List<ICpuInfo> cpuInfos, IntConsumer c) {
	        super.setEntityIds( cpuInfos, c);
	        for ( int i = 0;  i < cpuInfos.size();  i++) {
	            ICpuInfo info = cpuInfos.get( i);
		        if ( info instanceof IGroupCpuInfo) {
			        IGroupCpuInfo gi = (IGroupCpuInfo) info;
//			        if ( gi.getGroupId() == id) {
//
//			        }
		        }
	        }
	    }
	}

	final private CpuLayout   cpuLayout;

	private long mask;

	private AffinityManager() {
		cpuLayout = getLayout();
	}

	private CpuLayout getLayout() {
		CpuLayout layout = new NoCpuLayout(Runtime.getRuntime().availableProcessors());
		IAffinity impl = Affinity.getAffinityImpl();
		if (impl instanceof IDefaultLayoutAffinity) {
			IDefaultLayoutAffinity dla = (IDefaultLayoutAffinity) impl;
			layout = dla.getDefaultLayout();
		} else {
			if (new File("/proc/cpuinfo").exists()) {
				try {
					layout = VanillaCpuLayout.fromCpuInfo();
				} catch (Exception e) {
					LOGGER.warn("Unable to load /proc/cpuinfo", e);
				}
			}
		}
		return layout;
	}

	/**
	 * try to bind the current thread to a socket
	 * @param socketId
	 * @return true if the current cpu after the call is one the desired socket, or false
	 */
	public boolean bindToSocket( int socketId) {
		// implement for Windows case first, generalize when interfaces become available for VanillaLayout, too
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			Socket socket = w.packages.get(socketId);
			socket.bind();
			int	cpuId = Affinity.getCpu();
			if ( cpuLayout.socketId(cpuId) == socket.getId()) {
				return true;
			}
		}
		return false;
	}

	public boolean bindToCore(int coreId) {
		// implement for Windows case first, generalize when interfaces become available for VanillaLayout, too
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			Core core = w.cores.get(coreId);
			core.bind();
			int	cpuId = Affinity.getCpu();
			if ( cpuLayout.coreId(cpuId) == core.getId()) {
				return true;
			}
		}
		return false;
	}

	private void unregister(LayoutEntity current, Thread t) {
		if ( cpuLayout instanceof WindowsCpuLayout) {
			WindowsCpuLayout w = (WindowsCpuLayout) cpuLayout;
			for ( LayoutEntity g : w.groups) {
				if ( g != current) {
					g.unregister(t);
				}
			}
			for ( LayoutEntity n : w.nodes) {
				if ( n != current) {
					n.unregister(t);
				}
			}
			for ( LayoutEntity e : w.nodes) {
				if ( e != current) {
					e.unregister(t);
				}
			}
			for ( LayoutEntity e : w.nodes) {
				if ( e != current) {
					e.unregister( t);
				}
			}
		}
	}


}
