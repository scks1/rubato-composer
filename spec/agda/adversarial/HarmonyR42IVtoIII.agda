{-# OPTIONS --safe --without-K #-}

-- R42 sends IV7 = Fmaj7 to II7 = Dm7 (BfA Prop. 4.2). A claim that it
-- sends IV7 to III7 = Em7 must not pass.
-- EXPECT: false != true of type Bool

module adversarial.HarmonyR42IVtoIII where

open import Prelude
open import Harmony

R42-IV-III : eqChord (R42 (chord 4 5)) (chord 2 4) ≡ true
R42-IV-III = refl
