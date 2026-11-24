{-# OPTIONS --safe --without-K #-}

-- The affine bijections of Z3 form a group, and a group has only the
-- empty and the maximal sieve. Counting a sieve per subgroup must not pass.
-- EXPECT: 2 != 4 of type Nat

module adversarial.FiniteModAtGroupHasFourSieves where

open import Prelude
open import FiniteModAt

subgroups : length (sieves (Aut 3)) ≡ 4
subgroups = refl
