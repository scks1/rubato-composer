# Licensing and the separate-jar layout

Rubato Composer is licensed under the GNU General Public License, version 2 only (see `COPYING`).

Rubato depends on four libraries whose licenses the Free Software Foundation lists as incompatible with GPL version 2:

| Library | Version | License | Files |
|---|---|---|---|
| FlatLaf | 3.8 | Apache License 2.0 | `lib/flatlaf-3.8.jar` |
| Apache Commons Math | 3.2 | Apache License 2.0 | `lib/commons-math3-3.2.jar` |
| Apache Xerces-J | 2.6.2 | Apache Software License 1.1, plus the SAX (public domain) and W3C DOM notices for the bundled APIs | `lib/xercesImpl-2.6.2.jar` |
| Commons Collections with Generics (collections-generic) | 4.01 | Apache License 2.0 | `lib/collections-generic-4.01.jar` |

The builds no longer unpack these libraries into Rubato's own jar. Each library ships unchanged as its own jar in `lib/`, and its license and notice files sit in `lib/licenses/<library>/`. The `Class-Path` entry in Rubato's jar manifest names the four jars, so `java -jar rubato.jar` still finds them.

- Ant: `ant -f java/build.xml dist` writes `java/dist/rubato.jar` and `java/dist/lib/`.
- Maven: `mvn package` writes `java/target/rubato-composer-<version>-all.jar` and `java/target/lib/`, the core library as `java/core/target/rubato-core-<version>.jar`, which needs no other jar, and rubato-io as `java/io/target/rubato-io-<version>.jar`, which needs only the core jar.
- Keep the jar and its `lib/` directory together when you copy or package Rubato.

Rubato runs without some of these jars:

- Without FlatLaf, Rubato logs a warning and uses Swing's platform look and feel.
- Without Xerces, the JDK's built-in XML parser handles Rubato's XML files.
- Without Commons Math, only `GeometryTools.lagrangePredictValues` fails, and no code in Rubato calls it.
- Rubato needs collections-generic. BigBang's operation graph is a JUNG 2.0.1 graph, and JUNG and BigBang's graph view both use collections-generic. Without the jar, loading the built-in BigBang rubette throws NoClassDefFoundError at startup and the main window never opens.

## rubato-io (added 2026-11-21)

rubato-io (`java/io`: MusicXML, Standard MIDI Files and OSC) adds no library. It uses only the JDK's java.base and java.xml modules, and its classes go into Rubato's own jar (Ant `dist`, Maven `-all`) or into `dist/io/rubato-io.jar` (Ant `io-jar`). Its tests use JUnit 4.13.2 (Eclipse Public License 1.0, per its pom) with Hamcrest Core 1.3 (New BSD License, per hamcrest-parent's pom), the test dependency rubato-core already uses. The FSF lists EPL 1.0 as incompatible with GPL version 2; JUnit is test scope, runs only during the build and ships in no jar.

- The MusicXML reader asks for the JDK's own XML parser (`DocumentBuilderFactory.newDefaultInstance()`), so it never loads Xerces 2.6.2 from `lib/`, the Apache 1.1 library in the table above, even when that jar is on the class path.
- The MusicXML 4.0 schema (W3C Community Final Specification Agreement) is not in the repository. A test validates against a local copy only when the system property `musicxml.xsd` names one. Whether to vendor the schema is an open decision.

This layout keeps the GPL-2.0 program and the Apache-licensed libraries in separate files, the way an aggregate distribution does. Whether that separation is enough for a particular distribution is a legal question, and this file is not legal advice.
