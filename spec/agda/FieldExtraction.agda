{-# OPTIONS --safe --without-K #-}

-- FieldExtraction.agda: the field formula of algorithmic performance
-- field extraction on exact 2 x 2 integer examples, decided by evaluation.
--
-- Sources, by page (no text quoted): Mazzola, The Topos of Music, vol. II,
-- section 47.3.4 (p. 762) for Z(X) = U V^-1 Delta = det(V)^-1 U Adj(V) Delta
-- and its three cases (regular, direction only, no information), and
-- section 47.3.4.1 (pp. 762-763) for bases of difference vectors.
--
-- The examples are sources/FIELD-EXTRACTION-SPEC.md section 6: the affine
-- performance A = [[2,0],[1,2]] seen through two bases, a direction-only
-- image and two images without information. The module works over Z with
-- no tolerance; java/core's FieldExtraction decides the same cases in
-- floating point with the relative tolerances of D7, and its rule that a
-- zero direction means no information (D8) is modelled here exactly.
-- The scaling by det(V)^-1 is left out: the regular examples check
-- U Adj(V) Delta and det V separately. It proves no general theorem.

module FieldExtraction where

open import Prelude hiding (_+_; _*_)
open import Addresses using (_+ℤ_; _*ℤ_; eqℤ)

------------------------------------------------------------------------
-- 2 x 2 integer matrices, row-major, and vectors

data M2 : Set where
  m2 : Int → Int → Int → Int → M2

data V2 : Set where
  v2 : Int → Int → V2

-ℤ_ : Int → Int
-ℤ x = negsuc 0 *ℤ x

det : M2 → Int
det (m2 a b c d) = (a *ℤ d) +ℤ (-ℤ (b *ℤ c))

-- Adj(M)_ij = (-1)^(i+j) times the minor of M without row j and column i.
adj : M2 → M2
adj (m2 a b c d) = m2 d (-ℤ b) (-ℤ c) a

apply : M2 → V2 → V2
apply (m2 a b c d) (v2 x y) = v2 ((a *ℤ x) +ℤ (b *ℤ y)) ((c *ℤ x) +ℤ (d *ℤ y))

mul : M2 → M2 → M2
mul (m2 a b c d) (m2 e f g h) =
  m2 ((a *ℤ e) +ℤ (b *ℤ g)) ((a *ℤ f) +ℤ (b *ℤ h))
     ((c *ℤ e) +ℤ (d *ℤ g)) ((c *ℤ f) +ℤ (d *ℤ h))

Δ : V2
Δ = v2 (pos 1) (pos 1)

-- U Adj(V) Delta, the field vector times det V.
scaled : M2 → M2 → V2
scaled u v = apply u (apply (adj v) Δ)

------------------------------------------------------------------------
-- The three cases of p. 762, with D8's zero direction counted as none

data Case : Set where
  regular direction none : Case

isZero : Int → Bool
isZero x = eqℤ x (pos 0)

zeroM : M2 → Bool
zeroM (m2 a b c d) = if isZero a then (if isZero b then (if isZero c then isZero d else false) else false) else false

zeroV : V2 → Bool
zeroV (v2 x y) = if isZero x then isZero y else false

classify : M2 → M2 → Case
classify u v =
  if isZero (det v) then
    (if zeroM (adj v) then none else (if zeroV (scaled u v) then none else direction))
  else regular

------------------------------------------------------------------------
-- The affine performance A through two bases (F1)

A : M2
A = m2 (pos 2) (pos 0) (pos 1) (pos 2)

I : M2
I = m2 (pos 1) (pos 0) (pos 0) (pos 1)

-- The basis of the unit vectors and the basis (1,0), (1,1).
U₁ U₂ : M2
U₁ = I
U₂ = m2 (pos 1) (pos 1) (pos 0) (pos 1)

detA : det A ≡ pos 4
detA = refl

-- The image of a basis under the affine map is A U.
V₁ : mul A U₁ ≡ A
V₁ = refl

V₂ : mul A U₂ ≡ m2 (pos 2) (pos 2) (pos 1) (pos 3)
V₂ = refl

adjA : adj A ≡ m2 (pos 2) (pos 0) (negsuc 0) (pos 2)
adjA = refl

adjV₂ : adj (mul A U₂) ≡ m2 (pos 3) (negsuc 1) (negsuc 0) (pos 2)
adjV₂ = refl

det₁ : det (mul A U₁) ≡ pos 4
det₁ = refl

det₂ : det (mul A U₂) ≡ pos 4
det₂ = refl

-- Adj(V) Delta differs between the bases: (2, 1) and (1, 1).
adjΔ₁ : apply (adj (mul A U₁)) Δ ≡ v2 (pos 2) (pos 1)
adjΔ₁ = refl

adjΔ₂ : apply (adj (mul A U₂)) Δ ≡ v2 (pos 1) (pos 1)
adjΔ₂ = refl

-- U Adj(V) Delta is (2, 1) for both, so Z = (2, 1) / 4 = A^-1 Delta = (1/2, 1/4).
field₁ : scaled U₁ (mul A U₁) ≡ v2 (pos 2) (pos 1)
field₁ = refl

field₂ : scaled U₂ (mul A U₂) ≡ v2 (pos 2) (pos 1)
field₂ = refl

-- A (1/2, 1/4) = Delta, checked as A (2, 1) = 4 Delta.
inverse : apply A (v2 (pos 2) (pos 1)) ≡ v2 (pos 4) (pos 4)
inverse = refl

regular₁ : classify U₁ (mul A U₁) ≡ regular
regular₁ = refl

regular₂ : classify U₂ (mul A U₂) ≡ regular
regular₂ = refl

------------------------------------------------------------------------
-- Singular images

-- V = [[1,2],[2,4]]: det 0, Adj = [[4,-2],[-2,1]], direction (2, -1).
Vdir : M2
Vdir = m2 (pos 1) (pos 2) (pos 2) (pos 4)

detDir : det Vdir ≡ pos 0
detDir = refl

adjDir : adj Vdir ≡ m2 (pos 4) (negsuc 1) (negsuc 1) (pos 1)
adjDir = refl

dir : scaled I Vdir ≡ v2 (pos 2) (negsuc 0)
dir = refl

caseDir : classify I Vdir ≡ direction
caseDir = refl

-- V = 0: Adj = 0, no information.
Vzero : M2
Vzero = m2 (pos 0) (pos 0) (pos 0) (pos 0)

caseZero : classify I Vzero ≡ none
caseZero = refl

-- V = [[1,1],[1,1]]: Adj = [[1,-1],[-1,1]] is not 0 but Adj Delta = 0 (D8).
Vflat : M2
Vflat = m2 (pos 1) (pos 1) (pos 1) (pos 1)

adjFlatΔ : apply (adj Vflat) Δ ≡ v2 (pos 0) (pos 0)
adjFlatΔ = refl

caseFlat : classify I Vflat ≡ none
caseFlat = refl
