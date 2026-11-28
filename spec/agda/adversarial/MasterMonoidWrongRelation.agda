{-# OPTIONS --safe --without-K #-}

-- In M₂, d¹₀ d²₁ keeps vertex 2 of a triangle and d¹₁ d²₁ keeps vertex 0.
-- A file claiming the two words agree (on C major: G = C) must fail.
-- EXPECT: keep2 != keep0 of type Arr

module adversarial.MasterMonoidWrongRelation where

open import Prelude
open import MasterMonoid

wrong : normalize (d10 ∷ d21 ∷ []) ≡ normalize (d11 ∷ d21 ∷ [])
wrong = refl
