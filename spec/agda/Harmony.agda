{-# OPTIONS --safe --without-K #-}

-- Harmony.agda: finite facts about cadences, modulation quanta and the
-- PLRQ morphism R42 for the C-major scale, proved by computation.
--
-- Sources, by page (no text quoted):
--  * cadential sets: Mazzola, The Topos of Music, vol. I, section 26
--    (Defs. 86-87, pp. 453-461). A set of degrees is cadential when no
--    other interpretation e^p.S (p = 1..11) has all of its chords among
--    its own degree chords; it is minimal when no proper subset is
--    cadential (cadential sets are closed upwards, so it is enough to
--    remove one degree at a time);
--  * modulation quanta: Topos I section 27.1.5 (Def. 88, p. 469) and
--    Agustin-Aquino and Mazzola 2019, Algorithm 1 and Table 1. For a
--    modulator g (a TI map with g(S) = T) and a minimal cadential set of
--    T, the quantum Q is the orbit of the cadence's tones under g, the
--    trace is Q meet T, the trace must be rigid (no TI map other than
--    the identity fixes it), the pivots are the degrees of T whose chords
--    lie in the trace, and the row is "covered" when those chords cover
--    the trace (the rows without an asterisk in Table 1);
--  * R42 and P42: Agustin-Aquino 2026, Blues for Alice, Defs. 3.1 and
--    3.5; the types are numbered as in Cannas, Antonini and Pernazza
--    2017 (1 dominant, 2 minor, 3 half-diminished, 4 major, 5 diminished).
--
-- java/core's org.rubato.math.harmony computes the same values from the
-- scale (CadenceTest, ModulationTest, PlrqTest); here they are written
-- out as data and decided by evaluation.

module Harmony where

open import Prelude

------------------------------------------------------------------------
-- Lists of pitch classes

_++_ : {A : Set} → List A → List A → List A
[]       ++ ys = ys
(x ∷ xs) ++ ys = x ∷ (xs ++ ys)

filter : {A : Set} → (A → Bool) → List A → List A
filter p []       = []
filter p (x ∷ xs) = if p x then x ∷ filter p xs else filter p xs

concatMap : {A B : Set} → (A → List B) → List A → List B
concatMap f []       = []
concatMap f (x ∷ xs) = f x ++ concatMap f xs

mem : Nat → List Nat → Bool
mem x xs = any (λ y → x ==ⁿ y) xs

_⊆ᵇ_ : List Nat → List Nat → Bool
xs ⊆ᵇ ys = all (λ x → mem x ys) xs

sameSet : List Nat → List Nat → Bool
sameSet xs ys = (xs ⊆ᵇ ys) && (ys ⊆ᵇ xs)

eqL : List Nat → List Nat → Bool
eqL []       []       = true
eqL (x ∷ xs) (y ∷ ys) = (x ==ⁿ y) && eqL xs ys
eqL _        _        = false

memL : List Nat → List (List Nat) → Bool
memL s ss = any (eqL s) ss

-- two families of degree sets, each written in ascending order, agree
sameFamily : List (List Nat) → List (List Nat) → Bool
sameFamily xs ys = (length xs ==ⁿ length ys) && all (λ s → memL s ys) xs

remove : Nat → List Nat → List Nat
remove x xs = filter (λ y → not (x ==ⁿ y)) xs

union : List Nat → List Nat → List Nat
union xs ys = xs ++ filter (λ y → not (mem y xs)) ys

subsets : List Nat → List (List Nat)
subsets []       = [] ∷ []
subsets (x ∷ xs) = subsets xs ++ map (x ∷_) (subsets xs)

nonEmpty : List Nat → Bool
nonEmpty []      = false
nonEmpty (_ ∷ _) = true

_⇔ᵇ_ : Bool → Bool → Bool
true  ⇔ᵇ b = b
false ⇔ᵇ b = not b

------------------------------------------------------------------------
-- Z12

mod12 : Nat → Nat
mod12 n = mod-helper 0 11 n 11

tr : Nat → Nat → Nat
tr p x = mod12 (x + p)

-- a TI map: x ↦ u + x, or x ↦ u − x when the flag is set
data TI : Set where
  ti : Nat → Bool → TI

app : TI → Nat → Nat
app (ti u false) x = mod12 (u + x)
app (ti u true)  x = mod12 (u + (12 - mod12 x))

isId : TI → Bool
isId (ti 0 false) = true
isId _            = false

range : Nat → List Nat
range zero    = []
range (suc n) = range n ++ (n ∷ [])

ti12 : List TI
ti12 = map (λ u → ti u false) (range 12) ++ map (λ u → ti u true) (range 12)

image : TI → List Nat → List Nat
image g xs = map (app g) xs

rigid : List Nat → Bool
rigid xs = all (λ h → isId h || not (sameSet (image h xs) xs)) ti12

------------------------------------------------------------------------
-- The C-major scale and its triadic and tetradic degree chords

degrees : List Nat
degrees = 0 ∷ 1 ∷ 2 ∷ 3 ∷ 4 ∷ 5 ∷ 6 ∷ []

scale : List Nat
scale = 0 ∷ 2 ∷ 4 ∷ 5 ∷ 7 ∷ 9 ∷ 11 ∷ []

triad : Nat → List Nat
triad 0 = 0 ∷ 4 ∷ 7 ∷ []      -- I    C E G
triad 1 = 2 ∷ 5 ∷ 9 ∷ []      -- II   D F A
triad 2 = 4 ∷ 7 ∷ 11 ∷ []     -- III  E G B
triad 3 = 5 ∷ 9 ∷ 0 ∷ []      -- IV   F A C
triad 4 = 7 ∷ 11 ∷ 2 ∷ []     -- V    G B D
triad 5 = 9 ∷ 0 ∷ 4 ∷ []      -- VI   A C E
triad 6 = 11 ∷ 2 ∷ 5 ∷ []     -- VII  B D F
triad _ = []

tetrad : Nat → List Nat
tetrad 0 = 0 ∷ 4 ∷ 7 ∷ 11 ∷ []    -- I    C E G B
tetrad 1 = 2 ∷ 5 ∷ 9 ∷ 0 ∷ []     -- II   D F A C
tetrad 2 = 4 ∷ 7 ∷ 11 ∷ 2 ∷ []    -- III  E G B D
tetrad 3 = 5 ∷ 9 ∷ 0 ∷ 4 ∷ []     -- IV   F A C E
tetrad 4 = 7 ∷ 11 ∷ 2 ∷ 5 ∷ []    -- V    G B D F
tetrad 5 = 9 ∷ 0 ∷ 4 ∷ 7 ∷ []     -- VI   A C E G
tetrad 6 = 11 ∷ 2 ∷ 5 ∷ 9 ∷ []    -- VII  B D F A
tetrad _ = []

-- degree d of e^p.S
chordAt : (Nat → List Nat) → Nat → Nat → List Nat
chordAt ch p d = map (tr p) (ch d)

------------------------------------------------------------------------
-- Def. 87: cadential and minimal cadential sets

isDegreeChordOf : (Nat → List Nat) → Nat → List Nat → Bool
isDegreeChordOf ch p c = any (λ d → sameSet c (chordAt ch p d)) degrees

contains : (Nat → List Nat) → Nat → List Nat → Bool
contains ch p μ = all (λ d → isDegreeChordOf ch p (ch d)) μ

cadential : (Nat → List Nat) → List Nat → Bool
cadential ch μ = all (λ p → not (contains ch (suc p) μ)) (range 11)

minimal : (Nat → List Nat) → List Nat → Bool
minimal ch μ = nonEmpty μ && cadential ch μ && all (λ x → not (cadential ch (remove x μ))) μ

minimalSets : (Nat → List Nat) → List (List Nat)
minimalSets ch = filter (minimal ch) (subsets degrees)

-- Topos I p. 459: {VII}, {II,III}, {II,V}, {III,IV}, {IV,V}
triadic-cadences : sameFamily (minimalSets triad)
  ((6 ∷ []) ∷ (1 ∷ 2 ∷ []) ∷ (1 ∷ 4 ∷ []) ∷ (2 ∷ 3 ∷ []) ∷ (3 ∷ 4 ∷ []) ∷ []) ≡ true
triadic-cadences = refl

-- BfA p. 3: J5 {V}, J6 {VII}, J1 {I,II}, J2 {I,IV}, J3 {II,III}, J4 {III,IV}
tetradic-cadences : sameFamily (minimalSets tetrad)
  ((4 ∷ []) ∷ (6 ∷ []) ∷ (0 ∷ 1 ∷ []) ∷ (0 ∷ 3 ∷ []) ∷ (1 ∷ 2 ∷ []) ∷ (2 ∷ 3 ∷ []) ∷ []) ≡ true
tetradic-cadences = refl

-- {II7, V7} is cadential but not minimal ({V7} alone is)
II-V-cadential : cadential tetrad (1 ∷ 4 ∷ []) ≡ true
II-V-cadential = refl

------------------------------------------------------------------------
-- Def. 88 / Algorithm 1: one row of a quantum table

base : (Nat → List Nat) → Nat → List Nat → List Nat
base ch p μ = concatMap (chordAt ch p) μ

orbitN : Nat → TI → List Nat → List Nat
orbitN zero    g xs = xs
orbitN (suc n) g xs = orbitN n g (union xs (image g xs))

quantum : (Nat → List Nat) → Nat → List Nat → TI → List Nat
quantum ch p μ g = orbitN 12 g (base ch p μ)

target : Nat → List Nat
target p = map (tr p) scale

trace : (Nat → List Nat) → Nat → List Nat → TI → List Nat
trace ch p μ g = filter (λ x → mem x (target p)) (quantum ch p μ g)

pivots : (Nat → List Nat) → Nat → List Nat → TI → List Nat
pivots ch p μ g = filter (λ d → chordAt ch p d ⊆ᵇ trace ch p μ g) degrees

covered : (Nat → List Nat) → Nat → List Nat → TI → Bool
covered ch p μ g = trace ch p μ g ⊆ᵇ concatMap (chordAt ch p) (pivots ch p μ g)

-- g is a modulator to e^p.S, the cadence is minimal, the quantum is Q,
-- its trace is rigid, the pivots are piv, and the covering flag is cov
row : (Nat → List Nat) → Nat → List Nat → TI → List Nat → List Nat → Bool → Bool
row ch p μ g Q piv cov =
  sameSet (image g scale) (target p) && minimal ch μ && sameSet (quantum ch p μ g) Q
  && rigid (trace ch p μ g) && eqL (pivots ch p μ g) piv && (covered ch p μ g ⇔ᵇ cov)

-- Schoenberg's C → F with triads (Topos I section 27.1.5): cadence {VII}
-- of F, modulator x ↦ 9 − x, pivots {II, IV, VII}
schoenberg-C-F : row triad 5 (6 ∷ []) (ti 9 true) (2 ∷ 4 ∷ 5 ∷ 7 ∷ 10 ∷ 11 ∷ []) (1 ∷ 3 ∷ 6 ∷ []) true ≡ true
schoenberg-C-F = refl

-- Table 1, Tr1 {V7}: T5∘−1, pivots {III7, V7}
table1-tr1-V : row tetrad 1 (4 ∷ []) (ti 5 true) (0 ∷ 2 ∷ 3 ∷ 5 ∷ 6 ∷ 8 ∷ 9 ∷ 11 ∷ []) (2 ∷ 4 ∷ []) true ≡ true
table1-tr1-V = refl

-- Table 1, Tr1 {VII7}: T5∘−1, pivots {VII7}, marked with an asterisk
-- (the pivot chords do not cover the trace)
table1-tr1-VII : row tetrad 1 (6 ∷ []) (ti 5 true) (0 ∷ 2 ∷ 3 ∷ 5 ∷ 6 ∷ 7 ∷ 10 ∷ 11 ∷ []) (6 ∷ []) false ≡ true
table1-tr1-VII = refl

-- Table 1, Tr7 {V7}: T11∘−1, pivots {III7, V7}
table1-tr7-V : row tetrad 7 (4 ∷ []) (ti 11 true) (0 ∷ 2 ∷ 5 ∷ 6 ∷ 9 ∷ 11 ∷ []) (2 ∷ 4 ∷ []) true ≡ true
table1-tr7-V = refl

-- Tr3 {VII7} with T3: quantized and covered under Def. 88 and
-- Algorithm 1 as printed, but absent from Table 1 (HARMONY-SPEC 7.2)
omitted-tr3-VII : row tetrad 3 (6 ∷ []) (ti 3 false) (0 ∷ 2 ∷ 3 ∷ 5 ∷ 6 ∷ 8 ∷ 9 ∷ 11 ∷ []) (1 ∷ 6 ∷ []) true ≡ true
omitted-tr3-VII = refl

------------------------------------------------------------------------
-- Seventh chords as (type, root), R42, P42 and T^s

record Chord : Set where
  constructor chord
  field
    type : Nat
    root : Nat

eqChord : Chord → Chord → Bool
eqChord (chord a x) (chord b y) = (a ==ⁿ b) && (x ==ⁿ y)

allChords : List Chord
allChords = concatMap' (1 ∷ 2 ∷ 3 ∷ 4 ∷ 5 ∷ [])
  where
    concatMap' : List Nat → List Chord
    concatMap' []       = []
    concatMap' (t ∷ ts) = map (chord t) (range 12) ++ concatMap' ts

-- BfA Def. 3.1: M(x) ↔ m(x − 3), the vector (0, 3, 0, −3, 0)
R42 : Chord → Chord
R42 (chord 4 x) = chord 2 (mod12 (x + 9))
R42 (chord 2 x) = chord 4 (mod12 (x + 3))
R42 c           = c

-- BfA Def. 3.5: M(x) ↔ m(x)
P42 : Chord → Chord
P42 (chord 4 x) = chord 2 x
P42 (chord 2 x) = chord 4 x
P42 c           = c

T : Nat → Chord → Chord
T s (chord t x) = chord t (mod12 (x + s))

offsets : Nat → List Nat
offsets 1 = 0 ∷ 4 ∷ 7 ∷ 10 ∷ []
offsets 2 = 0 ∷ 3 ∷ 7 ∷ 10 ∷ []
offsets 3 = 0 ∷ 3 ∷ 6 ∷ 10 ∷ []
offsets 4 = 0 ∷ 4 ∷ 7 ∷ 11 ∷ []
offsets 5 = 0 ∷ 3 ∷ 6 ∷ 9 ∷ []
offsets _ = []

tones : Chord → List Nat
tones (chord t x) = map (tr x) (offsets t)

R42-involution : all (λ c → eqChord (R42 (R42 c)) c) allChords ≡ true
R42-involution = refl

R42-commutes-T5 : all (λ c → eqChord (R42 (T 5 c)) (T 5 (R42 c))) allChords ≡ true
R42-commutes-T5 = refl

-- R42(IV7) = II7 in C: Fmaj7 ↦ Dm7, and the tones are the degree tetrads
R42-IV-II : (eqChord (R42 (chord 4 5)) (chord 2 2) && sameSet (tones (chord 4 5)) (tetrad 3)
            && sameSet (tones (chord 2 2)) (tetrad 1)) ≡ true
R42-IV-II = refl

-- P42(I7 of K) = II7 of K − 2, for all twelve keys (II7 of a key k is the
-- minor seventh on k + 2)
P42-I-II : all (λ K → eqChord (P42 (chord 4 K)) (chord 2 (mod12 (mod12 (K + 10) + 2)))) (range 12) ≡ true
P42-I-II = refl
