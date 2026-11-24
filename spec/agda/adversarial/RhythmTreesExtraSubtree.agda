{-# OPTIONS --safe --without-K #-}

-- A node with two factors has exactly two branches (B = Fin(n), Ex. C.1,
-- p. 56). A third subtree under two factors must not typecheck; Java can
-- only refuse it at run time.
-- EXPECT: has type Fin 0

module adversarial.RhythmTreesExtraSubtree where

open import Prelude
open import RhythmTrees

extra : RhythmTree
extra = sup (2 / 1 , 1 / 1 ∷ 1 / 1 ∷ []) λ
  { fzero                → leaf (1 / 1)
  ; (fsuc fzero)         → leaf (1 / 1)
  ; (fsuc (fsuc fzero))  → leaf (1 / 1)
  }
