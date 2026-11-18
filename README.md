# Rubato Composer

## Info

Rubato Composer is a music software based on the concepts and models of mathematical music theory.

Visit http://rubato.org and http://www.encyclospace.org for more information.

Former repository: https://sourceforge.net/projects/rubatocomposer/

## Install

Download the latest release [here](https://github.com/rubato-composer/rubato-composer/releases).

The `rubato-bin-YYYYMMDD.tar.gz` contains a Java archive `rubato.jar` and a `lib/` folder of libraries it needs. Keep them together. The jar can be run by double-clicking it or by running
```bash
$ java -jar rubato.jar
```
in the console in the corresponding folder.

Alternatively, clone and build the src as subsequently described.

## Build

Clone the project, move to the folder, then run
```bash
$ ant
```
which generates tar.gz files with latest `src`, `bin`, and `api` annotated with the current date.

## Licensing

Rubato Composer is GPL-2.0-only. FlatLaf, Apache Commons Math, Xerces and collections-generic ship as separate jars in `lib/` next to `rubato.jar`; see [LICENSING.md](LICENSING.md).

## Notes

Make sure to read the BUGS file for any known issues.
