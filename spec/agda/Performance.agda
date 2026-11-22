{-# OPTIONS --safe --without-K #-}

-- Performance.agda: the discrete and finite parts of the performance
-- package org.rubato.math.performance.
--
-- 1. Stemma shapes (Mazzola, The Topos of Music II, Defs. 106-107,
--    p. 628). A stemma's daughter graph is a finite tree with leaves and
--    no LPS among its own daughters. Here a stemma shape is a rose tree,
--    so finiteness and tree shape hold by construction; the proofs show
--    that every shape has a leaf, that edges = nodes - 1, and that no node
--    lists itself among its daughters. The shape of Fig. 38.5 (p. 629)
--    has 21 nodes, 20 edges, 4 leaves and 7 operator layers, as
--    Stemma.java's test rebuilds it.
--
-- 2. Exact onsets of step tempi (Example 49, p. 552) over the positive
--    rationals: a bar of length l at tempo T lasts l/T minutes, and a
--    tempo of 0 cannot be written down. Polygonal, linear and other
--    continuous tempi need logarithms and stay in the Java tests.

module Performance where

open import Prelude

infixr 2 _×_
_×_ : Set → Set → Set
A × B = Σ A (λ _ → B)

------------------------------------------------------------------------
-- Arithmetic on Nat

trans : {A : Set} {x y z : A} → x ≡ y → y ≡ z → x ≡ z
trans refl q = q

cong₂ : {A B C : Set} (f : A → B → C) {a a' : A} {b b' : B} →
        a ≡ a' → b ≡ b' → f a b ≡ f a' b'
cong₂ f refl refl = refl

+-zero : (n : Nat) → n + 0 ≡ n
+-zero zero    = refl
+-zero (suc n) = cong suc (+-zero n)

+-suc : (m n : Nat) → m + suc n ≡ suc (m + n)
+-suc zero    n = refl
+-suc (suc m) n = cong suc (+-suc m n)

+-assoc : (a b c : Nat) → (a + b) + c ≡ a + (b + c)
+-assoc zero    b c = refl
+-assoc (suc a) b c = cong suc (+-assoc a b c)

+-comm : (m n : Nat) → m + n ≡ n + m
+-comm zero    n = sym (+-zero n)
+-comm (suc m) n = trans (cong suc (+-comm m n)) (sym (+-suc n m))

-- l + (e + r) ≡ e + (l + r)
+-swap : (l e r : Nat) → l + (e + r) ≡ e + (l + r)
+-swap l e r = trans (sym (+-assoc l e r))
                (trans (cong (λ x → x + r) (+-comm l e)) (+-assoc e l r))

infix 4 _≤_
data _≤_ : Nat → Nat → Set where
  z≤n : {n : Nat} → zero ≤ n
  s≤s : {m n : Nat} → m ≤ n → suc m ≤ suc n

≤-refl : (n : Nat) → n ≤ n
≤-refl zero    = z≤n
≤-refl (suc n) = s≤s (≤-refl n)

≤-step : {m n : Nat} → m ≤ n → m ≤ suc n
≤-step z≤n     = z≤n
≤-step (s≤s p) = s≤s (≤-step p)

≤-+r : (m n : Nat) → m ≤ m + n
≤-+r zero    n = z≤n
≤-+r (suc m) n = s≤s (≤-+r m n)

≤-+l : {k n : Nat} (m : Nat) → k ≤ n → k ≤ m + n
≤-+l zero    p = p
≤-+l (suc m) p = ≤-step (≤-+l m p)

¬s≤ : (n : Nat) → ¬ (suc n ≤ n)
¬s≤ zero    ()
¬s≤ (suc n) (s≤s p) = ¬s≤ n p

Pos : Nat → Set
Pos zero    = ⊥
Pos (suc _) = ⊤

pos-+ : (a b : Nat) → Pos a → Pos (a + b)
pos-+ zero    b ()
pos-+ (suc a) b _ = tt

pos-* : (a b : Nat) → Pos a → Pos b → Pos (a * b)
pos-* zero    b () _
pos-* (suc a) zero    _ ()
pos-* (suc a) (suc b) _ _ = tt

------------------------------------------------------------------------
-- 1. Stemma shapes

data Stemma : Set where
  lps : List Stemma → Stemma

leaf : Stemma
leaf = lps []

daughters : Stemma → List Stemma
daughters (lps ds) = ds

mutual
  size : Stemma → Nat
  size (lps ds) = suc (sizes ds)

  sizes : List Stemma → Nat
  sizes []       = 0
  sizes (d ∷ ds) = size d + sizes ds

mutual
  edges : Stemma → Nat
  edges (lps ds) = length ds + edgesL ds

  edgesL : List Stemma → Nat
  edgesL []       = 0
  edgesL (d ∷ ds) = edges d + edgesL ds

mutual
  leaves : Stemma → Nat
  leaves (lps [])       = 1
  leaves (lps (d ∷ ds)) = leavesL (d ∷ ds)

  leavesL : List Stemma → Nat
  leavesL []       = 0
  leavesL (d ∷ ds) = leaves d + leavesL ds

max : Nat → Nat → Nat
max zero    n       = n
max (suc m) zero    = suc m
max (suc m) (suc n) = suc (max m n)

-- the longest chain of operators from the mother to a leaf
mutual
  depth : Stemma → Nat
  depth (lps [])       = 0
  depth (lps (d ∷ ds)) = suc (depthL (d ∷ ds))

  depthL : List Stemma → Nat
  depthL []       = 0
  depthL (d ∷ ds) = max (depth d) (depthL ds)

-- Def. 107: a stemma has leaves.
leaves-pos : (t : Stemma) → Pos (leaves t)
leaves-pos (lps [])       = tt
leaves-pos (lps (d ∷ ds)) = pos-+ (leaves d) (leavesL ds) (leaves-pos d)

-- A tree has one edge fewer than nodes.
mutual
  edges-size : (t : Stemma) → suc (edges t) ≡ size t
  edges-size (lps ds) = cong suc (edgesL-sizes ds)

  edgesL-sizes : (ds : List Stemma) → length ds + edgesL ds ≡ sizes ds
  edgesL-sizes []       = refl
  edgesL-sizes (d ∷ ds) =
    trans (cong suc (+-swap (length ds) (edges d) (edgesL ds)))
          (cong₂ _+_ (edges-size d) (edgesL-sizes ds))

infix 4 _∈_
data _∈_ (x : Stemma) : List Stemma → Set where
  here  : {ds : List Stemma} → x ∈ (x ∷ ds)
  there : {d : Stemma} {ds : List Stemma} → x ∈ ds → x ∈ (d ∷ ds)

∈-size : {x : Stemma} {ds : List Stemma} → x ∈ ds → size x ≤ sizes ds
∈-size {x} {.x ∷ ds} here = ≤-+r (size x) (sizes ds)
∈-size {x} {d ∷ ds} (there p) = ≤-+l (size d) (∈-size p)

-- Def. 107: no LPS lists itself among its daughters.
not-own-daughter : (t : Stemma) → ¬ (t ∈ daughters t)
not-own-daughter (lps ds) p = ¬s≤ (sizes ds) (∈-size p)

-- Fig. 38.5 (p. 629): the mother, Ω1 separating the hands, Ω2-Ω4 on each
-- hand, Ω5 splitting each hand in two, Ω6 and Ω7 on each of the four parts.
chain : Nat → Stemma → Stemma
chain zero    t = t
chain (suc k) t = lps (chain k t ∷ [])

part : Stemma
part = chain 2 leaf

hand : Stemma
hand = chain 3 (lps (part ∷ part ∷ []))

fig38-5 : Stemma
fig38-5 = lps (hand ∷ hand ∷ [])

fig38-5-nodes : size fig38-5 ≡ 21
fig38-5-nodes = refl

fig38-5-edges : edges fig38-5 ≡ 20
fig38-5-edges = refl

fig38-5-leaves : leaves fig38-5 ≡ 4
fig38-5-leaves = refl

fig38-5-layers : depth fig38-5 ≡ 7
fig38-5-layers = refl

------------------------------------------------------------------------
-- 2. Step tempi over the positive rationals

-- num / den with a positive denominator
record Q : Set where
  constructor _÷_⟨_⟩
  field
    num   : Nat
    den   : Nat
    den>0 : Pos den

infix 4 _≈_
_≈_ : Q → Q → Set
(a ÷ b ⟨ _ ⟩) ≈ (c ÷ d ⟨ _ ⟩) = a * d ≡ c * b

infixl 6 _⊕_
_⊕_ : Q → Q → Q
(a ÷ b ⟨ pb ⟩) ⊕ (c ÷ d ⟨ pd ⟩) = (a * d + c * b) ÷ (b * d) ⟨ pos-* b d pb pd ⟩

zeroQ : Q
zeroQ = 0 ÷ 1 ⟨ tt ⟩

-- A tempo in quarters per minute, r / s with r > 0 and s > 0.
record Tempo : Set where
  constructor tempo
  field
    r     : Nat
    s     : Nat
    r>0   : Pos r
    s>0   : Pos s

-- A step of a step tempo: a length p / q in quarters and its tempo.
record Step : Set where
  constructor step
  field
    len : Q
    at  : Tempo

-- (p / q) / (r / s) = (p s) / (q r) minutes
minutes : Step → Q
minutes (step (p ÷ q ⟨ pq ⟩) (tempo r s pr _)) = (p * s) ÷ (q * r) ⟨ pos-* q r pq pr ⟩

-- eq. (33.1) for a step tempo: the physical onset after the steps
onset : List Step → Q
onset []       = zeroQ
onset (x ∷ xs) = minutes x ⊕ onset xs

mm : (n : Nat) → {Pos n} → Tempo
mm n {p} = tempo n 1 p tt

quarters : Nat → Q
quarters n = n ÷ 1 ⟨ tt ⟩

-- Three bars of four quarters at M.M. 60, 90 and 120 (a case of Example
-- 49 with numbers chosen here): 4/60 + 4/90 + 4/120 = 13/90 minutes,
-- 26/3 seconds. StepTempo.java gives 8.666... seconds for the same case.
three-bars : List Step
three-bars = step (quarters 4) (mm 60) ∷ step (quarters 4) (mm 90) ∷ step (quarters 4) (mm 120) ∷ []

three-bars-minutes : onset three-bars ≈ (13 ÷ 90 ⟨ tt ⟩)
three-bars-minutes = refl

seconds : Q → Q
seconds (a ÷ b ⟨ p ⟩) = (60 * a) ÷ b ⟨ p ⟩

three-bars-seconds : seconds (onset three-bars) ≈ (26 ÷ 3 ⟨ tt ⟩)
three-bars-seconds = refl

-- Constant tempo gives linear onsets: n bars of length l at one tempo
-- end at n l / T, checked here for n = 1 .. 4 at M.M. 72 with l = 3/2.
bars : Nat → Q → Tempo → List Step
bars zero    l t = []
bars (suc n) l t = step l t ∷ bars n l t

linear-onsets : (onset (bars 1 (3 ÷ 2 ⟨ tt ⟩) (mm 72)) ≈ (3 ÷ 144 ⟨ tt ⟩))
              × ((onset (bars 2 (3 ÷ 2 ⟨ tt ⟩) (mm 72)) ≈ (6 ÷ 144 ⟨ tt ⟩))
              × ((onset (bars 3 (3 ÷ 2 ⟨ tt ⟩) (mm 72)) ≈ (9 ÷ 144 ⟨ tt ⟩))
              × (onset (bars 4 (3 ÷ 2 ⟨ tt ⟩) (mm 72)) ≈ (12 ÷ 144 ⟨ tt ⟩))))
linear-onsets = refl , refl , refl , refl
