{-# OPTIONS --safe --without-K #-}

-- The face of the two-arrow path is the second difference
-- g(0) - 2 g(1) + g(2). The guess g(2) - g(0) must not pass.
-- EXPECT: pos 1 != negsuc 0 of type Int

module adversarial.GesturesPath2Ends where

open import Prelude
open import Gestures

ends-only : faces path2 ≡ negsuc 0 ∷ pos 0 ∷ pos 1 ∷ []
ends-only = refl
