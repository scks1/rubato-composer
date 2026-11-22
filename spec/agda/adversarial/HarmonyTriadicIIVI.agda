{-# OPTIONS --safe --without-K #-}

-- The triadic minimal cadential sets of C major are {VII}, {II,III},
-- {II,V}, {III,IV} and {IV,V} (Topos I p. 459). A list with {II,VI} in
-- place of {II,V} must not pass.
-- EXPECT: false != true of type Bool

module adversarial.HarmonyTriadicIIVI where

open import Prelude
open import Harmony

wrong-cadences : sameFamily (minimalSets triad)
  ((6 ∷ []) ∷ (1 ∷ 2 ∷ []) ∷ (1 ∷ 5 ∷ []) ∷ (2 ∷ 3 ∷ []) ∷ (3 ∷ 4 ∷ []) ∷ []) ≡ true
wrong-cadences = refl
