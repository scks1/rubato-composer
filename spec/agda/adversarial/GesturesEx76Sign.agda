{-# OPTIONS --safe --without-K #-}

-- Def. 112 (p. 969) gives an arrow the face g(head) - g(tail).
-- Example 76 (p. 971) prints g(tail) - g(head); that sign must not pass.
-- EXPECT: negsuc 0 != pos 1 of type Int

module adversarial.GesturesEx76Sign where

open import Prelude
open import Gestures

printed-sign : faces arrowD ≡ pos 1 ∷ negsuc 0 ∷ []
printed-sign = refl
