{-# OPTIONS --safe --without-K #-}

-- X2 = {0,1,3,7,8,11} is autocomplementary (T^9.7 exchanges it with its
-- complement) but T^8.5 fixes it, so it is not rigid and not strong
-- (CCW Example 2.2, p. 8). A claim that it is strong must not pass.
-- EXPECT: false != true of type Bool

module adversarial.CounterpointNotStrong where

open import Prelude
open import Counterpoint

X2 : List Nat
X2 = 0 ∷ 1 ∷ 3 ∷ 7 ∷ 8 ∷ 11 ∷ []

autocomplementary : complements X2 (aff 9 7) ≡ true
autocomplementary = refl

wrong-strong : strong X2 ≡ true
wrong-strong = refl
