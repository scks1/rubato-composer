{-# OPTIONS --safe --without-K #-}

-- VoiceLeading.agda: the voice-leading space of T/I acting on Z12 and on the
-- 24 consonant triads, with facts proved by computation.
--
-- Sources, by page (no text quoted): Drew Flieder, "Type Theory for the
-- Working Mathematical Music Theorist", arXiv 2512.04090v1:
--  * transporters: Example 2.11 (p. 18);
--  * Lewin's GIS and its two axioms: Example 3.1 (p. 23);
--  * the voice-leading space of a rule: Def. 4.1 (pp. 26-27);
--  * the rule of a group action and T/I on Z12: Example 4.1 (p. 28);
--  * conjugation-induced maps: Example 4.3 (p. 30);
--  * Sigma_VLS isomorphisms: section 4.4 and Prop. 4.1 (pp. 31-32).
--
-- The departures are in sources/VOICE-LEADING-SPEC.md. This module checks:
--  * every ordered pair of Z12 carries two transporters (288 arrows),
--    and those from 0 to 4 are T4 and I4;
--  * T alone gives one transporter per pair (a GIS, spec D9), T/I does not;
--  * T/I is simply transitive on the 24 triads (a GIS), T is not;
--  * among the 144 affine maps x -> ax + b of Z12, exactly 48 are
--    bijections that conjugate every element of T/I into T/I (the
--    vertex maps of the Sigma_VLS automorphisms, spec D6), and 24 of them
--    are themselves elements of T/I (Ex. 4.3).
-- That every normalizer element is affine is argued in the spec (D6) and
-- checked in Java by a search over all bijections; this module only counts
-- among the affine candidates. java/core's org.rubato.math.voiceleading
-- computes the same values (VoiceLeadingSpaceTest, GISTest).

module VoiceLeading where

open import Prelude

------------------------------------------------------------------------
-- Lists and Z12

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

sum : List Nat → Nat
sum []       = 0
sum (x ∷ xs) = x + sum xs

m12 : Nat → Nat
m12 x = mod-helper 0 11 x 11

z12 : List Nat
z12 = range 12

memN : Nat → List Nat → Bool
memN x xs = any (λ y → x ==ⁿ y) xs

------------------------------------------------------------------------
-- T/I as abstract elements and their action

data El : Set where
  T : Nat → El
  I : Nat → El

eqEl : El → El → Bool
eqEl (T a) (T b) = a ==ⁿ b
eqEl (I a) (I b) = a ==ⁿ b
eqEl _     _     = false

memEl : El → List El → Bool
memEl g gs = any (eqEl g) gs

ts : List El
ts = map T z12

tis : List El
tis = ts ++ map I z12

-- T k : x -> x + k, I k : x -> k - x, both mod 12 (inputs below 12)
act : El → Nat → Nat
act (T k) x = m12 (x + k)
act (I k) x = m12 ((k + 12) - x)

transp : List El → Nat → Nat → List El
transp gs x y = filter (λ g → act g x ==ⁿ y) gs

allPairs : (Nat → Nat → Bool) → Bool
allPairs p = all (λ x → all (λ y → p x y) z12) z12

------------------------------------------------------------------------
-- Pitch classes

two-everywhere : allPairs (λ x y → length (transp tis x y) ==ⁿ 2) ≡ true
two-everywhere = refl

arrows-288 : sum (concatMap (λ x → map (λ y → length (transp tis x y)) z12) z12) ≡ 288
arrows-288 = refl

zero-to-four : transp tis 0 4 ≡ T 4 ∷ I 4 ∷ []
zero-to-four = refl

three-to-four : transp tis 3 4 ≡ T 1 ∷ I 7 ∷ []
three-to-four = refl

-- T alone: one transporter per pair, the condition of spec D9
t-simply-transitive : allPairs (λ x y → length (transp ts x y) ==ⁿ 1) ≡ true
t-simply-transitive = refl

ti-not-simply-transitive : allPairs (λ x y → length (transp tis x y) ==ⁿ 1) ≡ false
ti-not-simply-transitive = refl

------------------------------------------------------------------------
-- Affine vertex maps that normalize T/I

Aff : Set
Aff = Σ Nat (λ _ → Nat)

affines : List Aff
affines = concatMap (λ a → map (λ b → (a , b)) z12) z12

app : Aff → Nat → Nat
app (a , b) x = m12 (a * x + b)

bijective : Aff → Bool
bijective h = all (λ y → any (λ x → app h x ==ⁿ y) z12) z12

-- h g h⁻¹ lies in T/I when some f in T/I has f (h x) = h (g x) for every x
conjugatesInto : Aff → El → Bool
conjugatesInto h g = any (λ f → all (λ x → act f (app h x) ==ⁿ app h (act g x)) z12) tis

normalizes : Aff → Bool
normalizes h = bijective h && all (conjugatesInto h) tis

normalizer : List Aff
normalizer = filter normalizes affines

normalizer-48 : length normalizer ≡ 48
normalizer-48 = refl

-- the affine maps that are elements of T/I: a = 1 (T b) or a = 11 (I b)
inTI : Aff → Bool
inTI (a , _) = (a ==ⁿ 1) || (a ==ⁿ 11)

conjugation-induced-24 : length (filter inTI normalizer) ≡ 24
conjugation-induced-24 = refl

-- the units: x -> 5x and x -> 7x normalize T/I but are not in it
five-normalizes : normalizes (5 , 0) ≡ true
five-normalizes = refl

two-does-not : normalizes (2 , 0) ≡ false
two-does-not = refl

------------------------------------------------------------------------
-- Consonant triads as sets of pitch classes

Triad : Set
Triad = List Nat

major minor : Nat → Triad
major r = r ∷ m12 (r + 4) ∷ m12 (r + 7) ∷ []
minor r = r ∷ m12 (r + 3) ∷ m12 (r + 7) ∷ []

triads : List Triad
triads = map major z12 ++ map minor z12

sameSet : Triad → Triad → Bool
sameSet s t = all (λ x → memN x t) s && all (λ x → memN x s) t

actT : El → Triad → Triad
actT g t = map (act g) t

transpT : List El → Triad → Triad → List El
transpT gs s t = filter (λ g → sameSet (actT g s) t) gs

allTriadPairs : (Triad → Triad → Bool) → Bool
allTriadPairs p = all (λ s → all (λ t → p s t) triads) triads

ti-triads-simply-transitive : allTriadPairs (λ s t → length (transpT tis s t) ==ⁿ 1) ≡ true
ti-triads-simply-transitive = refl

t-triads-not : length (transpT ts (major 0) (minor 0)) ≡ 0
t-triads-not = refl

c-to-a-minor : transpT tis (major 0) (minor 9) ≡ I 4 ∷ []
c-to-a-minor = refl

a-minor-to-g : transpT tis (minor 9) (major 7) ≡ I 11 ∷ []
a-minor-to-g = refl
