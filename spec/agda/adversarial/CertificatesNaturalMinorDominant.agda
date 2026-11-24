{-# OPTIONS --safe --without-K #-}

-- In A natural minor the sum of Ex. B.3 is empty (D6); claiming that its
-- V triad holds a membership proof of the leading tone must not pass.
-- EXPECT: 8 != 7 of type Nat

module adversarial.CertificatesNaturalMinorDominant where

open import Prelude
open import Certificates

wrong : leadingTone 9 ∈ triad 9 naturalMinor 5
wrong = there here
