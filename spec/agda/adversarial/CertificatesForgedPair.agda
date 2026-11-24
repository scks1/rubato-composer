{-# OPTIONS --safe --without-K #-}

-- A forged witness: for interval class 3 of {0, 1, 4, 6} the pair (4, 7)
-- with 7's membership proof borrowed from 6. 7 is not in the set.
-- EXPECT: 7 != 6 of type Nat

module adversarial.CertificatesForgedPair where

open import Prelude
open import Certificates

forged : Hit 12 (0 ∷ 1 ∷ 4 ∷ 6 ∷ []) 3
forged = 4 , 7 , there (there here) , there (there (there here)) , refl
