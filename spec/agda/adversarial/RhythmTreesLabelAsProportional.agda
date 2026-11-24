{-# OPTIONS --safe --without-K #-}

-- Proportional timing (spec D1) is not the label reading: Figure 2's leaves
-- do not last their own labels 2, 2, 1, 1, 1, 3/2, 2 in it.
-- EXPECT: false != true of type Bool

module adversarial.RhythmTreesLabelAsProportional where

open import Prelude
open import RhythmTrees

wrong :
  allEq (proportionalLeaves figure2)
        (2 / 1 ∷ 2 / 1 ∷ 1 / 1 ∷ 1 / 1 ∷ 1 / 1 ∷ 3 / 2 ∷ 2 / 1 ∷ []) ≡ true
wrong = refl
