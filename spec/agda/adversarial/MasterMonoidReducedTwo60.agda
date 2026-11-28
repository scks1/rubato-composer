{-# OPTIONS --safe --without-K #-}

-- M₂ has 58 reduced words of length 2: of the 64 two-letter words, the
-- six d¹ᵢ d²ⱼ compose. A file claiming 60 must fail.
-- EXPECT: 58 != 60 of type Nat

module adversarial.MasterMonoidReducedTwo60 where

open import Prelude
open import MasterMonoid

wrong : count isReduced (wordsOf 2) ≡ 60
wrong = refl
