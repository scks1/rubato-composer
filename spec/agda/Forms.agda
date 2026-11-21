{-# OPTIONS --safe --without-K #-}

-- Forms.agda: Rubato Composer's forms and zero-addressed denotators as
-- the Java core (org.rubato.math.yoneda) implements them, the relation
-- "denotator d has form F" in a repository, and a decision procedure for
-- it with soundness and completeness proofs.
--
-- README.md lists what is modelled and each place where the Java code
-- departs from Mazzola's theory (labels D1, D2, ...).

module Forms where

open import Prelude
open import OrderedSets

------------------------------------------------------------------------
-- Names. Java identifies a registered form by its name (D1).

Name : Set
Name = String

------------------------------------------------------------------------
-- Modules and elements (org.rubato.math.module). Only the modules the
-- registered music forms use are modelled; any other module is
-- `otherM` and has no elements here.

data Mod : Set where
  ZM QM RM CM : Mod
  ZnM    : Nat → Mod            -- Z/nZ
  RFreeM : Nat → Mod            -- R^n for n ≥ 2 (RProperFreeModule)
  otherM : String → Mod

data Elem : Set where
  zE  : Int → Elem               -- ZElement (Java int, D5)
  znE : Nat → Nat → Elem         -- ZnElement: modulus, value
  qE  : Int → Nat → Elem         -- QElement: numerator, denominator (D14)
  rE  : Float → Elem             -- RElement (Java double, D6)
  cE  : Float → Float → Elem     -- CElement: real and imaginary parts
  rvE : List Float → Elem        -- RProperFreeElement

-- Module.hasElement, plus the invariants Java's constructors maintain:
-- a ZnElement's value is reduced below the modulus, a Rational's
-- denominator is positive.
inMod : Mod → Elem → Bool
inMod ZM         (zE _)    = true
inMod (ZnM n)    (znE m v) = (n ==ⁿ m) && (v <ᵇ n)
inMod QM         (qE _ d)  = 0 <ᵇ d
inMod RM         (rE _)    = true
inMod CM         (cE _ _)  = true
inMod (RFreeM n) (rvE xs)  = length xs ==ⁿ n
inMod _          _         = false

------------------------------------------------------------------------
-- Element order: the sign of each ModuleElement.compareTo.

-- RElement: epsilon is 4 * Double.MIN_VALUE (RElement.java).
-- Double.MIN_VALUE = 2^-1074
minSubnormal : Float
minSubnormal with primFloatEncode (pos 1) (negsuc 1073)
... | just x  = x
... | nothing = 0.0

epsR : Float
epsR = primFloatTimes 4.0 minSubnormal

absF : Float → Float
absF x = if primFloatLess x 0.0 then primFloatNegate x else x

-- RElement.compareTo: equal within epsilon, otherwise -1 if smaller and
-- 1 in every other case, NaN included (D6).
cmpR : Float → Float → Ordering
cmpR a b = if primFloatLess (absF (primFloatMinus a b)) epsR then eq
           else (if primFloatLess a b then lt else gt)

-- Complex.compareTo: real parts, then imaginary parts, by < and >.
cmpC : Float → Float → Float → Float → Ordering
cmpC a b c d = if primFloatLess a c then lt else
               (if primFloatLess c a then gt else
               (if primFloatLess b d then lt else
               (if primFloatLess d b then gt else eq)))

-- RProperFreeElement.compareTo: lengths, then d = a - b per component
-- (d < 0 is smaller, d > 0 is larger, anything else continues).
cmpRV : List Float → List Float → Ordering
cmpRV xs ys = thenO (cmpNat (length xs) (length ys)) (go xs ys)
  where
    go : List Float → List Float → Ordering
    go (a ∷ as) (b ∷ bs) =
      if primFloatLess (primFloatMinus a b) 0.0 then lt else
      (if primFloatLess 0.0 (primFloatMinus a b) then gt else go as bs)
    go _ _ = eq

-- Int times a natural number, for cross-multiplying rationals.
mulIN : Int → Nat → Int
mulIN (pos m)    d       = pos (m * d)
mulIN (negsuc m) zero    = pos 0
mulIN (negsuc m) (suc d) = negsuc (m * suc d + d)

elemIx : Elem → Nat
elemIx (zE _)    = 0
elemIx (znE _ _) = 1
elemIx (qE _ _)  = 2
elemIx (rE _)    = 3
elemIx (cE _ _)  = 4
elemIx (rvE _)   = 5

-- Z and Q use exact arithmetic here, where Java's int arithmetic
-- overflows (D5). Elements of different modules fall back to an
-- arbitrary fixed order; Java compares their modules, which the spec
-- does not model, and well-formed comparisons never reach this case.
cmpElem : Elem → Elem → Ordering
cmpElem (zE a)      (zE b)      = cmpInt a b
cmpElem (znE _ a)   (znE _ b)   = cmpNat a b
cmpElem (qE n₁ d₁)  (qE n₂ d₂)  = cmpInt (mulIN n₁ d₂) (mulIN n₂ d₁)
cmpElem (rE a)      (rE b)      = cmpR a b
cmpElem (cE a b)    (cE c d)    = cmpC a b c d
cmpElem (rvE xs)    (rvE ys)    = cmpRV xs ys
cmpElem x           y           = cmpNat (elemIx x) (elemIx y)

------------------------------------------------------------------------
-- Forms. A form body names its coordinate forms, so recursive forms such
-- as MacroScore and SoundScore are definitions in a repository (D11).

data Body : Set where
  simple  : Mod → Body
  limit   : List Name → List String → Body   -- factors, labels ([] if none)
  colimit : List Name → List String → Body   -- cofactors, labels
  power   : Name → Body
  list    : Name → Body                      -- D7

data Def : Set where
  _≔_ : Name → Body → Def

infix 6 _≔_

-- A repository: Repository's form table.
Env : Set
Env = List Def

lookup : Env → Name → Maybe Body
lookup []            _ = nothing
lookup ((n ≔ b) ∷ E) m = if n ==ˢ m then just b else lookup E m

------------------------------------------------------------------------
-- Zero-addressed denotators. The first argument is the denotator's own
-- name (Nothing for an anonymous denotator, D3), the second the name of
-- its form.

data Den : Set where
  simpleD  : Maybe String → Name → Elem → Den
  limitD   : Maybe String → Name → List Den → Den
  colimitD : Maybe String → Name → Nat → Den → Den
  powerD   : Maybe String → Name → List Den → Den
  listD    : Maybe String → Name → List Den → Den

formOf : Den → Name
formOf (simpleD  _ f _)   = f
formOf (limitD   _ f _)   = f
formOf (colimitD _ f _ _) = f
formOf (powerD   _ f _)   = f
formOf (listD    _ f _)   = f

nameOf : Den → Maybe String
nameOf (simpleD  n _ _)   = n
nameOf (limitD   n _ _)   = n
nameOf (colimitD n _ _ _) = n
nameOf (powerD   n _ _)   = n
nameOf (listD    n _ _)   = n

-- Yoneda.SIMPLE = 0, LIMIT = 1, COLIMIT = 2, POWER = 3, LIST = 4
kindIx : Den → Nat
kindIx (simpleD  _ _ _)   = 0
kindIx (limitD   _ _ _)   = 1
kindIx (colimitD _ _ _ _) = 2
kindIx (powerD   _ _ _)   = 3
kindIx (listD    _ _ _)   = 4

------------------------------------------------------------------------
-- Denotator order: the sign of Denotator.compareTo.

-- Form.compareTo for registered forms: equal names are equal forms,
-- otherwise by type, then by name (D13).
cmpForm : Den → Den → Ordering
cmpForm d e = if formOf d ==ˢ formOf e then eq
              else thenO (cmpNat (kindIx d) (kindIx e)) (cmpString (formOf d) (formOf e))

-- Denotator.nameCompareTo: anonymous first, then NameEntry.compareTo,
-- which for a name without "/" is String.compareTo (D16).
cmpName : Maybe String → Maybe String → Ordering
cmpName nothing  nothing  = eq
cmpName nothing  (just _) = lt
cmpName (just _) nothing  = gt
cmpName (just a) (just b) = cmpString a b

-- Form, then name, then content. Limit, power and list contents compare
-- with ListMorphismMap.compareTo and AutoListMorphismMap.compareTo:
-- element by element, and when one list is a prefix of the other the
-- shorter one is greater (D8). Colimits compare the index, then the
-- factor (IndexMorphismMap.compareTo).
cmpDen  : Den → Den → Ordering
cmpBody : Den → Den → Ordering
cmpDens : List Den → List Den → Ordering

cmpDen d e = thenO (cmpForm d e) (thenO (cmpName (nameOf d) (nameOf e)) (cmpBody d e))

cmpBody (simpleD _ _ a)    (simpleD _ _ b)    = cmpElem a b
cmpBody (limitD _ _ ds)    (limitD _ _ es)    = cmpDens ds es
cmpBody (colimitD _ _ i d) (colimitD _ _ j e) = thenO (cmpNat i j) (cmpDen d e)
cmpBody (powerD _ _ ds)    (powerD _ _ es)    = cmpDens ds es
cmpBody (listD _ _ ds)     (listD _ _ es)     = cmpDens ds es
cmpBody d                  e                  = cmpNat (kindIx d) (kindIx e)

cmpDens []       []       = eq
cmpDens []       (_ ∷ _)  = gt
cmpDens (_ ∷ _)  []       = lt
cmpDens (d ∷ ds) (e ∷ es) = thenO (cmpDen d e) (cmpDens ds es)

------------------------------------------------------------------------
-- "d has form F in repository E"

data _⊢_∶_ (E : Env) : Den → Name → Set
data Coords (E : Env) : List Den → List Name → Set
data Elems (E : Env) : List Den → Name → Set

infix 3 _⊢_∶_

data _⊢_∶_ E where
  simple  : {n : Maybe String} {F : Name} {M : Mod} {e : Elem} →
            lookup E F ≡ just (simple M) → inMod M e ≡ true →
            E ⊢ simpleD n F e ∶ F
  -- exactly one coordinate per factor (D9)
  limit   : {n : Maybe String} {F : Name} {Gs : List Name} {ls : List String} {ds : List Den} →
            lookup E F ≡ just (limit Gs ls) → Coords E ds Gs →
            E ⊢ limitD n F ds ∶ F
  -- an index inside the cofactor list (D10)
  colimit : {n : Maybe String} {F : Name} {Gs : List Name} {ls : List String} {i : Nat} {G : Name} {d : Den} →
            lookup E F ≡ just (colimit Gs ls) → nth i Gs ≡ just G → E ⊢ d ∶ G →
            E ⊢ colimitD n F i d ∶ F
  -- every element has the coordinate form, kept in TreeMap order (D2)
  power   : {n : Maybe String} {F G : Name} {ds : List Den} →
            lookup E F ≡ just (power G) → Elems E ds G → Asc cmpDen ds →
            E ⊢ powerD n F ds ∶ F
  -- every element has the coordinate form, in any order, repeats allowed
  list    : {n : Maybe String} {F G : Name} {ds : List Den} →
            lookup E F ≡ just (list G) → Elems E ds G →
            E ⊢ listD n F ds ∶ F

data Coords E where
  []  : Coords E [] []
  _∷_ : {d : Den} {ds : List Den} {G : Name} {Gs : List Name} →
        E ⊢ d ∶ G → Coords E ds Gs → Coords E (d ∷ ds) (G ∷ Gs)

data Elems E where
  []  : {G : Name} → Elems E [] G
  _∷_ : {d : Den} {ds : List Den} {G : Name} →
        E ⊢ d ∶ G → Elems E ds G → Elems E (d ∷ ds) G

------------------------------------------------------------------------
-- Decision procedure

check       : Env → Den → Name → Bool
checkDen    : Env → Den → Bool
checkSimple : Maybe Body → Elem → Bool
checkLimit  : Env → Maybe Body → List Den → Bool
checkCoords : Env → List Den → List Name → Bool
checkColim  : Env → Maybe Body → Nat → Den → Bool
checkAt     : Env → Maybe Name → Den → Bool
checkPower  : Env → Maybe Body → List Den → Bool
checkList   : Env → Maybe Body → List Den → Bool
checkElems  : Env → List Den → Name → Bool

check E d G = (formOf d ==ˢ G) && checkDen E d

checkDen E (simpleD _ F e)    = checkSimple (lookup E F) e
checkDen E (limitD _ F ds)    = checkLimit E (lookup E F) ds
checkDen E (colimitD _ F i d) = checkColim E (lookup E F) i d
checkDen E (powerD _ F ds)    = checkPower E (lookup E F) ds
checkDen E (listD _ F ds)     = checkList E (lookup E F) ds

checkSimple (just (simple M)) e = inMod M e
checkSimple _                 _ = false

checkLimit E (just (limit Gs _)) ds = checkCoords E ds Gs
checkLimit E _                   _  = false

checkCoords E []       []       = true
checkCoords E (d ∷ ds) (G ∷ Gs) = check E d G && checkCoords E ds Gs
checkCoords E _        _        = false

checkColim E (just (colimit Gs _)) i d = checkAt E (nth i Gs) d
checkColim E _                     _ _ = false

checkAt E (just G) d = check E d G
checkAt E nothing  _ = false

checkPower E (just (power G)) ds = checkElems E ds G && ascB cmpDen ds
checkPower E _                _  = false

checkList E (just (list G)) ds = checkElems E ds G
checkList E _               _  = false

checkElems E []       G = true
checkElems E (d ∷ ds) G = check E d G && checkElems E ds G

------------------------------------------------------------------------
-- Soundness: check E d F ≡ true implies E ⊢ d ∶ F

absurd : {A : Set} → false ≡ true → A
absurd ()

sound       : (E : Env) (d : Den) (G : Name) → check E d G ≡ true → E ⊢ d ∶ G
soundDen    : (E : Env) (d : Den) → checkDen E d ≡ true → E ⊢ d ∶ formOf d
soundCoords : (E : Env) (ds : List Den) (Gs : List Name) → checkCoords E ds Gs ≡ true → Coords E ds Gs
soundElems  : (E : Env) (ds : List Den) (G : Name) → checkElems E ds G ≡ true → Elems E ds G

sound E d G p with ==ˢ-sound (formOf d) G (&&-left (formOf d ==ˢ G) (checkDen E d) p)
... | refl = soundDen E d (&&-right (formOf d ==ˢ formOf d) (checkDen E d) p)

soundDen E (simpleD n F e) p with lookup E F in l
... | just (simple M)    = simple l p
... | just (limit _ _)   = absurd p
... | just (colimit _ _) = absurd p
... | just (power _)     = absurd p
... | just (list _)      = absurd p
... | nothing            = absurd p
soundDen E (limitD n F ds) p with lookup E F in l
... | just (limit Gs ls) = limit l (soundCoords E ds Gs p)
... | just (simple _)    = absurd p
... | just (colimit _ _) = absurd p
... | just (power _)     = absurd p
... | just (list _)      = absurd p
... | nothing            = absurd p
soundDen E (colimitD n F i d) p with lookup E F in l
... | just (colimit Gs ls) = colimitCase (nth i Gs) refl p
  where
    colimitCase : (m : Maybe Name) → nth i Gs ≡ m → checkAt E m d ≡ true → E ⊢ colimitD n F i d ∶ F
    colimitCase (just G) e q = colimit l e (sound E d G q)
    colimitCase nothing  _ q = absurd q
... | just (simple _)    = absurd p
... | just (limit _ _)   = absurd p
... | just (power _)     = absurd p
... | just (list _)      = absurd p
... | nothing            = absurd p
soundDen E (powerD n F ds) p with lookup E F in l
... | just (power G)     = power l (soundElems E ds G (&&-left (checkElems E ds G) (ascB cmpDen ds) p))
                                   (ascB-sound cmpDen ds (&&-right (checkElems E ds G) (ascB cmpDen ds) p))
... | just (simple _)    = absurd p
... | just (limit _ _)   = absurd p
... | just (colimit _ _) = absurd p
... | just (list _)      = absurd p
... | nothing            = absurd p
soundDen E (listD n F ds) p with lookup E F in l
... | just (list G)      = list l (soundElems E ds G p)
... | just (simple _)    = absurd p
... | just (limit _ _)   = absurd p
... | just (colimit _ _) = absurd p
... | just (power _)     = absurd p
... | nothing            = absurd p

soundCoords E []       []       _ = []
soundCoords E []       (_ ∷ _)  p = absurd p
soundCoords E (_ ∷ _)  []       p = absurd p
soundCoords E (d ∷ ds) (G ∷ Gs) p =
  sound E d G (&&-left (check E d G) (checkCoords E ds Gs) p) ∷
  soundCoords E ds Gs (&&-right (check E d G) (checkCoords E ds Gs) p)

soundElems E []       G _ = []
soundElems E (d ∷ ds) G p =
  sound E d G (&&-left (check E d G) (checkElems E ds G) p) ∷
  soundElems E ds G (&&-right (check E d G) (checkElems E ds G) p)

------------------------------------------------------------------------
-- Completeness: E ⊢ d ∶ F implies check E d F ≡ true

complete       : {E : Env} {d : Den} {G : Name} → E ⊢ d ∶ G → check E d G ≡ true
completeCoords : {E : Env} {ds : List Den} {Gs : List Name} → Coords E ds Gs → checkCoords E ds Gs ≡ true
completeElems  : {E : Env} {ds : List Den} {G : Name} → Elems E ds G → checkElems E ds G ≡ true

complete {E} (simple {F = F} {M = M} {e = e} l m) = &&-intro (==ˢ-refl F) (go l)
  where go : {mb : Maybe Body} → mb ≡ just (simple M) → checkSimple mb e ≡ true
        go refl = m
complete {E} (limit {F = F} {Gs = Gs} {ls = ls} {ds = ds} l c) = &&-intro (==ˢ-refl F) (go l)
  where go : {mb : Maybe Body} → mb ≡ just (limit Gs ls) → checkLimit E mb ds ≡ true
        go refl = completeCoords c
complete {E} (colimit {F = F} {Gs = Gs} {ls = ls} {i = i} {G = G} {d = d} l n w) = &&-intro (==ˢ-refl F) (go l)
  where go : {mb : Maybe Body} → mb ≡ just (colimit Gs ls) → checkColim E mb i d ≡ true
        go refl rewrite n = complete w
complete {E} (power {F = F} {G = G} {ds = ds} l es a) = &&-intro (==ˢ-refl F) (go l)
  where go : {mb : Maybe Body} → mb ≡ just (power G) → checkPower E mb ds ≡ true
        go refl = &&-intro (completeElems es) (ascB-complete cmpDen a)
complete {E} (list {F = F} {G = G} {ds = ds} l es) = &&-intro (==ˢ-refl F) (go l)
  where go : {mb : Maybe Body} → mb ≡ just (list G) → checkList E mb ds ≡ true
        go refl = completeElems es

completeCoords []       = refl
completeCoords (w ∷ ws) = &&-intro (complete w) (completeCoords ws)

completeElems []       = refl
completeElems (w ∷ ws) = &&-intro (complete w) (completeElems ws)

------------------------------------------------------------------------
-- Repository invariants

refsOf : Body → List Name
refsOf (simple _)     = []
refsOf (limit Gs _)   = Gs
refsOf (colimit Gs _) = Gs
refsOf (power G)      = G ∷ []
refsOf (list G)       = G ∷ []

namesOf : Env → List Name
namesOf []            = []
namesOf ((n ≔ _) ∷ E) = n ∷ namesOf E

elemˢ : Name → List Name → Bool
elemˢ x = any (λ y → x ==ˢ y)

-- Repository.register refuses a second form under a taken name (D1).
uniqueB : List Name → Bool
uniqueB []       = true
uniqueB (n ∷ ns) = not (elemˢ n ns) && uniqueB ns

-- Every coordinate form is defined in the repository.
closedB : Env → Bool
closedB E = all (λ d → all (λ r → elemˢ r (namesOf E)) (refs d)) E
  where refs : Def → List Name
        refs (_ ≔ b) = refsOf b

-- Limit and colimit labels, when present, name every factor once.
labelsOkB : Env → Bool
labelsOkB = all ok
  where
    okLabels : List Name → List String → Bool
    okLabels _  []       = true
    okLabels Gs ls@(_ ∷ _) = (length ls ==ⁿ length Gs) && uniqueB ls
    ok : Def → Bool
    ok (_ ≔ limit Gs ls)   = okLabels Gs ls
    ok (_ ≔ colimit Gs ls) = okLabels Gs ls
    ok _                   = true

factorCount : Env → Name → Maybe Nat
factorCount E F with lookup E F
... | just (limit Gs _)   = just (length Gs)
... | just (colimit Gs _) = just (length Gs)
... | _                   = nothing
