{-# OPTIONS --safe --without-K #-}

-- Counterpoint.agda: finite facts about the six strong dichotomies of Z12
-- and their dual-number deformations, proved by computation. The
-- definitions follow Agustín-Aquino, Junod and Mazzola, Computational
-- Counterpoint Worlds (CCW), section 2.1 (pp. 5-9) and section 2.2
-- (pp. 10-16), as java/core's org.rubato.math.counterpoint implements them.
--
-- Every fact is a Boolean decision procedure over all of Z12 (or Z12^2)
-- that reduces to true; involution and complementation are also given
-- pointwise as propositional equalities.

module Counterpoint where

open import Prelude

------------------------------------------------------------------------
-- Z12 and its affine maps T^t . u : x |-> u x + t

m12 : Nat → Nat
m12 m = mod-helper 0 11 m 11

z12 : List Nat
z12 = 0 ∷ 1 ∷ 2 ∷ 3 ∷ 4 ∷ 5 ∷ 6 ∷ 7 ∷ 8 ∷ 9 ∷ 10 ∷ 11 ∷ []

units : List Nat
units = 1 ∷ 5 ∷ 7 ∷ 11 ∷ []

data Aff : Set where
  aff : (t u : Nat) → Aff

ap : Aff → Nat → Nat
ap (aff t u) x = m12 (u * x + t)

_++_ : {A : Set} → List A → List A → List A
[]       ++ ys = ys
(x ∷ xs) ++ ys = x ∷ (xs ++ ys)

concatMap : {A B : Set} → (A → List B) → List A → List B
concatMap f []       = []
concatMap f (x ∷ xs) = f x ++ concatMap f xs

-- the 48 affine bijections of Z12
affs : List Aff
affs = concatMap (λ t → map (aff t) units) z12

count : {A : Set} → (A → Bool) → List A → Nat
count p []       = 0
count p (x ∷ xs) = if p x then suc (count p xs) else count p xs

------------------------------------------------------------------------
-- Dichotomies, given by their consonances X (the complement is Y)

-- Boolean equivalence
_⇔ᵇ_ : Bool → Bool → Bool
true  ⇔ᵇ b = b
false ⇔ᵇ b = not b

mem : Nat → List Nat → Bool
mem x xs = any (λ y → x ==ⁿ y) xs

-- f maps X into Y; f is a bijection and |X| = |Y| = 6, so onto Y
complements : List Nat → Aff → Bool
complements X f = all (λ x → not (mem (ap f x) X)) X

-- f maps X onto X
fixes : List Nat → Aff → Bool
fixes X f = all (λ x → mem (ap f x) X) X

-- strong (CCW Def. 2.3, p. 8): exactly one affine map exchanges X and Y,
-- and only the identity fixes X
strong : List Nat → Bool
strong X = (count (complements X) affs ==ⁿ 1) && (count (fixes X) affs ==ⁿ 1)

involution : Aff → Bool
involution p = all (λ x → ap p (ap p x) ==ⁿ x) z12

------------------------------------------------------------------------
-- The six classes, with the polarity StrongDichotomies.java computes

X64 X68 X71 X75 X78 X82 : List Nat
X64 = 2 ∷ 4 ∷ 5 ∷ 7 ∷ 9 ∷ 11 ∷ []
X68 = 0 ∷ 1 ∷ 2 ∷ 3 ∷ 5 ∷ 8 ∷ []
X71 = 0 ∷ 1 ∷ 2 ∷ 3 ∷ 6 ∷ 7 ∷ []
X75 = 0 ∷ 1 ∷ 2 ∷ 4 ∷ 5 ∷ 8 ∷ []
X78 = 0 ∷ 1 ∷ 2 ∷ 4 ∷ 6 ∷ 10 ∷ []
X82 = 0 ∷ 3 ∷ 4 ∷ 7 ∷ 8 ∷ 9 ∷ []

p64 p68 p71 p75 p78 p82 : Aff
p64 = aff 5 11
p68 = aff 6 5
p71 = aff 11 11
p75 = aff 11 11
p78 = aff 9 11
p82 = aff 2 5      -- CCW Example 2.2, p. 8

data Class : Set where
  c64 c68 c71 c75 c78 c82 : Class

cons : Class → List Nat
cons c64 = X64
cons c68 = X68
cons c71 = X71
cons c75 = X75
cons c78 = X78
cons c82 = X82

pol : Class → Aff
pol c64 = p64
pol c68 = p68
pol c71 = p71
pol c75 = p75
pol c78 = p78
pol c82 = p82

classes : List Class
classes = c64 ∷ c68 ∷ c71 ∷ c75 ∷ c78 ∷ c82 ∷ []

-- Fact 1: each representative is strong, and the listed map is its polarity
-- (strength makes the polarity unique)
all-strong : all (λ c → strong (cons c)) classes ≡ true
all-strong = refl

polarities : all (λ c → complements (cons c) (pol c)) classes ≡ true
polarities = refl

-- Fact 2: each polarity is an involution
involutions : all (λ c → involution (pol c)) classes ≡ true
involutions = refl

-- The same facts pointwise, for each x in Z12
data _∈_ {A : Set} (x : A) : List A → Set where
  here  : {xs : List A} → x ∈ (x ∷ xs)
  there : {y : A} {xs : List A} → x ∈ xs → x ∈ (y ∷ xs)

all-∈ : {A : Set} (p : A → Bool) {x : A} (xs : List A) → all p xs ≡ true → x ∈ xs → p x ≡ true
all-∈ p (y ∷ ys) h here      = &&-left (p y) (all p ys) h
all-∈ p (y ∷ ys) h (there i) = all-∈ p ys (&&-right (p y) (all p ys) h) i

involution-at : (c : Class) {x : Nat} → x ∈ z12 → ap (pol c) (ap (pol c) x) ≡ x
involution-at c {x} i =
  ==ⁿ-sound _ x (all-∈ (λ y → ap (pol c) (ap (pol c) y) ==ⁿ y) z12 (all-∈ (λ d → involution (pol d)) classes involutions (cls c)) i)
  where
    cls : (d : Class) → d ∈ classes
    cls c64 = here
    cls c68 = there here
    cls c71 = there (there here)
    cls c75 = there (there (there here))
    cls c78 = there (there (there (there here)))
    cls c82 = there (there (there (there (there here))))

dissonant-image : (c : Class) {x : Nat} → x ∈ cons c → mem (ap (pol c) x) (cons c) ≡ false
dissonant-image c {x} i = not-true (all-∈ (λ y → not (mem (ap (pol c) y) (cons c))) (cons c) (pc c) i)
  where
    not-true : {b : Bool} → not b ≡ true → b ≡ false
    not-true {false} _ = refl
    not-true {true} ()
    pc : (d : Class) → complements (cons d) (pol d) ≡ true
    pc c64 = refl
    pc c68 = refl
    pc c71 = refl
    pc c75 = refl
    pc c78 = refl
    pc c82 = refl

------------------------------------------------------------------------
-- Z12[ε] as Z12^2: x + ε.y has cantus firmus x and interval y

data Pt : Set where
  pt : (x y : Nat) → Pt

points : List Pt
points = concatMap (λ x → map (pt x) z12) z12

-- the induced dichotomy X[ε] = Z12 + ε.X (CCW Example 2.4, p. 11)
consonant : List Nat → Pt → Bool
consonant X (pt _ y) = mem y X

eqPt : Pt → Pt → Bool
eqPt (pt a b) (pt c d) = (a ==ⁿ c) && (b ==ⁿ d)

-- T^{a+ε.b} . (u+ε.v) : x + ε.y |-> u x + a + ε.(u y + v x + b)
data DAff : Set where
  daff : (a b u v : Nat) → DAff

dap : DAff → Pt → Pt
dap (daff a b u v) (pt x y) = pt (m12 (u * x + a)) (m12 (u * y + v * x + b))

-- the induced polarity at cantus firmus x0 of the polarity T^t.u:
-- p_x0[ε] = T^{x0(1-u)+ε.t} . u (CCW Prop. 2.2, p. 11)
induced : Aff → Nat → DAff
induced (aff t u) x0 = daff (m12 (x0 * (13 - u))) t u 0

-- Fact 3a: every induced polarity p_x0[ε] fixes x0 + ε.y's cantus firmus
-- x0 and exchanges consonant and dissonant points, for all x0 and all 144
-- points
cantusOf : Pt → Nat
cantusOf (pt x _) = x

induced-swaps : Class → Bool
induced-swaps c = all (λ p → all (λ x0 → swaps x0 p) z12) points
  where
    swaps : Nat → Pt → Bool
    swaps x0 p = (cantusOf (dap (induced (pol c) x0) (pt x0 (intervalOf p))) ==ⁿ x0)
              && (consonant (cons c) (dap (induced (pol c) x0) p) ⇔ᵇ not (consonant (cons c) p))
      where
        intervalOf : Pt → Nat
        intervalOf (pt _ y) = y

induced-polarities : all induced-swaps classes ≡ true
induced-polarities = refl

-- Fact 3b: a cantus-firmus translation T^{x0} preserves consonance
translations-preserve : all (λ c → all (λ x0 → all (λ p → consonant (cons c) (dap (daff x0 0 1 0) p) ⇔ᵇ consonant (cons c) p) points) z12) classes ≡ true
translations-preserve = refl

------------------------------------------------------------------------
-- A contrapuntal symmetry carries the polarity over to the deformed
-- dichotomy (gX[ε]/gY[ε]): CCW Example 2.5, pp. 15-16, for (K/D) at
-- ξ = ε.9 with g = T^{ε.8} . (5 + ε.4)

image : DAff → List Nat → List Pt
image g X = map (dap g) (filterPt (consonant X) points)
  where
    filterPt : (Pt → Bool) → List Pt → List Pt
    filterPt q []       = []
    filterPt q (p ∷ ps) = if q p then p ∷ filterPt q ps else filterPt q ps

memPt : Pt → List Pt → Bool
memPt p ps = any (eqPt p) ps

g25 : DAff
g25 = daff 0 8 5 4

gK : List Pt
gK = image g25 X82

-- ξ = ε.9 is not in gK[ε]
xi-outside : memPt (pt 0 9) gK ≡ false
xi-outside = refl

-- p_0[ε] exchanges gK[ε] and its complement: a point is in gK[ε] exactly
-- when its image under p_0[ε] is not
deformed-polarity : all (λ p → memPt (dap (induced p82 0) p) gK ⇔ᵇ not (memPt p gK)) points ≡ true
deformed-polarity = refl

-- |gK[ε] ∩ K[ε]| = 56, the maximum CCW Example 2.5 states
intersection-56 : count (consonant X82) gK ≡ 56
intersection-56 = refl
