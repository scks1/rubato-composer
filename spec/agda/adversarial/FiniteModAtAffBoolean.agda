{-# OPTIONS --safe --without-K #-}

-- Aff(Z2) is not a groupoid, so its presheaf topos is not Boolean
-- (Topos I p. 335): not not {0} = {0} but {0} v not {0} is not the top.
-- A claim that it is Boolean must not pass.
-- EXPECT: false != true of type Bool

module adversarial.FiniteModAtAffBoolean where

open import Prelude
open import FiniteModAt

claim : boolean (Aff 2) ≡ true
claim = refl
