{-# OPTIONS --safe --without-K #-}

-- Example 2's trace {0, 2, 6, 9, 11} has five tones, so it cannot be a
-- strong dichotomy of Z12 (MONTEVERDI-SPEC, W3). A claim that it is must
-- not pass.
-- EXPECT: false != true of type Bool

module adversarial.MonteverdiEx2TraceStrong where

open import Prelude
import Counterpoint as C

ex2-strong : C.strong (0 ∷ 2 ∷ 6 ∷ 9 ∷ 11 ∷ []) ≡ true
ex2-strong = refl
