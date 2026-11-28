{-# OPTIONS --safe --without-K #-}

-- Every Tonnetz edge lies on two triads. A file claiming three must fail.
-- EXPECT: false != true of type Bool

module adversarial.TonnetzEdgeOnThreeTriads where

open import Prelude
open import MasterMonoid

wrong : all (λ e → count (hasFace e) triads ==ⁿ 3) edges ≡ true
wrong = refl
