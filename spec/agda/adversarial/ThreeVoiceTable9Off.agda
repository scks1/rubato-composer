{-# OPTIONS --safe --without-K #-}

-- Table 9 (arXiv 2606.01102, p. 25) gives 256 successors for h11 = (3, 9).
-- A count one lower must not pass.
-- EXPECT: 256 != 255 of type Nat

module adversarial.ThreeVoiceTable9Off where

open import Prelude
open import Counterpoint
open import ThreeVoice

h11 : length (succ (st 0 3 9)) ≡ 255
h11 = refl
