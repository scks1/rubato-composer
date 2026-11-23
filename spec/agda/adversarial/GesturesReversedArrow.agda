{-# OPTIONS --safe --without-K #-}

-- Swapping the two vertices of an arrow does not give a digraph
-- morphism, because the image arrow would have to run backwards.
-- EXPECT: false != true of type Bool

module adversarial.GesturesReversedArrow where

open import Prelude
open import Gestures

reversed : isMorphism arrowD arrowD (1 ∷ 0 ∷ []) (0 ∷ []) ≡ true
reversed = refl
