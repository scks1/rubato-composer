package org.rubato.composer.test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.composer.RequiredLibraries;
import org.rubato.composer.RequiredLibraries.Library;

/**
 * Checks how RequiredLibraries finds missing jars and words its message.
 */
public class RequiredLibrariesTest extends TestCase {

	public void testRequiredLibrariesArePresentInTheBuild() {
		assertEquals(0, RequiredLibraries.findMissing(RequiredLibraries.REQUIRED, RequiredLibraries.class.getClassLoader()).size());
	}

	public void testMissingLibraryIsFound() {
		//a loader with no parent sees only the JDK's platform classes
		ClassLoader bare = new URLClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
		List<Library> missing = RequiredLibraries.findMissing(RequiredLibraries.REQUIRED, bare);
		assertEquals(RequiredLibraries.REQUIRED.length, missing.size());
		assertEquals("collections-generic-4.01.jar", missing.get(0).jar);
		Library jdk = new Library("JDK", "none.jar", "java.util.List", "everything");
		assertEquals(0, RequiredLibraries.findMissing(new Library[]{jdk}, bare).size());
	}

	public void testMessageNamesTheJarAndTheFolder() {
		List<Library> missing = List.of(RequiredLibraries.REQUIRED);
		String message = RequiredLibraries.getMessage(missing, new File("/opt/rubato/rubato.jar"));
		assertTrue(message, message.contains("collections-generic-4.01.jar"));
		assertTrue(message, message.contains("BigBang"));
		assertTrue(message, message.contains("/opt/rubato/lib"));
		assertTrue(message, message.contains("next to rubato.jar"));
		assertFalse(message, message.contains("%"));
		String noFolder = RequiredLibraries.getMessage(missing, null);
		assertTrue(noFolder, noFolder.contains("class path"));
	}
}
