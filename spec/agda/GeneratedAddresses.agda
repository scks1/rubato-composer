{-# OPTIONS --safe --without-K #-}

-- GeneratedAddresses.agda: written by spec/agda/gen/ExportAddresses.java
-- from the running Java code. Do not edit; run spec/agda/run-checks.sh.

module GeneratedAddresses where

open import Prelude hiding (_+_; _*_)
open import Addresses

module Zm = Free ℤ-ring
module Qm = Free ℚ-ring
module Rm = Free ℝ-ring

-- 10 address changes over Z
zChanges : List Zm.ChangeCase
zChanges =
    Zm.change 1 1 1
      (Zm.affine ((((pos 1) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 65) ∷ᵛ ⟨⟩))
      (Zm.affine ((((pos 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 1) ∷ᵛ ⟨⟩))
      ((((pos 0) ∷ᵛ ⟨⟩) Zm.↦ ((pos 66) ∷ᵛ ⟨⟩)) ∷ (((pos 2) ∷ᵛ ⟨⟩) Zm.↦ ((pos 70) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Zm.change 1 1 1
      (Zm.affine ((((negsuc 0) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 67) ∷ᵛ ⟨⟩))
      (Zm.affine ((((pos 3) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((negsuc 1) ∷ᵛ ⟨⟩))
      ((((pos 0) ∷ᵛ ⟨⟩) Zm.↦ ((pos 69) ∷ᵛ ⟨⟩)) ∷ (((pos 2) ∷ᵛ ⟨⟩) Zm.↦ ((pos 63) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Zm.change 1 2 1
      (Zm.affine ((((pos 1) ∷ᵛ (pos 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 60) ∷ᵛ ⟨⟩))
      (Zm.affine ((((pos 1) ∷ᵛ ⟨⟩)) ∷ᵛ (((negsuc 0) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 4) ∷ᵛ (pos 0) ∷ᵛ ⟨⟩))
      ((((pos 0) ∷ᵛ ⟨⟩) Zm.↦ ((pos 64) ∷ᵛ ⟨⟩)) ∷ (((pos 2) ∷ᵛ ⟨⟩) Zm.↦ ((pos 62) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Zm.change 2 1 2
      (Zm.affine ((((pos 3) ∷ᵛ ⟨⟩)) ∷ᵛ (((negsuc 1) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 0) ∷ᵛ (pos 7) ∷ᵛ ⟨⟩))
      (Zm.affine ((((pos 1) ∷ᵛ (pos 1) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((negsuc 0) ∷ᵛ ⟨⟩))
      ((((pos 0) ∷ᵛ (pos 0) ∷ᵛ ⟨⟩) Zm.↦ ((negsuc 2) ∷ᵛ (pos 9) ∷ᵛ ⟨⟩)) ∷ (((pos 2) ∷ᵛ (pos 0) ∷ᵛ ⟨⟩) Zm.↦ ((pos 3) ∷ᵛ (pos 5) ∷ᵛ ⟨⟩)) ∷ (((pos 0) ∷ᵛ (pos 2) ∷ᵛ ⟨⟩) Zm.↦ ((pos 3) ∷ᵛ (pos 5) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Zm.change 2 2 2
      (Zm.affine ((((pos 1) ∷ᵛ (pos 2) ∷ᵛ ⟨⟩)) ∷ᵛ (((pos 0) ∷ᵛ (negsuc 0) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 5) ∷ᵛ (pos 6) ∷ᵛ ⟨⟩))
      (Zm.affine ((((pos 2) ∷ᵛ (pos 0) ∷ᵛ ⟨⟩)) ∷ᵛ (((pos 1) ∷ᵛ (pos 1) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 1) ∷ᵛ (negsuc 2) ∷ᵛ ⟨⟩))
      ((((pos 0) ∷ᵛ (pos 0) ∷ᵛ ⟨⟩) Zm.↦ ((pos 0) ∷ᵛ (pos 9) ∷ᵛ ⟨⟩)) ∷ (((pos 2) ∷ᵛ (pos 0) ∷ᵛ ⟨⟩) Zm.↦ ((pos 8) ∷ᵛ (pos 7) ∷ᵛ ⟨⟩)) ∷ (((pos 0) ∷ᵛ (pos 2) ∷ᵛ ⟨⟩) Zm.↦ ((pos 4) ∷ᵛ (pos 7) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Zm.change 1 1 1
      (Zm.affine ((((pos 0) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 66) ∷ᵛ ⟨⟩))
      (Zm.affine ((((pos 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 1) ∷ᵛ ⟨⟩))
      ((((pos 0) ∷ᵛ ⟨⟩) Zm.↦ ((pos 66) ∷ᵛ ⟨⟩)) ∷ (((pos 2) ∷ᵛ ⟨⟩) Zm.↦ ((pos 66) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Zm.change 2 1 1
      (Zm.affine ((((pos 0) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 66) ∷ᵛ ⟨⟩))
      (Zm.affine ((((pos 1) ∷ᵛ (pos 3) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 0) ∷ᵛ ⟨⟩))
      ((((pos 0) ∷ᵛ (pos 0) ∷ᵛ ⟨⟩) Zm.↦ ((pos 66) ∷ᵛ ⟨⟩)) ∷ (((pos 2) ∷ᵛ (pos 0) ∷ᵛ ⟨⟩) Zm.↦ ((pos 66) ∷ᵛ ⟨⟩)) ∷ (((pos 0) ∷ᵛ (pos 2) ∷ᵛ ⟨⟩) Zm.↦ ((pos 66) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Zm.change 0 1 1
      (Zm.affine ((((pos 1) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 65) ∷ᵛ ⟨⟩))
      (Zm.affine ((⟨⟩) ∷ᵛ ⟨⟩) ((pos 2) ∷ᵛ ⟨⟩))
      (((⟨⟩) Zm.↦ ((pos 67) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Zm.change 0 2 2
      (Zm.affine ((((pos 1) ∷ᵛ (pos 2) ∷ᵛ ⟨⟩)) ∷ᵛ (((pos 0) ∷ᵛ (negsuc 0) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 5) ∷ᵛ (pos 6) ∷ᵛ ⟨⟩))
      (Zm.affine ((⟨⟩) ∷ᵛ (⟨⟩) ∷ᵛ ⟨⟩) ((pos 1) ∷ᵛ (negsuc 0) ∷ᵛ ⟨⟩))
      (((⟨⟩) Zm.↦ ((pos 4) ∷ᵛ (pos 7) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Zm.change 0 1 1
      (Zm.affine ((((pos 0) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 66) ∷ᵛ ⟨⟩))
      (Zm.affine ((⟨⟩) ∷ᵛ ⟨⟩) ((pos 4) ∷ᵛ ⟨⟩))
      (((⟨⟩) Zm.↦ ((pos 66) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ []

-- 5 evaluations over Z
zAts : List Zm.AtCase
zAts =
    Zm.atCase 1 1 (Zm.affine ((((pos 1) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 65) ∷ᵛ ⟨⟩)) ((pos 0) ∷ᵛ ⟨⟩) ((pos 65) ∷ᵛ ⟨⟩)
  ∷ Zm.atCase 1 1 (Zm.affine ((((pos 1) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 65) ∷ᵛ ⟨⟩)) ((pos 1) ∷ᵛ ⟨⟩) ((pos 66) ∷ᵛ ⟨⟩)
  ∷ Zm.atCase 1 1 (Zm.affine ((((negsuc 0) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 67) ∷ᵛ ⟨⟩)) ((pos 1) ∷ᵛ ⟨⟩) ((pos 66) ∷ᵛ ⟨⟩)
  ∷ Zm.atCase 2 2 (Zm.affine ((((pos 1) ∷ᵛ (pos 2) ∷ᵛ ⟨⟩)) ∷ᵛ (((pos 0) ∷ᵛ (negsuc 0) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 5) ∷ᵛ (pos 6) ∷ᵛ ⟨⟩)) ((pos 3) ∷ᵛ (negsuc 1) ∷ᵛ ⟨⟩) ((pos 4) ∷ᵛ (pos 8) ∷ᵛ ⟨⟩)
  ∷ Zm.atCase 1 1 (Zm.affine ((((pos 0) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) ((pos 66) ∷ᵛ ⟨⟩)) ((pos 9) ∷ᵛ ⟨⟩) ((pos 66) ∷ᵛ ⟨⟩)
  ∷ []

-- 10 address changes over Q
qChanges : List Qm.ChangeCase
qChanges =
    Qm.change 1 1 1
      (Qm.affine (((((pos 1) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 65) / 2) ∷ᵛ ⟨⟩))
      (Qm.affine (((((pos 2) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 1) / 2) ∷ᵛ ⟨⟩))
      (((((pos 0) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 131) / 4) ∷ᵛ ⟨⟩)) ∷ ((((pos 2) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 133) / 4) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Qm.change 1 1 1
      (Qm.affine (((((negsuc 0) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 67) / 2) ∷ᵛ ⟨⟩))
      (Qm.affine (((((pos 3) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((negsuc 1) / 2) ∷ᵛ ⟨⟩))
      (((((pos 0) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 34) / 1) ∷ᵛ ⟨⟩)) ∷ ((((pos 2) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 133) / 4) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Qm.change 1 2 1
      (Qm.affine (((((pos 1) / 2) ∷ᵛ ((pos 2) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 60) / 2) ∷ᵛ ⟨⟩))
      (Qm.affine (((((pos 1) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ((((negsuc 0) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 4) / 2) ∷ᵛ ((pos 0) / 2) ∷ᵛ ⟨⟩))
      (((((pos 0) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 31) / 1) ∷ᵛ ⟨⟩)) ∷ ((((pos 2) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 123) / 4) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Qm.change 2 1 2
      (Qm.affine (((((pos 3) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ((((negsuc 1) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 0) / 2) ∷ᵛ ((pos 7) / 2) ∷ᵛ ⟨⟩))
      (Qm.affine (((((pos 1) / 2) ∷ᵛ ((pos 1) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((negsuc 0) / 2) ∷ᵛ ⟨⟩))
      (((((pos 0) / 2) ∷ᵛ ((pos 0) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((negsuc 2) / 4) ∷ᵛ ((pos 4) / 1) ∷ᵛ ⟨⟩)) ∷ ((((pos 2) / 2) ∷ᵛ ((pos 0) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 0) / 1) ∷ᵛ ((pos 7) / 2) ∷ᵛ ⟨⟩)) ∷ ((((pos 0) / 2) ∷ᵛ ((pos 2) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 0) / 1) ∷ᵛ ((pos 7) / 2) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Qm.change 2 2 2
      (Qm.affine (((((pos 1) / 2) ∷ᵛ ((pos 2) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ((((pos 0) / 2) ∷ᵛ ((negsuc 0) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 5) / 2) ∷ᵛ ((pos 6) / 2) ∷ᵛ ⟨⟩))
      (Qm.affine (((((pos 2) / 2) ∷ᵛ ((pos 0) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ((((pos 1) / 2) ∷ᵛ ((pos 1) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 1) / 2) ∷ᵛ ((negsuc 2) / 2) ∷ᵛ ⟨⟩))
      (((((pos 0) / 2) ∷ᵛ ((pos 0) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 5) / 4) ∷ᵛ ((pos 15) / 4) ∷ᵛ ⟨⟩)) ∷ ((((pos 2) / 2) ∷ᵛ ((pos 0) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 9) / 4) ∷ᵛ ((pos 7) / 2) ∷ᵛ ⟨⟩)) ∷ ((((pos 0) / 2) ∷ᵛ ((pos 2) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 7) / 4) ∷ᵛ ((pos 7) / 2) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Qm.change 1 1 1
      (Qm.affine (((((pos 0) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 66) / 2) ∷ᵛ ⟨⟩))
      (Qm.affine (((((pos 2) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 1) / 2) ∷ᵛ ⟨⟩))
      (((((pos 0) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 33) / 1) ∷ᵛ ⟨⟩)) ∷ ((((pos 2) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 33) / 1) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Qm.change 2 1 1
      (Qm.affine (((((pos 0) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 66) / 2) ∷ᵛ ⟨⟩))
      (Qm.affine (((((pos 1) / 2) ∷ᵛ ((pos 3) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 0) / 2) ∷ᵛ ⟨⟩))
      (((((pos 0) / 2) ∷ᵛ ((pos 0) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 33) / 1) ∷ᵛ ⟨⟩)) ∷ ((((pos 2) / 2) ∷ᵛ ((pos 0) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 33) / 1) ∷ᵛ ⟨⟩)) ∷ ((((pos 0) / 2) ∷ᵛ ((pos 2) / 2) ∷ᵛ ⟨⟩) Qm.↦ (((pos 33) / 1) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Qm.change 0 1 1
      (Qm.affine (((((pos 1) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 65) / 2) ∷ᵛ ⟨⟩))
      (Qm.affine ((⟨⟩) ∷ᵛ ⟨⟩) (((pos 2) / 2) ∷ᵛ ⟨⟩))
      (((⟨⟩) Qm.↦ (((pos 33) / 1) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Qm.change 0 2 2
      (Qm.affine (((((pos 1) / 2) ∷ᵛ ((pos 2) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ((((pos 0) / 2) ∷ᵛ ((negsuc 0) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 5) / 2) ∷ᵛ ((pos 6) / 2) ∷ᵛ ⟨⟩))
      (Qm.affine ((⟨⟩) ∷ᵛ (⟨⟩) ∷ᵛ ⟨⟩) (((pos 1) / 2) ∷ᵛ ((negsuc 0) / 2) ∷ᵛ ⟨⟩))
      (((⟨⟩) Qm.↦ (((pos 9) / 4) ∷ᵛ ((pos 13) / 4) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Qm.change 0 1 1
      (Qm.affine (((((pos 0) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 66) / 2) ∷ᵛ ⟨⟩))
      (Qm.affine ((⟨⟩) ∷ᵛ ⟨⟩) (((pos 4) / 2) ∷ᵛ ⟨⟩))
      (((⟨⟩) Qm.↦ (((pos 33) / 1) ∷ᵛ ⟨⟩)) ∷ [])
  ∷ []

-- 5 evaluations over Q
qAts : List Qm.AtCase
qAts =
    Qm.atCase 1 1 (Qm.affine (((((pos 1) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 65) / 2) ∷ᵛ ⟨⟩)) (((pos 0) / 2) ∷ᵛ ⟨⟩) (((pos 65) / 2) ∷ᵛ ⟨⟩)
  ∷ Qm.atCase 1 1 (Qm.affine (((((pos 1) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 65) / 2) ∷ᵛ ⟨⟩)) (((pos 1) / 2) ∷ᵛ ⟨⟩) (((pos 131) / 4) ∷ᵛ ⟨⟩)
  ∷ Qm.atCase 1 1 (Qm.affine (((((negsuc 0) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 67) / 2) ∷ᵛ ⟨⟩)) (((pos 1) / 2) ∷ᵛ ⟨⟩) (((pos 133) / 4) ∷ᵛ ⟨⟩)
  ∷ Qm.atCase 2 2 (Qm.affine (((((pos 1) / 2) ∷ᵛ ((pos 2) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ((((pos 0) / 2) ∷ᵛ ((negsuc 0) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 5) / 2) ∷ᵛ ((pos 6) / 2) ∷ᵛ ⟨⟩)) (((pos 3) / 2) ∷ᵛ ((negsuc 1) / 2) ∷ᵛ ⟨⟩) (((pos 9) / 4) ∷ᵛ ((pos 7) / 2) ∷ᵛ ⟨⟩)
  ∷ Qm.atCase 1 1 (Qm.affine (((((pos 0) / 2) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((pos 66) / 2) ∷ᵛ ⟨⟩)) (((pos 9) / 2) ∷ᵛ ⟨⟩) (((pos 33) / 1) ∷ᵛ ⟨⟩)
  ∷ []

-- 10 address changes over R
rChanges : List Rm.ChangeCase
rChanges =
    Rm.change 1 1 1
      (Rm.affine (((0.5 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (32.5 ∷ᵛ ⟨⟩))
      (Rm.affine (((1.0 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (0.5 ∷ᵛ ⟨⟩))
      (((0.0 ∷ᵛ ⟨⟩) Rm.↦ (32.75 ∷ᵛ ⟨⟩)) ∷ ((1.0 ∷ᵛ ⟨⟩) Rm.↦ (33.25 ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Rm.change 1 1 1
      (Rm.affine (((((primFloatNegate 0.5)) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (33.5 ∷ᵛ ⟨⟩))
      (Rm.affine (((1.5 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((primFloatNegate 1.0)) ∷ᵛ ⟨⟩))
      (((0.0 ∷ᵛ ⟨⟩) Rm.↦ (34.0 ∷ᵛ ⟨⟩)) ∷ ((1.0 ∷ᵛ ⟨⟩) Rm.↦ (33.25 ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Rm.change 1 2 1
      (Rm.affine (((0.5 ∷ᵛ 1.0 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (30.0 ∷ᵛ ⟨⟩))
      (Rm.affine (((0.5 ∷ᵛ ⟨⟩)) ∷ᵛ ((((primFloatNegate 0.5)) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (2.0 ∷ᵛ 0.0 ∷ᵛ ⟨⟩))
      (((0.0 ∷ᵛ ⟨⟩) Rm.↦ (31.0 ∷ᵛ ⟨⟩)) ∷ ((1.0 ∷ᵛ ⟨⟩) Rm.↦ (30.75 ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Rm.change 2 1 2
      (Rm.affine (((1.5 ∷ᵛ ⟨⟩)) ∷ᵛ ((((primFloatNegate 1.0)) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (0.0 ∷ᵛ 3.5 ∷ᵛ ⟨⟩))
      (Rm.affine (((0.5 ∷ᵛ 0.5 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (((primFloatNegate 0.5)) ∷ᵛ ⟨⟩))
      (((0.0 ∷ᵛ 0.0 ∷ᵛ ⟨⟩) Rm.↦ (((primFloatNegate 0.75)) ∷ᵛ 4.0 ∷ᵛ ⟨⟩)) ∷ ((1.0 ∷ᵛ 0.0 ∷ᵛ ⟨⟩) Rm.↦ (0.0 ∷ᵛ 3.5 ∷ᵛ ⟨⟩)) ∷ ((0.0 ∷ᵛ 1.0 ∷ᵛ ⟨⟩) Rm.↦ (0.0 ∷ᵛ 3.5 ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Rm.change 2 2 2
      (Rm.affine (((0.5 ∷ᵛ 1.0 ∷ᵛ ⟨⟩)) ∷ᵛ ((0.0 ∷ᵛ ((primFloatNegate 0.5)) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (2.5 ∷ᵛ 3.0 ∷ᵛ ⟨⟩))
      (Rm.affine (((1.0 ∷ᵛ 0.0 ∷ᵛ ⟨⟩)) ∷ᵛ ((0.5 ∷ᵛ 0.5 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (0.5 ∷ᵛ ((primFloatNegate 1.5)) ∷ᵛ ⟨⟩))
      (((0.0 ∷ᵛ 0.0 ∷ᵛ ⟨⟩) Rm.↦ (1.25 ∷ᵛ 3.75 ∷ᵛ ⟨⟩)) ∷ ((1.0 ∷ᵛ 0.0 ∷ᵛ ⟨⟩) Rm.↦ (2.25 ∷ᵛ 3.5 ∷ᵛ ⟨⟩)) ∷ ((0.0 ∷ᵛ 1.0 ∷ᵛ ⟨⟩) Rm.↦ (1.75 ∷ᵛ 3.5 ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Rm.change 1 1 1
      (Rm.affine (((0.0 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (33.0 ∷ᵛ ⟨⟩))
      (Rm.affine (((1.0 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (0.5 ∷ᵛ ⟨⟩))
      (((0.0 ∷ᵛ ⟨⟩) Rm.↦ (33.0 ∷ᵛ ⟨⟩)) ∷ ((1.0 ∷ᵛ ⟨⟩) Rm.↦ (33.0 ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Rm.change 2 1 1
      (Rm.affine (((0.0 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (33.0 ∷ᵛ ⟨⟩))
      (Rm.affine (((0.5 ∷ᵛ 1.5 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (0.0 ∷ᵛ ⟨⟩))
      (((0.0 ∷ᵛ 0.0 ∷ᵛ ⟨⟩) Rm.↦ (33.0 ∷ᵛ ⟨⟩)) ∷ ((1.0 ∷ᵛ 0.0 ∷ᵛ ⟨⟩) Rm.↦ (33.0 ∷ᵛ ⟨⟩)) ∷ ((0.0 ∷ᵛ 1.0 ∷ᵛ ⟨⟩) Rm.↦ (33.0 ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Rm.change 0 1 1
      (Rm.affine (((0.5 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (32.5 ∷ᵛ ⟨⟩))
      (Rm.affine ((⟨⟩) ∷ᵛ ⟨⟩) (1.0 ∷ᵛ ⟨⟩))
      (((⟨⟩) Rm.↦ (33.0 ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Rm.change 0 2 2
      (Rm.affine (((0.5 ∷ᵛ 1.0 ∷ᵛ ⟨⟩)) ∷ᵛ ((0.0 ∷ᵛ ((primFloatNegate 0.5)) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (2.5 ∷ᵛ 3.0 ∷ᵛ ⟨⟩))
      (Rm.affine ((⟨⟩) ∷ᵛ (⟨⟩) ∷ᵛ ⟨⟩) (0.5 ∷ᵛ ((primFloatNegate 0.5)) ∷ᵛ ⟨⟩))
      (((⟨⟩) Rm.↦ (2.25 ∷ᵛ 3.25 ∷ᵛ ⟨⟩)) ∷ [])
  ∷ Rm.change 0 1 1
      (Rm.affine (((0.0 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (33.0 ∷ᵛ ⟨⟩))
      (Rm.affine ((⟨⟩) ∷ᵛ ⟨⟩) (2.0 ∷ᵛ ⟨⟩))
      (((⟨⟩) Rm.↦ (33.0 ∷ᵛ ⟨⟩)) ∷ [])
  ∷ []

-- 5 evaluations over R
rAts : List Rm.AtCase
rAts =
    Rm.atCase 1 1 (Rm.affine (((0.5 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (32.5 ∷ᵛ ⟨⟩)) (0.0 ∷ᵛ ⟨⟩) (32.5 ∷ᵛ ⟨⟩)
  ∷ Rm.atCase 1 1 (Rm.affine (((0.5 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (32.5 ∷ᵛ ⟨⟩)) (0.5 ∷ᵛ ⟨⟩) (32.75 ∷ᵛ ⟨⟩)
  ∷ Rm.atCase 1 1 (Rm.affine (((((primFloatNegate 0.5)) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (33.5 ∷ᵛ ⟨⟩)) (0.5 ∷ᵛ ⟨⟩) (33.25 ∷ᵛ ⟨⟩)
  ∷ Rm.atCase 2 2 (Rm.affine (((0.5 ∷ᵛ 1.0 ∷ᵛ ⟨⟩)) ∷ᵛ ((0.0 ∷ᵛ ((primFloatNegate 0.5)) ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (2.5 ∷ᵛ 3.0 ∷ᵛ ⟨⟩)) (1.5 ∷ᵛ ((primFloatNegate 1.0)) ∷ᵛ ⟨⟩) (2.25 ∷ᵛ 3.5 ∷ᵛ ⟨⟩)
  ∷ Rm.atCase 1 1 (Rm.affine (((0.0 ∷ᵛ ⟨⟩)) ∷ᵛ ⟨⟩) (33.0 ∷ᵛ ⟨⟩)) (4.5 ∷ᵛ ⟨⟩) (33.0 ∷ᵛ ⟨⟩)
  ∷ []

