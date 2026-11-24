{-# OPTIONS --safe --without-K #-}

-- The factors of Figure 2's root (2, 2.5, 3) sum to 15/2, not to its label
-- 19/2 (p. 55: they need not). A claim that they fill the root must fail.
-- EXPECT: false != true of type Bool

module adversarial.RhythmTreesFactorsFillRoot where

open import Prelude
open import RhythmTrees

wrong : (sumQ (2 / 1 ∷ 5 / 2 ∷ 3 / 1 ∷ []) ≈ᵇ (19 / 2)) ≡ true
wrong = refl
