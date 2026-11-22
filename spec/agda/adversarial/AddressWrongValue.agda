{-# OPTIONS --safe --without-K #-}

-- The address change of e65.1 (x ↦ x + 65) along x ↦ 2x + 1 takes the
-- value 66 at 0. A case claiming 67, the value at 1 of the unchanged
-- point, must not pass the check.
-- EXPECT: false != true of type Bool

module adversarial.AddressWrongValue where

open import Prelude hiding (_+_; _*_)
open import Addresses

module Zm = Free ℤ-ring

wrong : Zm.ChangeCase
wrong = Zm.change 1 1 1
  (Zm.affine (((pos 1) ∷ᵛ ⟨⟩) ∷ᵛ ⟨⟩) ((pos 65) ∷ᵛ ⟨⟩))
  (Zm.affine (((pos 2) ∷ᵛ ⟨⟩) ∷ᵛ ⟨⟩) ((pos 1) ∷ᵛ ⟨⟩))
  ((((pos 0) ∷ᵛ ⟨⟩) Zm.↦ ((pos 67) ∷ᵛ ⟨⟩)) ∷ [])

wrong-passes : Zm.checkChange wrong ≡ true
wrong-passes = refl
