{-# OPTIONS --safe --without-K #-}

-- d²₀ d¹₀ is reduced: d²₀ leaves [2] and d¹₀ lands in [0]. A file claiming
-- it reduces to one letter must fail.
-- EXPECT: d20 != keep2 of type Arr

module adversarial.MasterMonoidComposablePair where

open import Prelude
open import MasterMonoid

wrong : normalize (d20 ∷ d10 ∷ []) ≡ keep2 ∷ []
wrong = refl
