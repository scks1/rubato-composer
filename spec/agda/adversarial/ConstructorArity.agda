{-# OPTIONS --safe --without-K #-}

-- EXPECT: [] != ("Onset"
-- Builds a Note derivation by hand with no coordinates.

module adversarial.ConstructorArity where

open import Prelude
open import OrderedSets
open import Forms
open import Registry
open import Generated using (javaEnv; javaIntegerSetOverflow; javaRealSetNaN)

bad : handEnv ⊢ limitD nothing "Note" [] ∶ "Note"
bad = limit refl []
