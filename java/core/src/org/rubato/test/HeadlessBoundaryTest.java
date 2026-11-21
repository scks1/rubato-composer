package org.rubato.test;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import junit.framework.TestCase;

import org.rubato.base.Repository;

/**
 * Reads the constant pool of every class in the core library and checks that
 * none names a desktop class (AWT, Swing, Java 2D images, applets, sound) or
 * a Rubato class outside the core library. The compiler already sees only
 * java.base, java.xml and java.logging, so this test guards the Ant build and
 * class names that reach the code as strings in descriptors.
 */
public class HeadlessBoundaryTest extends TestCase {
	
	private static final String[] DESKTOP_PREFIXES = {
		"java/awt/", "javax/swing/", "javax/imageio/", "java/applet/", "javax/sound/", "javax/print/"
	};
	
	public void testCoreClassesNameNoDesktopOrApplicationClass() throws IOException {
		URL location = Repository.class.getProtectionDomain().getCodeSource().getLocation();
		File root = new File(location.getPath());
		List<ClassBytes> classes = new ArrayList<ClassBytes>();
		if (root.isDirectory()) {
			this.collect(root, root, classes);
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
		assertTrue("found the core classes in " + root + ": " + classes.size(), classes.size() > 300);
		Set<String> own = new HashSet<String>();
		for (ClassBytes c : classes) {
			own.add(c.name.substring(0, c.name.length() - ".class".length()));
		}
		Set<String> problems = new TreeSet<String>();
		for (ClassBytes c : classes) {
			for (String utf8 : utf8Constants(c.bytes)) {
				for (String prefix : DESKTOP_PREFIXES) {
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
					if (!own.contains(name)) {
						problems.add(c.name + " names " + name + ", which is not in the core library");
					}
					i = utf8.indexOf("org/rubato/", end);
				}
			}
		}
		assertTrue(problems.toString(), problems.isEmpty());
	}
	
	private void collect(File root, File dir, List<ClassBytes> classes) throws IOException {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				this.collect(root, file, classes);
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
