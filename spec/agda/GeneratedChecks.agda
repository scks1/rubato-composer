{-# OPTIONS --safe --without-K #-}

-- GeneratedChecks.agda: what the spec proves about the forms and
-- denotators that gen/ExportAgda.java read out of the running Java code.

module GeneratedChecks where

open import Prelude
open import Forms
open import Registry using (handEnv)
open import Generated

------------------------------------------------------------------------
-- The Java repository satisfies the repository invariants.

javaEnv-unique : uniqueB (namesOf javaEnv) ≡ true
javaEnv-unique = refl

javaEnv-closed : closedB javaEnv ≡ true
javaEnv-closed = refl

javaEnv-labels : labelsOkB javaEnv ≡ true
javaEnv-labels = refl

------------------------------------------------------------------------
-- The hand-written forms in Registry.agda are the ones Java registers.

handNames : List Name
handNames = namesOf handEnv

registry-agrees : map (lookup javaEnv) handNames ≡ map (lookup handEnv) handNames
registry-agrees = refl

------------------------------------------------------------------------
-- Denotators the Java code built are well-formed in the spec.

javaScore-ok : javaEnv ⊢ javaScore ∶ "Score"
javaScore-ok = sound _ _ _ refl

-- Java padded the missing Duration and Voice, so the stored note has
-- all five coordinates (D9).
javaPaddedNote-ok : javaEnv ⊢ javaPaddedNote ∶ "Note"
javaPaddedNote-ok = sound _ _ _ refl

javaMacroScore-ok : javaEnv ⊢ javaMacroScore ∶ "MacroScore"
javaMacroScore-ok = sound _ _ _ refl

javaSoundScore-ok : javaEnv ⊢ javaSoundScore ∶ "SoundScore"
javaSoundScore-ok = sound _ _ _ refl

javaFMNode-ok : javaEnv ⊢ javaFMNode ∶ "SoundNode"
javaFMNode-ok = sound _ _ _ refl

javaGeneralScore-ok : javaEnv ⊢ javaGeneralScore ∶ "GeneralScore"
javaGeneralScore-ok = sound _ _ _ refl

javaEnvelope-ok : javaEnv ⊢ javaEnvelope ∶ "Envelope"
javaEnvelope-ok = sound _ _ _ refl

javaComplex-ok : javaEnv ⊢ javaComplex ∶ "Complex"
javaComplex-ok = sound _ _ _ refl

------------------------------------------------------------------------
-- Sets Java stores that the spec rejects.

-- Java's int subtraction overflows, so it stores 0, 1, MIN_VALUE; the
-- spec's exact order wants MIN_VALUE first (D5).
javaIntegerSet-rejected : check javaEnv javaIntegerSetOverflow "IntegerSet" ≡ false
javaIntegerSet-rejected = refl

-- Java keeps both NaNs, since NaN compares greater than NaN (D6).
javaRealSet-rejected : check javaEnv javaRealSetNaN "RealSet" ≡ false
javaRealSet-rejected = refl
