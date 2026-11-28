#!/usr/bin/env bash
# run-checks.sh: cross-checks the Agda spec against the Java core.
#
#  1. compiles gen/ExportAgda.java, gen/ExportAddresses.java,
#     gen/ExportCertificates.java and gen/DepartureProbe.java against the
#     built composer jar
#  2. runs ExportAgda, ExportAddresses and ExportCertificates and compares
#     their output with the committed Generated.agda, GeneratedAddresses.agda
#     and GeneratedCertificates.agda (any difference fails)
#  3. runs DepartureProbe and keeps its output
#  4. typechecks every positive module from a clean state, Counterpoint.agda,
#     Global.agda, Harmony.agda, Performance.agda, FiniteModAt.agda, FieldExtraction.agda, VoiceLeading.agda, RhythmTrees.agda, ThreeVoice.agda and MasterMonoid.agda included (their facts need no generated values),
#     and Certificates.agda with the committed GeneratedCertificates.agda
#  4b. typechecks GeneratedCertificates.agda once more through
#     check-certificate.sh, the script a user runs on a rubette's file
#  5. typechecks every file in adversarial/ and requires each to fail
#     with the text on its "-- EXPECT:" line, at a location in that file
#
# Usage: spec/agda/run-checks.sh [log-dir]
# Needs: a JDK, the jar from `mvn verify` or `ant dist`, and agda 2.6.3
# with its prim library. AGDA overrides the agda binary; JAR overrides
# the jar path. Set SKIP_JAVA=1 to skip steps 1-3 (as the CI job does).
set -u
here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
logdir="${1:-$repo/logs/agda-$(date +%F)}"
mkdir -p "$logdir" || exit 2
logdir="$(cd "$logdir" && pwd)"
export LC_ALL=C.UTF-8
AGDA="${AGDA:-agda}"
if [ -z "${Agda_datadir:-}" ] && [ -d "$HOME/.local/share/agda-prim/datadir" ]; then
  export Agda_datadir="$HOME/.local/share/agda-prim/datadir"
fi
JAR="${JAR:-$repo/java/target/rubato-composer-0.1.0-SNAPSHOT-all.jar}"
fail=0
say() { echo "$*" | tee -a "$logdir/summary.txt"; }
: > "$logdir/summary.txt"
say "run-checks.sh $(date -Is)"
say "agda: $("$AGDA" --version)"

cd "$here" || exit 2

if [ "${SKIP_JAVA:-0}" != 1 ]; then
  say "java: $(java -version 2>&1 | head -1)"
  [ -f "$JAR" ] || { say "FAIL jar not found: $JAR"; exit 2; }
  say "jar: $JAR ($(sha256sum "$JAR" | cut -c1-16))"
  cls="$(mktemp -d)"
  if javac -encoding UTF-8 -d "$cls" -cp "$JAR" gen/ExportAgda.java gen/ExportAddresses.java gen/ExportCertificates.java gen/DepartureProbe.java \
       > "$logdir/javac.log" 2>&1; then
    say "PASS javac"
  else
    say "FAIL javac (see javac.log)"; exit 1
  fi
  jrun() { java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Djava.awt.headless=true -cp "$cls:$JAR" "$@"; }
  if jrun ExportAgda > "$logdir/Generated.fresh.agda" 2> "$logdir/export.stderr"; then
    if diff -u Generated.agda "$logdir/Generated.fresh.agda" > "$logdir/generated.diff"; then
      say "PASS generator output matches Generated.agda"
    else
      say "FAIL generator output differs from Generated.agda (see generated.diff)"; fail=1
    fi
  else
    say "FAIL ExportAgda (see export.stderr)"; fail=1
  fi
  if jrun ExportAddresses > "$logdir/GeneratedAddresses.fresh.agda" 2> "$logdir/export-addresses.stderr"; then
    if diff -u GeneratedAddresses.agda "$logdir/GeneratedAddresses.fresh.agda" > "$logdir/generated-addresses.diff"; then
      say "PASS generator output matches GeneratedAddresses.agda"
    else
      say "FAIL generator output differs from GeneratedAddresses.agda (see generated-addresses.diff)"; fail=1
    fi
  else
    say "FAIL ExportAddresses (see export-addresses.stderr)"; fail=1
  fi
  if jrun ExportCertificates > "$logdir/GeneratedCertificates.fresh.agda" 2> "$logdir/export-certificates.stderr"; then
    if diff -u GeneratedCertificates.agda "$logdir/GeneratedCertificates.fresh.agda" > "$logdir/generated-certificates.diff"; then
      say "PASS generator output matches GeneratedCertificates.agda"
    else
      say "FAIL generator output differs from GeneratedCertificates.agda (see generated-certificates.diff)"; fail=1
    fi
  else
    say "FAIL ExportCertificates (see export-certificates.stderr)"; fail=1
  fi
  if jrun DepartureProbe > "$logdir/departure-probe.txt" 2> "$logdir/departure-probe.stderr"; then
    say "PASS departure probe ($(wc -l < "$logdir/departure-probe.txt") lines, departure-probe.txt)"
  else
    say "FAIL departure probe"; fail=1
  fi
  rm -rf "$cls"
fi

find . -name '*.agdai' -delete
: > "$logdir/agda-positive.log"
for m in Prelude OrderedSets Forms Registry Generated GeneratedChecks Addresses GeneratedAddresses AddressChecks Counterpoint Global Harmony Performance Gestures Monteverdi FiniteModAt FieldExtraction VoiceLeading RhythmTrees ThreeVoice MasterMonoid Certificates GeneratedCertificates; do
  if out="$("$AGDA" --no-libraries -i . "$m.agda" 2>&1)"; then
    say "PASS $m.agda"
  else
    say "FAIL $m.agda"; fail=1
  fi
  { echo "== $m.agda"; echo "$out"; } >> "$logdir/agda-positive.log"
done

: > "$logdir/check-certificate.log"
if AGDA="$AGDA" bash "$here/check-certificate.sh" "$here/GeneratedCertificates.agda" > "$logdir/check-certificate.log" 2>&1; then
  say "PASS check-certificate.sh GeneratedCertificates.agda"
else
  say "FAIL check-certificate.sh GeneratedCertificates.agda (see check-certificate.log)"; fail=1
fi

: > "$logdir/agda-adversarial.log"
for f in adversarial/*.agda; do
  expect="$(sed -n 's/^-- EXPECT: //p' "$f" | head -1)"
  out="$("$AGDA" --no-libraries -i . "$f" 2>&1)"; rc=$?
  { echo "== $f (exit $rc, expect: $expect)"; echo "$out"; } >> "$logdir/agda-adversarial.log"
  if [ $rc -eq 0 ]; then
    say "FAIL $f typechecked but must not"; fail=1
  elif [ -z "$expect" ]; then
    say "FAIL $f has no EXPECT line"; fail=1
  elif ! grep -qF -- "$expect" <<< "$out"; then
    say "FAIL $f failed without the expected text"; fail=1
  elif ! grep -qF -- "$here/$f:" <<< "$out"; then
    say "FAIL $f failed somewhere other than in its own file"; fail=1
  else
    say "PASS $f rejected as expected"
  fi
done
find . -name '*.agdai' -delete

if [ $fail -eq 0 ]; then say "ALL PASS"; else say "SOME CHECKS FAILED"; fi
exit $fail
