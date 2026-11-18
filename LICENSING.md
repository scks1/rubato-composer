# Licensing and the separate-jar layout

Rubato Composer is licensed under the GNU General Public License, version 2 only (see `COPYING`).

Rubato depends on three libraries whose licenses the Free Software Foundation lists as incompatible with GPL version 2:

| Library | Version | License | Files |
|---|---|---|---|
| FlatLaf | 3.8 | Apache License 2.0 | `lib/flatlaf-3.8.jar` |
| Apache Commons Math | 3.2 | Apache License 2.0 | `lib/commons-math3-3.2.jar` |
| Apache Xerces-J | 2.6.2 | Apache Software License 1.1, plus the SAX (public domain) and W3C DOM notices for the bundled APIs | `lib/xercesImpl-2.6.2.jar` |

The builds no longer unpack these libraries into Rubato's own jar. Each library ships unchanged as its own jar in `lib/`, and its license and notice files sit in `lib/licenses/<library>/`. The `Class-Path` entry in Rubato's jar manifest names the three jars, so `java -jar rubato.jar` still finds them.

- Ant: `ant -f java/build.xml dist` writes `java/dist/rubato.jar` and `java/dist/lib/`.
- Maven: `mvn package` writes `target/rubato-composer-<version>-all.jar` and `target/lib/`.
- Keep the jar and its `lib/` directory together when you copy or package Rubato.

Rubato runs without some of these jars:

- Without FlatLaf, Rubato logs a warning and uses Swing's platform look and feel.
- Without Xerces, the JDK's built-in XML parser handles Rubato's XML files.
- Without Commons Math, only `GeometryTools.lagrangePredictValues` fails, and no code in Rubato calls it.

This layout keeps the GPL-2.0 program and the Apache-licensed libraries in separate files, the way an aggregate distribution does. Whether that separation is enough for a particular distribution is a legal question, and this file is not legal advice.
