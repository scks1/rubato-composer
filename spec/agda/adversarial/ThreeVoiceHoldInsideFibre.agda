{-# OPTIONS --safe --without-K #-}

-- Hold (spec D12) is not contained in hold-fibre (D6): from (0; 0, 0) it
-- admits (3; 9, 9), which hold-fibre refuses (P03-hold-vs-fibre.txt). A
-- claim that hold refuses this step must not pass.
-- EXPECT: true != false of type Bool

module adversarial.ThreeVoiceHoldInsideFibre where

open import Prelude
open import Counterpoint
open import ThreeVoice

hold-refuses : holdAllAdmitted (st 0 0 0) (st 3 9 9) ≡ false
hold-refuses = refl
