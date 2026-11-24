{-# OPTIONS --safe --without-K #-}

-- V = [[1,2],[2,4]] has determinant 0 and a nonzero adjugate, so only the
-- direction of the field is known (Topos II p. 762). Calling it regular
-- must not pass.
-- EXPECT: direction != regular of type Case

module adversarial.FieldExtractionDirectionRegular where

open import Prelude hiding (_+_; _*_)
open import FieldExtraction

wrong : classify I Vdir ≡ regular
wrong = refl
