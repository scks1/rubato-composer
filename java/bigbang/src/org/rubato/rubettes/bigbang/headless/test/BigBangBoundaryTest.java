package org.rubato.rubettes.bigbang.headless.test;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import junit.framework.TestCase;

import org.rubato.base.Repository;
import org.rubato.rubettes.bigbang.headless.BigBang;
import org.rubato.rubettes.bigbang.headless.BigBangExample;

/**
 * Checks that the BigBang module stays apart from the desktop and from the application.
 * <ol>
 * <li>The constant pool of every module class names no desktop class (AWT, Swing, Java 2D images,
 * applets, sound, printing, java.beans), no JUNG or collections-generic class, and no Rubato class
 * outside rubato-core and this module.</li>
 * <li>BigBangExample runs in a new JVM that sees only java.base, java.xml and java.logging, with
 * only rubato-core and this module on its class path, and prints the expected notes.</li>
 * </ol>
 * The compiler already sees only those three Java modules, so the first check guards the Ant
 * build and names that reach the code as strings in descriptors.
 */
public class BigBangBoundaryTest extends TestCase {
	
	private static final String[] FORBIDDEN_PREFIXES = {
		"java/awt/", "javax/swing/", "javax/imageio/", "java/applet/", "javax/sound/", "javax/print/",
		"java/beans/", "edu/uci/ics/jung/", "org/apache/commons/collections15/"
	};
	
	public void testModuleClassesNameNoDesktopLibraryOrApplicationClass() throws IOException {
		List<ClassBytes> core = classes(location(Repository.class));
		List<ClassBytes> module = classes(location(BigBang.class));
		assertTrue("found the core classes: " + core.size(), core.size() > 300);
		assertTrue("found the module classes: " + module.size(), module.size() > 50);
		Set<String> known = new HashSet<String>();
		for (ClassBytes c : core) {
			known.add(c.name.substring(0, c.name.length() - ".class".length()));
		}
		for (ClassBytes c : module) {
			known.add(c.name.substring(0, c.name.length() - ".class".length()));
		}
		Set<String> problems = new TreeSet<String>();
		for (ClassBytes c : module) {
			if (c.name.contains("/test/")) {
				continue;
			}
			for (String utf8 : utf8Constants(c.bytes)) {
				for (String prefix : FORBIDDEN_PREFIXES) {
					if (utf8.contains(prefix)) {
						problems.add(c.name + " names " + utf8);
					}
				}
				int i = utf8.indexOf("org/rubato/");
				while (i >= 0) {
					int end = i;
					while (end < utf8.length() && ";<>[()".indexOf(utf8.charAt(end)) < 0) {
						end++;
					}
					String name = utf8.substring(i, end);
					if (!known.contains(name)) {
						problems.add(c.name + " names " + name + ", which is in neither rubato-core nor rubato-bigbang");
					}
					i = utf8.indexOf("org/rubato/", end);
				}
			}
		}
		assertTrue(problems.toString(), problems.isEmpty());
	}
	
	public void testExampleRunsWithOnlyBaseXmlAndLogging() throws Exception {
		String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		String classPath = location(Repository.class) + File.pathSeparator + location(BigBang.class);
		ProcessBuilder builder = new ProcessBuilder(java, "--limit-modules", "java.base,java.xml,java.logging",
				"-Djava.awt.headless=true", "-cp", classPath, BigBangExample.class.getName());
		builder.redirectErrorStream(true);
		builder.environment().remove("DISPLAY");
		Process process = builder.start();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		InputStream in = process.getInputStream();
		in.transferTo(output);
		assertTrue("the example finished", process.waitFor(120, TimeUnit.SECONDS));
		String text = output.toString("UTF-8");
		assertEquals(text, 0, process.exitValue());
		assertFalse(text, text.contains("Exception"));
		assertTrue(text, text.contains("input, form Score:"));
		assertTrue(text, text.contains("(4 operations)"));
		assertTrue(text, text.contains("onset 2.5, pitch 68.0"));
		assertTrue(text, text.contains("after undoing the scaling:\n  onset 4.0, pitch 60.0"));
	}
	
	private static String location(Class<?> type) {
		return new File(type.getProtectionDomain().getCodeSource().getLocation().getPath()).getPath();
	}
	
	private static List<ClassBytes> classes(String location) throws IOException {
		File root = new File(location);
		List<ClassBytes> classes = new ArrayList<ClassBytes>();
		if (root.isDirectory()) {
			collect(root, root, classes);
		}
		else {
			JarFile jar = new JarFile(root);
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.getName().endsWith(".class")) {
					InputStream in = jar.getInputStream(entry);
					classes.add(new ClassBytes(entry.getName(), in.readAllBytes()));
					in.close();
				}
			}
			jar.close();
		}
		return classes;
	}
	
	private static void collect(File root, File dir, List<ClassBytes> classes) throws IOException {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				collect(root, file, classes);
			}
			else if (file.getName().endsWith(".class")) {
				String name = root.toURI().relativize(file.toURI()).getPath();
				FileInputStream in = new FileInputStream(file);
				classes.add(new ClassBytes(name, in.readAllBytes()));
				in.close();
			}
		}
	}
	
	//the UTF-8 entries of a class file's constant pool (JVMS 4.4)
	private static List<String> utf8Constants(byte[] bytes) throws IOException {
		DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(bytes));
		assertEquals(0xCAFEBABE, in.readInt());
		in.readUnsignedShort();
		in.readUnsignedShort();
		int count = in.readUnsignedShort();
		List<String> strings = new ArrayList<String>();
		for (int i = 1; i < count; i++) {
			int tag = in.readUnsignedByte();
			switch (tag) {
				case 1: strings.add(in.readUTF()); break;
				case 3: case 4: in.readInt(); break;
				case 5: case 6: in.readLong(); i++; break;
				case 7: case 8: case 16: case 19: case 20: in.readUnsignedShort(); break;
				case 9: case 10: case 11: case 12: case 17: case 18: in.readInt(); break;
				case 15: in.readUnsignedByte(); in.readUnsignedShort(); break;
				default: fail("unknown constant pool tag " + tag);
			}
		}
		return strings;
	}
	
	private static final class ClassBytes {
		final String name;
		final byte[] bytes;
		ClassBytes(String name, byte[] bytes) {
			this.name = name;
			this.bytes = bytes;
		}
	}
}
