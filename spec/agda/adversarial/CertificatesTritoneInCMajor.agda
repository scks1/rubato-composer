{-# OPTIONS --safe --without-K #-}

-- F sharp (6) is not in C major; no membership proof exists.
-- EXPECT: 6 != 5 of type Nat

module adversarial.CertificatesTritoneInCMajor where

open import Prelude
open import Certificates

wrong : 6 ∈ scale 0 major
wrong = there (there (there here))
