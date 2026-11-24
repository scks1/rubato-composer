{-# OPTIONS --safe --without-K #-}

-- A positive certificate for {0, 1, 4, 6} that leaves out class 3.
-- EXPECT: Incomplete pattern matching for wrong

module adversarial.CertificatesMissingClass where

open import Prelude
open import Certificates

wrong : AllInterval 12 (0 ∷ 1 ∷ 4 ∷ 6 ∷ [])
wrong 0 _ = 0 , 0 , here , here , refl
wrong 1 _ = 0 , 1 , here , there here , refl
wrong 2 _ = 4 , 6 , there (there here) , there (there (there here)) , refl
wrong 4 _ = 0 , 4 , here , there (there here) , refl
wrong 5 _ = 1 , 6 , there here , there (there (there here)) , refl
wrong 6 _ = 0 , 6 , here , there (there (there here)) , refl
wrong (suc (suc (suc (suc (suc (suc (suc i))))))) (s≤s (s≤s (s≤s (s≤s (s≤s (s≤s ()))))))
