{-# OPTIONS --safe --without-K #-}

-- Reflexive (spec D13, ours) is V1 with held pairs skipped: from the 26
-- sources with lower voice 0 it admits V1's 5,334 steps and 213 more with a
-- held pair. A claim that it admits exactly V1's 5,334 must not pass.
-- EXPECT: 5547 != 5334 of type Nat

module adversarial.ThreeVoiceReflexiveIsV1 where

open import Prelude
open import Counterpoint
open import ThreeVoice

reflexive-is-v1 : reflEdges ≡ 5334
reflexive-is-v1 = refl
