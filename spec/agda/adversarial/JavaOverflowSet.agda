{-# OPTIONS --safe --without-K #-}

-- EXPECT: false != true
-- The IntegerSet Java stores for 1, MIN_VALUE, 0 (D5).

module adversarial.JavaOverflowSet where

open import Prelude
open import OrderedSets
open import Forms
open import Registry
open import Generated using (javaEnv; javaIntegerSetOverflow; javaRealSetNaN)

bad : javaEnv ⊢ javaIntegerSetOverflow ∶ "IntegerSet"
bad = sound _ _ _ refl
