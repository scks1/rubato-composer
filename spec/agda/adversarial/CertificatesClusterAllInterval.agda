{-# OPTIONS --safe --without-K #-}

-- {0, 1, 2, 3} misses interval class 4 (spec section 4); a positive
-- certificate that offers the pair (0, 3) for class 4 must not pass.
-- EXPECT: 3 != 4 of type Nat

module adversarial.CertificatesClusterAllInterval where

open import Prelude
open import Certificates

wrong : Hit 12 (0 ∷ 1 ∷ 2 ∷ 3 ∷ []) 4
wrong = 0 , 3 , here , there (there (there here)) , refl
