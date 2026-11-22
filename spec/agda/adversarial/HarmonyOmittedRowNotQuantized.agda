{-# OPTIONS --safe --without-K #-}

-- Table 1 of Agustin-Aquino and Mazzola 2019 has no row (Tr3, {VII7},
-- T3), but Def. 88 and Algorithm 1 as printed quantize it. A claim that
-- the row fails must not pass.
-- EXPECT: true != false of type Bool

module adversarial.HarmonyOmittedRowNotQuantized where

open import Prelude
open import Harmony

not-quantized : row tetrad 3 (6 ∷ []) (ti 3 false) (0 ∷ 2 ∷ 3 ∷ 5 ∷ 6 ∷ 8 ∷ 9 ∷ 11 ∷ []) (1 ∷ 6 ∷ []) true ≡ false
not-quantized = refl
