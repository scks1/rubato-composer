{-# OPTIONS --safe --without-K #-}

-- {0, 1, 4, 6} is all-interval, so no refutation of class 4 exists: the
-- pair (0, 4) is no absurd case.
-- EXPECT: should be empty

module adversarial.CertificatesRefuteAllInterval where

open import Prelude
open import Certificates

wrong : ¬ Hit 12 (0 ∷ 1 ∷ 4 ∷ 6 ∷ []) 4
wrong (_ , _ , here , there (there here) , ())
