{-# OPTIONS --safe --without-K #-}

-- The subobject classifier of digraphs has five arrows (two loops at
-- the maximal vertex). A four-arrow Omega must not pass.
-- EXPECT: 5 != 4 of type Nat

module adversarial.GesturesOmegaFourArrows where

open import Prelude
open import Gestures

four : length (arrows Ω) ≡ 4
four = refl
