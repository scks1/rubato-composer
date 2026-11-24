{-# OPTIONS --safe --without-K #-}

-- The image of the basis (1,0), (1,1) under A = [[2,0],[1,2]] is
-- [[2,2],[1,3]], with determinant 4. A determinant of 6 must not pass.
-- EXPECT: 4 != 6 of type Nat

module adversarial.FieldExtractionDetSix where

open import Prelude hiding (_+_; _*_)
open import FieldExtraction

wrong : det (mul A U₂) ≡ pos 6
wrong = refl
