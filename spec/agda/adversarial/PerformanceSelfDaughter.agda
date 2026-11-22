{-# OPTIONS --safe --without-K #-}

-- Def. 107: no LPS lists itself among its daughters. A proof that the
-- stemma with one leaf daughter is its own daughter must not pass.
-- EXPECT: leaf ∷ [] != [] of type List Stemma

module adversarial.PerformanceSelfDaughter where

open import Prelude
open import Performance

bad : lps (leaf ∷ []) ∈ (leaf ∷ [])
bad = here
