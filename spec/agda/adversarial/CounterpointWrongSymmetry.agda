{-# OPTIONS --safe --without-K #-}

-- For (K/D) at ξ = ε.9, T^{ε.8}.(5 + ε.4) is contrapuntal (CCW Example
-- 2.5, pp. 15-16). T^{ε.8}.(1 + ε.4) also leaves ξ outside gK[ε] and also
-- meets K[ε] in 56 points, but p_0[ε] = T^{ε.2}.5 is not a polarity of its
-- deformed dichotomy, so it is not contrapuntal. A claim that p_0[ε] is a
-- polarity there must not pass.
-- EXPECT: false != true of type Bool

module adversarial.CounterpointWrongSymmetry where

open import Prelude
open import Counterpoint

g : DAff
g = daff 0 8 1 4

same-intersection : count (consonant X82) (image g X82) ≡ 56
same-intersection = refl

xi-outside-g : memPt (pt 0 9) (image g X82) ≡ false
xi-outside-g = refl

wrong-polarity : all (λ p → memPt (dap (induced p82 0) p) (image g X82) ⇔ᵇ not (memPt p (image g X82))) points ≡ true
wrong-polarity = refl
