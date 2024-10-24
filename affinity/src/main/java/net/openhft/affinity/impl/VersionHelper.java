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

public class VersionHelper {
	private static final String DELIM = ".";
	private final int major;
	private final int minor;
	private final int release;

	public VersionHelper(int major_, int minor_, int release_) {
		major = major_;
		minor = minor_;
		release = release_;
	}

	public VersionHelper(String ver) {
		if (ver != null && (ver = ver.trim()).length() > 0) {
			final String[] parts = ver.split("\\.");
			major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
			minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
			release = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

		} else {
			major = minor = release = 0;
		}
	}

	public String toString() {
		return major + DELIM + minor + DELIM + release;
	}

	public boolean equals(Object o) {
		if (o != null && (o instanceof VersionHelper)) {
			VersionHelper ver = (VersionHelper) o;
			return this.major == ver.major
					&& this.minor == ver.minor
					&& this.release == ver.release;

		} else {
			return false;
		}
	}

	public int hashCode() {
		return (major << 16) | (minor << 8) | release;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public boolean majorMinorEquals(final VersionHelper ver) {
		return ver != null
				&& this.major == ver.major
				&& this.minor == ver.minor;
	}

	public boolean isSameOrNewer(final VersionHelper ver) {
		if (ver == null) {
			return false;
		}
		if (major > ver.major) {
			return true;
		}
		if (major < ver.major) {
			return false;
		}
		if (minor > ver.minor) {
			return true;
		}
		if (minor < ver.minor) {
			return false;
		}
		return release >= ver.release;
	}
}

