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

import net.openhft.affinity.ICpuInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.parseInt;

/**
 * @author peter.lawrey
 */
public class ApicCpuLayout extends VanillaCpuLayout {

	public static ApicCpuLayout fromCpuInfo(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		String line;
		List<ICpuInfo> cpuDetails = new ArrayList<>();
		ApicCpuInfo details = new ApicCpuInfo();
		Map<String, Integer> threadCount = new LinkedHashMap<String, Integer>();

		while ((line = br.readLine()) != null) {
			if (line.trim().isEmpty()) {
				String key = details.getSocketId() + "," + details.getCoreId();
				Integer count = threadCount.get(key);
				if (count == null) {
					threadCount.put(key, count = 1);
				} else {
					threadCount.put(key, count += 1);
				}
				details.setThreadId(count - 1);
				cpuDetails.add(details);
				details = new ApicCpuInfo();
				details.setCoreId(cpuDetails.size());
				continue;
			}
			String[] words = line.split("\\s*:\\s*", 2);
			if (words[0].equals("physical id")) {
				details.setSocketId(parseInt(words[1]));
			} else if (words[0].equals("core id")) {
				details.setCoreId(parseInt(words[1]));
			} else if (words[0].equals("apicid")) {
				details.setApicId(parseInt(words[1]));
			}
		}
		return new ApicCpuLayout(cpuDetails);
	}

	ApicCpuLayout(@NotNull List<ICpuInfo> cpuDetails) {
		super(cpuDetails);
	}


	@NotNull
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0, cpuDetailsSize = cpuDetails.size(); i < cpuDetailsSize; i++) {
			ICpuInfo cpuDetail = cpuDetails.get(i);
			sb.append(i).append(": ").append(cpuDetail).append('\n');
		}
		return sb.toString();
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ApicCpuLayout that = (ApicCpuLayout) o;

		if (coresPerSocket() != that.coresPerSocket()) {
			return false;
		}
		if (sockets() != that.sockets()) {
			return false;
		}
		if (threadsPerCore() != that.threadsPerCore()) {
			return false;
		}
		return cpuDetails.equals(that.cpuDetails);
	}

	public ICpuInfo getCPUInfo(int index) {
		return cpuDetails.get(index);
	}

}
