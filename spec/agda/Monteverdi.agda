{-# OPTIONS --safe --without-K #-}

-- Monteverdi.agda: finite facts behind stage 7, proved by computation.
--
-- Sources, by page (no text quoted):
--  * Agustin-Aquino and Curiel-Lopez, An Algebraic-Symmetric Analysis of
--    Monteverdi (arXiv 2512.18862): the symmetry tables of section 5.1
--    (pp. 8-11) and Examples 1-3 of section 4.1 (pp. 5-6);
--  * contrapuntal symmetries: Agustin-Aquino, Junod and Mazzola,
--    Computational Counterpoint Worlds (CCW), Defs. 2.5-2.6 (pp. 14-15)
--    and the reduction to H = {e^{eps.t} . (u + eps.v)} (Thm. 2.2, p. 19).
--
-- Part 1 computes, for each consonance k of Fux's dichotomy (K/D), the
-- contrapuntal symmetries of eps.k in H (576 maps), and reads each table
-- row as MONTEVERDI-SPEC reading D does: the symmetries g of eps.k_i whose
-- deformed consonances gK[eps] contain (x_{i+1} - x_i) + eps.k_{i+1}.
-- Part 2 checks the three quanta with Harmony.agda's `row`, their
-- invariance under the modulator, and the unique polarity of the traces of
-- Examples 1 and 3. java/core's SymmetryTrace, Modulations and Dichotomy
-- compute the same values (SymmetryTraceTest, KeyPathTest).

module Monteverdi where

open import Prelude
open import Harmony
import Counterpoint as C

------------------------------------------------------------------------
-- Part 1: Z12[eps] and the group H

-- K = {0, 3, 4, 7, 8, 9}
K : List Nat
K = 0 ∷ 3 ∷ 4 ∷ 7 ∷ 8 ∷ 9 ∷ []

-- e^{eps.t} . (u + eps.v) : x + eps.y |-> u x + eps.(u y + v x + t)
record HSym : Set where
  constructor h
  field
    t u v : Nat

eqH : HSym → HSym → Bool
eqH (h a b c) (h d e f) = (a ==ⁿ d) && (b ==ⁿ e) && (c ==ⁿ f)

units : List Nat
units = 1 ∷ 5 ∷ 7 ∷ 11 ∷ []

hgroup : List HSym
hgroup = concatMap (λ t → concatMap (λ u → map (h t u) (range 12)) units) (range 12)

-- x + eps.y lies in gK[eps]: its preimage under g has a consonant interval.
-- Every unit of Z12 is its own inverse, so the preimage of (x, y) is
-- (u x, u (y - t - v u x)).
inGK : HSym → Nat → Nat → Bool
inGK (h t u v) x y = mem (mod12 (u * mod12 (y + 12 - mod12 (t + v * mod12 (u * x))))) K

-- the induced polarity at cantus 0 of K's polarity 5x + 2: (5x, 5y + 2)
polarOK : HSym → Bool
polarOK g = all (λ x → all (λ y → inGK g (mod12 (5 * x)) (mod12 (5 * y + 2)) ⇔ᵇ not (inGK g x y)) (range 12)) (range 12)

sumN : List Nat → Nat
sumN []       = 0
sumN (n ∷ ns) = n + sumN ns

-- |gK[eps] ∩ K[eps]|
meet : HSym → Nat
meet g = sumN (map (λ x → C.count (λ y → inGK g x y) K) (range 12))

-- the maximum of a list; the accumulator is passed on through a match on
-- the comparison, so no recursive call is duplicated (Agda's evaluator
-- does not share a let-bound value)
maxOf : List Nat → Nat
maxOf ns = go 0 ns
  where
    go : Nat → List Nat → Nat
    step : Bool → Nat → Nat → List Nat → Nat
    go acc []       = acc
    go acc (n ∷ ns) = step (acc <ᵇ n) acc n ns
    step true  _   n ns = go n ns
    step false acc _ ns = go acc ns

memH : HSym → List HSym → Bool
memH g gs = any (eqH g) gs

sameHs : List HSym → List HSym → Bool
sameHs xs ys = (length xs ==ⁿ length ys) && all (λ g → memH g ys) xs

-- the maps of H whose deformed dichotomy has the induced polarity at 0
-- (192 of 576); `candidates-exact` re-derives the list from H
CANDS : List HSym
CANDS =
  h 0 1 0 ∷ h 0 1 1 ∷ h 0 1 2 ∷ h 0 1 3 ∷ h 0 1 4 ∷ h 0 1 5 ∷ h 0 1 6 ∷ h 0 1 7 ∷
  h 0 1 8 ∷ h 0 1 9 ∷ h 0 1 10 ∷ h 0 1 11 ∷ h 0 7 0 ∷ h 0 7 1 ∷ h 0 7 2 ∷ h 0 7 3 ∷
  h 0 7 4 ∷ h 0 7 5 ∷ h 0 7 6 ∷ h 0 7 7 ∷ h 0 7 8 ∷ h 0 7 9 ∷ h 0 7 10 ∷ h 0 7 11 ∷
  h 2 5 0 ∷ h 2 5 1 ∷ h 2 5 2 ∷ h 2 5 3 ∷ h 2 5 4 ∷ h 2 5 5 ∷ h 2 5 6 ∷ h 2 5 7 ∷
  h 2 5 8 ∷ h 2 5 9 ∷ h 2 5 10 ∷ h 2 5 11 ∷ h 2 11 0 ∷ h 2 11 1 ∷ h 2 11 2 ∷ h 2 11 3 ∷
  h 2 11 4 ∷ h 2 11 5 ∷ h 2 11 6 ∷ h 2 11 7 ∷ h 2 11 8 ∷ h 2 11 9 ∷ h 2 11 10 ∷ h 2 11 11 ∷
  h 3 1 0 ∷ h 3 1 1 ∷ h 3 1 2 ∷ h 3 1 3 ∷ h 3 1 4 ∷ h 3 1 5 ∷ h 3 1 6 ∷ h 3 1 7 ∷
  h 3 1 8 ∷ h 3 1 9 ∷ h 3 1 10 ∷ h 3 1 11 ∷ h 3 7 0 ∷ h 3 7 1 ∷ h 3 7 2 ∷ h 3 7 3 ∷
  h 3 7 4 ∷ h 3 7 5 ∷ h 3 7 6 ∷ h 3 7 7 ∷ h 3 7 8 ∷ h 3 7 9 ∷ h 3 7 10 ∷ h 3 7 11 ∷
  h 5 5 0 ∷ h 5 5 1 ∷ h 5 5 2 ∷ h 5 5 3 ∷ h 5 5 4 ∷ h 5 5 5 ∷ h 5 5 6 ∷ h 5 5 7 ∷
  h 5 5 8 ∷ h 5 5 9 ∷ h 5 5 10 ∷ h 5 5 11 ∷ h 5 11 0 ∷ h 5 11 1 ∷ h 5 11 2 ∷ h 5 11 3 ∷
  h 5 11 4 ∷ h 5 11 5 ∷ h 5 11 6 ∷ h 5 11 7 ∷ h 5 11 8 ∷ h 5 11 9 ∷ h 5 11 10 ∷ h 5 11 11 ∷
  h 6 1 0 ∷ h 6 1 1 ∷ h 6 1 2 ∷ h 6 1 3 ∷ h 6 1 4 ∷ h 6 1 5 ∷ h 6 1 6 ∷ h 6 1 7 ∷
  h 6 1 8 ∷ h 6 1 9 ∷ h 6 1 10 ∷ h 6 1 11 ∷ h 6 7 0 ∷ h 6 7 1 ∷ h 6 7 2 ∷ h 6 7 3 ∷
  h 6 7 4 ∷ h 6 7 5 ∷ h 6 7 6 ∷ h 6 7 7 ∷ h 6 7 8 ∷ h 6 7 9 ∷ h 6 7 10 ∷ h 6 7 11 ∷
  h 8 5 0 ∷ h 8 5 1 ∷ h 8 5 2 ∷ h 8 5 3 ∷ h 8 5 4 ∷ h 8 5 5 ∷ h 8 5 6 ∷ h 8 5 7 ∷
  h 8 5 8 ∷ h 8 5 9 ∷ h 8 5 10 ∷ h 8 5 11 ∷ h 8 11 0 ∷ h 8 11 1 ∷ h 8 11 2 ∷ h 8 11 3 ∷
  h 8 11 4 ∷ h 8 11 5 ∷ h 8 11 6 ∷ h 8 11 7 ∷ h 8 11 8 ∷ h 8 11 9 ∷ h 8 11 10 ∷ h 8 11 11 ∷
  h 9 1 0 ∷ h 9 1 1 ∷ h 9 1 2 ∷ h 9 1 3 ∷ h 9 1 4 ∷ h 9 1 5 ∷ h 9 1 6 ∷ h 9 1 7 ∷
  h 9 1 8 ∷ h 9 1 9 ∷ h 9 1 10 ∷ h 9 1 11 ∷ h 9 7 0 ∷ h 9 7 1 ∷ h 9 7 2 ∷ h 9 7 3 ∷
  h 9 7 4 ∷ h 9 7 5 ∷ h 9 7 6 ∷ h 9 7 7 ∷ h 9 7 8 ∷ h 9 7 9 ∷ h 9 7 10 ∷ h 9 7 11 ∷
  h 11 5 0 ∷ h 11 5 1 ∷ h 11 5 2 ∷ h 11 5 3 ∷ h 11 5 4 ∷ h 11 5 5 ∷ h 11 5 6 ∷ h 11 5 7 ∷
  h 11 5 8 ∷ h 11 5 9 ∷ h 11 5 10 ∷ h 11 5 11 ∷ h 11 11 0 ∷ h 11 11 1 ∷ h 11 11 2 ∷ h 11 11 3 ∷
  h 11 11 4 ∷ h 11 11 5 ∷ h 11 11 6 ∷ h 11 11 7 ∷ h 11 11 8 ∷ h 11 11 9 ∷ h 11 11 10 ∷ h 11 11 11 ∷ []

candidates-exact : sameHs (filter polarOK hgroup) CANDS ≡ true
candidates-exact = refl

-- Def. 2.5 in H: eps.k outside gK[eps], g in CANDS (the induced polarity
-- exchanges gK[eps] and its complement), and gK[eps] meets K[eps] maximally
admissible : Nat → List HSym
admissible k = filter (λ g → not (inGK g 0 k)) CANDS

-- the admissible maps whose meet with K[eps] has n points; with n the
-- maximum (facts best-k) these are the contrapuntal symmetries of eps.k
symsWith : Nat → Nat → List HSym
symsWith k n = filter (λ g → meet g ==ⁿ n) (admissible k)

best-0 : maxOf (map meet (admissible 0)) ≡ 48
best-0 = refl

best-3 : maxOf (map meet (admissible 3)) ≡ 56
best-3 = refl

best-4 : maxOf (map meet (admissible 4)) ≡ 48
best-4 = refl

best-7 : maxOf (map meet (admissible 7)) ≡ 60
best-7 = refl

best-8 : maxOf (map meet (admissible 8)) ≡ 48
best-8 = refl

best-9 : maxOf (map meet (admissible 9)) ≡ 56
best-9 = refl

-- the symmetries of each consonance, as SymmetryTrace and the paper name them
S0 S3 S4 S7 S8 S9 : List HSym
S0 = h 6 1 6 ∷ h 6 7 6 ∷ h 11 11 0 ∷ h 11 11 4 ∷ h 11 11 8 ∷ []
S3 = h 8 5 4 ∷ h 8 5 8 ∷ []
S4 = h 6 1 6 ∷ h 6 7 6 ∷ []
S7 = h 0 7 0 ∷ []
S8 = h 3 7 0 ∷ h 3 7 4 ∷ h 3 7 8 ∷ h 6 1 6 ∷ h 6 7 6 ∷ []
S9 = h 8 5 4 ∷ h 8 5 8 ∷ []

syms-0 : sameHs (symsWith 0 48) S0 ≡ true
syms-0 = refl

syms-3 : sameHs (symsWith 3 56) S3 ≡ true
syms-3 = refl

syms-4 : sameHs (symsWith 4 48) S4 ≡ true
syms-4 = refl

syms-7 : sameHs (symsWith 7 60) S7 ≡ true
syms-7 = refl

syms-8 : sameHs (symsWith 8 48) S8 ≡ true
syms-8 = refl

syms-9 : sameHs (symsWith 9 56) S9 ≡ true
syms-9 = refl

S : Nat → List HSym
S 0 = S0
S 3 = S3
S 4 = S4
S 7 = S7
S 8 = S8
S 9 = S9
S _ = []

-- a sequence of intervals x + eps.y and the cardinalities of its steps
data Iv : Set where
  iv : Nat → Nat → Iv

stepSet : Iv → Iv → List HSym
stepSet (iv x1 k1) (iv x2 k2) = filter (λ g → inGK g (mod12 (x2 + 12 - x1)) k2) (S k1)

cards : List Iv → List Nat
cards (a ∷ b ∷ rest) = length (stepSet a b) ∷ cards (b ∷ rest)
cards _              = []

eqNs : List Nat → List Nat → Bool
eqNs = eqL

confitebor ma-tu io-mi-son laudate gloria-8-6 gloria-10-8 : List Iv
confitebor  = iv 0 7 ∷ iv 0 4 ∷ iv 10 7 ∷ iv 10 4 ∷ iv 9 7 ∷ iv 9 3 ∷ iv 7 7 ∷ iv 7 4 ∷ iv 5 7 ∷ []
ma-tu       = iv 5 7 ∷ iv 5 4 ∷ iv 3 7 ∷ iv 3 4 ∷ iv 2 7 ∷ []
io-mi-son   = iv 7 7 ∷ iv 7 3 ∷ iv 5 7 ∷ iv 5 4 ∷ iv 3 7 ∷ iv 3 4 ∷ iv 2 7 ∷ iv 2 3 ∷ iv 0 7 ∷ []
laudate     = iv 2 0 ∷ iv 2 9 ∷ iv 0 0 ∷ iv 0 9 ∷ iv 11 0 ∷ iv 11 8 ∷ iv 9 0 ∷ iv 9 8 ∷ iv 7 0 ∷ iv 7 9 ∷ []
gloria-8-6  = iv 2 0 ∷ iv 2 9 ∷ iv 0 0 ∷ iv 0 9 ∷ iv 11 0 ∷ iv 11 8 ∷ iv 9 0 ∷ iv 9 8 ∷ iv 7 0 ∷ []
gloria-10-8 = iv 11 3 ∷ iv 11 0 ∷ iv 9 3 ∷ iv 9 0 ∷ iv 7 4 ∷ []

-- pp. 8-11: the cardinality columns, 41 rows in all
cards-confitebor : eqNs (cards confitebor) (1 ∷ 1 ∷ 1 ∷ 1 ∷ 1 ∷ 1 ∷ 1 ∷ 1 ∷ []) ≡ true
cards-confitebor = refl

cards-ma-tu : eqNs (cards ma-tu) (1 ∷ 1 ∷ 1 ∷ 1 ∷ []) ≡ true
cards-ma-tu = refl

cards-io-mi-son : eqNs (cards io-mi-son) (1 ∷ 1 ∷ 1 ∷ 1 ∷ 1 ∷ 1 ∷ 1 ∷ 1 ∷ []) ≡ true
cards-io-mi-son = refl

cards-laudate : eqNs (cards laudate) (2 ∷ 2 ∷ 2 ∷ 2 ∷ 3 ∷ 2 ∷ 3 ∷ 2 ∷ 2 ∷ []) ≡ true
cards-laudate = refl

cards-gloria-8-6 : eqNs (cards gloria-8-6) (2 ∷ 2 ∷ 2 ∷ 2 ∷ 3 ∷ 2 ∷ 3 ∷ 2 ∷ []) ≡ true
cards-gloria-8-6 = refl

cards-gloria-10-8 : eqNs (cards gloria-10-8) (2 ∷ 5 ∷ 2 ∷ 2 ∷ []) ≡ true
cards-gloria-10-8 = refl

-- finding W1: the 0 -> 9 steps of Laudate (rows 1, 3, 9) have two
-- symmetries, the 0 -> 8 steps (rows 5, 7) three
laudate-W1 : eqNs (length (stepSet (iv 2 0) (iv 2 9)) ∷ length (stepSet (iv 11 0) (iv 11 8)) ∷ []) (2 ∷ 3 ∷ []) ≡ true
laudate-W1 = refl

------------------------------------------------------------------------
-- Part 2: the quanta of Examples 1-3 (section 4.1, pp. 5-6)

-- Ex. 1, C -> D: cadence k4 = {IV,V} of D, modulator T6 . -1,
-- pivots II, IV, V, VII
ex1 : row triad 2 (3 ∷ 4 ∷ []) (ti 6 true) (1 ∷ 2 ∷ 4 ∷ 5 ∷ 7 ∷ 9 ∷ 11 ∷ []) (1 ∷ 3 ∷ 4 ∷ 6 ∷ []) true ≡ true
ex1 = refl

-- Ex. 2, C -> G: cadence k5 = {VII} of G, modulator T11 . -1,
-- pivots III, V, VII
ex2 : row triad 7 (6 ∷ []) (ti 11 true) (0 ∷ 2 ∷ 5 ∷ 6 ∷ 9 ∷ 11 ∷ []) (2 ∷ 4 ∷ 6 ∷ []) true ≡ true
ex2 = refl

-- Ex. 3, C -> E-flat: cadence k1 = {II,V} of E-flat, modulator T7 . -1,
-- pivots II, III, V, VII
ex3 : row triad 3 (1 ∷ 4 ∷ []) (ti 7 true) (0 ∷ 2 ∷ 5 ∷ 7 ∷ 8 ∷ 9 ∷ 10 ∷ 11 ∷ []) (1 ∷ 2 ∷ 4 ∷ 6 ∷ []) true ≡ true
ex3 = refl

-- invariance: each quantum is mapped onto itself by its modulator
invariance : (sameSet (image (ti 6 true) (quantum triad 2 (3 ∷ 4 ∷ []) (ti 6 true))) (quantum triad 2 (3 ∷ 4 ∷ []) (ti 6 true))
           && sameSet (image (ti 11 true) (quantum triad 7 (6 ∷ []) (ti 11 true))) (quantum triad 7 (6 ∷ []) (ti 11 true))
           && sameSet (image (ti 7 true) (quantum triad 3 (1 ∷ 4 ∷ []) (ti 7 true))) (quantum triad 3 (1 ∷ 4 ∷ []) (ti 7 true))) ≡ true
invariance = refl

-- rigidity: only the identity of TI fixes each trace (also inside `row`)
rigidity : (rigid (trace triad 2 (3 ∷ 4 ∷ []) (ti 6 true)) && rigid (trace triad 7 (6 ∷ []) (ti 11 true))
           && rigid (trace triad 3 (1 ∷ 4 ∷ []) (ti 7 true))) ≡ true
rigidity = refl

-- the traces, as Modulations.quantize gives them
traces : (sameSet (trace triad 2 (3 ∷ 4 ∷ []) (ti 6 true)) (1 ∷ 2 ∷ 4 ∷ 7 ∷ 9 ∷ 11 ∷ [])
         && sameSet (trace triad 7 (6 ∷ []) (ti 11 true)) (0 ∷ 2 ∷ 6 ∷ 9 ∷ 11 ∷ [])
         && sameSet (trace triad 3 (1 ∷ 4 ∷ []) (ti 7 true)) (0 ∷ 2 ∷ 5 ∷ 7 ∷ 8 ∷ 10 ∷ [])) ≡ true
traces = refl

-- unique polarity: the six-tone traces of Ex. 1 and Ex. 3 are strong
-- dichotomies; exactly one of the 48 affine maps exchanges each with its
-- complement, 11x + 7 and 11x + 11 (x |-> 7 - x and x |-> 11 - x)
X1 X3 : List Nat
X1 = 1 ∷ 2 ∷ 4 ∷ 7 ∷ 9 ∷ 11 ∷ []
X3 = 0 ∷ 2 ∷ 5 ∷ 7 ∷ 8 ∷ 10 ∷ []

unique-polarity : (C.strong X1 && C.complements X1 (C.aff 7 11)
                   && C.strong X3 && C.complements X3 (C.aff 11 11)) ≡ true
unique-polarity = refl

-- finding W3: Ex. 2's trace has five tones, so it is no dichotomy of Z12
ex2-five : length (trace triad 7 (6 ∷ []) (ti 11 true)) ≡ 5
ex2-five = refl
