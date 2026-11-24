{-# OPTIONS --safe --without-K #-}

-- The zero image has a zero adjugate: no information (Topos II p. 762).
-- Calling it direction only must not pass.
-- EXPECT: none != direction of type Case

module adversarial.FieldExtractionZeroDirection where

open import Prelude hiding (_+_; _*_)
open import FieldExtraction

wrong : classify I Vzero ≡ direction
wrong = refl
