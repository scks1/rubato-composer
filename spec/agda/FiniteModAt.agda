{-# OPTIONS --safe --without-K #-}

-- FiniteModAt.agda: a finite stand-in for Mod@, the presheaf topos over
-- the affine maps of Z2 (and Z3), with facts proved by computation.
--
-- Sources, by page (no text quoted):
--  * Mod@ as the presheaves on Mod with a subobject classifier: Mazzola,
--    The Topos of Music, vol. I, section 6.2.2 (p. 54);
--  * Omega as the functor of sieves (Example 127) and the subobject
--    classifier (Def. 169): Topos IV appendix G.3.1 (p. 1428);
--  * negation on subfunctors (Prop. 125 (iv)) and Booleanness by not not
--    (Prop. 124): Topos IV p. 1433;
--  * a presheaf topos is Boolean iff its index category is a groupoid:
--    Topos I section 18.3 (p. 335).
--
-- The book gives no finite example; this one is a departure
-- (sources/FINITE-MODAT-SPEC.md, D1). The address category is a one-object
-- category: the affine maps Z_n -> Z_n, found by testing every value table.
-- A sieve on the object is a set of arrows closed under precomposition.
-- java/core's org.rubato.math.topos computes the same values
-- (FiniteModAtTest). This module recomputes them from the definitions and
-- decides each claim by evaluation; it proves no general theorem.

module FiniteModAt where

open import Prelude

------------------------------------------------------------------------
-- Lists

infixr 5 _++_
_++_ : {A : Set} → List A → List A → List A
[]       ++ ys = ys
(x ∷ xs) ++ ys = x ∷ (xs ++ ys)

concatMap : {A B : Set} → (A → List B) → List A → List B
concatMap f []       = []
concatMap f (x ∷ xs) = f x ++ concatMap f xs

filter : {A : Set} → (A → Bool) → List A → List A
filter p []       = []
filter p (x ∷ xs) = if p x then x ∷ filter p xs else filter p xs

range : Nat → List Nat
range n = go n []
  where
  go : Nat → List Nat → List Nat
  go zero    acc = acc
  go (suc k) acc = go k (k ∷ acc)

at : List Nat → Nat → Nat
at []       _       = 0
at (x ∷ xs) zero    = x
at (x ∷ xs) (suc k) = at xs k

modN : Nat → Nat → Nat
modN zero    x = x
modN (suc k) x = mod-helper 0 k x k

------------------------------------------------------------------------
-- Value tables Z_n -> Z_n and the affine ones

Table : Set
Table = List Nat

eqT : Table → Table → Bool
eqT []       []       = true
eqT (x ∷ xs) (y ∷ ys) = (x ==ⁿ y) && eqT xs ys
eqT _        _        = false

memT : Table → List Table → Bool
memT t ts = any (eqT t) ts

tablesOf : Nat → Nat → List Table
tablesOf zero    n = [] ∷ []
tablesOf (suc m) n = concatMap (λ v → map (λ t → v ∷ t) (tablesOf m n)) (range n)

-- f(x + y) + f(0) = f(x) + f(y) mod n, which says f - f(0) is additive
affine : Nat → Table → Bool
affine n t = all (λ x → all (λ y →
               modN n (at t (modN n (x + y)) + at t 0) ==ⁿ modN n (at t x + at t y))
             (range n)) (range n)

bijective : Nat → Table → Bool
bijective n t = all (λ v → memN v t) (range n)
  where
  memN : Nat → List Nat → Bool
  memN v xs = any (λ y → v ==ⁿ y) xs

Aff : Nat → List Table
Aff n = filter (affine n) (tablesOf n n)

Aut : Nat → List Table
Aut n = filter (bijective n) (Aff n)

-- g after f
_∘_ : Table → Table → Table
g ∘ f = map (at g) f

------------------------------------------------------------------------
-- Sieves on the one object of a category with arrow list C

sublists : {A : Set} → List A → List (List A)
sublists []       = [] ∷ []
sublists (x ∷ xs) = let r = sublists xs in map (λ s → x ∷ s) r ++ r

closed : List Table → List Table → Bool
closed C S = all (λ g → all (λ h → memT (g ∘ h) S) C) S

sieves : List Table → List (List Table)
sieves C = filter (closed C) (sublists C)

-- Prop. 125 (iv): g is in not S iff g after h is outside S for every h
neg : List Table → List Table → List Table
neg C S = filter (λ g → all (λ h → not (memT (g ∘ h) S)) C) C

subset : List Table → List Table → Bool
subset S T = all (λ s → memT s T) S

setEq : List Table → List Table → Bool
setEq S T = subset S T && subset T S

-- Prop. 124: Boolean iff not not S = S for every S
boolean : List Table → Bool
boolean C = all (λ S → setEq (neg C (neg C S)) S) (sieves C)

------------------------------------------------------------------------
-- Facts

aff2-size : length (Aff 2) ≡ 4
aff2-size = refl

aff3-size : length (Aff 3) ≡ 9
aff3-size = refl

aut3-size : length (Aut 3) ≡ 6
aut3-size = refl

-- Omega(Z2) in Aff(Z2): five sieves (FINITE-MODAT-SPEC section 6)
omega-aff2 : length (sieves (Aff 2)) ≡ 5
omega-aff2 = refl

omega-aff3 : length (sieves (Aff 3)) ≡ 9
omega-aff3 = refl

-- a group has only the empty and the maximal sieve
omega-aut2 : length (sieves (Aut 2)) ≡ 2
omega-aut2 = refl

omega-aut3 : length (sieves (Aut 3)) ≡ 2
omega-aut3 = refl

-- Topos I p. 335 on these examples: not Boolean for Aff, Boolean for Aut
aff2-not-boolean : boolean (Aff 2) ≡ false
aff2-not-boolean = refl

aff3-not-boolean : boolean (Aff 3) ≡ false
aff3-not-boolean = refl

aut2-boolean : boolean (Aut 2) ≡ true
aut2-boolean = refl

aut3-boolean : boolean (Aut 3) ≡ true
aut3-boolean = refl

-- the witness: the sieve {0} (the constant 0)
zero2 : List Table
zero2 = (0 ∷ 0 ∷ []) ∷ []

one2 : List Table
one2 = (1 ∷ 1 ∷ []) ∷ []

zero2-sieve : closed (Aff 2) zero2 ≡ true
zero2-sieve = refl

not-zero2 : setEq (neg (Aff 2) zero2) one2 ≡ true
not-zero2 = refl

not-not-zero2 : setEq (neg (Aff 2) (neg (Aff 2) zero2)) zero2 ≡ true
not-not-zero2 = refl

-- {0} v not {0} = {0, 1} has two arrows, not the four of the top
excluded-middle-fails : length (zero2 ++ neg (Aff 2) zero2) ≡ 2
excluded-middle-fails = refl
