#!/usr/bin/env bash
# check-certificate.sh: typechecks a certificate module that Rubato's Agda
# Certificate rubette (or AgdaCertificates.module) wrote.
#
# Usage: spec/agda/check-certificate.sh FILE.agda
#
# Copies Prelude.agda, Certificates.agda and FILE into a fresh temporary
# directory and runs agda 2.6.3 there, so no interface files land next to the
# spec or the certificate. Exit 0 means Agda accepted every certificate in the
# file; agda's messages go to standard output. AGDA overrides the agda binary.
set -u
here="$(cd "$(dirname "$0")" && pwd)"
[ $# -eq 1 ] || { echo "usage: $0 FILE.agda" >&2; exit 2; }
file="$1"
[ -f "$file" ] || { echo "not found: $file" >&2; exit 2; }
base="$(basename "$file")"
case "$base" in
  Prelude.agda|Certificates.agda) echo "$base is a library module, not a certificate" >&2; exit 2 ;;
  *.agda) ;;
  *) echo "$base: a certificate file ends in .agda" >&2; exit 2 ;;
esac
export LC_ALL=C.UTF-8
AGDA="${AGDA:-agda}"
if [ -z "${Agda_datadir:-}" ] && [ -d "$HOME/.local/share/agda-prim/datadir" ]; then
  export Agda_datadir="$HOME/.local/share/agda-prim/datadir"
fi
tmp="$(mktemp -d)" || exit 2
trap 'rm -rf "$tmp"' EXIT
cp "$here/Prelude.agda" "$here/Certificates.agda" "$tmp/" || exit 2
cp "$file" "$tmp/$base" || exit 2
"$AGDA" --no-libraries -i "$tmp" "$tmp/$base"
