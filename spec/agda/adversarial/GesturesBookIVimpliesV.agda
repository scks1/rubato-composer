{-# OPTIONS --safe --without-K #-}

-- Topos III Example 69 (p. 930) states IV => V = III u V. The
-- implication also holds VII's loop at 11, so the claim must not pass.
-- EXPECT: false != true of type Bool

module adversarial.GesturesBookIVimpliesV where

open import Prelude
open import Gestures

book-IV⇒V : sameSub (IV ⇒ V) (∪ (III ∷ V ∷ [])) ≡ true
book-IV⇒V = refl
