{-# OPTIONS --safe --without-K #-}

-- Fig. 38.5 (Topos II p. 629) has four leaves, LA7, LB7, RA7 and RB7.
-- A claim of five must not pass.
-- EXPECT: 4 != 5 of type Nat

module adversarial.PerformanceFig385FiveLeaves where

open import Prelude
open import Performance

bad : leaves fig38-5 ≡ 5
bad = refl
