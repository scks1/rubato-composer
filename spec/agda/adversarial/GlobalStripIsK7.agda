{-# OPTIONS --safe --without-K #-}

-- The tetradic nerve has K7 as its 1-skeleton (AM 2020 section 6), but
-- the triadic nerve has only 14 of the 21 edges: I and II share no tone.
-- A claim that the strip's 1-skeleton is K7 must not pass.
-- EXPECT: false != true of type Bool

module adversarial.GlobalStripIsK7 where

open import Prelude
open import Global

strip-k7 : all (λ σ → not (length σ ==ⁿ 2) || memL σ strip) (subsets degrees) ≡ true
strip-k7 = refl
