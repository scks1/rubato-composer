{-# OPTIONS --safe --without-K #-}

-- EXPECT: false != true
-- The RealSet Java stores for NaN, NaN, 1.0 (D6).

module adversarial.JavaNaNSet where

open import Prelude
open import OrderedSets
open import Forms
open import Registry
open import Generated using (javaEnv; javaIntegerSetOverflow; javaRealSetNaN)

bad : javaEnv ⊢ javaRealSetNaN ∶ "RealSet"
bad = sound _ _ _ refl
