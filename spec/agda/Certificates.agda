{-# OPTIONS --safe --without-K #-}

-- Certificates.agda: the claims that Rubato's Agda certificates prove, as
-- types (propositions as types). Each certificate is a definition in another
-- module whose type is one of the types below, or its negation, and whose body
-- is the proof; java/core's org.rubato.math.certificates.AgdaCertificates
-- writes them, and GeneratedCertificates.agda holds a committed set.
--
-- Sources, by page (no text quoted): Drew Flieder, "Type Theory for the
-- Working Mathematical Music Theorist", arXiv 2512.04090v1:
--  * triad(s, d) by stacking every other scale note: Example 2.3 (p. 11);
--  * scale membership as a propositional function: Example 2.4 (p. 12);
--  * containsLeadingTone and dominant(V) iff containsLeadingTone:
--    Example 2.6 (p. 13);
--  * propositions as the types of their proofs: Appendix B.2 (p. 46);
--  * allInterval(C) as a product over interval classes of a sum over pairs:
--    Example B.2 (pp. 51-52);
--  * "every chord with dominant function contains the leading tone" as a
--    product over a sum, vacuous when the sum is empty: Example B.3 (p. 53).
--
-- The departures (D1-D11) are in sources/AGDA-CERTIFICATES-SPEC.md: lists for
-- sets (D1), interval classes as the representatives 0 .. n/2 (D2), keys as a
-- tonic and one of three modes (D5), domfunc(c, k) as c = V of k together
-- with containsLeadingTone(k) (D6).

module Certificates where

open import Prelude

------------------------------------------------------------------------
-- Pairs, membership, order

infixr 2 _×_
_×_ : Set → Set → Set
A × B = Σ A (λ _ → B)

-- x ∈ xs: a position of x in xs (D1).
infix 4 _∈_
data _∈_ (x : Nat) : List Nat → Set where
  here  : {xs : List Nat} → x ∈ (x ∷ xs)
  there : {y : Nat} {xs : List Nat} → x ∈ xs → x ∈ (y ∷ xs)

infix 4 _≤_
data _≤_ : Nat → Nat → Set where
  z≤n : {n : Nat} → zero ≤ n
  s≤s : {m n : Nat} → m ≤ n → suc m ≤ suc n

------------------------------------------------------------------------
-- Z_n and interval classes (Ex. B.2, D2)

infixl 7 _mod_
_mod_ : Nat → Nat → Nat
a mod zero    = a
a mod (suc m) = mod-helper 0 m a m

half : Nat → Nat
half n = div-helper 0 1 n 1

-- pcint(x, y) = y - x in Z_n, for x and y below n.
pcint : Nat → Nat → Nat → Nat
pcint n x y = (y + n - x) mod n

-- intclass(d) = min(d, n - d).
intclass : Nat → Nat → Nat
intclass n d = if (n - d) <ᵇ d then n - d else d

-- The inner sum of Ex. B.2: a pair (x, y) of C × C whose interval class is i.
Hit : Nat → List Nat → Nat → Set
Hit n C i = Σ Nat (λ x → Σ Nat (λ y → Σ (x ∈ C) (λ _ → Σ (y ∈ C) (λ _ → intclass n (pcint n x y) ≡ i))))

-- allInterval(C) := Π over i : IC of the sum above.
AllInterval : Nat → List Nat → Set
AllInterval n C = (i : Nat) → i ≤ half n → Hit n C i

------------------------------------------------------------------------
-- Keys, scales, the dominant and the leading tone (Ex. 2.3, 2.4, 2.6, B.3; D5, D6)

data Mode : Set where
  major harmonicMinor naturalMinor : Mode

steps : Mode → List Nat
steps major         = 0 ∷ 2 ∷ 4 ∷ 5 ∷ 7 ∷ 9 ∷ 11 ∷ []
steps harmonicMinor = 0 ∷ 2 ∷ 3 ∷ 5 ∷ 7 ∷ 8 ∷ 11 ∷ []
steps naturalMinor  = 0 ∷ 2 ∷ 3 ∷ 5 ∷ 7 ∷ 8 ∷ 10 ∷ []

-- The key's seven pitch classes, listed from the tonic t.
scale : Nat → Mode → List Nat
scale t m = map (λ s → (t + s) mod 12) (steps m)

-- Degree d (1 is the tonic; degrees above 7 wrap).
degree : Nat → Mode → Nat → Nat
degree t m d with nth ((d + 6) mod 7) (scale t m)
... | just p  = p
... | nothing = 0

triad : Nat → Mode → Nat → List Nat
triad t m d = degree t m d ∷ degree t m (d + 2) ∷ degree t m (d + 4) ∷ []

-- lt(k): a semitone below the tonic.
leadingTone : Nat → Nat
leadingTone t = (t + 11) mod 12

-- Ex. 2.6: degree 7 lies a semitone below degree 1.
ContainsLeadingTone : Nat → Mode → Set
ContainsLeadingTone t m = degree t m 7 ≡ (degree t m 1 + 11) mod 12

-- domfunc(c, k), D6: c is the V triad of the key and the key has a leading tone.
DomFunc : List Nat → Nat → Mode → Set
DomFunc c t m = (c ≡ triad t m 5) × ContainsLeadingTone t m

-- Ex. B.3: Π over x : Σ c domfunc(c, k) of contains(pr1 x, lt k).
DominantContainsLeadingTone : Nat → Mode → Set
DominantContainsLeadingTone t m = (x : Σ (List Nat) (λ c → DomFunc c t m)) → leadingTone t ∈ fst x
