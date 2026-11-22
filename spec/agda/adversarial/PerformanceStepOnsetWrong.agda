{-# OPTIONS --safe --without-K #-}

-- Bars of four quarters at M.M. 60, 90 and 120 end at 13/90 minutes.
-- Treating every bar as M.M. 60, 12/60 minutes, must not pass.
-- EXPECT: 5616000 != 7776000 of type Nat

module adversarial.PerformanceStepOnsetWrong where

open import Prelude
open import Performance

bad : onset three-bars ≈ (12 ÷ 60 ⟨ tt ⟩)
bad = refl
