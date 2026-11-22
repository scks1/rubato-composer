{-# OPTIONS --safe --without-K #-}

-- A disc has Euler characteristic 1; the harmonic strip, a Moebius
-- strip, has 0 (f-vector 7, 14, 7). A claim that V + F = E + 1 must not
-- pass.
-- EXPECT: 14 != 15 of type Nat

module adversarial.GlobalEulerOne where

open import Prelude
open import Global

strip-disc : faces 0 strip + faces 2 strip ≡ faces 1 strip + 1
strip-disc = refl
