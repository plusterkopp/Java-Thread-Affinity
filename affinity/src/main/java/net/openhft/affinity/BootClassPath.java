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

package net.openhft.affinity;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Checks whether a class is visible to the bootstrap class loader.
 * Uses only public API, so it works on Java 8 as well as Java 9+ (where {@code sun.misc.URLClassPath}
 * and the {@code sun.boot.class.path} property no longer exist).
 */
enum BootClassPath {
	INSTANCE;

	// a URLClassLoader without URLs and with a null parent only sees resources of the bootstrap loader
	private final ClassLoader bootLoader = new URLClassLoader(new URL[0], null);

	public final boolean has(String binaryClassName) {
		String resourceClassName = binaryClassName.replace('.', '/').concat(".class");
		return bootLoader.getResource(resourceClassName) != null;
	}
}
