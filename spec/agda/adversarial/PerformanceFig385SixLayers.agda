{-# OPTIONS --safe --without-K #-}

-- Fig. 38.5 has seven operator layers, Ω1 to Ω7. A claim of six must
-- not pass.
-- EXPECT: 7 != 6 of type Nat

module adversarial.PerformanceFig385SixLayers where

open import Prelude
open import Performance

bad : depth fig38-5 ≡ 6
bad = refl
