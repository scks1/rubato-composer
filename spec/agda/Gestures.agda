{-# OPTIONS --safe --without-K #-}

-- Gestures.agda: finite facts about digraphs, the subobject classifier
-- of the topos of digraphs, Example 69's implications and the face
-- coefficients of hypergestures, proved by computation.
--
-- Sources, by page (no text quoted):
--  * digraphs, digraph morphisms and subdigraphs: Mazzola, The Topos of
--    Music, vol. III, section 61.1 (pp. 913-916);
--  * the topos of digraphs, its subobject classifier and implication:
--    Topos III section 63 (pp. 929-931) and Topos IV appendix J.1
--    (pp. 1457-1460). A digraph is a presheaf on the site with objects
--    V, A and two arrows t, h : V -> A; Omega(V) holds the sieves on V,
--    Omega(A) the sieves on A, and the tail of an arrow sieve S is its
--    pullback along t;
--  * Example 69 (p. 930): the C-major degrees as networks x -> t + g.x
--    on pitch classes, and the implications V => I and IV => V. The book
--    states IV => V = III u V; this module shows the implication has the
--    extra loop at 11 that degree VII contributes (GESTURES-SPEC E5);
--  * face coefficients: Def. 112 (p. 969) with the sign head minus tail
--    (Def. 112; Example 76 on p. 971 prints the opposite sign, E6).
--
-- java/core's org.rubato.math.gesture computes the same values
-- (DigraphToposTest, FacesTest). This module recomputes them from the
-- definitions and decides each claim by evaluation. It proves no
-- general theorem: each fact holds for the listed digraphs only.

module Gestures where

open import Prelude

------------------------------------------------------------------------
-- Lists and numbers

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

count : {A : Set} → (A → Bool) → List A → Nat
count p xs = length (filter p xs)

sumN : List Nat → Nat
sumN []       = 0
sumN (x ∷ xs) = x + sumN xs

prodN : List Nat → Nat
prodN []       = 1
prodN (x ∷ xs) = x * prodN xs

pow2 : Nat → Nat
pow2 zero    = 1
pow2 (suc n) = 2 * pow2 n

range : Nat → List Nat
range n = go n []
  where
  go : Nat → List Nat → List Nat
  go zero    acc = acc
  go (suc k) acc = go k (k ∷ acc)

look : List Nat → Nat → Nat
look []       _       = 0
look (x ∷ xs) zero    = x
look (x ∷ xs) (suc k) = look xs k

memN : Nat → List Nat → Bool
memN x xs = any (λ y → x ==ⁿ y) xs

mod12 : Nat → Nat
mod12 n = mod-helper 0 11 n 11

mod12-check : mod12 25 ≡ 1
mod12-check = refl

-- Integers: Agda.Builtin.Int's pos and negsuc.

_⊖_ : Nat → Nat → Int
m     ⊖ zero  = pos m
zero  ⊖ suc n = negsuc n
suc m ⊖ suc n = m ⊖ n

_+ℤ_ : Int → Int → Int
pos m    +ℤ pos n    = pos (m + n)
pos m    +ℤ negsuc n = m ⊖ suc n
negsuc m +ℤ pos n    = n ⊖ suc m
negsuc m +ℤ negsuc n = negsuc (suc (m + n))

negℤ : Int → Int
negℤ (pos zero)    = pos zero
negℤ (pos (suc n)) = negsuc n
negℤ (negsuc n)    = pos (suc n)

infixr 2 _×_
_×_ : Set → Set → Set
A × B = Σ A (λ _ → B)

------------------------------------------------------------------------
-- Digraphs (Topos III 61.1). Vertices are 0 .. nv-1; an arrow records
-- its tail and its head.

data Arr : Set where
  arr : Nat → Nat → Arr

tl : Arr → Nat
tl (arr t _) = t

hd : Arr → Nat
hd (arr _ h) = h

eqArr : Arr → Arr → Bool
eqArr (arr a b) (arr c d) = (a ==ⁿ c) && (b ==ⁿ d)

memA : Arr → List Arr → Bool
memA a sa = any (eqArr a) sa

record Digraph : Set where
  constructor dg
  field
    nv     : Nat
    arrows : List Arr
open Digraph public

nthA : List Arr → Nat → Arr
nthA []       _       = arr 0 0
nthA (a ∷ sa) zero    = a
nthA (a ∷ sa) (suc k) = nthA sa k

------------------------------------------------------------------------
-- Morphisms: a vertex map vm and an arrow map am (arrow indices) with
-- tail and head preserved.

arrowsOk : Digraph → List Nat → List Nat → List Arr → Bool
arrowsOk E vm []       []       = true
arrowsOk E vm (k ∷ am) (a ∷ sa) =
  (k <ᵇ length (arrows E))
  && eqArr (nthA (arrows E) k) (arr (look vm (tl a)) (look vm (hd a)))
  && arrowsOk E vm am sa
arrowsOk E vm _        _        = false

isMorphism : Digraph → Digraph → List Nat → List Nat → Bool
isMorphism D E vm am =
  (length vm ==ⁿ nv D) && all (λ v → v <ᵇ nv E) vm
  && arrowsOk E vm am (arrows D)

compose : List Nat → List Nat → List Nat → List Nat → List Nat × List Nat
compose vm1 am1 vm2 am2 = map (look vm2) vm1 , map (look am2) am1

-- small digraphs

empty point arrowD loopD path2 path3 parallel cycle2 fork join wheel : Digraph
empty    = dg 0 []
point    = dg 1 []
arrowD   = dg 2 (arr 0 1 ∷ [])
loopD    = dg 1 (arr 0 0 ∷ [])
path2    = dg 3 (arr 0 1 ∷ arr 1 2 ∷ [])
path3    = dg 4 (arr 0 1 ∷ arr 1 2 ∷ arr 2 3 ∷ [])
parallel = dg 2 (arr 0 1 ∷ arr 0 1 ∷ [])
cycle2   = dg 2 (arr 0 1 ∷ arr 1 0 ∷ [])
fork     = dg 3 (arr 0 1 ∷ arr 0 2 ∷ [])
join     = dg 3 (arr 1 0 ∷ arr 2 0 ∷ [])
wheel    = dg 3 (arr 0 1 ∷ arr 1 2 ∷ arr 2 0 ∷ arr 1 1 ∷ [])

-- f : arrow -> path2 onto the second arrow; g : path2 -> loop.
f-ok : isMorphism arrowD path2 (1 ∷ 2 ∷ []) (1 ∷ []) ≡ true
f-ok = refl

g-ok : isMorphism path2 loopD (0 ∷ 0 ∷ 0 ∷ []) (0 ∷ 0 ∷ []) ≡ true
g-ok = refl

gf-ok : isMorphism arrowD loopD
          (fst (compose (1 ∷ 2 ∷ []) (1 ∷ []) (0 ∷ 0 ∷ 0 ∷ []) (0 ∷ 0 ∷ [])))
          (snd (compose (1 ∷ 2 ∷ []) (1 ∷ []) (0 ∷ 0 ∷ 0 ∷ []) (0 ∷ 0 ∷ [])))
        ≡ true
gf-ok = refl

id-path3 : isMorphism path3 path3 (range 4) (range 3) ≡ true
id-path3 = refl

-- The arrow's only endomorphism is the identity: swapping its vertices
-- fails (adversarial/GesturesReversedArrow.agda claims otherwise).
swap-arrow : isMorphism arrowD arrowD (1 ∷ 0 ∷ []) (0 ∷ []) ≡ false
swap-arrow = refl

-- Collapsing the 2-cycle onto a loop is a morphism; onto an arrow it is not.
cycle-to-loop : isMorphism cycle2 loopD (0 ∷ 0 ∷ []) (0 ∷ 0 ∷ []) ≡ true
cycle-to-loop = refl

------------------------------------------------------------------------
-- The subobject classifier from sieves (Topos IV J.1, p. 1460).
-- A sieve on A is a subset of {id_A, t, h} recorded as three booleans;
-- it is closed when it contains t and h whenever it contains id_A.
-- Vertex 0 is the empty sieve on V (false), vertex 1 the maximal one.

b2n : Bool → Nat
b2n true  = 1
b2n false = 0

triples : List (List Bool)
triples = concatMap (λ i → concatMap (λ t → map (λ h → i ∷ t ∷ h ∷ [])
                    (false ∷ true ∷ [])) (false ∷ true ∷ [])) (false ∷ true ∷ [])

closed : List Bool → Bool
closed (i ∷ t ∷ h ∷ []) = if i then t && h else true
closed _                = false

sieveArrow : List Bool → Arr
sieveArrow (i ∷ t ∷ h ∷ []) = arr (b2n t) (b2n h)
sieveArrow _                = arr 0 0

Ω : Digraph
Ω = dg 2 (map sieveArrow (filter closed triples))

Ω-arrows : length (arrows Ω) ≡ 5
Ω-arrows = refl

-- loops at f, arrows f -> t, arrows t -> f, loops at t
Ω-shape : map (λ a → count (eqArr a) (arrows Ω)) (arr 0 0 ∷ arr 0 1 ∷ arr 1 0 ∷ arr 1 1 ∷ [])
          ≡ 1 ∷ 1 ∷ 1 ∷ 2 ∷ []
Ω-shape = refl

------------------------------------------------------------------------
-- |Hom(D, Ω)| = |Sub(D)| (characteristic maps), and the Yoneda counts
-- |Hom(arrow, D)| = |A(D)|, |Hom(point, D)| = |V(D)|.

allMaps : Nat → Nat → List (List Nat)
allMaps zero    m = [] ∷ []
allMaps (suc n) m = concatMap (λ xs → map (λ v → v ∷ xs) (range m)) (allMaps n m)

choices : Digraph → List Nat → Arr → Nat
choices E vm a = count (eqArr (arr (look vm (tl a)) (look vm (hd a)))) (arrows E)

homCount : Digraph → Digraph → Nat
homCount D E = sumN (map (λ vm → prodN (map (choices E vm) (arrows D)))
                         (allMaps (nv D) (nv E)))

subCount : Digraph → Nat
subCount D = sumN (map (λ sv → pow2 (count (λ a → (look sv (tl a) ==ⁿ 1)
                                                 && (look sv (hd a) ==ⁿ 1)) (arrows D)))
                       (allMaps (nv D) 2))

samples : List Digraph
samples = empty ∷ point ∷ arrowD ∷ loopD ∷ path2 ∷ parallel ∷ cycle2 ∷ wheel ∷ []

hom-Ω : map (λ D → homCount D Ω) samples ≡ 1 ∷ 2 ∷ 5 ∷ 3 ∷ 13 ∷ 7 ∷ 7 ∷ 31 ∷ []
hom-Ω = refl

sub-counts : map subCount samples ≡ 1 ∷ 2 ∷ 5 ∷ 3 ∷ 13 ∷ 7 ∷ 7 ∷ 31 ∷ []
sub-counts = refl

yoneda-arrow : map (λ D → homCount arrowD D) samples ≡ map (λ D → length (arrows D)) samples
yoneda-arrow = refl

yoneda-point : map (λ D → homCount point D) samples ≡ map nv samples
yoneda-point = refl

------------------------------------------------------------------------
-- Example 69 (p. 930). Vertices are pitch classes of C major; a degree
-- with data (t, g, p) is the network x -> t + g.x (mod 12) started at p
-- and followed until a pitch class repeats. Parallel arrows from
-- different degrees are merged here (the Java tests check both the
-- merged 16-arrow and the unmerged 17-arrow digraph).

network : Nat → Nat → Nat → List Arr
network t g p = go 12 (mod12 p) []
  where
  go : Nat → Nat → List Nat → List Arr
  go zero    x seen = []
  go (suc k) x seen =
    if memN x seen then [] else (arr x (mod12 (t + g * x)) ∷ go k (mod12 (t + g * x)) (x ∷ seen))

record Sub : Set where
  constructor sub
  field
    sv : List Nat
    sa : List Arr
open Sub public

ends : List Arr → List Nat
ends []       = []
ends (a ∷ sa) = tl a ∷ hd a ∷ ends sa

degree : Nat → Nat → Nat → Sub
degree t g p = sub (ends (network t g p)) (network t g p)

I II III IV V VI VII : Sub
I   = degree 7 3 0
II  = degree 11 3 9
III = degree 7 3 11
IV  = degree 9 3 5
V   = degree 5 3 7
VI  = degree 9 3 4
VII = degree 1 2 2

VII-arrows : sa VII ≡ arr 2 5 ∷ arr 5 11 ∷ arr 11 11 ∷ []
VII-arrows = refl

scale : List Nat
scale = 0 ∷ 2 ∷ 4 ∷ 5 ∷ 7 ∷ 9 ∷ 11 ∷ []

dedupe : List Arr → List Arr
dedupe []       = []
dedupe (a ∷ sa) = if memA a sa then dedupe sa else (a ∷ dedupe sa)

ΓC : List Arr
ΓC = dedupe (sa I ++ sa II ++ sa III ++ sa IV ++ sa V ++ sa VI ++ sa VII)

ΓC-size : length ΓC ≡ 16
ΓC-size = refl

-- Implication in Sub(ΓC): the largest subdigraph X with X ∩ A ⊆ B.
_⇒_ : Sub → Sub → Sub
A ⇒ B = sub XV (filter (λ a → (not (memA a (sa A)) || memA a (sa B))
                               && memN (tl a) XV && memN (hd a) XV) ΓC)
  where
  XV : List Nat
  XV = filter (λ v → not (memN v (sv A)) || memN v (sv B)) scale

∪ : List Sub → Sub
∪ []       = sub [] []
∪ (s ∷ ss) = sub (sv s ++ sv (∪ ss)) (sa s ++ sa (∪ ss))

sameSub : Sub → Sub → Bool
sameSub X Y = all (λ v → memN v (sv Y)) (sv X) && all (λ v → memN v (sv X)) (sv Y)
              && all (λ a → memA a (sa Y)) (sa X) && all (λ a → memA a (sa X)) (sa Y)

-- The book's first implication holds.
V⇒I : sameSub (V ⇒ I) (∪ (I ∷ IV ∷ VI ∷ [])) ≡ true
V⇒I = refl

-- The second differs from III u V (adversarial/GesturesBookIVimpliesV.agda
-- states the book's claim): same vertices, one extra arrow, VII's loop at 11.
IV⇒V-vertices : (all (λ v → memN v (sv (∪ (III ∷ V ∷ [])))) (sv (IV ⇒ V))
                 && all (λ v → memN v (sv (IV ⇒ V))) (sv (∪ (III ∷ V ∷ [])))) ≡ true
IV⇒V-vertices = refl

IV⇒V-extra : filter (λ a → not (memA a (sa (∪ (III ∷ V ∷ []))))) (sa (IV ⇒ V))
             ≡ arr 11 11 ∷ []
IV⇒V-extra = refl

IV⇒V-covers : all (λ a → memA a (sa (IV ⇒ V))) (sa (∪ (III ∷ V ∷ []))) ≡ true
IV⇒V-covers = refl

------------------------------------------------------------------------
-- Face coefficients (Def. 112, p. 969). For a digraph with no arrows
-- every vertex has coefficient 1; otherwise each arrow (t, h) adds the
-- coefficients of the digraph with t removed and subtracts those with h
-- removed (removing a vertex removes the arrows at it). The recursion
-- removes one vertex per step, so the vertex count is the fuel.

dropV : Nat → List Nat → List Arr → List Nat × List Arr
dropV x sv sa = filter (λ v → not (x ==ⁿ v)) sv ,
                filter (λ a → not (tl a ==ⁿ x) && not (hd a ==ⁿ x)) sa

φ : Nat → List Nat → List Arr → Nat → Int
φ fuel    sv []       v = if memN v sv then pos 1 else pos 0
φ zero    sv (_ ∷ _)  v = pos 0
φ (suc k) sv (a ∷ sa) v = sumArr (a ∷ sa)
  where
  sumArr : List Arr → Int
  sumArr []       = pos 0
  sumArr (b ∷ bs) =
    (φ k (fst (dropV (tl b) sv (a ∷ sa))) (snd (dropV (tl b) sv (a ∷ sa))) v
     +ℤ negℤ (φ k (fst (dropV (hd b) sv (a ∷ sa))) (snd (dropV (hd b) sv (a ∷ sa))) v))
    +ℤ sumArr bs

faces : Digraph → List Int
faces D = map (φ (nv D) (range (nv D)) (arrows D)) (range (nv D))

faces-point    : faces point ≡ pos 1 ∷ []
faces-point    = refl
faces-arrow    : faces arrowD ≡ negsuc 0 ∷ pos 1 ∷ []
faces-arrow    = refl
faces-path2    : faces path2 ≡ pos 1 ∷ negsuc 1 ∷ pos 1 ∷ []
faces-path2    = refl
faces-path3    : faces path3 ≡ negsuc 0 ∷ pos 3 ∷ negsuc 2 ∷ pos 1 ∷ []
faces-path3    = refl
faces-loop     : faces loopD ≡ pos 0 ∷ []
faces-loop     = refl
faces-parallel : faces parallel ≡ negsuc 1 ∷ pos 2 ∷ []
faces-parallel = refl
faces-cycle2   : faces cycle2 ≡ pos 0 ∷ pos 0 ∷ []
faces-cycle2   = refl
faces-fork     : faces fork ≡ pos 2 ∷ pos 1 ∷ pos 1 ∷ []
faces-fork     = refl
faces-join     : faces join ≡ pos 2 ∷ negsuc 2 ∷ negsuc 2 ∷ []
faces-join     = refl
faces-Ω        : faces Ω ≡ pos 0 ∷ pos 0 ∷ []
faces-Ω        = refl
