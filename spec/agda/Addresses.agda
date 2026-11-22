{-# OPTIONS --safe --without-K #-}

-- Addresses.agda: denotators at an address, for the free modules over a
-- ring, and the change of address along an affine map.
--
-- Topos of Music I, section 6.2.1 (p. 53): an A-addressed point of a
-- module M is a morphism A -> M; for free modules over a ring these are
-- the affine maps. Section 6.2.2 (p. 54): an address change along
-- f : B -> A sends a point x : A -> M to x . f : B -> M. Evaluation at
-- an element a of A is the address change along the constant map from
-- the zero module with value a (pp. 53-54).
--
-- The model keeps a point as an affine map in matrix form, which is what
-- the Java classes ZFreeAffineMorphism, QFreeAffineMorphism and
-- RFreeAffineMorphism hold, and defines composition of affine maps by
-- matrix product. The theorem `compose-apply` proves that this
-- composition is precomposition of the maps, for any ring whose laws are
-- given as hypotheses (`Laws`). No instance proves the laws: the integers
-- satisfy them but the proofs are not written here, the unreduced
-- fractions used for Q satisfy them only up to cross-multiplication, and
-- floating point does not satisfy them. The Java behaviour is checked
-- against the model on exported cases in GeneratedAddresses.agda.

module Addresses where

open import Prelude hiding (_+_; _*_)
open import Agda.Builtin.Nat using () renaming (_+_ to _+ⁿ_; _*_ to _*ⁿ_)

infixr 5 _∷ᵛ_
infixl 6 _+ℤ_
infixl 7 _*ℤ_

_∙_ : {A : Set} {x y z : A} → x ≡ y → y ≡ z → x ≡ z
refl ∙ q = q

cong₂ : {A B C : Set} (f : A → B → C) {a a' : A} {b b' : B} →
        a ≡ a' → b ≡ b' → f a b ≡ f a' b'
cong₂ f refl refl = refl

------------------------------------------------------------------------
-- Vectors

data Vec (A : Set) : Nat → Set where
  ⟨⟩    : Vec A zero
  _∷ᵛ_ : {n : Nat} → A → Vec A n → Vec A (suc n)

mapV : {A B : Set} {n : Nat} → (A → B) → Vec A n → Vec B n
mapV f ⟨⟩        = ⟨⟩
mapV f (x ∷ᵛ xs) = f x ∷ᵛ mapV f xs

replicateV : {A : Set} (n : Nat) → A → Vec A n
replicateV zero    _ = ⟨⟩
replicateV (suc n) a = a ∷ᵛ replicateV n a

allV : {A : Set} {n : Nat} → (A → Bool) → Vec A n → Bool
allV p ⟨⟩        = true
allV p (x ∷ᵛ xs) = p x && allV p xs

------------------------------------------------------------------------
-- Rings: the operations, and the laws as a separate record.

record Ring : Set₁ where
  field
    C       : Set
    0# 1#   : C
    _+_ _*_ : C → C → C
    _≟_     : C → C → Bool

record Laws (R : Ring) : Set where
  open Ring R
  field
    +-assoc  : (a b c : C) → (a + b) + c ≡ a + (b + c)
    +-comm   : (a b : C) → a + b ≡ b + a
    +-idˡ    : (a : C) → 0# + a ≡ a
    *-assoc  : (a b c : C) → (a * b) * c ≡ a * (b * c)
    *-zeroˡ  : (a : C) → 0# * a ≡ 0#
    *-zeroʳ  : (a : C) → a * 0# ≡ 0#
    distribˡ : (a b c : C) → a * (b + c) ≡ (a * b) + (a * c)
    distribʳ : (a b c : C) → (a + b) * c ≡ (a * c) + (b * c)

------------------------------------------------------------------------
-- Free modules C^n and affine maps C^m -> C^n over a ring.

module Free (R : Ring) where
  open Ring R

  infixl 6 _⊕_

  zeroV : (n : Nat) → Vec C n
  zeroV n = replicateV n 0#

  _⊕_ : {n : Nat} → Vec C n → Vec C n → Vec C n
  ⟨⟩        ⊕ ⟨⟩        = ⟨⟩
  (a ∷ᵛ as) ⊕ (b ∷ᵛ bs) = (a + b) ∷ᵛ (as ⊕ bs)

  _⊛_ : {n : Nat} → C → Vec C n → Vec C n
  r ⊛ v = mapV (λ a → r * a) v

  dot : {m : Nat} → Vec C m → Vec C m → C
  dot ⟨⟩        ⟨⟩        = 0#
  dot (a ∷ᵛ as) (b ∷ᵛ bs) = (a * b) + dot as bs

  -- n rows of length m: a linear map C^m -> C^n
  Matrix : Nat → Nat → Set
  Matrix n m = Vec (Vec C m) n

  mul : {n m : Nat} → Matrix n m → Vec C m → Vec C n
  mul A v = mapV (λ r → dot r v) A

  -- the combination of the rows of B with the coefficients r
  lincomb : {m k : Nat} → Vec C m → Matrix m k → Vec C k
  lincomb {k = k} ⟨⟩        ⟨⟩        = zeroV k
  lincomb         (r ∷ᵛ rs) (b ∷ᵛ bs) = (r ⊛ b) ⊕ lincomb rs bs

  record Affine (m n : Nat) : Set where
    constructor affine
    field
      lin   : Matrix n m
      shift : Vec C n
  open Affine public

  -- the value of the point at an element of its address
  apply : {m n : Nat} → Affine m n → Vec C m → Vec C n
  apply f v = mul (lin f) v ⊕ shift f

  -- f . g by matrix product
  compose : {k m n : Nat} → Affine m n → Affine k m → Affine k n
  compose f g = affine (mapV (λ r → lincomb r (lin g)) (lin f))
                       (mul (lin f) (shift g) ⊕ shift f)

  -- the address change of a point d along f
  changeAddress : {k m n : Nat} → Affine m n → Affine k m → Affine k n
  changeAddress d f = compose d f

  -- the constant map from the zero module with value a
  constFrom0 : {m : Nat} → Vec C m → Affine zero m
  constFrom0 {m} a = affine (replicateV m ⟨⟩) a

  -- evaluation at a, as Java's Denotator.at computes it
  at : {m n : Nat} → Affine m n → Vec C m → Vec C n
  at d a = apply d a

  eqV : {n : Nat} → Vec C n → Vec C n → Bool
  eqV ⟨⟩        ⟨⟩        = true
  eqV (a ∷ᵛ as) (b ∷ᵛ bs) = (a ≟ b) && eqV as bs

  ----------------------------------------------------------------------
  -- Composition is precomposition, for a ring with its laws.

  module Proofs (L : Laws R) where
    open Laws L

    -- (a + b) + (c + d) = (a + c) + (b + d)
    interchange : (a b c d : C) → (a + b) + (c + d) ≡ (a + c) + (b + d)
    interchange a b c d =
      +-assoc a b (c + d)
      ∙ (cong (λ x → a + x) (sym (+-assoc b c d))
      ∙ (cong (λ x → a + (x + d)) (+-comm b c)
      ∙ (cong (λ x → a + x) (+-assoc c b d)
      ∙ sym (+-assoc a c (b + d)))))

    dot-zeroˡ : {m : Nat} (v : Vec C m) → dot (zeroV m) v ≡ 0#
    dot-zeroˡ ⟨⟩        = refl
    dot-zeroˡ (b ∷ᵛ bs) = cong₂ _+_ (*-zeroˡ b) (dot-zeroˡ bs) ∙ +-idˡ 0#

    dot-⊕ˡ : {m : Nat} (x y v : Vec C m) → dot (x ⊕ y) v ≡ dot x v + dot y v
    dot-⊕ˡ ⟨⟩        ⟨⟩        ⟨⟩        = sym (+-idˡ 0#)
    dot-⊕ˡ (a ∷ᵛ as) (b ∷ᵛ bs) (c ∷ᵛ cs) =
      cong₂ _+_ (distribʳ a b c) (dot-⊕ˡ as bs cs)
      ∙ interchange (a * c) (b * c) (dot as cs) (dot bs cs)

    dot-⊕ʳ : {m : Nat} (r x y : Vec C m) → dot r (x ⊕ y) ≡ dot r x + dot r y
    dot-⊕ʳ ⟨⟩        ⟨⟩        ⟨⟩        = sym (+-idˡ 0#)
    dot-⊕ʳ (a ∷ᵛ as) (b ∷ᵛ bs) (c ∷ᵛ cs) =
      cong₂ _+_ (distribˡ a b c) (dot-⊕ʳ as bs cs)
      ∙ interchange (a * b) (a * c) (dot as bs) (dot as cs)

    dot-⊛ˡ : {m : Nat} (r : C) (x v : Vec C m) → dot (r ⊛ x) v ≡ r * dot x v
    dot-⊛ˡ r ⟨⟩        ⟨⟩        = sym (*-zeroʳ r)
    dot-⊛ˡ r (a ∷ᵛ as) (b ∷ᵛ bs) =
      cong₂ _+_ (*-assoc r a b) (dot-⊛ˡ r as bs)
      ∙ sym (distribˡ r (a * b) (dot as bs))

    dot-lincomb : {m k : Nat} (r : Vec C m) (B : Matrix m k) (v : Vec C k) →
                  dot (lincomb r B) v ≡ dot r (mul B v)
    dot-lincomb ⟨⟩        ⟨⟩        v = dot-zeroˡ v
    dot-lincomb (r ∷ᵛ rs) (b ∷ᵛ bs) v =
      dot-⊕ˡ (r ⊛ b) (lincomb rs bs) v
      ∙ cong₂ _+_ (dot-⊛ˡ r b v) (dot-lincomb rs bs v)

    rows : {n m k : Nat} (B : Matrix m k) (t : Vec C m) (v : Vec C k)
           (A : Matrix n m) (s : Vec C n) →
           mul (mapV (λ r → lincomb r B) A) v ⊕ (mul A t ⊕ s)
             ≡ mul A (mul B v ⊕ t) ⊕ s
    rows B t v ⟨⟩        ⟨⟩        = refl
    rows B t v (r ∷ᵛ rs) (s ∷ᵛ ss) = cong₂ _∷ᵛ_ head (rows B t v rs ss)
      where
        head : dot (lincomb r B) v + (dot r t + s) ≡ dot r (mul B v ⊕ t) + s
        head = cong (λ x → x + (dot r t + s)) (dot-lincomb r B v)
               ∙ (sym (+-assoc (dot r (mul B v)) (dot r t) s)
               ∙ cong (λ x → x + s) (sym (dot-⊕ʳ r (mul B v) t)))

    -- composition by matrix product is precomposition
    compose-apply : {k m n : Nat} (f : Affine m n) (g : Affine k m) (v : Vec C k) →
                    apply (compose f g) v ≡ apply f (apply g v)
    compose-apply f g v = rows (lin g) (shift g) v (lin f) (shift f)

    -- the constant map from the zero module takes the value a
    const-apply : {m : Nat} (a : Vec C m) → apply (constFrom0 a) ⟨⟩ ≡ a
    const-apply ⟨⟩        = refl
    const-apply (x ∷ᵛ xs) = cong₂ _∷ᵛ_ (+-idˡ x) (const-apply xs)

    -- Topos I, pp. 53-54: evaluation at a is the address change along
    -- the constant map 0 -> A with value a
    at-is-change : {m n : Nat} (d : Affine m n) (a : Vec C m) →
                   at d a ≡ apply (changeAddress d (constFrom0 a)) ⟨⟩
    at-is-change d a = sym (compose-apply d (constFrom0 a) ⟨⟩ ∙ cong (apply d) (const-apply a))

    -- address change is functorial: changing along f, then along g, is
    -- changing along f . g
    change-functorial : {j k m n : Nat} (d : Affine m n) (f : Affine k m) (g : Affine j k)
                        (v : Vec C j) →
                        apply (changeAddress (changeAddress d f) g) v
                          ≡ apply (changeAddress d (compose f g)) v
    change-functorial d f g v =
      compose-apply (compose d f) g v
      ∙ (compose-apply d f (apply g v)
      ∙ sym (compose-apply d (compose f g) v ∙ cong (apply d) (compose-apply f g v)))

  ----------------------------------------------------------------------
  -- Cases exported from Java (GeneratedAddresses.agda).

  record Sample (k n : Nat) : Set where
    constructor _↦_
    field
      point : Vec C k
      value : Vec C n

  -- a point d at the address C^m, an address change f : C^k -> C^m, and
  -- Java's values of d.changeAddress(f) at sample elements of C^k
  record ChangeCase : Set where
    constructor change
    field
      k m n   : Nat
      d       : Affine m n
      f       : Affine k m
      samples : List (Sample k n)

  -- a point d at C^m, an element a, and Java's value of d.at(a)
  record AtCase : Set where
    constructor atCase
    field
      m n  : Nat
      d    : Affine m n
      a    : Vec C m
      java : Vec C n

  -- Java's value equals the model's composition and the precomposition
  checkChange : ChangeCase → Bool
  checkChange (change k m n d f ss) =
    all (λ s → eqV (apply (compose d f) (Sample.point s)) (Sample.value s)
            && eqV (apply d (apply f (Sample.point s))) (Sample.value s)) ss

  -- Java's d.at(a) equals the model's value and the value of the change
  -- along the constant map
  checkAt : AtCase → Bool
  checkAt (atCase m n d a j) =
    eqV (at d a) j && eqV (apply (changeAddress d (constFrom0 a)) ⟨⟩) j

------------------------------------------------------------------------
-- The rings: Z with exact integers, Q as unreduced fractions with a
-- positive denominator compared by cross-multiplication, R as floats.
-- Java's int and Rational arithmetic overflow where these do not (D5);
-- the exported cases stay far from overflow.

_⊖_ : Nat → Nat → Int
zero  ⊖ zero  = pos zero
suc m ⊖ zero  = pos (suc m)
zero  ⊖ suc n = negsuc n
suc m ⊖ suc n = m ⊖ n

_+ℤ_ : Int → Int → Int
pos m    +ℤ pos n    = pos (m +ⁿ n)
pos m    +ℤ negsuc n = m ⊖ suc n
negsuc m +ℤ pos n    = n ⊖ suc m
negsuc m +ℤ negsuc n = negsuc (suc (m +ⁿ n))

negℕ : Nat → Int
negℕ zero    = pos zero
negℕ (suc k) = negsuc k

_*ℤ_ : Int → Int → Int
pos m    *ℤ pos n    = pos (m *ⁿ n)
pos m    *ℤ negsuc n = negℕ (m *ⁿ suc n)
negsuc m *ℤ pos n    = negℕ (suc m *ⁿ n)
negsuc m *ℤ negsuc n = pos (suc m *ⁿ suc n)

eqℤ : Int → Int → Bool
eqℤ (pos m)    (pos n)    = m ==ⁿ n
eqℤ (negsuc m) (negsuc n) = m ==ⁿ n
eqℤ _          _          = false

ℤ-ring : Ring
ℤ-ring = record { C = Int ; 0# = pos 0 ; 1# = pos 1 ; _+_ = _+ℤ_ ; _*_ = _*ℤ_ ; _≟_ = eqℤ }

-- Int times a natural number
_*ℤℕ_ : Int → Nat → Int
a *ℤℕ d = a *ℤ pos d

record ℚ : Set where
  constructor _/_
  field
    num : Int
    den : Nat

addQ mulQ : ℚ → ℚ → ℚ
addQ (a / b) (c / d) = ((a *ℤℕ d) +ℤ (c *ℤℕ b)) / (b *ⁿ d)
mulQ (a / b) (c / d) = (a *ℤ c) / (b *ⁿ d)

eqQ : ℚ → ℚ → Bool
eqQ (a / b) (c / d) = eqℤ (a *ℤℕ d) (c *ℤℕ b)

ℚ-ring : Ring
ℚ-ring = record { C = ℚ ; 0# = pos 0 / 1 ; 1# = pos 1 / 1 ; _+_ = addQ ; _*_ = mulQ ; _≟_ = eqQ }

ℝ-ring : Ring
ℝ-ring = record { C = Float ; 0# = 0.0 ; 1# = 1.0 ; _+_ = primFloatPlus ; _*_ = primFloatTimes
                ; _≟_ = primFloatEquality }
