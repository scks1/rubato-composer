{-# OPTIONS --safe --without-K #-}

-- The harmonic strip's triangles are the tone stars (Topos I p. 262).
-- Replacing the star of C, {I, IV, VI}, by the three consecutive degrees
-- {I, II, III}, which share no tone, must not pass.
-- EXPECT: false != true of type Bool

module adversarial.GlobalWrongTriangle where

open import Prelude
open import Global

wrong-stars : List (List Nat)
wrong-stars =
    (I ∷ II ∷ III ∷ [])
  ∷ (II ∷ V ∷ VII ∷ [])
  ∷ (I ∷ III ∷ VI ∷ [])
  ∷ (II ∷ IV ∷ VII ∷ [])
  ∷ (I ∷ III ∷ V ∷ [])
  ∷ (II ∷ IV ∷ VI ∷ [])
  ∷ (III ∷ V ∷ VII ∷ [])
  ∷ []

wrong-exactly : exactly triad wrong-stars ≡ true
wrong-exactly = refl
