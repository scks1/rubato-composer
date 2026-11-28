{-# OPTIONS --safe --without-K #-}

-- Prop. 4.4 gives M₂ three relations. A file claiming four must fail.
-- EXPECT: 3 != 4 of type Nat

module adversarial.MasterMonoidFourRelations where

open import Prelude
open import MasterMonoid

wrong : length (rels 2) ≡ 4
wrong = refl
