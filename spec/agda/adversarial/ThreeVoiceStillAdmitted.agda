{-# OPTIONS --safe --without-K #-}

-- Eq. (4) (arXiv 2606.01102, p. 6): no admitted step keeps an active pair
-- stationary. C E G to C E♭ G holds the fifth C-G between the lower and
-- upper voices, so the paper's relation refuses it (the hold-fibre
-- relaxation, ours, admits it). A claim that the strict relation admits it
-- must not pass.
-- EXPECT: false != true of type Bool

module adversarial.ThreeVoiceStillAdmitted where

open import Prelude
open import Counterpoint
open import ThreeVoice

held-fifth : memSt (st 0 3 7) (succ (st 0 4 7)) ≡ true
held-fifth = refl
