{-# OPTIONS --safe --without-K #-}

-- Generated.agda: written by spec/agda/gen/ExportAgda.java from the
-- running Java repository. Do not edit; run spec/agda/run-checks.sh.

module Generated where

open import Prelude
open import Forms

nanF infF : Float
nanF = primFloatDiv 0.0 0.0
infF = primFloatDiv 1.0 0.0

-- 48 forms reachable from 18 root forms
javaEnv : Env
javaEnv =
    "Amplitude" ≔ simple RM
  ∷
    "AnchorSound" ≔ limit ("Loudness" ∷ "Pitch" ∷ "OvertoneIndex" ∷ []) []
  ∷
    "Boolean" ≔ simple (ZnM 2)
  ∷
    "BooleanList" ≔ list "Boolean"
  ∷
    "BooleanPair" ≔ limit ("Boolean" ∷ "Boolean" ∷ []) []
  ∷
    "Complex" ≔ simple CM
  ∷
    "Duration" ≔ simple RM
  ∷
    "Dyad" ≔ limit ("Pitch" ∷ "Pitch" ∷ []) []
  ∷
    "Dyads" ≔ power "Dyad"
  ∷
    "Envelope" ≔ list "EnvelopeNode"
  ∷
    "EnvelopeNode" ≔ limit ("Onset" ∷ "Amplitude" ∷ []) []
  ∷
    "FMNode" ≔ limit ("Partial" ∷ "FMSet" ∷ []) []
  ∷
    "FMSet" ≔ power "FMNode"
  ∷
    "GeneralScore" ≔ power "NoteOrRest"
  ∷
    "GenericSound" ≔ limit ("AnchorSound" ∷ "SatelliteSounds" ∷ "Operation" ∷ []) []
  ∷
    "HarmonicOvertone" ≔ limit ("OvertoneIndex" ∷ "Loudness" ∷ []) []
  ∷
    "HarmonicOvertones" ≔ power "HarmonicOvertone"
  ∷
    "HarmonicSpectrum" ≔ limit ("Pitch" ∷ "HarmonicOvertones" ∷ []) []
  ∷
    "Integer" ≔ simple ZM
  ∷
    "IntegerOrReal" ≔ colimit ("Integer" ∷ "Real" ∷ []) []
  ∷
    "IntegerSet" ≔ power "Integer"
  ∷
    "Knot" ≔ limit ("Note" ∷ "MacroScore" ∷ []) []
  ∷
    "Loudness" ≔ simple ZM
  ∷
    "MacroScore" ≔ power "Knot"
  ∷
    "Modulators" ≔ power "SoundNote"
  ∷
    "Note" ≔ limit ("Onset" ∷ "Pitch" ∷ "Loudness" ∷ "Duration" ∷ "Voice" ∷ []) ("onset" ∷ "pitch" ∷ "loudness" ∷ "duration" ∷ "voice" ∷ [])
  ∷
    "NoteOrRest" ≔ colimit ("Note" ∷ "Rest" ∷ []) []
  ∷
    "Onset" ≔ simple RM
  ∷
    "Operation" ≔ simple (ZnM 3)
  ∷
    "OvertoneIndex" ≔ simple ZM
  ∷
    "Pan" ≔ simple RM
  ∷
    "Partial" ≔ limit ("Loudness" ∷ "Pitch" ∷ "Pan" ∷ []) []
  ∷
    "Pitch" ≔ simple QM
  ∷
    "PitchClass" ≔ simple (ZnM 12)
  ∷
    "PitchClassNote" ≔ limit ("Onset" ∷ "PitchClass" ∷ "Loudness" ∷ "Duration" ∷ "Voice" ∷ []) []
  ∷
    "PitchClassScore" ≔ power "PitchClassNote"
  ∷
    "Real" ≔ simple RM
  ∷
    "RealNote" ≔ simple (RFreeM 5)
  ∷
    "RealScore" ≔ power "RealNote"
  ∷
    "RealSet" ≔ power "Real"
  ∷
    "Rest" ≔ limit ("Onset" ∷ "Duration" ∷ "Voice" ∷ []) []
  ∷
    "SatelliteSounds" ≔ list "GenericSound"
  ∷
    "Score" ≔ power "Note"
  ∷
    "SoundNode" ≔ limit ("SoundNote" ∷ "SoundScore" ∷ []) []
  ∷
    "SoundNote" ≔ limit ("Onset" ∷ "Pitch" ∷ "Loudness" ∷ "Duration" ∷ "Voice" ∷ "Modulators" ∷ []) []
  ∷
    "SoundScore" ≔ power "SoundNode"
  ∷
    "Spectrum" ≔ power "Partial"
  ∷
    "Voice" ≔ simple ZM
  ∷
  []

-- a Score of three notes, inserted out of order
javaScore : Den
javaScore =
  powerD nothing "Score"
    (
      limitD nothing "Note"
        (
          simpleD nothing "Onset" (rE 0.0)
        ∷
          simpleD nothing "Pitch" (qE (pos 60) 1)
        ∷
          simpleD nothing "Loudness" (zE (pos 80))
        ∷
          simpleD nothing "Duration" (rE 1.0)
        ∷
          simpleD nothing "Voice" (zE (pos 0))
        ∷
          [])
    ∷
      limitD nothing "Note"
        (
          simpleD nothing "Onset" (rE 2.0)
        ∷
          simpleD nothing "Pitch" (qE (pos 64) 1)
        ∷
          simpleD nothing "Loudness" (zE (pos 80))
        ∷
          simpleD nothing "Duration" (rE 1.0)
        ∷
          simpleD nothing "Voice" (zE (pos 0))
        ∷
          [])
    ∷
      limitD (just "melody-start") "Note"
        (
          simpleD nothing "Onset" (rE 0.5)
        ∷
          simpleD nothing "Pitch" (qE (pos 62) 1)
        ∷
          simpleD nothing "Loudness" (zE (pos 80))
        ∷
          simpleD nothing "Duration" (rE 1.0)
        ∷
          simpleD nothing "Voice" (zE (pos 0))
        ∷
          [])
    ∷
      [])

-- a Note built from three coordinates (D9)
javaPaddedNote : Den
javaPaddedNote =
  limitD nothing "Note"
    (
      simpleD nothing "Onset" (rE 2.5)
    ∷
      simpleD nothing "Pitch" (qE (pos 121) 2)
    ∷
      simpleD nothing "Loudness" (zE (pos 90))
    ∷
      simpleD nothing "Duration" (rE 0.0)
    ∷
      simpleD nothing "Voice" (zE (pos 0))
    ∷
      [])

-- a MacroScore with a satellite note
javaMacroScore : Den
javaMacroScore =
  powerD nothing "MacroScore"
    (
      limitD nothing "Knot"
        (
          limitD nothing "Note"
            (
              simpleD nothing "Onset" (rE 0.0)
            ∷
              simpleD nothing "Pitch" (qE (pos 60) 1)
            ∷
              simpleD nothing "Loudness" (zE (pos 80))
            ∷
              simpleD nothing "Duration" (rE 1.0)
            ∷
              simpleD nothing "Voice" (zE (pos 0))
            ∷
              [])
        ∷
          powerD nothing "MacroScore"
            (
              limitD nothing "Knot"
                (
                  limitD nothing "Note"
                    (
                      simpleD nothing "Onset" (rE 1.0)
                    ∷
                      simpleD nothing "Pitch" (qE (pos 67) 1)
                    ∷
                      simpleD nothing "Loudness" (zE (pos 80))
                    ∷
                      simpleD nothing "Duration" (rE 1.0)
                    ∷
                      simpleD nothing "Voice" (zE (pos 0))
                    ∷
                      [])
                ∷
                  powerD nothing "MacroScore"
                    []
                ∷
                  [])
            ∷
              [])
        ∷
          [])
    ∷
      [])

-- SoundNoteGenerator.createMultiLevelSoundScore, three levels
javaSoundScore : Den
javaSoundScore =
  powerD nothing "SoundScore"
    (
      limitD nothing "SoundNode"
        (
          limitD nothing "SoundNote"
            (
              simpleD nothing "Onset" (rE 0.0)
            ∷
              simpleD nothing "Pitch" (qE (pos 60) 1)
            ∷
              simpleD nothing "Loudness" (zE (pos 100))
            ∷
              simpleD nothing "Duration" (rE 1.0)
            ∷
              simpleD nothing "Voice" (zE (pos 0))
            ∷
              powerD nothing "Modulators"
                []
            ∷
              [])
        ∷
          powerD nothing "SoundScore"
            (
              limitD nothing "SoundNode"
                (
                  limitD nothing "SoundNote"
                    (
                      simpleD nothing "Onset" (rE 1.0)
                    ∷
                      simpleD nothing "Pitch" (qE (pos 64) 1)
                    ∷
                      simpleD nothing "Loudness" (zE (pos 99))
                    ∷
                      simpleD nothing "Duration" (rE 1.0)
                    ∷
                      simpleD nothing "Voice" (zE (pos 1))
                    ∷
                      powerD nothing "Modulators"
                        []
                    ∷
                      [])
                ∷
                  powerD nothing "SoundScore"
                    (
                      limitD nothing "SoundNode"
                        (
                          limitD nothing "SoundNote"
                            (
                              simpleD nothing "Onset" (rE 2.0)
                            ∷
                              simpleD nothing "Pitch" (qE (pos 67) 1)
                            ∷
                              simpleD nothing "Loudness" (zE (pos 98))
                            ∷
                              simpleD nothing "Duration" (rE 1.0)
                            ∷
                              simpleD nothing "Voice" (zE (pos 2))
                            ∷
                              powerD nothing "Modulators"
                                []
                            ∷
                              [])
                        ∷
                          powerD nothing "SoundScore"
                            []
                        ∷
                          [])
                    ∷
                      [])
                ∷
                  [])
            ∷
              [])
        ∷
          [])
    ∷
      [])

-- SoundNoteGenerator.createFMNodeDenotator, model Double modulator
javaFMNode : Den
javaFMNode =
  limitD nothing "SoundNode"
    (
      limitD nothing "SoundNote"
        (
          simpleD nothing "Onset" (rE 0.0)
        ∷
          simpleD nothing "Pitch" (qE (pos 60) 1)
        ∷
          simpleD nothing "Loudness" (zE (pos 100))
        ∷
          simpleD nothing "Duration" (rE 1.0)
        ∷
          simpleD nothing "Voice" (zE (pos 0))
        ∷
          powerD nothing "Modulators"
            (
              limitD nothing "SoundNote"
                (
                  simpleD nothing "Onset" (rE 0.0)
                ∷
                  simpleD nothing "Pitch" (qE (pos 20) 1)
                ∷
                  simpleD nothing "Loudness" (zE (pos 10))
                ∷
                  simpleD nothing "Duration" (rE 0.0)
                ∷
                  simpleD nothing "Voice" (zE (pos 0))
                ∷
                  powerD nothing "Modulators"
                    []
                ∷
                  [])
            ∷
              limitD nothing "SoundNote"
                (
                  simpleD nothing "Onset" (rE 0.0)
                ∷
                  simpleD nothing "Pitch" (qE (pos 50) 1)
                ∷
                  simpleD nothing "Loudness" (zE (pos 10))
                ∷
                  simpleD nothing "Duration" (rE 0.0)
                ∷
                  simpleD nothing "Voice" (zE (pos 0))
                ∷
                  powerD nothing "Modulators"
                    []
                ∷
                  [])
            ∷
              [])
        ∷
          [])
    ∷
      powerD nothing "SoundScore"
        []
    ∷
      [])

-- a GeneralScore holding a note and a rest (colimit)
javaGeneralScore : Den
javaGeneralScore =
  powerD nothing "GeneralScore"
    (
      colimitD nothing "NoteOrRest" 0
        (
          limitD nothing "Note"
            (
              simpleD nothing "Onset" (rE 0.0)
            ∷
              simpleD nothing "Pitch" (qE (pos 60) 1)
            ∷
              simpleD nothing "Loudness" (zE (pos 80))
            ∷
              simpleD nothing "Duration" (rE 1.0)
            ∷
              simpleD nothing "Voice" (zE (pos 0))
            ∷
              [])
        )
    ∷
      colimitD nothing "NoteOrRest" 1
        (
          limitD nothing "Rest"
            (
              simpleD nothing "Onset" (rE 1.0)
            ∷
              simpleD nothing "Duration" (rE 0.5)
            ∷
              simpleD nothing "Voice" (zE (pos 0))
            ∷
              [])
        )
    ∷
      [])

-- an Envelope (list) with a repeated node
javaEnvelope : Den
javaEnvelope =
  listD nothing "Envelope"
    (
      limitD nothing "EnvelopeNode"
        (
          simpleD nothing "Onset" (rE 0.0)
        ∷
          simpleD nothing "Amplitude" (rE 0.0)
        ∷
          [])
    ∷
      limitD nothing "EnvelopeNode"
        (
          simpleD nothing "Onset" (rE 0.1)
        ∷
          simpleD nothing "Amplitude" (rE 1.0)
        ∷
          [])
    ∷
      limitD nothing "EnvelopeNode"
        (
          simpleD nothing "Onset" (rE 0.0)
        ∷
          simpleD nothing "Amplitude" (rE 0.0)
        ∷
          [])
    ∷
      [])

-- a Complex simple denotator
javaComplex : Den
javaComplex =
  simpleD (just "i") "Complex" (cE 0.0 1.0)

-- IntegerSet of 1, Integer.MIN_VALUE and 0 (D5)
javaIntegerSetOverflow : Den
javaIntegerSetOverflow =
  powerD nothing "IntegerSet"
    (
      simpleD nothing "Integer" (zE (pos 0))
    ∷
      simpleD nothing "Integer" (zE (pos 1))
    ∷
      simpleD nothing "Integer" (zE (negsuc 2147483647))
    ∷
      [])

-- RealSet of NaN, NaN and 1.0 (D6)
javaRealSetNaN : Den
javaRealSetNaN =
  powerD nothing "RealSet"
    (
      simpleD nothing "Real" (rE nanF)
    ∷
      simpleD nothing "Real" (rE nanF)
    ∷
      simpleD nothing "Real" (rE 1.0)
    ∷
      [])

