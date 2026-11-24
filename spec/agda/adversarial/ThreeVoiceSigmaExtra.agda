{-# OPTIONS --safe --without-K #-}

-- The fifth ε.7 has exactly one maximizing contrapuntal symmetry in H,
-- T^{ε.0}.7, which meets K[ε] in 60 points. T^{ε.6}.(1+ε.6), a maximizing
-- symmetry of the unison and of the third ε.4, is not one for ε.7. A claim
-- that Σ(ε.7) contains both must not pass.
-- EXPECT: false != true of type Bool

module adversarial.ThreeVoiceSigmaExtra where

open import Prelude
open import Counterpoint
open import ThreeVoice

sigma7 : sameList (daff 0 0 7 0 ∷ daff 0 6 1 6 ∷ []) (sigmaSearch 7) ≡ true
sigma7 = refl
