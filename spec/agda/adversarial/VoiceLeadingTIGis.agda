{-# OPTIONS --safe --without-K #-}

-- T/I on Z12 has two transporters over every pair, so it is not simply
-- transitive and gives no GIS (spec D9). Claiming it does must not pass.
-- EXPECT: false != true of type Bool

module adversarial.VoiceLeadingTIGis where

open import Prelude
open import VoiceLeading

wrong : allPairs (λ x y → length (transp tis x y) ==ⁿ 1) ≡ true
wrong = refl
