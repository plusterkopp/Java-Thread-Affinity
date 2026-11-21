# Java Thread Affinity (pkmd)

Binds Java threads to groups of CPUs (sockets, NUMA nodes, L3/L2 caches, cores) rather than to single CPUs.
It detects the CPU layout on Windows (`GetLogicalProcessorInformationEx`, with processor groups for more than 64 logical CPUs)
and on Linux (via hwloc's `lstopo-no-graphics`).

This started in 2013 as a fork of [OpenHFT/Java-Thread-Affinity](https://github.com/OpenHFT/Java-Thread-Affinity)
and is now developed independently. Upstream and this project serve different use cases, and the code has diverged too far to merge.

```xml
<dependency>
    <groupId>de.pkmd</groupId>
    <artifactId>affinity</artifactId>
    <version>3.5.0-SNAPSHOT</version>
</dependency>
```

Releases are tagged `vX.Y.Z`. Releases before 3.5.0 used four-part version numbers (3.4.0.1 … 3.4.0.9).

## Usage

Everything lives in package `de.pkmd.affinity`. `AffinityManager` detects the layout once and hands out its entities
(sockets, NUMA nodes, processor groups, caches, cores). Each entity binds the calling thread to all CPUs it contains.

```java
AffinityManager am = AffinityManager.getInstance();
am.dumpLayout();                        // prints the detected layout

// bind the current thread to the first socket
Socket socket = am.getSocket(0);
socket.bind();

// or to the first L3 cache, if the layout knows caches
CpuLayout layout = am.getLayout();
if (layout instanceof CacheCpuLayout) {
    for (Cache cache : ((CacheCpuLayout) layout).getCaches()) {
        if (cache.getLevel() == 3) {
            cache.bind();
            break;
        }
    }
}
```

`am.getBoundTo(thread)` tells which entities a thread is bound to, and `am.getLocation(entity)` describes where an entity sits in the layout.

## Platforms

- **Windows:** layout from `GetLogicalProcessorInformationEx` via JNA and the small `affinityInfo.dll` that ships inside the jar.
  Handles processor groups, i.e. machines with more than 64 logical CPUs.
- **Linux:** layout from hwloc (`lstopo-no-graphics`, package `hwloc`), including caches and NUMA nodes.
  Without hwloc, the layout comes from `/proc/cpuinfo`, which only knows sockets, cores and SMT threads.
- **Other systems:** `NullAffinity`, which neither binds nor detects a layout.

## Migrating from 3.4.x

- Replace `net.openhft.affinity` with `de.pkmd.affinity` in imports and in logger configuration.
- Upstream's locking model is gone: `AffinityLock`, `LockInventory`, `AffinityStrategies`, `AffinityStrategy`, `AffinityThreadFactory`,
  `AffinitySupport` and the `Affinity.acquireLock()`/`acquireCore()` passthroughs. Use `AffinityManager.getInstance().getLayout()`
  instead of `AffinityLock.cpuLayout()`.
- Also removed: `Affinity.setThreadId()`, the macOS, Solaris and generic POSIX implementations, and `net.openhft.ticker`.
- The complete 3.4 line, with all classes inherited from OpenHFT, remains available as tag `v3.4.0.9` and branch `3-openhft-fork`.

## Background

This fork aims to provide support for another use case: Binding to groups of CPUs instead of single cores. This is useful for exploiting cache locality for groups of threads with similar data access patterns.

Another feature that was missing in 2013 when I started working in this fork was layout introspection for the Windows platform.
For Linux, this is done by reading and parsing `/proc/cpuinfo`, which does not exist in Windows outside of Cygwin. 

Instead, Windows provides `GetLogicalProcessorInformationEx`. Here, we get a number of `SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX` structs, that describe the CPU layout in terms of caches, numa nodes, core and packages (sockets). Also there are groups, which are used by Windows to keep processor masks within 64 bits. Systems with more than 64 logical CPUs will therefore have more than one group. Since the scheduler normally uses the CPUs on the one group only, for a process on a machine with 72 lCPUs (like 2x E5-4669 v3 where each CPU socket counts as a group of 36 lCPUs), `Runtime.availableProcessors` will return 36. Without affinities, no thread will run on the other half of CPUs. Of course, this gets worse on 4 socket configurations.

We gather all `SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX` using JNA and a small DLL built from affinityInfo.cpp. We then construct a number of `LayoutEntities`, each having its own cpu mask. At the time of fork, there was only support for 64 lCPUs on the Linux side using `BitSet`s. On the Windows side, I had to create a `GroupAffinityMask` comprising a `groupId` and a `long` `mask`. 

As per Peters request at the time, I left all the original interfaces untouched and provided my own for the new features. There is a new kind of `CpuLayout`, `WindowsCpuLayout` that is constructed upon `SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX` structures and extends `VanillaCpuLayout` with lists of `Group`s, `Cache`s and `NumaNode`s. To access each of them, there is an interface: `NumaCpuLayout`, `GroupedCpuLayout` and `CacheCpuLayout`. Likewise, `WindowsCpuInfo` extends `VanillaCpuInfo` with `numaId`, `groupId` and `mask`. 

When I started this work, the only relevant machines had Intel CPUs, where relations are rather simple. One socket has a number of cores, possibly with SMT, each core has its own L1+2 cache. The socket then has a hopefully large L3 cache and its own path to memory, aka numa node. In order to do what I wanted (exploiting caches), one could either focus on L2 caches, which was equivalent to a core, or L3 cache, which was equivalent to a socket. The Linux implementation provides access to both and is therefore OK as long as one uses Intel hardware.

Enter AMD Zen.

Now with an Epyc 7301, two cores are organized into core complexes which each have their own L3 cache. Two core complexes form a numa node. Each socket has four of them. There is no longer a simple 1:1 mapping of socket to numa node and L3 cache. Using only naive socket binding on a hardware like this yields huge performance losses. 

Unfortunately, Linux currently does not provide the same one-stop introspection API as Windows. 

Caches do not execute code, and are therefore not referenced in `CpuInfo`s. Instead, they form their own hierarchy. We can only relate `LayoutEntities` (including caches) to each other by their mask.

To bind threads to `LayoutEntities`, they provide a `bind()` method to bind the current thread, keeping track of the threads that are currently bound to it. Alternatively, the `AffinityManager` provides methods to bind to each kind `LayoutEntity`, returning `true` if successful. This replaces upstream's `AffinityLock` and `AffinityStrategy`, which are no longer part of this library.

Not contained in this package is how to bind worker threads in a pool. Since this can be done only by each thread for itself, I subclassed `Thread` as `AffinityThread`. For constructors with a `Runnable` argument, I wrap that `Runnable` into one that first binds the new `Thread`. 

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

To use these threads, `ThreadPool`s need a custom `ThreadFactory` that creates `AffinityThread`s.

Usually, I do not unbind or rebind threads after they have been initially bound. However, this is possible and covered in unit tests.

### Linux layout via hwloc (October 2023)

The `hwloc` package provides layout introspection capabilities similar to Windows. When running under Linux, we therefore call `lstopo-no-graphics -v --no-io` and parse the output. 
