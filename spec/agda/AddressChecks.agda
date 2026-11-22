{-# OPTIONS --safe --without-K #-}

-- AddressChecks.agda: Java's address changes and evaluations, read out of
-- the running code by gen/ExportAddresses.java, agree with the model in
-- Addresses.agda. For every exported case the value Java computed equals
-- the model's composition by matrix product and the precomposition d (f x);
-- every evaluation d.at(a) equals d a and the change along the constant
-- map 0 -> C^m with value a.

module AddressChecks where

open import Prelude hiding (_+_; _*_)
open import Addresses
open import GeneratedAddresses

z-changes : all Zm.checkChange zChanges ≡ true
z-changes = refl

z-ats : all Zm.checkAt zAts ≡ true
z-ats = refl

q-changes : all Qm.checkChange qChanges ≡ true
q-changes = refl

q-ats : all Qm.checkAt qAts ≡ true
q-ats = refl

r-changes : all Rm.checkChange rChanges ≡ true
r-changes = refl

r-ats : all Rm.checkAt rAts ≡ true
r-ats = refl

-- the cases that change along a constant from the zero module, which
-- Java got wrong before the commit "Fix denotators at non-zero addresses"
fromZero : List Zm.ChangeCase → List Zm.ChangeCase
fromZero [] = []
fromZero (c ∷ cs) with Zm.ChangeCase.k c
... | zero  = c ∷ fromZero cs
... | suc _ = fromZero cs

z-constant-changes : length (fromZero zChanges) ≡ 3
z-constant-changes = refl
