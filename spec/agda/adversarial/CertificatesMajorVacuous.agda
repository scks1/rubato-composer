{-# OPTIONS --safe --without-K #-}

-- C major has a leading tone, so Ex. B.3's claim is not vacuous there (D6).
-- EXPECT: should be empty

module adversarial.CertificatesMajorVacuous where

open import Prelude
open import Certificates

wrong : DominantContainsLeadingTone 0 major
wrong (_ , _ , ())
