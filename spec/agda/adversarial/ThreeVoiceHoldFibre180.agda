{-# OPTIONS --safe --without-K #-}

-- The hold-fibre relaxation (spec D6) admits 168 of the 192 parsimonious
-- realizations whose target lies in the mask; 180 is the figure of the
-- assessment's other relaxation, "reflexive", which the build does not
-- offer. A claim that hold-fibre admits 180 must not pass.
-- EXPECT: 168 != 180 of type Nat

module adversarial.ThreeVoiceHoldFibre180 where

open import Prelude
open import Counterpoint
open import ThreeVoice

hold-180 : countR holdR realizations ≡ 180
hold-180 = refl
