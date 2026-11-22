{-# OPTIONS --safe --without-K #-}

-- P42 (M(x) <-> m(x)) and T8 L42 agree on major sevenths only; on minor
-- sevenths they differ. A claim that they agree on every chord must not
-- pass. L42 is (42) with the vector (0, -4, 0, 4, 0).
-- EXPECT: false != true of type Bool

module adversarial.HarmonyP42IsT8L42 where

open import Prelude
open import Harmony

L42 : Chord → Chord
L42 (chord 4 x) = chord 2 (mod12 (x + 4))
L42 (chord 2 x) = chord 4 (mod12 (x + 8))
L42 c           = c

P42-is-T8L42 : all (λ c → eqChord (P42 c) (T 8 (L42 c))) allChords ≡ true
P42-is-T8L42 = refl
