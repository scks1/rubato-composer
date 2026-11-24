{-# OPTIONS --safe --without-K #-}

-- 48 affine maps normalize T/I on Z12, twice the 24 that T/I induces by
-- conjugation (Ex. 4.3). A count of 24 must not pass.
-- EXPECT: 48 != 24 of type Nat

module adversarial.VoiceLeadingNormalizer24 where

open import Prelude
open import VoiceLeading

wrong : length normalizer ≡ 24
wrong = refl
