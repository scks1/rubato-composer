{-# OPTIONS --safe --without-K #-}

-- The discussion of Laudate Dominum (arXiv 2512.18862, section 5.1) gives
-- its perfect-to-imperfect steps three symmetries, but row 1 (2 + eps.0 to
-- 2 + eps.9) has two under every reading tried (MONTEVERDI-SPEC, W1). A
-- claim of three must not pass.
-- EXPECT: 2 != 3 of type Nat

module adversarial.MonteverdiLaudateRowOneThree where

open import Prelude
open import Harmony using (filter)
open import Monteverdi

row-one-three : length (stepSet (iv 2 0) (iv 2 9)) ≡ 3
row-one-three = refl
