{-# OPTIONS --safe --without-K #-}

-- GeneratedCertificates.agda: do not edit; spec/agda/gen/ExportCertificates.java writes it
-- from AgdaCertificates.exampleModule(). Proof certificates written by Rubato's Agda certificates
-- (org.rubato.math.certificates.AgdaCertificates). Each definition's type is a claim of
-- spec/agda/Certificates.agda and its body is the proof; check the file with
-- spec/agda/check-certificate.sh GeneratedCertificates.agda

module GeneratedCertificates where

open import Prelude
open import Certificates

-- {0, 1, 4, 6} is all-interval in Z12
ai0146 : AllInterval 12 (0 ∷ 1 ∷ 4 ∷ 6 ∷ [])
ai0146 0 _ = 0 , 0 , here , here , refl
ai0146 1 _ = 0 , 1 , here , there here , refl
ai0146 2 _ = 4 , 6 , there (there here) , there (there (there here)) , refl
ai0146 3 _ = 1 , 4 , there here , there (there here) , refl
ai0146 4 _ = 0 , 4 , here , there (there here) , refl
ai0146 5 _ = 1 , 6 , there here , there (there (there here)) , refl
ai0146 6 _ = 0 , 6 , here , there (there (there here)) , refl
ai0146 (suc (suc (suc (suc (suc (suc (suc i))))))) (s≤s (s≤s (s≤s (s≤s (s≤s (s≤s ()))))))

-- {0, 1, 3, 7} is all-interval in Z12
ai0137 : AllInterval 12 (0 ∷ 1 ∷ 3 ∷ 7 ∷ [])
ai0137 0 _ = 0 , 0 , here , here , refl
ai0137 1 _ = 0 , 1 , here , there here , refl
ai0137 2 _ = 1 , 3 , there here , there (there here) , refl
ai0137 3 _ = 0 , 3 , here , there (there here) , refl
ai0137 4 _ = 3 , 7 , there (there here) , there (there (there here)) , refl
ai0137 5 _ = 0 , 7 , here , there (there (there here)) , refl
ai0137 6 _ = 1 , 7 , there here , there (there (there here)) , refl
ai0137 (suc (suc (suc (suc (suc (suc (suc i))))))) (s≤s (s≤s (s≤s (s≤s (s≤s (s≤s ()))))))

-- {0, 1, 3} is all-interval in Z7
ai013z7 : AllInterval 7 (0 ∷ 1 ∷ 3 ∷ [])
ai013z7 0 _ = 0 , 0 , here , here , refl
ai013z7 1 _ = 0 , 1 , here , there here , refl
ai013z7 2 _ = 1 , 3 , there here , there (there here) , refl
ai013z7 3 _ = 0 , 3 , here , there (there here) , refl
ai013z7 (suc (suc (suc (suc i)))) (s≤s (s≤s (s≤s ())))

-- {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11} is all-interval in Z12
chromatic : AllInterval 12 (0 ∷ 1 ∷ 2 ∷ 3 ∷ 4 ∷ 5 ∷ 6 ∷ 7 ∷ 8 ∷ 9 ∷ 10 ∷ 11 ∷ [])
chromatic 0 _ = 0 , 0 , here , here , refl
chromatic 1 _ = 0 , 1 , here , there here , refl
chromatic 2 _ = 0 , 2 , here , there (there here) , refl
chromatic 3 _ = 0 , 3 , here , there (there (there here)) , refl
chromatic 4 _ = 0 , 4 , here , there (there (there (there here))) , refl
chromatic 5 _ = 0 , 5 , here , there (there (there (there (there here)))) , refl
chromatic 6 _ = 0 , 6 , here , there (there (there (there (there (there here))))) , refl
chromatic (suc (suc (suc (suc (suc (suc (suc i))))))) (s≤s (s≤s (s≤s (s≤s (s≤s (s≤s ()))))))

-- {0, 1, 2, 3} is not all-interval in Z12: no pair has interval class 4
cluster0123 : ¬ AllInterval 12 (0 ∷ 1 ∷ 2 ∷ 3 ∷ [])
cluster0123 f = miss (f 4 (s≤s (s≤s (s≤s (s≤s z≤n)))))
  where
  miss : ¬ Hit 12 (0 ∷ 1 ∷ 2 ∷ 3 ∷ []) 4
  miss (_ , _ , here , here , ())
  miss (_ , _ , here , there here , ())
  miss (_ , _ , here , there (there here) , ())
  miss (_ , _ , here , there (there (there here)) , ())
  miss (_ , _ , there here , here , ())
  miss (_ , _ , there here , there here , ())
  miss (_ , _ , there here , there (there here) , ())
  miss (_ , _ , there here , there (there (there here)) , ())
  miss (_ , _ , there (there here) , here , ())
  miss (_ , _ , there (there here) , there here , ())
  miss (_ , _ , there (there here) , there (there here) , ())
  miss (_ , _ , there (there here) , there (there (there here)) , ())
  miss (_ , _ , there (there (there here)) , here , ())
  miss (_ , _ , there (there (there here)) , there here , ())
  miss (_ , _ , there (there (there here)) , there (there here) , ())
  miss (_ , _ , there (there (there here)) , there (there (there here)) , ())

-- {0, 4, 7} is not all-interval in Z12: no pair has interval class 1
majorTriad : ¬ AllInterval 12 (0 ∷ 4 ∷ 7 ∷ [])
majorTriad f = miss (f 1 (s≤s z≤n))
  where
  miss : ¬ Hit 12 (0 ∷ 4 ∷ 7 ∷ []) 1
  miss (_ , _ , here , here , ())
  miss (_ , _ , here , there here , ())
  miss (_ , _ , here , there (there here) , ())
  miss (_ , _ , there here , here , ())
  miss (_ , _ , there here , there here , ())
  miss (_ , _ , there here , there (there here) , ())
  miss (_ , _ , there (there here) , here , ())
  miss (_ , _ , there (there here) , there here , ())
  miss (_ , _ , there (there here) , there (there here) , ())

-- {} is not all-interval in Z12: no pair has interval class 0
emptySet : ¬ AllInterval 12 []
emptySet f = miss (f 0 z≤n)
  where
  miss : ¬ Hit 12 [] 0
  miss (_ , _ , () , _)

-- {0, 1, 3} is not all-interval in Z24: no pair has interval class 4
trichordZ24 : ¬ AllInterval 24 (0 ∷ 1 ∷ 3 ∷ [])
trichordZ24 f = miss (f 4 (s≤s (s≤s (s≤s (s≤s z≤n)))))
  where
  miss : ¬ Hit 24 (0 ∷ 1 ∷ 3 ∷ []) 4
  miss (_ , _ , here , here , ())
  miss (_ , _ , here , there here , ())
  miss (_ , _ , here , there (there here) , ())
  miss (_ , _ , there here , here , ())
  miss (_ , _ , there here , there here , ())
  miss (_ , _ , there here , there (there here) , ())
  miss (_ , _ , there (there here) , here , ())
  miss (_ , _ , there (there here) , there here , ())
  miss (_ , _ , there (there here) , there (there here) , ())

-- In C major the dominant triad {G, B, D} contains the leading tone B
domMajor0 : DominantContainsLeadingTone 0 major
domMajor0 (_ , refl , _) = there here

-- In C# major the dominant triad {Ab, C, Eb} contains the leading tone C
domMajor1 : DominantContainsLeadingTone 1 major
domMajor1 (_ , refl , _) = there here

-- In D major the dominant triad {A, C#, E} contains the leading tone C#
domMajor2 : DominantContainsLeadingTone 2 major
domMajor2 (_ , refl , _) = there here

-- In Eb major the dominant triad {Bb, D, F} contains the leading tone D
domMajor3 : DominantContainsLeadingTone 3 major
domMajor3 (_ , refl , _) = there here

-- In E major the dominant triad {B, Eb, F#} contains the leading tone Eb
domMajor4 : DominantContainsLeadingTone 4 major
domMajor4 (_ , refl , _) = there here

-- In F major the dominant triad {C, E, G} contains the leading tone E
domMajor5 : DominantContainsLeadingTone 5 major
domMajor5 (_ , refl , _) = there here

-- In F# major the dominant triad {C#, F, Ab} contains the leading tone F
domMajor6 : DominantContainsLeadingTone 6 major
domMajor6 (_ , refl , _) = there here

-- In G major the dominant triad {D, F#, A} contains the leading tone F#
domMajor7 : DominantContainsLeadingTone 7 major
domMajor7 (_ , refl , _) = there here

-- In Ab major the dominant triad {Eb, G, Bb} contains the leading tone G
domMajor8 : DominantContainsLeadingTone 8 major
domMajor8 (_ , refl , _) = there here

-- In A major the dominant triad {E, Ab, B} contains the leading tone Ab
domMajor9 : DominantContainsLeadingTone 9 major
domMajor9 (_ , refl , _) = there here

-- In Bb major the dominant triad {F, A, C} contains the leading tone A
domMajor10 : DominantContainsLeadingTone 10 major
domMajor10 (_ , refl , _) = there here

-- In B major the dominant triad {F#, Bb, C#} contains the leading tone Bb
domMajor11 : DominantContainsLeadingTone 11 major
domMajor11 (_ , refl , _) = there here

-- In C harmonic minor the dominant triad {G, B, D} contains the leading tone B
domHarmonic0 : DominantContainsLeadingTone 0 harmonicMinor
domHarmonic0 (_ , refl , _) = there here

-- In C# harmonic minor the dominant triad {Ab, C, Eb} contains the leading tone C
domHarmonic1 : DominantContainsLeadingTone 1 harmonicMinor
domHarmonic1 (_ , refl , _) = there here

-- In D harmonic minor the dominant triad {A, C#, E} contains the leading tone C#
domHarmonic2 : DominantContainsLeadingTone 2 harmonicMinor
domHarmonic2 (_ , refl , _) = there here

-- In Eb harmonic minor the dominant triad {Bb, D, F} contains the leading tone D
domHarmonic3 : DominantContainsLeadingTone 3 harmonicMinor
domHarmonic3 (_ , refl , _) = there here

-- In E harmonic minor the dominant triad {B, Eb, F#} contains the leading tone Eb
domHarmonic4 : DominantContainsLeadingTone 4 harmonicMinor
domHarmonic4 (_ , refl , _) = there here

-- In F harmonic minor the dominant triad {C, E, G} contains the leading tone E
domHarmonic5 : DominantContainsLeadingTone 5 harmonicMinor
domHarmonic5 (_ , refl , _) = there here

-- In F# harmonic minor the dominant triad {C#, F, Ab} contains the leading tone F
domHarmonic6 : DominantContainsLeadingTone 6 harmonicMinor
domHarmonic6 (_ , refl , _) = there here

-- In G harmonic minor the dominant triad {D, F#, A} contains the leading tone F#
domHarmonic7 : DominantContainsLeadingTone 7 harmonicMinor
domHarmonic7 (_ , refl , _) = there here

-- In Ab harmonic minor the dominant triad {Eb, G, Bb} contains the leading tone G
domHarmonic8 : DominantContainsLeadingTone 8 harmonicMinor
domHarmonic8 (_ , refl , _) = there here

-- In A harmonic minor the dominant triad {E, Ab, B} contains the leading tone Ab
domHarmonic9 : DominantContainsLeadingTone 9 harmonicMinor
domHarmonic9 (_ , refl , _) = there here

-- In Bb harmonic minor the dominant triad {F, A, C} contains the leading tone A
domHarmonic10 : DominantContainsLeadingTone 10 harmonicMinor
domHarmonic10 (_ , refl , _) = there here

-- In B harmonic minor the dominant triad {F#, Bb, C#} contains the leading tone Bb
domHarmonic11 : DominantContainsLeadingTone 11 harmonicMinor
domHarmonic11 (_ , refl , _) = there here

-- In C natural minor degree 7 (Bb) is not a leading tone, so no chord has dominant function and the claim holds vacuously
domNatural0 : DominantContainsLeadingTone 0 naturalMinor
domNatural0 (_ , _ , ())

-- In C# natural minor degree 7 (B) is not a leading tone, so no chord has dominant function and the claim holds vacuously
domNatural1 : DominantContainsLeadingTone 1 naturalMinor
domNatural1 (_ , _ , ())

-- In D natural minor degree 7 (C) is not a leading tone, so no chord has dominant function and the claim holds vacuously
domNatural2 : DominantContainsLeadingTone 2 naturalMinor
domNatural2 (_ , _ , ())

-- In Eb natural minor degree 7 (C#) is not a leading tone, so no chord has dominant function and the claim holds vacuously
domNatural3 : DominantContainsLeadingTone 3 naturalMinor
domNatural3 (_ , _ , ())

-- In E natural minor degree 7 (D) is not a leading tone, so no chord has dominant function and the claim holds vacuously
domNatural4 : DominantContainsLeadingTone 4 naturalMinor
domNatural4 (_ , _ , ())

-- In F natural minor degree 7 (Eb) is not a leading tone, so no chord has dominant function and the claim holds vacuously
domNatural5 : DominantContainsLeadingTone 5 naturalMinor
domNatural5 (_ , _ , ())

-- In F# natural minor degree 7 (E) is not a leading tone, so no chord has dominant function and the claim holds vacuously
domNatural6 : DominantContainsLeadingTone 6 naturalMinor
domNatural6 (_ , _ , ())

-- In G natural minor degree 7 (F) is not a leading tone, so no chord has dominant function and the claim holds vacuously
domNatural7 : DominantContainsLeadingTone 7 naturalMinor
domNatural7 (_ , _ , ())

-- In Ab natural minor degree 7 (F#) is not a leading tone, so no chord has dominant function and the claim holds vacuously
domNatural8 : DominantContainsLeadingTone 8 naturalMinor
domNatural8 (_ , _ , ())

-- In A natural minor degree 7 (G) is not a leading tone, so no chord has dominant function and the claim holds vacuously
domNatural9 : DominantContainsLeadingTone 9 naturalMinor
domNatural9 (_ , _ , ())

-- In Bb natural minor degree 7 (Ab) is not a leading tone, so no chord has dominant function and the claim holds vacuously
domNatural10 : DominantContainsLeadingTone 10 naturalMinor
domNatural10 (_ , _ , ())

-- In B natural minor degree 7 (A) is not a leading tone, so no chord has dominant function and the claim holds vacuously
domNatural11 : DominantContainsLeadingTone 11 naturalMinor
domNatural11 (_ , _ , ())

-- All 4 pitch classes lie in C major
cMajorChord : All (λ p → p ∈ scale 0 major) (0 ∷ 4 ∷ 7 ∷ 11 ∷ [])
cMajorChord = here ∷ there (there here) ∷ there (there (there (there here))) ∷ there (there (there (there (there (there here))))) ∷ []

-- All 5 pitch classes lie in A harmonic minor
aHarmonicLine : All (λ p → p ∈ scale 9 harmonicMinor) (9 ∷ 11 ∷ 0 ∷ 8 ∷ 9 ∷ [])
aHarmonicLine = here ∷ there here ∷ there (there here) ∷ there (there (there (there (there (there here))))) ∷ here ∷ []

-- All 0 pitch classes lie in C major
noNotes : All (λ p → p ∈ scale 0 major) []
noNotes = []

-- Pitch class 6 (note 2) lies outside C major
tritoneOutside : ¬ All (λ p → p ∈ scale 0 major) (0 ∷ 6 ∷ 1 ∷ [])
tritoneOutside (_ ∷ p ∷ _) = out p
  where
  out : ¬ (6 ∈ scale 0 major)
  out (there (there (there (there (there (there (there ())))))))

-- Pitch class 8 (note 2) lies outside A natural minor
gSharpOutside : ¬ All (λ p → p ∈ scale 9 naturalMinor) (9 ∷ 8 ∷ [])
gSharpOutside (_ ∷ p ∷ _) = out p
  where
  out : ¬ (8 ∈ scale 9 naturalMinor)
  out (there (there (there (there (there (there (there ())))))))
