{-# OPTIONS --safe --without-K #-}

-- A stemma is a tree (Def. 107), so it has one edge fewer than nodes.
-- Counting 21 edges for the 21 nodes of Fig. 38.5 must not pass.
-- EXPECT: 20 != 21 of type Nat

module adversarial.PerformanceEdgesEqualNodes where

open import Prelude
open import Performance

bad : edges fig38-5 ≡ size fig38-5
bad = refl
