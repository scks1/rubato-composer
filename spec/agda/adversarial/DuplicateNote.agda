{-# OPTIONS --safe --without-K #-}

-- EXPECT: false != true
-- A Score that holds the same note twice.

module adversarial.DuplicateNote where

open import Prelude
open import OrderedSets
open import Forms
open import Registry
open import Generated using (javaEnv; javaIntegerSetOverflow; javaRealSetNaN)

bad : handEnv ⊢ powerD nothing "Score" (n0 ∷ n0 ∷ []) ∶ "Score"
bad = sound _ _ _ refl
