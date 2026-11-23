{-# OPTIONS --safe --without-K #-}

-- Example 1's trace {1, 2, 4, 7, 9, 11} has the polarity 11x + 7; the
-- map 11x + 11, the polarity of Example 3's trace, does not exchange it
-- with its complement. A claim that it does must not pass.
-- EXPECT: false != true of type Bool

module adversarial.MonteverdiEx1WrongPolarity where

open import Prelude
import Counterpoint as C

wrong : C.complements (1 ∷ 2 ∷ 4 ∷ 7 ∷ 9 ∷ 11 ∷ []) (C.aff 11 11) ≡ true
wrong = refl
