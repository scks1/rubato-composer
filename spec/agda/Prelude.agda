{-# OPTIONS --safe --without-K #-}

-- Prelude.agda: the few definitions the spec needs, built on Agda's
-- primitive modules only (no standard library).

module Prelude where

open import Agda.Builtin.Equality public
open import Agda.Builtin.Bool public
open import Agda.Builtin.Nat public renaming (_<_ to _<ᵇ_; _==_ to _==ⁿ_)
open import Agda.Builtin.Int public
open import Agda.Builtin.List public
open import Agda.Builtin.Maybe public
open import Agda.Builtin.Unit public
open import Agda.Builtin.Sigma public
open import Agda.Builtin.String public
open import Agda.Builtin.Char public
open import Agda.Builtin.Float public
open import Agda.Builtin.String.Properties
open import Agda.Builtin.Char.Properties

-- Agda 2.6.3's primitive library has no empty type.
data ⊥ : Set where

¬_ : Set → Set
¬ A = A → ⊥

infixr 6 _&&_
_&&_ : Bool → Bool → Bool
true  && b = b
false && _ = false

infixr 5 _||_
_||_ : Bool → Bool → Bool
true  || _ = true
false || b = b

not : Bool → Bool
not true  = false
not false = true

if_then_else_ : {A : Set} → Bool → A → A → A
if true  then x else _ = x
if false then _ else y = y

cong : {A B : Set} (f : A → B) {x y : A} → x ≡ y → f x ≡ f y
cong f refl = refl

sym : {A : Set} {x y : A} → x ≡ y → y ≡ x
sym refl = refl

true≢false : true ≡ false → ⊥
true≢false ()

&&-left : (a b : Bool) → (a && b) ≡ true → a ≡ true
&&-left true  _ _ = refl
&&-left false _ ()

&&-right : (a b : Bool) → (a && b) ≡ true → b ≡ true
&&-right true  _ p = p
&&-right false _ ()

&&-intro : {a b : Bool} → a ≡ true → b ≡ true → (a && b) ≡ true
&&-intro refl refl = refl

length : {A : Set} → List A → Nat
length []       = 0
length (_ ∷ xs) = suc (length xs)

nth : {A : Set} → Nat → List A → Maybe A
nth _       []       = nothing
nth zero    (x ∷ _)  = just x
nth (suc n) (_ ∷ xs) = nth n xs

map : {A B : Set} → (A → B) → List A → List B
map f []       = []
map f (x ∷ xs) = f x ∷ map f xs

all : {A : Set} → (A → Bool) → List A → Bool
all p []       = true
all p (x ∷ xs) = p x && all p xs

any : {A : Set} → (A → Bool) → List A → Bool
any p []       = false
any p (x ∷ xs) = p x || any p xs

------------------------------------------------------------------------
-- Natural-number equality, with soundness and reflexivity

==ⁿ-sound : (m n : Nat) → (m ==ⁿ n) ≡ true → m ≡ n
==ⁿ-sound zero    zero    _ = refl
==ⁿ-sound zero    (suc n) ()
==ⁿ-sound (suc m) zero    ()
==ⁿ-sound (suc m) (suc n) p = cong suc (==ⁿ-sound m n p)

==ⁿ-refl : (n : Nat) → (n ==ⁿ n) ≡ true
==ⁿ-refl zero    = refl
==ⁿ-refl (suc n) = ==ⁿ-refl n

------------------------------------------------------------------------
-- String equality through character codes. Agda's primStringEquality
-- has no soundness lemma in the primitive library, so the spec decides
-- equality on code-point lists and proves it sound with the two
-- injectivity primitives.

eqChars : List Char → List Char → Bool
eqChars []       []       = true
eqChars (a ∷ as) (b ∷ bs) = (primCharToNat a ==ⁿ primCharToNat b) && eqChars as bs
eqChars _        _        = false

eqChars-sound : (as bs : List Char) → eqChars as bs ≡ true → as ≡ bs
eqChars-sound []       []       _ = refl
eqChars-sound []       (_ ∷ _)  ()
eqChars-sound (_ ∷ _)  []       ()
eqChars-sound (a ∷ as) (b ∷ bs) p
  with primCharToNatInjective a b
         (==ⁿ-sound (primCharToNat a) (primCharToNat b)
            (&&-left (primCharToNat a ==ⁿ primCharToNat b) (eqChars as bs) p))
... | refl with eqChars-sound as bs (&&-right (primCharToNat a ==ⁿ primCharToNat a) (eqChars as bs) p)
... | refl = refl

eqChars-refl : (as : List Char) → eqChars as as ≡ true
eqChars-refl []       = refl
eqChars-refl (a ∷ as) = &&-intro (==ⁿ-refl (primCharToNat a)) (eqChars-refl as)

infix 4 _==ˢ_
_==ˢ_ : String → String → Bool
s ==ˢ t = eqChars (primStringToList s) (primStringToList t)

==ˢ-sound : (s t : String) → (s ==ˢ t) ≡ true → s ≡ t
==ˢ-sound s t p = primStringToListInjective s t (eqChars-sound _ _ p)

==ˢ-refl : (s : String) → (s ==ˢ s) ≡ true
==ˢ-refl s = eqChars-refl (primStringToList s)

------------------------------------------------------------------------
-- Three-way comparison, the analogue of the sign of Java's compareTo

data Ordering : Set where
  lt eq gt : Ordering

flipO : Ordering → Ordering
flipO lt = gt
flipO eq = eq
flipO gt = lt

-- Java's `if ((c = ...) != 0) return c; else continue`
thenO : Ordering → Ordering → Ordering
thenO eq o = o
thenO lt _ = lt
thenO gt _ = gt

cmpNat : Nat → Nat → Ordering
cmpNat zero    zero    = eq
cmpNat zero    (suc _) = lt
cmpNat (suc _) zero    = gt
cmpNat (suc m) (suc n) = cmpNat m n

cmpNat-flip : (m n : Nat) → cmpNat n m ≡ flipO (cmpNat m n)
cmpNat-flip zero    zero    = refl
cmpNat-flip zero    (suc _) = refl
cmpNat-flip (suc _) zero    = refl
cmpNat-flip (suc m) (suc n) = cmpNat-flip m n

-- Exact integer order. Java's ZElement.compareTo subtracts 32-bit ints
-- and overflows; see README, departure D5.
cmpInt : Int → Int → Ordering
cmpInt (pos m)    (pos n)    = cmpNat m n
cmpInt (pos _)    (negsuc _) = gt
cmpInt (negsuc _) (pos _)    = lt
cmpInt (negsuc m) (negsuc n) = cmpNat n m

cmpInt-flip : (a b : Int) → cmpInt b a ≡ flipO (cmpInt a b)
cmpInt-flip (pos m)    (pos n)    = cmpNat-flip m n
cmpInt-flip (pos _)    (negsuc _) = refl
cmpInt-flip (negsuc _) (pos _)    = refl
cmpInt-flip (negsuc m) (negsuc n) = cmpNat-flip n m

-- Java's String.compareTo: lexicographic on characters, then the shorter
-- string first. Java compares UTF-16 code units and this compares code
-- points; the two agree on the Basic Multilingual Plane (README, D15).
cmpChars : List Char → List Char → Ordering
cmpChars []       []       = eq
cmpChars []       (_ ∷ _)  = lt
cmpChars (_ ∷ _)  []       = gt
cmpChars (a ∷ as) (b ∷ bs) = thenO (cmpNat (primCharToNat a) (primCharToNat b)) (cmpChars as bs)

cmpString : String → String → Ordering
cmpString s t = cmpChars (primStringToList s) (primStringToList t)

------------------------------------------------------------------------
-- Pointwise and universal predicates on lists

data All {A : Set} (P : A → Set) : List A → Set where
  []  : All P []
  _∷_ : {x : A} {xs : List A} → P x → All P xs → All P (x ∷ xs)
