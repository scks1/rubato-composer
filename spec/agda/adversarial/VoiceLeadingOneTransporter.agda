{-# OPTIONS --safe --without-K #-}

-- T/I has two transporters from 0 to 4 (T4 and I4); a list with T4 alone
-- must not pass.
-- EXPECT: != [] of type List El

module adversarial.VoiceLeadingOneTransporter where

open import Prelude
open import VoiceLeading

wrong : transp tis 0 4 ≡ T 4 ∷ []
wrong = refl
