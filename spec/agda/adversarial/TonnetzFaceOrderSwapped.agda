{-# OPTIONS --safe --without-K #-}

-- With the triad faces listed fifth-edge first (root-fifth, third-fifth,
-- root-third) the semisimplicial identities fail. A file claiming they hold
-- must fail.
-- EXPECT: false != true of type Bool

module adversarial.TonnetzFaceOrderSwapped where

open import Prelude
open import MasterMonoid

wrong : all (identities t-d1 t-d0 t-d2) triads ≡ true
wrong = refl
