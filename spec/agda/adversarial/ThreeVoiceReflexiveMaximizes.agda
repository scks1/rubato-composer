{-# OPTIONS --safe --without-K #-}

-- The reflexive relaxation (spec D13, ours) has no joint maximization, so
-- it admits E G C to E♭ G C, which strict and hold drop as not maximal. A
-- claim that reflexive refuses it must not pass.
-- EXPECT: true != false of type Bool

module adversarial.ThreeVoiceReflexiveMaximizes where

open import Prelude
open import Counterpoint
open import ThreeVoice

reflexive-refuses : reflAdmitted (st 4 3 8) (st 3 4 9) ≡ false
reflexive-refuses = refl
