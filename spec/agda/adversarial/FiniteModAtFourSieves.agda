{-# OPTIONS --safe --without-K #-}

-- Omega(Z2) over the affine maps of Z2 has five sieves: the empty one,
-- {0}, {1}, {0, 1} and all four arrows. A count of four must not pass.
-- EXPECT: 5 != 4 of type Nat

module adversarial.FiniteModAtFourSieves where

open import Prelude
open import FiniteModAt

four : length (sieves (Aff 2)) ≡ 4
four = refl
