# Agda spec of Rubato's forms and denotators

This directory holds an Agda model of Rubato Composer's form and
denotator system as the Java core implements it (`org.rubato.math.yoneda`,
`org.rubato.math.module`, `org.rubato.base.Repository`). A generator
reads the registered forms and some denotators out of the running Java
code and writes them as Agda terms, and the Agda files prove what the
model says about them.

The Java code is the source of truth for behaviour. The theory
vocabulary (form, denotator, limit, colimit, power, address) comes from
the Rubato manual in `docs/manual.tex` and from Mazzola, Noll and
Lluis-Puebla, *Perspectives in Mathematical and Computational Music
Theory* (p. 92 lists the form types). The table of departures below
names each place where the Java code does something the theory does
not say, and what the model does there.

Agda 2.6.3 with only its primitive library (`Agda.Builtin.*`), no
standard library. Every module uses `--safe --without-K`.

## Files

| File | Contents |
|---|---|
| `Prelude.agda` | ⊥, booleans and their lemmas, lists, string equality with a soundness proof, a three-way `Ordering`, exact orders on Nat, Int and String |
| `OrderedSets.agda` | sorted lists for an arbitrary comparison: `Asc`, TreeMap-style `insert`, `fromList`, and the proofs listed below |
| `Forms.agda` | modules and elements, form bodies, repositories (`Env`), zero-addressed denotators, Java's denotator order `cmpDen`, the relation `E ⊢ d ∶ F`, the decision procedure `check` with soundness and completeness, repository invariants |
| `Registry.agda` | the music forms written by hand from the Java sources (`handEnv`) and the proofs about them |
| `Generated.agda` | written by `gen/ExportAgda.java`: 48 forms reachable from 18 root forms of the Java repository (`javaEnv`), and 10 denotators built by Java code. Do not edit |
| `GeneratedChecks.agda` | proofs about `Generated.agda` |
| `adversarial/*.agda` | 9 files that must fail to typecheck, each with an `-- EXPECT:` line |
| `gen/ExportAgda.java` | the generator |
| `gen/DepartureProbe.java` | prints Java's behaviour for each departure case P1 to P11 |
| `run-checks.sh` | runs everything below |

## Running

```
cd upstream && . ../env.sh            # JDK 21 on this VM
mvn -B -o verify                      # or ant dist; builds the -all jar
spec/agda/run-checks.sh [log-dir]
```

`run-checks.sh` compiles both Java programs against
`java/target/rubato-composer-0.1.0-SNAPSHOT-all.jar` (override with
`JAR=`), runs the generator and fails if its output differs from the
committed `Generated.agda`, runs the probe, typechecks the six positive
modules from a clean state (it deletes `*.agdai` first), and typechecks
each adversarial file, which passes only when Agda rejects the file with
the expected text at a location inside that file. `SKIP_JAVA=1` skips
the Java steps. `AGDA=` picks the Agda binary.

To refresh `Generated.agda` after a change to the registered forms, run
the generator by hand (see the script) and commit the new file together
with the change; the proofs in `GeneratedChecks.agda` then say whether
the new forms still agree with `Registry.agda`.

## What is modelled

- **Forms** of the five Java types (`Yoneda.SIMPLE` to `Yoneda.LIST`):
  simple over a module, limit and colimit with coordinate form names and
  optional labels, power and list over one coordinate form. A form refers
  to its coordinates by name, so recursive forms (MacroScore, SoundScore,
  FMSet, Modulators) are entries in a repository.
- **Repositories** as lists of `name ≔ body`, with three checkable
  invariants: names unique (`uniqueB`), every referenced form defined
  (`closedB`), labels either absent or one per coordinate (`labelsOkB`).
- **Modules** Z, Q, R, C, Z/n and Rⁿ, with Java's element invariants:
  Z/n values below the modulus and Q denominators positive.
- **Zero-addressed denotators** of each type, with an optional name.
- **Java's order on denotators** (`cmpDen`): form, then denotator name,
  then content, following `Denotator.compareTo`, `Form.compareTo`,
  `NameDenotator`/`NameEntry`, the morphism-map `compareTo` methods and
  each `ModuleElement.compareTo`, apart from the overflow in D5.
- **Well-formedness** `E ⊢ d ∶ F`: d's form is F, F is registered,
  a simple element lies in F's module, a limit has exactly one coordinate
  of the right form per factor, a colimit's index names a cofactor and its
  factor has that cofactor's form, a power's elements have the coordinate
  form and are strictly ascending under `cmpDen` (the TreeMap order), and
  a list's elements have the coordinate form in any order.

## What is not modelled

- Addresses other than the zero address, address changes and
  morphisms between modules (D4). The generator refuses a denotator that
  is not null-addressed.
- Module morphisms, `ModuleMorphism` coordinates and every module other
  than the six above (polynomial rings, product rings, direct sums,
  strings). They appear as `otherM` with no elements.
- Operations on denotators: union, intersection, `Denotator.map`, the
  logeo package, path access with `int[]` paths, Scheme.
- Denotator name structure: names are flat strings (D16).
- Saving and loading XML, the rubettes, the UI.
- Mutation. Java denotators are mutable (`setFactor`, `appendFactor`);
  the model describes values.

## What is proved

Proofs by computation (`refl`) on concrete terms are marked (c);
the rest are proofs for all inputs.

`OrderedSets.agda`, for any comparison `c`:
- `insert-asc`, `fromList-asc`: if `c` is flip-symmetric
  (`c b a ≡ flipO (c a b)`), TreeMap-style insertion keeps a list
  ascending, so `fromList` always returns an ascending list.
- `head-below-all`, `no-duplicates`: if `c`'s `lt` is transitive, the
  head of an ascending list is below every later element, and so no two
  elements of an ascending list compare `eq`.
- `ascB-sound`, `ascB-complete`: the boolean test agrees with `Asc`.

`Prelude.agda`: `==ˢ-sound`, `==ˢ-refl` (string equality), and
flip symmetry of the exact Nat and Int orders (`cmpNat-flip`,
`cmpInt-flip`).

`Forms.agda`:
- `sound`: `check E d F ≡ true` implies `E ⊢ d ∶ F`.
- `complete`: `E ⊢ d ∶ F` implies `check E d F ≡ true`.

`Registry.agda`:
- (c) `handEnv` satisfies the three repository invariants.
- (c) Note has 5 factors with labels onset, pitch, loudness, duration,
  voice; SoundNote has 6 factors with Modulators at index 5; no form
  named Layer is registered (upstream 376ff94 removed Layer from
  SoundNote; known-bugs 9ffdfc9 made SoundNoteGenerator follow it).
- (c, through `sound`) a three-note Score, a two-level MacroScore, a
  SoundScore whose note has a modulator, an IntegerOrReal colimit, the
  empty Score and a BooleanList with repeats are well-formed.
- (c) `check` rejects a Note with four coordinates, a coordinate of the
  wrong form, a colimit index outside the cofactors, a colimit factor of
  the other cofactor's form, a Score with a repeated note, an unsorted
  Score, a Z/2 value 2, a rational with denominator 0, a real where Z is
  required, and a denotator of an unregistered form.
- (c) `fromList cmpDen` turns n2, n0, n2 into n0, n2, as Java's
  PowerDenotator does (probe P3); names sort before content (P4); a list
  that is a prefix of another is greater (P8).
- `cmpR-not-flip-symmetric`: Java's real order is not flip-symmetric,
  with NaN and 1.0 as the counterexample, so `fromList-asc` does not
  apply to sets of reals. (c) `cmpR nan nan ≡ gt`,
  `cmpR 0.0 -0.0 ≡ eq`.
- `integerSet-sorted`: building an IntegerSet with the exact integer
  order always gives an ascending list.

`GeneratedChecks.agda`:
- (c) `javaEnv`, the 48 forms Java registers, satisfies the three
  invariants.
- (c) `registry-agrees`: for each of the 22 hand-written forms,
  `javaEnv` holds the same body.
- (c, through `sound`) eight denotators built by Java are well-formed: a
  Score (inserted out of order; one note named), the Note Java padded
  from three coordinates, a MacroScore with a satellite, the three-level
  SoundScore from `SoundNoteGenerator.createMultiLevelSoundScore`, an FM
  SoundNode from `createFMNodeDenotator` (model "Double modulator"), a
  GeneralScore holding a Note and a Rest through the NoteOrRest colimit,
  an Envelope list with a repeated node, and a Complex.
- (c) the IntegerSet Java stores for 1, MIN_VALUE, 0 and the RealSet
  Java stores for NaN, NaN, 1.0 are rejected (D5, D6).

`adversarial/`: each file states something false and Agda rejects it.
ShortNote, DuplicateNote, ColimitIndex and UnregisteredForm try to prove
ill-formed denotators well-formed; JavaNaNSet and JavaOverflowSet try the
two Java sets above; SoundNoteLayer claims SoundNote has 7 factors;
RealFlipSym claims flip symmetry of `cmpR` at NaN and 1.0;
ConstructorArity builds a Note derivation with no coordinates directly
from the constructors.

## Stated but not proved

- `cmpDen` is flip-symmetric or has transitive `lt` on well-formed
  denotators. It is false in general because of NaN (proved above for
  `cmpR`), and for NaN-free reals it needs facts about floating-point
  subtraction that Agda's primitive floats do not expose. So the model
  does not prove that Java-built power denotators are duplicate-free; it
  checks ascending order on each concrete set, and `no-duplicates` gives
  duplicate-freedom for any order whose `lt` is transitive.
- The model's `cmpDen` matches Java's `compareTo` in sign. This rests on
  reading the Java code and on probe P3, P4, P8 and P11, and on the
  generated Score being accepted in the order Java stored it; it is not
  proved against Java.
- `ExportAgda.java` prints the Java values faithfully. The script checks
  that its output is stable, and `registry-agrees` compares it with the
  hand-written forms, but the printer itself is unverified Java.

## Departures from the theory

"Theory" means the cited texts. Where they don't settle a point the row
says so. Probe lines (P1 to P11) are in the log of each run of
`run-checks.sh` (`departure-probe.txt`).

| | Java | Theory | Model | Evidence |
|---|---|---|---|---|
| D1 | `Form.equals` compares names when both forms are registered; `Repository.register` returns null for a different form under a taken name | not checked against the texts; the row records how Java uses names | forms are repository entries named by strings, and `uniqueB` holds | P10; `handEnv-unique`, `javaEnv-unique` |
| D2 | a power denotator is a `TreeMap` keyed by `Denotator.compareTo`: finite, sorted, compareTo-equal elements merged (the first one stays). `PowerDenotator._normalize` is commented out | a set of denotators of the coordinate form; the cited texts give no order | finite list, strictly ascending under `cmpDen` | P3; `insertion-order`, `rej-duplicate`, `rej-unsorted` |
| D3 | denotator names take part in `compareTo`; two notes equal in content but named differently are two set elements | the manual says denotators may be anonymous; the texts don't say names affect set membership | `cmpDen` compares names after forms and before content | P4; `named-later`, `named-distinct` |
| D4 | denotators have addresses (`getAddress`, `nullAddressed`) | denotators are addressed; the zero address is one case | zero address only; the generator refuses others | not modelled |
| D5 | `ZElement.compareTo` returns `value - other` and `Rational.compareTo` cross-multiplies 32-bit ints; both overflow | Z and Q are exact | exact Int and cross-multiplication on unbounded integers | P5, P7; `javaIntegerSet-rejected`, `JavaOverflowSet` |
| D6 | `RElement` is equal within epsilon = 4·Double.MIN_VALUE (settable), NaN compares greater than everything including NaN; RProperFreeElement treats a NaN component difference as equal | R is the real numbers | Agda primitive floats with Java's comparison rules | P6; `cmpR-not-flip-symmetric`, `javaRealSet-rejected`, `RealFlipSym`, `JavaNaNSet` |
| D7 | a List form type (`Yoneda.LIST`), auto-named `_List(X)` | p. 92 of *Perspectives* lists Simple, Syn, Power, Limit and Colimit; no List. The Rubato manual lists Simple, Limit, Colimit, Power and List | `list` body; order kept, repeats allowed | P8; `boolList-ok`, `javaEnvelope-ok` |
| D8 | lists, limits and powers compare element by element, and when one is a prefix of the other the shorter is greater (`bCount - aCount`) | the cited texts give no order | `cmpDens` does the same | P8; `shorter-greater` |
| D9 | `LimitDenotator` pads missing coordinates with default denotators and drops extra ones | a limit denotator has one coordinate per factor | exactly one coordinate per factor; padding happens before the model sees the value | P1, P2; `rej-short-note`, `javaPaddedNote-ok`, `ShortNote` |
| D10 | a colimit index outside the cofactors throws `ArrayIndexOutOfBoundsException`; a factor of the wrong cofactor form makes DenoFactory return null | the index names a cofactor | `nth i Gs ≡ just G` | P9; `rej-colimit-index`, `rej-colimit-cofactor`, `ColimitIndex` |
| D11 | recursive forms through `FormReference`, resolved by name in the repository | not checked against the texts | forms refer to coordinates by name; `closedB` checks every name is defined | `sampleMacro-ok`, `javaMacroScore-ok`, `javaSoundScore-ok` |
| D12 | `CoolFormRegistrant.registerLimitForm` and `registerColimitForm` leave `setLabels` commented out, citing a bug in saving and loading labels to and from XML, so SoundNote and the other sound and music forms have no labels; Repository's Note keeps its labels | not checked against the texts | labels are optional; `labelsOkB` | `registry-agrees`, `javaEnv-labels` |
| D13 | `Form.compareTo`: equal forms 0, otherwise by type number, then by name | the cited texts give no order | `cmpForm` does the same | P11 |
| D14 | `Rational` keeps the denominator positive and reduces by gcd | Q | denominator positive is checked, gcd reduction is not, so the model accepts 2/4 | `rej-q-zero-denominator` |
| D15 | strings compare by UTF-16 code unit | not checked against the texts | code points; the two orders agree unless a character outside the Basic Multilingual Plane meets one from U+E000 to U+FFFF | not tested |
| D16 | `NameDenotator` splits names on "/" into `NameEntry` segments and compares segment lists | not checked against the texts | names are flat strings; correct for names without "/" | not tested |
| D17 | no Syn form type | *Perspectives* lists Syn | no Syn | — |

The texts were read for terminology only. Each "Theory" entry repeats
what the cited source says or says that the sources don't settle the
point. The Topos of Music itself was not consulted.

## CI

`.github/workflows/ci.yml` has an `agda-spec` job: it builds the jar
with `mvn -DskipTests package`, installs `agda-bin` and `libghc-agda-dev`
from Ubuntu 24.04's archive (Agda 2.6.3, the version used here), runs
`run-checks.sh` and uploads its logs. The job has not run on GitHub: the
branch is local. Package names and the prim-library location come from
this VM's notes on the Ubuntu noble packages, not from a runner.

## Untested

- The CI job (above).
- D15 and D16 have no probe.
- Agda versions other than 2.6.3.
