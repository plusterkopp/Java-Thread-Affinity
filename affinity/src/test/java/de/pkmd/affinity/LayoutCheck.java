/*
 * Copyright 2026 Ralf Helbing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.pkmd.affinity;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Prints the detected layout and runs all unit tests on the current machine.
 * {@code mvn -P check package} packs it with the tests and all dependencies into
 * {@code affinity-<version>-check.jar}, which runs wherever Java 8+ is installed:
 * {@code java -jar affinity-<version>-check.jar > layout-check.txt 2>&1}
 */
public class LayoutCheck {

	public static void main(String[] args) throws Exception {
		System.out.println("Java " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ") on "
				+ System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch")
				+ ", " + Runtime.getRuntime().availableProcessors() + " processors available to the JVM");
		System.out.println("Affinity implementation: " + Affinity.getAffinityImpl().getClass().getName());

		AffinityManager am = AffinityManager.getInstance();
		System.out.println("\n=== detected layout");
		am.dumpLayout();
		System.out.println("\n=== raw layout");
		am.dumpRawLayout();

		List<Class<?>> tests = findTests();
		System.out.println("\n=== running " + tests.size() + " test classes");
		JUnitCore junit = new JUnitCore();
		junit.addListener(new RunListener() {
			@Override
			public void testFailure(Failure failure) {
				System.out.println("FAILED:  " + failure.getDescription());
			}

			@Override
			public void testAssumptionFailure(Failure failure) {
				System.out.println("skipped: " + failure.getDescription() + " (" + failure.getMessage() + ")");
			}

			@Override
			public void testIgnored(Description description) {
				System.out.println("ignored: " + description);
			}
		});
		Result result = junit.run(tests.toArray(new Class<?>[0]));

		System.out.println("\n=== result");
		System.out.println("tests run: " + result.getRunCount() + ", failed: " + result.getFailureCount()
				+ ", skipped: " + result.getAssumptionFailureCount() + ", ignored: " + result.getIgnoreCount());
		for (Failure failure : result.getFailures()) {
			System.out.println("\n--- " + failure.getDescription());
			System.out.println(failure.getTrace());
		}
		System.out.println(result.wasSuccessful() ? "ALL TESTS PASSED" : "THERE WERE FAILURES");
		System.exit(result.wasSuccessful() ? 0 : 1);
	}

	/** All concrete *Test classes next to this class, in the check jar or in target/test-classes. */
	private static List<Class<?>> findTests() throws Exception {
		List<String> paths = new ArrayList<>();
		URL location = LayoutCheck.class.getProtectionDomain().getCodeSource().getLocation();
		if (location.getPath().endsWith(".jar")) {
			// read through the URL: on a network share it is file://server/share/..., which java.io.File rejects
			try (JarInputStream jar = new JarInputStream(location.openStream())) {
				for (JarEntry entry; (entry = jar.getNextJarEntry()) != null; ) {
					paths.add(entry.getName());
				}
			}
		} else {
			collect(Paths.get(location.toURI()).toFile(), "", paths);
		}
		Collections.sort(paths);
		List<Class<?>> tests = new ArrayList<>();
		for (String path : paths) {
			if (path.startsWith("de/pkmd/affinity/") && path.endsWith("Test.class") && !path.contains("$")) {
				String name = path.substring(0, path.length() - ".class".length()).replace('/', '.');
				Class<?> c = Class.forName(name, false, LayoutCheck.class.getClassLoader());
				if (!Modifier.isAbstract(c.getModifiers())) {
					tests.add(c);
				}
			}
		}
		return tests;
	}

	private static void collect(File dir, String prefix, List<String> paths) {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File f : files) {
			if (f.isDirectory()) {
				collect(f, prefix + f.getName() + "/", paths);
			} else {
				paths.add(prefix + f.getName());
			}
		}
	}
}
