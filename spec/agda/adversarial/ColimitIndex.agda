{-# OPTIONS --safe --without-K #-}

-- EXPECT: false != true
-- Colimit index 5 on a two-cofactor form; Java throws (D10).

module adversarial.ColimitIndex where

open import Prelude
open import OrderedSets
open import Forms
open import Registry
open import Generated using (javaEnv; javaIntegerSetOverflow; javaRealSetNaN)

bad : handEnv ⊢ colimitD nothing "IntegerOrReal" 5 (simpleD nothing "Integer" (zE (pos 1))) ∶ "IntegerOrReal"
bad = sound _ _ _ refl
