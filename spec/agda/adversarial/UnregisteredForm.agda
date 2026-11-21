{-# OPTIONS --safe --without-K #-}

-- EXPECT: false != true
-- A denotator of the unregistered form Layer.

module adversarial.UnregisteredForm where

open import Prelude
open import OrderedSets
open import Forms
open import Registry
open import Generated using (javaEnv; javaIntegerSetOverflow; javaRealSetNaN)

bad : handEnv ⊢ simpleD nothing "Layer" (zE (pos 0)) ∶ "Layer"
bad = sound _ _ _ refl
