This fork aims to provide support for another use case: Binding to groups of CPUs instead of single cores. This is useful for exploiting cache locality for groups of threads with similar data access patterns.

Another feature that was missing in 2013 when I started working in this fork was layout introspection for the Windows platform.
For Linux, this is done by reading and parsing `/proc/cpuinfo`, which does not exist in Windows outside of Cygwin. 

Instead, Windows provides `GetLogicalProcessorInformationEx`. Here, we get a number of `SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX` structs, that describe the CPU layout in terms of caches, numa nodes, core and packages (sockets). Also there are groups, which are used by Windows to keep processor masks within 64 bits. Systems with more than 64 logical CPUs will therefore have more than one group. Since the scheduler normally uses the CPUs on the one group only, a process on a machine with 72 lCPUs (like 2x E5-4669 v3 where each CPU socket counts as a group of 36 lCPUs), `Runtime.availableProcessors` will return 36. Without affinities, no thread will run on the other half of CPUs. Of course, this gets worse on 4 socket configurations.

We gather all `SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX` using JNA and a small DLL built from affinityInfo.cpp. We then construct a number of `LayoutEntities`, each having its own cpu mask. At the time of fork, there was only support for 64 lCPUs on the Linux side using `BitSet`s. On the Windows side, I had to create a `GroupAffinityMask` comprising a `groupId` and a `long` `mask`. 

As per Peters request at the time, I left all the original interfaces untouched and provided my own for the new features. There is a new kind of `CpuLayout`, `WindowsCpuLayout` that is constructed upon `SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX` structures and extends `VanillaCpuLayout` with lists of `Group`s, `Cache`s and `NumaNode`s. To access each of them, there is an interface: `NumaCpuLayout`, `GroupedCpuLayout` and `CacheCpuLayout`. Likewise, `WindowsCpuInfo` extends V`anillaCpuInfo` with `numaId`, `groupId` and `mask`. 

When I started this work, the only relevant machines had Intel CPUs, where relations are rather simple. One socket has a number of cores, possibly with SMT, each core has its own L1+2 cache. The socket then has a hopefully large L3 cache and its own path to memory, aka numa node. In order to do what I wanted (exploiting caches), one could either focus on L2 caches, which was equivalent to a core, or L3 cache, which was equivalent to a socket. The Linux implementation provides access to both and is therefore OK as long as one uses Intel hardware.

Enter AMD Zen.

Now with an Epyc 7301, two cores are organized into core complexes which each have their own L3 cache. Two core complexes form a numa node. Each socket has four of them. There is no longer a simple 1:1 mapping of socket to numa node and L3 cache. Using only naive socket binding on a hardware like this yields huge performance losses.

Caches do not execute code, and are therefore not referenced in `CpuInfo`s. Instead, they form their own hierarchy. We can only relate `LayoutEntities` (including caches) to each other by their mask.

To bind threads to `LayoutEntities`, they provide a `bind()` method to bind the current thread, keeping track of the threads that are currently bound to it. Alternatively, the `AffinityManager` provides methods to bind to each kind `LayoutEntity`, returning `true` if successful. This functionality is vaguely similar to `AffinityStrategy`, but better matched to what I wanted to do.

Not contained in this package is how to bind worker threads in a pool. Since this can be done only by each thread for itself, I subclassed `Thread` as `AffitityThread`. For constructors with a `Runnable` argument, I wrap that `Runnable` into one that first binds the new `Thread`. 

	public AffinityThread( Runnable target, String name, LayoutEntity bindTo) {
		super( createBindingRunnable( bindTo, target), name);
	}

	private static Runnable createBindingRunnable( LayoutEntity bindTo, Runnable target) {
		if ( bindTo == null) {
			return target;
		}
		Runnable	bindingRunnable = new Runnable() {
			@Override
			public void run() {
				bindTo.bind();
				target.run();
			}
		};
		return bindingRunnable;
	}

Usually, I do not unbind or rebind threads after they have been initially bound. This is however possible and covered in unit tests.

I would like to see this fork somehow remerged back into the original package if my use case becomes more relevant to the people maintaining it. If I should add more comments to my already totally self-explanatory core to help this effort, please contact me.
