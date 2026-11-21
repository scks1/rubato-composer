{-# OPTIONS --safe --without-K #-}

-- Registry.agda: the music forms written out by hand from the Java
-- sources, and the properties the spec proves about them.
--
-- Sources:
--   Repository.initGlobalRepository (java/core/src/org/rubato/base/Repository.java)
--     Integer, Real, Boolean, BooleanList, IntegerOrReal, IntegerSet,
--     RealSet, Onset, Pitch, Loudness, Duration, Voice, Note, Score,
--     Knot, MacroScore
--   CoolFormRegistrant.registerSoundForms
--   (java/src/org/rubato/rubettes/util/CoolFormRegistrant.java)
--     Pan, Partial, SoundNote, Modulators, SoundNode, SoundScore
--
-- GeneratedChecks.agda proves that the forms the running Java code
-- registers are these.

module Registry where

open import Prelude
open import OrderedSets
open import Forms

handEnv : Env
handEnv =
  "Integer"       ≔ simple ZM ∷
  "Real"          ≔ simple RM ∷
  "Boolean"       ≔ simple (ZnM 2) ∷
  "BooleanList"   ≔ list "Boolean" ∷
  "IntegerOrReal" ≔ colimit ("Integer" ∷ "Real" ∷ []) [] ∷
  "IntegerSet"    ≔ power "Integer" ∷
  "RealSet"       ≔ power "Real" ∷
  "Onset"         ≔ simple RM ∷
  "Pitch"         ≔ simple QM ∷
  "Loudness"      ≔ simple ZM ∷
  "Duration"      ≔ simple RM ∷
  "Voice"         ≔ simple ZM ∷
  "Note"          ≔ limit ("Onset" ∷ "Pitch" ∷ "Loudness" ∷ "Duration" ∷ "Voice" ∷ [])
                          ("onset" ∷ "pitch" ∷ "loudness" ∷ "duration" ∷ "voice" ∷ []) ∷
  "Score"         ≔ power "Note" ∷
  "Knot"          ≔ limit ("Note" ∷ "MacroScore" ∷ []) [] ∷
  "MacroScore"    ≔ power "Knot" ∷
  "Pan"           ≔ simple RM ∷
  "Partial"       ≔ limit ("Loudness" ∷ "Pitch" ∷ "Pan" ∷ []) [] ∷
  -- registered without labels: CoolFormRegistrant leaves setLabels
  -- commented out, citing a bug in saving labels to XML (D12)
  "SoundNote"     ≔ limit ("Onset" ∷ "Pitch" ∷ "Loudness" ∷ "Duration" ∷ "Voice" ∷ "Modulators" ∷ []) [] ∷
  "Modulators"    ≔ power "SoundNote" ∷
  "SoundNode"     ≔ limit ("SoundNote" ∷ "SoundScore" ∷ []) [] ∷
  "SoundScore"    ≔ power "SoundNode" ∷
  []

------------------------------------------------------------------------
-- Repository invariants

handEnv-unique : uniqueB (namesOf handEnv) ≡ true
handEnv-unique = refl

handEnv-closed : closedB handEnv ≡ true
handEnv-closed = refl

handEnv-labels : labelsOkB handEnv ≡ true
handEnv-labels = refl

------------------------------------------------------------------------
-- Note and SoundNote

note-has-5-factors : factorCount handEnv "Note" ≡ just 5
note-has-5-factors = refl

note-labels : lookup handEnv "Note" ≡
  just (limit ("Onset" ∷ "Pitch" ∷ "Loudness" ∷ "Duration" ∷ "Voice" ∷ [])
              ("onset" ∷ "pitch" ∷ "loudness" ∷ "duration" ∷ "voice" ∷ []))
note-labels = refl

soundNote-has-6-factors : factorCount handEnv "SoundNote" ≡ just 6
soundNote-has-6-factors = refl

-- Upstream commit 376ff94 (2014) removed Layer from SoundNote; the
-- known-bugs commit 9ffdfc9 made SoundNoteGenerator follow it.
soundNote-factor-5 : nth 5 ("Onset" ∷ "Pitch" ∷ "Loudness" ∷ "Duration" ∷ "Voice" ∷ "Modulators" ∷ []) ≡ just "Modulators"
soundNote-factor-5 = refl

no-layer-form : lookup handEnv "Layer" ≡ nothing
no-layer-form = refl

------------------------------------------------------------------------
-- Sample denotators

onset pitch' loud dur voice' : Float → Den
onset   x = simpleD nothing "Onset" (rE x)
pitch'  x = simpleD nothing "Pitch" (qE (pos 60) 1)
loud    x = simpleD nothing "Loudness" (zE (pos 80))
dur     x = simpleD nothing "Duration" (rE x)
voice'  x = simpleD nothing "Voice" (zE (pos 0))

mkNote : Float → Int → Den
mkNote o p = limitD nothing "Note"
  (simpleD nothing "Onset" (rE o) ∷
   simpleD nothing "Pitch" (qE p 1) ∷
   simpleD nothing "Loudness" (zE (pos 80)) ∷
   simpleD nothing "Duration" (rE 1.0) ∷
   simpleD nothing "Voice" (zE (pos 0)) ∷ [])

n0 n1 n2 : Den
n0 = mkNote 0.0 (pos 60)
n1 = mkNote 1.0 (pos 64)
n2 = mkNote 2.0 (pos 67)

sampleScore : Den
sampleScore = powerD nothing "Score" (n0 ∷ n1 ∷ n2 ∷ [])

sampleScore-ok : handEnv ⊢ sampleScore ∶ "Score"
sampleScore-ok = sound handEnv sampleScore "Score" refl

-- A MacroScore whose first knot carries a satellite note (recursive form).
sampleMacro : Den
sampleMacro = powerD nothing "MacroScore"
  (limitD nothing "Knot" (n0 ∷ powerD nothing "MacroScore"
     (limitD nothing "Knot" (n1 ∷ powerD nothing "MacroScore" [] ∷ []) ∷ []) ∷ []) ∷ [])

sampleMacro-ok : handEnv ⊢ sampleMacro ∶ "MacroScore"
sampleMacro-ok = sound handEnv sampleMacro "MacroScore" refl

-- A SoundNote with one modulator, itself a SoundNote without modulators.
mkSoundNote : Float → List Den → Den
mkSoundNote o mods = limitD nothing "SoundNote"
  (simpleD nothing "Onset" (rE o) ∷
   simpleD nothing "Pitch" (qE (pos 60) 1) ∷
   simpleD nothing "Loudness" (zE (pos 100)) ∷
   simpleD nothing "Duration" (rE 1.0) ∷
   simpleD nothing "Voice" (zE (pos 0)) ∷
   powerD nothing "Modulators" mods ∷ [])

sampleSoundScore : Den
sampleSoundScore = powerD nothing "SoundScore"
  (limitD nothing "SoundNode" (mkSoundNote 0.0 (mkSoundNote 0.0 [] ∷ []) ∷ powerD nothing "SoundScore" [] ∷ []) ∷ [])

sampleSoundScore-ok : handEnv ⊢ sampleSoundScore ∶ "SoundScore"
sampleSoundScore-ok = sound handEnv sampleSoundScore "SoundScore" refl

-- A colimit denotator, and the empty set and list
sampleIntOrReal : handEnv ⊢ colimitD nothing "IntegerOrReal" 1 (simpleD nothing "Real" (rE 0.5)) ∶ "IntegerOrReal"
sampleIntOrReal = sound _ _ _ refl

emptyScore-ok : handEnv ⊢ powerD nothing "Score" [] ∶ "Score"
emptyScore-ok = sound _ _ _ refl

-- A list keeps order and repeats.
boolList-ok : handEnv ⊢ listD nothing "BooleanList"
                (simpleD nothing "Boolean" (znE 2 1) ∷ simpleD nothing "Boolean" (znE 2 1) ∷
                 simpleD nothing "Boolean" (znE 2 0) ∷ []) ∶ "BooleanList"
boolList-ok = sound _ _ _ refl

------------------------------------------------------------------------
-- What the decision procedure rejects (each is a computation to false)

-- a Note with four coordinates: Java pads it (D9), the spec does not
rej-short-note : check handEnv (limitD nothing "Note" (onset 0.0 ∷ pitch' 0.0 ∷ loud 0.0 ∷ dur 1.0 ∷ [])) "Note" ≡ false
rej-short-note = refl

-- a coordinate of the wrong form
rej-wrong-coordinate : check handEnv (limitD nothing "Note" (onset 0.0 ∷ onset 0.0 ∷ loud 0.0 ∷ dur 1.0 ∷ voice' 0.0 ∷ [])) "Note" ≡ false
rej-wrong-coordinate = refl

-- a colimit index outside the cofactors: Java throws (D10)
rej-colimit-index : check handEnv (colimitD nothing "IntegerOrReal" 5 (simpleD nothing "Integer" (zE (pos 1)))) "IntegerOrReal" ≡ false
rej-colimit-index = refl

-- the right index with the other cofactor's form
rej-colimit-cofactor : check handEnv (colimitD nothing "IntegerOrReal" 1 (simpleD nothing "Integer" (zE (pos 1)))) "IntegerOrReal" ≡ false
rej-colimit-cofactor = refl

-- a set that repeats an element
rej-duplicate : check handEnv (powerD nothing "Score" (n0 ∷ n0 ∷ [])) "Score" ≡ false
rej-duplicate = refl

-- a set out of TreeMap order
rej-unsorted : check handEnv (powerD nothing "Score" (n1 ∷ n0 ∷ [])) "Score" ≡ false
rej-unsorted = refl

-- elements outside their module
rej-zn : check handEnv (simpleD nothing "Boolean" (znE 2 2)) "Boolean" ≡ false
rej-zn = refl

rej-q-zero-denominator : check handEnv (simpleD nothing "Pitch" (qE (pos 1) 0)) "Pitch" ≡ false
rej-q-zero-denominator = refl

rej-real-for-integer : check handEnv (simpleD nothing "Loudness" (rE 80.0)) "Loudness" ≡ false
rej-real-for-integer = refl

-- a form that is not registered
rej-layer : check handEnv (simpleD nothing "Layer" (zE (pos 0))) "Layer" ≡ false
rej-layer = refl

------------------------------------------------------------------------
-- Java's order

-- PowerDenotator construction sorts and drops compareTo-equal elements:
-- inserting n2, n0, n2 stores n0, n2 (probe P3).
insertion-order : fromList cmpDen (n2 ∷ n0 ∷ n2 ∷ []) ≡ n0 ∷ n2 ∷ []
insertion-order = refl

-- Names come before content: an anonymous note sorts before a named
-- note with an earlier onset (D3, probe P4).
named-later : cmpDen n2 (limitD (just "a") "Note" (onset 0.0 ∷ pitch' 0.0 ∷ loud 0.0 ∷ dur 1.0 ∷ voice' 0.0 ∷ [])) ≡ lt
named-later = refl

-- Two notes that differ only in their names are two set elements.
named-distinct : check handEnv (powerD nothing "Score" (n0 ∷ limitD (just "b") "Note" (onset 0.0 ∷ pitch' 0.0 ∷ loud 0.0 ∷ dur 1.0 ∷ voice' 0.0 ∷ []) ∷ [])) "Score" ≡ true
named-distinct = refl

-- When one list is a prefix of the other the shorter is greater (D8).
shorter-greater : cmpDens (n0 ∷ []) (n0 ∷ n1 ∷ []) ≡ gt
shorter-greater = refl

-- RElement.compareTo with NaN: greater than everything, itself included,
-- so the order is not flip-symmetric and the sorted-set argument below
-- does not apply to reals (D6, probe P6).
nanF : Float
nanF = primFloatDiv 0.0 0.0

nan-not-reflexive : cmpR nanF nanF ≡ gt
nan-not-reflexive = refl

nan-above-one : cmpR nanF 1.0 ≡ gt
nan-above-one = refl

one-above-nan : cmpR 1.0 nanF ≡ gt
one-above-nan = refl

cmpR-not-flip-symmetric : ¬ FlipSym cmpR
cmpR-not-flip-symmetric s = gt≢lt (s nanF 1.0)
  where gt≢lt : gt ≡ lt → ⊥
        gt≢lt ()

-- Signed zeros compare equal.
signed-zero : cmpR 0.0 (primFloatNegate 0.0) ≡ eq
signed-zero = refl

-- The epsilon is 4 × 2^-1074, printed by Java as 2.0E-323 (probe P6).
epsilon-positive : primFloatLess 0.0 epsR ≡ true
epsilon-positive = refl

-- Exact integer order is flip-symmetric, so building an IntegerSet from
-- any list gives an ascending list, by the general lemma; in Java the
-- int subtraction overflows instead (D5).
integerSet-sorted : (xs : List Int) → Asc cmpInt (fromList cmpInt xs)
integerSet-sorted = fromList-asc cmpInt cmpInt-flip
