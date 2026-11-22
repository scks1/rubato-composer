{-# OPTIONS --safe --without-K #-}

-- A tempo of 0 has no 1/T (eq. (33.1)), so it cannot be written down:
-- its positivity witness is an element of the empty type.
-- EXPECT: ⊤ !=< ⊥

module adversarial.PerformanceZeroTempo where

open import Prelude
open import Performance

bad : Tempo
bad = tempo 0 1 tt tt
