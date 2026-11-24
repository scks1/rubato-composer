{-# OPTIONS --safe --without-K #-}

-- RhythmTrees.agda: rhythm trees as a W-type, with the lengths of Figure 2's
-- leaves under both timings of sources/RHYTHM-TREES-SPEC.md, proved by
-- computation.
--
-- Sources, by page (no text quoted): Drew Flieder, "Type Theory for the
-- Working Mathematical Music Theorist", arXiv 2512.04090v1:
--  * W-types: section 2.5.6 (p. 18) and Appendix C (pp. 53-54);
--  * rhythm trees, A = Q x List(Q) and B(d, [r1..rn]) = Fin(n): Example C.1
--    (pp. 55-56);
--  * the tree with root 9.5: Figure 2 (p. 56).
--
-- The paper does not say how a tree becomes lengths. This module states
-- the two timings of the spec: proportional (D1, the default), where child k
-- of a node lasting s lasts s * r_k / (r_1 + ... + r_n), and label (D2),
-- where every node lasts its own label. It checks:
--  * Figure 2's seven leaves in proportional timing last 38/15, 19/27,
--    19/54, 19/18, 19/18, 57/35 and 76/35, which sum to the root's 19/2;
--  * in label timing they last 2, 2, 1, 1, 1, 3/2 and 2, which do not sum
--    to 19/2;
--  * Ex. C.1's 5 divided as 1, 1, 1 gives three parts of 5/3.
-- Rationals are unreduced pairs compared by cross-multiplication; the
-- values here have positive denominators. One subtree per factor is part
-- of the type (B = Fin(n)): adversarial/RhythmTreesExtraSubtree.agda shows
-- a third subtree under two factors failing to typecheck, which Java checks
-- at run time (RhythmTree, RhythmTreeDenotators). java/core's
-- org.rubato.math.rhythm computes the same values (RhythmTreeTest).

module RhythmTrees where

open import Prelude

------------------------------------------------------------------------
-- Rationals as pairs

infix 8 _/_
record Q : Set where
  constructor _/_
  field
    num : Nat
    den : Nat

infixl 6 _+q_
_+q_ : Q → Q → Q
(a / b) +q (c / d) = (a * d + c * b) / (b * d)

infixl 7 _*q_ _÷q_
_*q_ : Q → Q → Q
(a / b) *q (c / d) = (a * c) / (b * d)

_÷q_ : Q → Q → Q
(a / b) ÷q (c / d) = (a * d) / (b * c)

_≈ᵇ_ : Q → Q → Bool
(a / b) ≈ᵇ (c / d) = (a * d) ==ⁿ (c * b)

sumQ : List Q → Q
sumQ []       = 0 / 1
sumQ (r ∷ rs) = r +q sumQ rs

allEq : List Q → List Q → Bool
allEq []       []       = true
allEq (x ∷ xs) (y ∷ ys) = (x ≈ᵇ y) && allEq xs ys
allEq _        _        = false

infixr 5 _++_
_++_ : {A : Set} → List A → List A → List A
[]       ++ ys = ys
(x ∷ xs) ++ ys = x ∷ (xs ++ ys)

------------------------------------------------------------------------
-- W-types (Appendix C) and rhythm trees (Example C.1)

data W (A : Set) (B : A → Set) : Set where
  sup : (a : A) → (B a → W A B) → W A B

data Fin : Nat → Set where
  fzero : {n : Nat} → Fin (suc n)
  fsuc  : {n : Nat} → Fin n → Fin (suc n)

-- A label is a duration d with its factors [r1, ..., rn].
Label : Set
Label = Σ Q (λ _ → List Q)

-- A node with n factors has one branch per position of Fin(n).
Arity : Label → Set
Arity (d , rs) = Fin (length rs)

RhythmTree : Set
RhythmTree = W Label Arity

-- The recursion principle of W: a node's value from its label and the
-- values of its subtrees.
Wrec : {C : Set} → ((a : Label) → (Arity a → C) → C) → RhythmTree → C
Wrec h (sup a f) = h a (λ b → Wrec h (f b))

label : RhythmTree → Q
label (sup (d , _) _) = d

-- The values of the branches in order.
tab : {X : Set} (n : Nat) → (Fin n → X) → List X
tab zero    f = []
tab (suc n) f = f fzero ∷ tab n (λ k → f (fsuc k))

concat : {X : Set} → List (List X) → List X
concat []         = []
concat (xs ∷ xss) = xs ++ concat xss

------------------------------------------------------------------------
-- Proportional timing (D1): each subtree's value is a function from the
-- length its parent gives it to the lengths of its leaves.

share : (rs : List Q) → (Fin (length rs) → Q → List Q) → Q → Q → List Q
share []       g total s = []
share (r ∷ rs) g total s = g fzero ((s *q r) ÷q total) ++ share rs (λ k → g (fsuc k)) total s

stepP : (a : Label) → (Arity a → Q → List Q) → Q → List Q
stepP (d , [])       g s = s ∷ []
stepP (d , (r ∷ rs)) g s = share (r ∷ rs) g (sumQ (r ∷ rs)) s

proportionalLeaves : RhythmTree → List Q
proportionalLeaves t = Wrec stepP t (label t)

-- Label timing (D2): every leaf lasts its own label.
stepL : (a : Label) → (Arity a → List Q) → List Q
stepL (d , [])       g = d ∷ []
stepL (d , (r ∷ rs)) g = concat (tab (suc (length rs)) g)

labelLeaves : RhythmTree → List Q
labelLeaves = Wrec stepL

------------------------------------------------------------------------
-- Figure 2 (p. 56): 9.5 (2 2.5 (1 (2 1) 1 1) 3 (1.5 2)); every child's own
-- label is its factor.

leaf : Q → RhythmTree
leaf d = sup (d , []) (λ ())

oneSplit : RhythmTree
oneSplit = sup (1 / 1 , 2 / 1 ∷ 1 / 1 ∷ []) λ
  { fzero               → leaf (2 / 1)
  ; (fsuc fzero)        → leaf (1 / 1)
  ; (fsuc (fsuc ()))
  }

twoAndHalf : RhythmTree
twoAndHalf = sup (5 / 2 , 1 / 1 ∷ 1 / 1 ∷ 1 / 1 ∷ []) λ
  { fzero                → oneSplit
  ; (fsuc fzero)         → leaf (1 / 1)
  ; (fsuc (fsuc fzero))  → leaf (1 / 1)
  ; (fsuc (fsuc (fsuc ())))
  }

three : RhythmTree
three = sup (3 / 1 , 3 / 2 ∷ 2 / 1 ∷ []) λ
  { fzero               → leaf (3 / 2)
  ; (fsuc fzero)        → leaf (2 / 1)
  ; (fsuc (fsuc ()))
  }

figure2 : RhythmTree
figure2 = sup (19 / 2 , 2 / 1 ∷ 5 / 2 ∷ 3 / 1 ∷ []) λ
  { fzero                → leaf (2 / 1)
  ; (fsuc fzero)         → twoAndHalf
  ; (fsuc (fsuc fzero))  → three
  ; (fsuc (fsuc (fsuc ())))
  }

figure2-leaves : length (proportionalLeaves figure2) ≡ 7
figure2-leaves = refl

figure2-proportional :
  allEq (proportionalLeaves figure2)
        (38 / 15 ∷ 19 / 27 ∷ 19 / 54 ∷ 19 / 18 ∷ 19 / 18 ∷ 57 / 35 ∷ 76 / 35 ∷ []) ≡ true
figure2-proportional = refl

figure2-fills-root : (sumQ (proportionalLeaves figure2) ≈ᵇ (19 / 2)) ≡ true
figure2-fills-root = refl

figure2-label :
  allEq (labelLeaves figure2)
        (2 / 1 ∷ 2 / 1 ∷ 1 / 1 ∷ 1 / 1 ∷ 1 / 1 ∷ 3 / 2 ∷ 2 / 1 ∷ []) ≡ true
figure2-label = refl

figure2-label-misses-root : (sumQ (labelLeaves figure2) ≈ᵇ (19 / 2)) ≡ false
figure2-label-misses-root = refl

-- The factors of Figure 2's root do not sum to its label (p. 55: they need
-- not).
figure2-factors : (sumQ (2 / 1 ∷ 5 / 2 ∷ 3 / 1 ∷ []) ≈ᵇ (15 / 2)) ≡ true
figure2-factors = refl

-- Example C.1 (p. 55): 5 divided proportionally into 1, 1, 1.
threeParts : RhythmTree
threeParts = sup (5 / 1 , 1 / 1 ∷ 1 / 1 ∷ 1 / 1 ∷ []) λ
  { fzero                → leaf (1 / 1)
  ; (fsuc fzero)         → leaf (1 / 1)
  ; (fsuc (fsuc fzero))  → leaf (1 / 1)
  ; (fsuc (fsuc (fsuc ())))
  }

three-equal-parts : allEq (proportionalLeaves threeParts) (5 / 3 ∷ 5 / 3 ∷ 5 / 3 ∷ []) ≡ true
three-equal-parts = refl
