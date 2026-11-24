{-# OPTIONS --safe --without-K #-}

-- Prop. 6.8 (arXiv 2606.01102, p. 23) admits P only from a minor triad in
-- voicing type 351, for example E♭ G C to E G C. The reverse step, P from
-- the major triad in type 351 (E G C to E♭ G C), passes every pair but the
-- joint maximization of eq. (2) drops it. A claim that it is admitted must
-- not pass.
-- EXPECT: false != true of type Bool

module adversarial.ThreeVoiceMajorP351 where

open import Prelude
open import Counterpoint
open import ThreeVoice

-- E G C is (4; 3, 8), E♭ G C is (3; 4, 9)
reverse-step : admitted (buildTable table8) (st 4 3 8) (st 3 4 9) ≡ true
reverse-step = refl
