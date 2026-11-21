{-# OPTIONS --safe --without-K #-}

-- EXPECT: false != true
-- A Note with four coordinates. Java pads it (D9); the spec rejects it.

module adversarial.ShortNote where

open import Prelude
open import OrderedSets
open import Forms
open import Registry
open import Generated using (javaEnv; javaIntegerSetOverflow; javaRealSetNaN)

bad : handEnv ⊢ limitD nothing "Note" (onset 0.0 ∷ pitch' 0.0 ∷ loud 0.0 ∷ dur 1.0 ∷ []) ∶ "Note"
bad = sound _ _ _ refl
