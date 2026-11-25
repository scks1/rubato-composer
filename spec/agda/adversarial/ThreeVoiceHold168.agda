{-# OPTIONS --safe --without-K #-}

-- The hold relaxation (spec D12, ours) maximizes the moving pairs over all
-- states in the mask and admits 144 of the 192 parsimonious realizations
-- whose target lies in the mask; 168 is hold-fibre's figure. A claim that
-- hold admits 168 must not pass.
-- EXPECT: 144 != 168 of type Nat

module adversarial.ThreeVoiceHold168 where

open import Prelude
open import Counterpoint
open import ThreeVoice

hold-168 : countR holdAllR realizations ≡ 168
hold-168 = refl
