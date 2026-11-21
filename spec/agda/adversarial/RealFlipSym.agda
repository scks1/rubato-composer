{-# OPTIONS --safe --without-K #-}

-- EXPECT: gt != lt
-- Claims RElement.compareTo is flip-symmetric at NaN and 1.0 (D6).

module adversarial.RealFlipSym where

open import Prelude
open import OrderedSets
open import Forms
open import Registry
open import Generated using (javaEnv; javaIntegerSetOverflow; javaRealSetNaN)

-- FlipSym cmpR at a = NaN, b = 1.0
bad : cmpR 1.0 nanF ≡ flipO (cmpR nanF 1.0)
bad = refl
