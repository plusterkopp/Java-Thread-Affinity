package net.openhft.affinity.impl;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Provides another method to define a layout using a simple string.
 * Created by ralf h on 23.01.2014.
 */
public class WindowsCpuLayout extends VanillaCpuLayout {

	WindowsCpuLayout(@NotNull List<CpuInfo> cpuDetails) {
		super(cpuDetails);
	}

	/**
	 * To be compatible with {@link VanillaCpuLayout}, the address as s/c/t for the sequence of logical CPUs for a dual-socket, four-core, two-thread config is 0/0/0,
	 * 0/1/0 ... 0/3/0, 1/0/0 ... 1/3/0, ... 0/0/1 ... 1/3/1 which is (hopyfully) the same enumeration sequence as in
	 * cpuinfo files.
	 *
	 * @param desc
	 *            String "#sockets/#coresPerSocket/#threadsPerCore"
	 * @return a layout with s*c*t cpus
	 */
	@NotNull
	public static WindowsCpuLayout fromCpuDesc( String desc) {
		List<CpuInfo> cpuDetails = new ArrayList<CpuInfo>();
		String[]	descParts = desc.split( "\\s*/\\s*", 3);
		int	sockets = Integer.parseInt( descParts[ 0]);
		int	coresPerSocket = Integer.parseInt( descParts[ 1]);
		int	threadsPerCore = Integer.parseInt( descParts[ 2]);

		for ( int t = 0;  t < threadsPerCore;  t++) {
			for ( int s = 0;  s < sockets;  s++) {
				for ( int c = 0;  c < coresPerSocket;  c++) {
					CpuInfo	info = new CpuInfo( s,  c,  t);
					cpuDetails.add( info);
				}
			}
		}
		return new WindowsCpuLayout( cpuDetails);
	}

}
