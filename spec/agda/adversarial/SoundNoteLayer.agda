{-# OPTIONS --safe --without-K #-}

-- EXPECT: 6 != 7
-- SoundNote with the Layer coordinate removed in upstream 376ff94 counted back in.

module adversarial.SoundNoteLayer where

open import Prelude
open import OrderedSets
open import Forms
open import Registry
open import Generated using (javaEnv; javaIntegerSetOverflow; javaRealSetNaN)

bad : factorCount handEnv "SoundNote" ≡ just 7
bad = refl
