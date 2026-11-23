{-# OPTIONS --safe --without-K #-}

-- The two-arrow path has 13 subdigraphs and 13 maps into Omega.
-- A count of 12 must not pass.
-- EXPECT: 13 != 12 of type Nat

module adversarial.GesturesHomSubMismatch where

open import Prelude
open import Gestures

twelve : homCount path2 Ω ≡ 12
twelve = refl
