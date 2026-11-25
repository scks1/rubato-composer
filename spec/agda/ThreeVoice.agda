{-# OPTIONS --safe --without-K #-}

-- ThreeVoice.agda: three-voice first-species counterpoint after Okumura
-- and Shibayama, arXiv 2606.01102v3, for the Fuxian dichotomy (K/D) and the
-- mask H_Fux, as java/core's ThreeVoiceCounterpoint implements it
-- (sources/THREE-VOICE-COUNTERPOINT-SPEC.md). Every fact is a Boolean or
-- numeric computation that reduces by refl:
--
--  * the maximizing contrapuntal symmetries Σ of ε.r in the subgroup H
--    (r ∈ K), given as data from ContrapuntalSymmetries.analyse, are
--    checked against a search of all 576 elements of H;
--  * H_Fux (Def. 3.1, p. 7) in Table 8's order (p. 24), 26 configurations
--    and 312 states;
--  * the successor sets S_H(ξ) of eq. (2) (p. 6) for the 26 sources with
--    lower voice 0, computed by the joint maximization, give Table 9
--    (p. 25);
--  * the 288 parsimonious P/L/R realizations of §6.4: 192 have the target in
--    the mask and 12 are admitted, all P from a minor triad in voicing type
--    351 (Prop. 6.8, p. 23); under the hold-fibre relaxation (spec D6, ours)
--    168 are admitted, under hold (D12, ours) 144 and under reflexive (D13,
--    ours) 180, and reflexive admits 5,547 steps from the lower-voice-0
--    sources.

module ThreeVoice where

open import Prelude
open import Counterpoint

------------------------------------------------------------------------
-- Membership in g K[ε] through the inverse of g. For g = T^{a+ε.b}.(u+ε.v)
-- with u a unit (u² = 1 mod 12), g⁻¹(x' + ε.y') = x + ε.y with
-- x = u (x' − a) and y = u (y' − v x − b).

sub12 : Nat → Nat → Nat
sub12 p q = m12 (p + (12 - m12 q))

inImgAt : Nat → Nat → Nat → Nat → Nat → Bool
inImgAt b u v y' x = mem (m12 (u * sub12 (sub12 y' (v * x)) b)) X82

inImg : DAff → Pt → Bool
inImg (daff a b u v) (pt x' y') = inImgAt b u v y' (m12 (u * sub12 x' a))

------------------------------------------------------------------------
-- Σ at cantus 0, from ContrapuntalSymmetries.analyse (the H elements
-- T^{ε.b}.(u+ε.v); logs/three-voice-counterpoint-2026-11-24/D02-sigma0.txt)

sigma0 : Nat → List DAff
sigma0 0 = daff 0 6 1 6 ∷ daff 0 6 7 6 ∷ daff 0 11 11 0 ∷ daff 0 11 11 4 ∷ daff 0 11 11 8 ∷ []
sigma0 3 = daff 0 8 5 4 ∷ daff 0 8 5 8 ∷ []
sigma0 4 = daff 0 6 1 6 ∷ daff 0 6 7 6 ∷ []
sigma0 7 = daff 0 0 7 0 ∷ []
sigma0 8 = daff 0 3 7 0 ∷ daff 0 3 7 4 ∷ daff 0 3 7 8 ∷ daff 0 6 1 6 ∷ daff 0 6 7 6 ∷ []
sigma0 9 = daff 0 8 5 4 ∷ daff 0 8 5 8 ∷ []
sigma0 _ = []

-- the 576 elements of H
hgroup : List DAff
hgroup = concatMap (λ b → concatMap (λ u → map (daff 0 b u) z12) units) z12

kpoints : List Pt
kpoints = concatMap (λ x → map (pt x) X82) z12

-- g is a candidate for ε.r: ε.r ∉ gK[ε] and p_0[ε] exchanges gK[ε] and its complement
candidate : Nat → DAff → Bool
candidate r g = not (inImg g (pt 0 r))
             && all (λ p → inImg g (dap (induced p82 0) p) ⇔ᵇ not (inImg g p)) points

meet : DAff → Nat
meet g = count (inImg g) kpoints

-- (a let would be substituted into both branches and evaluated twice)
max2 : Nat → Nat → Nat
max2 n m = if m <ᵇ n then n else m

maxOf : List Nat → Nat
maxOf []       = 0
maxOf (n ∷ ns) = max2 n (maxOf ns)

eqDAff : DAff → DAff → Bool
eqDAff (daff a b u v) (daff a' b' u' v') = (a ==ⁿ a') && (b ==ⁿ b') && (u ==ⁿ u') && (v ==ⁿ v')

-- Σ(ε.r) by search: the candidates in H whose meet with K[ε] is maximal.
-- Computed values are passed as arguments, which Agda's call-by-need
-- evaluation shares; a where-bound value would be recomputed at each use.
filterD : (DAff → Bool) → List DAff → List DAff
filterD q []       = []
filterD q (g ∷ gs) = if q g then g ∷ filterD q gs else filterD q gs

keepMeet : Nat → List DAff → List DAff
keepMeet m gs = filterD (λ g → meet g ==ⁿ m) gs

withMax : List DAff → List DAff
withMax cands = keepMeet (maxOf (map meet cands)) cands

sigmaSearch : Nat → List DAff
sigmaSearch r = withMax (filterD (candidate r) hgroup)

sameList : List DAff → List DAff → Bool
sameList xs ys = (length xs ==ⁿ length ys) && all (λ g → any (eqDAff g) ys) xs

-- Fact 1: the data above is exactly the search result, for every r in K
sigma-data : all (λ r → sameList (sigma0 r) (sigmaSearch r)) X82 ≡ true
sigma-data = refl

------------------------------------------------------------------------
-- States (a; b, c), projections and active pairs (§2.3, p. 5)

data St : Set where
  st : (a b c : Nat) → St

data Pair : Set where
  LM LU MU : Pair

proj : Pair → St → Pt
proj LM (st a b c) = pt a b
proj LU (st a b c) = pt a c
proj MU (st a b c) = pt (m12 (a + b)) (sub12 c b)

active : St → List Pair
active s = LM ∷ LU ∷ (if mem (sub12 (cOf s) (bOf s)) X82 then MU ∷ [] else [])
  where
    bOf cOf : St → Nat
    bOf (st _ b _) = b
    cOf (st _ _ c) = c

-- Σ of a projection at its own cantus x: T^x ∘ g ∘ T^{−x} = T^{(1−u)x + ε.(b − v x)}.(u+ε.v)
conj : Nat → DAff → DAff
conj x (daff _ b u v) = daff (m12 (x * (13 - u))) (sub12 b (v * x)) u v

sigmaAt : Pt → List DAff
sigmaAt (pt x r) = map (conj x) (sigma0 r)

------------------------------------------------------------------------
-- H_Fux (Def. 3.1, p. 7) and Table 8 (p. 24)

filterPt : (Pt → Bool) → List Pt → List Pt
filterPt q []       = []
filterPt q (p ∷ ps) = if q p then p ∷ filterPt q ps else filterPt q ps

defOK : Pt → Bool
defOK (pt b c) = not (mem (sub12 c b) (1 ∷ 2 ∷ 10 ∷ 11 ∷ []))
              && not (((b ==ⁿ 4) && (c ==ⁿ 8)) || ((b ==ⁿ 8) && (c ==ⁿ 4)))

hfux : List Pt
hfux = filterPt defOK (concatMap (λ b → map (pt b) X82) X82)

table8 : List Pt
table8 = pt 0 0 ∷ pt 0 3 ∷ pt 0 4 ∷ pt 0 7 ∷ pt 0 8 ∷ pt 0 9 ∷
         pt 3 0 ∷ pt 3 3 ∷ pt 3 7 ∷ pt 3 8 ∷ pt 3 9 ∷
         pt 4 0 ∷ pt 4 4 ∷ pt 4 7 ∷ pt 4 9 ∷
         pt 7 0 ∷ pt 7 3 ∷ pt 7 4 ∷ pt 7 7 ∷
         pt 8 0 ∷ pt 8 3 ∷ pt 8 8 ∷
         pt 9 0 ∷ pt 9 3 ∷ pt 9 4 ∷ pt 9 9 ∷ []

-- Fact 2: Def. 3.1 gives Table 8's 26 configurations, in that order
mask-is-table8 : hfux ≡ table8
mask-is-table8 = refl

states : List St
states = concatMap (λ a → map (λ { (pt b c) → st a b c }) table8) z12

-- Fact 3: 312 states
states-312 : length states ≡ 312
states-312 = refl

inMask : St → Bool
inMask (st a b c) = memPt (pt b c) table8

------------------------------------------------------------------------
-- The successor set S_H(ξ) of eq. (2): the union of C_H(ξ; g) over the
-- tuples g of maximal |C_H(ξ; g)|, one symmetry per active pair

Pred : Set
Pred = St → Bool

product : List (List Pred) → List (List Pred)
product []         = [] ∷ []
product (gs ∷ gss) = concatMap (λ g → map (g ∷_) (product gss)) gs

holds : List Pred → St → Bool
holds tup t = all (λ q → q t) tup

pairPreds : St → Pair → List Pred
pairPreds s α = map (λ g t → inImg g (proj α t)) (sigmaAt (proj α s))

filterL : {A : Set} → (A → Bool) → List A → List A
filterL q []       = []
filterL q (x ∷ xs) = if q x then x ∷ filterL q xs else filterL q xs

-- a tuple with its size |C_H(ξ; g)| within the base set
data Sized : Set where
  sized : List Pred → Nat → Sized

sizeAll : List St → List (List Pred) → List Sized
sizeAll base []           = []
sizeAll base (tup ∷ tups) = sized tup (count (holds tup) base) ∷ sizeAll base tups

maxSize : List Sized → Nat
maxSize []                = 0
maxSize (sized _ n ∷ ss) = max2 n (maxSize ss)

atSize : Nat → List Sized → List (List Pred)
atSize m []                 = []
atSize m (sized tup n ∷ ss) = if n ==ⁿ m then tup ∷ atSize m ss else atSize m ss

-- the tuples of maximal size
maximalOf : List Sized → List (List Pred)
maximalOf ss = atSize (maxSize ss) ss

inSome : List (List Pred) → List St → List St
inSome tups base = filterL (λ t → any (λ tup → holds tup t) tups) base

unionOf : List St → List (List Pred) → List St
unionOf base tups = inSome (maximalOf (sizeAll base tups)) base

succ : St → List St
succ s = unionOf states (product (map (pairPreds s) (active s)))

-- Fact 4: Table 9 (p. 25), the 26 counts by configuration (b, c) at lower voice 0
table9 : List Nat
table9 = map (λ { (pt b c) → length (succ (st 0 b c)) }) table8

table9-counts : table9 ≡ 138 ∷ 156 ∷ 138 ∷ 136 ∷ 126 ∷ 156 ∷
                         184 ∷ 132 ∷ 136 ∷ 204 ∷ 256 ∷
                         126 ∷ 138 ∷ 138 ∷ 206 ∷
                         198 ∷ 168 ∷ 146 ∷ 152 ∷
                         138 ∷ 152 ∷ 120 ∷
                         184 ∷ 256 ∷ 140 ∷ 132 ∷ []
table9-counts = refl

------------------------------------------------------------------------
-- Successors at other lower voices by translation. S_H commutes with the
-- translation T^j of all three voices (the Java test counts 12 × 4,156 =
-- 49,872 edges); here two sources are checked directly.

eqSt : St → St → Bool
eqSt (st a b c) (st a' b' c') = (a ==ⁿ a') && (b ==ⁿ b') && (c ==ⁿ c')

memSt : St → List St → Bool
memSt t ts = any (eqSt t) ts

shift : Nat → St → St
shift j (st a b c) = st (m12 (a + j)) b c

sameSet : List St → List St → Bool
sameSet xs ys = (length xs ==ⁿ length ys) && all (λ t → memSt t ys) xs

-- Fact 5: S_H(5; 7, 4) = T^5 S_H(0; 7, 4) and S_H(11; 0, 0) = T^11 S_H(0; 0, 0)
translated : (sameSet (succ (st 5 7 4)) (map (shift 5) (succ (st 0 7 4)))
           && sameSet (succ (st 11 0 0)) (map (shift 11) (succ (st 0 0 0)))) ≡ true
translated = refl

-- A table of S_H at lower voice 0, built once and passed as an argument so
-- that each entry is computed at most once
data Entry : Set where
  entry : Pt → List St → Entry

Table : Set
Table = List Entry

buildTable : List Pt → Table
buildTable []             = []
buildTable (pt b c ∷ hs) = entry (pt b c) (succ (st 0 b c)) ∷ buildTable hs

lookupS : Table → Nat → Nat → List St
lookupS []                      b c = []
lookupS (entry (pt b' c') ss ∷ es) b c = if (b ==ⁿ b') && (c ==ⁿ c') then ss else lookupS es b c

-- s → t is admitted when T^{−a} t lies in S_H(0; b, c) for s = (a; b, c)
admitted : Table → St → St → Bool
admitted tbl (st a b c) t = inMask t && memSt (shift (12 - a) t) (lookupS tbl b c)

------------------------------------------------------------------------
-- Example 3.3 (p. 8): all eleven steps are admitted

example33 : List St
example33 = st 2 3 0 ∷ st 9 8 3 ∷ st 5 4 7 ∷ st 2 7 3 ∷ st 1 8 3 ∷ st 2 7 3 ∷
            st 4 8 3 ∷ st 5 7 4 ∷ st 7 3 7 ∷ st 5 7 4 ∷ st 4 8 3 ∷ st 2 3 0 ∷ []

steps : Table → List St → List Bool
steps tbl (s ∷ t ∷ ts) = admitted tbl s t ∷ steps tbl (t ∷ ts)
steps tbl _            = []

-- Fact 6: eleven steps, all true
example33-admitted : steps (buildTable table8) example33
                   ≡ true ∷ true ∷ true ∷ true ∷ true ∷ true ∷ true ∷ true ∷ true ∷ true ∷ true ∷ []
example33-admitted = refl

------------------------------------------------------------------------
-- The 288 parsimonious realizations of P, L and R (§6.4, p. 23)

data Tr : Set where
  P L R : Tr

data Real : Set where
  -- transformation, source major?, voicing type (members from the lower voice up: 1, 3 or 5), source, target
  real : Tr → Bool → Nat → St → St → Real

-- root, third, fifth
triad : Nat → Bool → List Nat
triad r major = m12 r ∷ m12 (r + (if major then 4 else 3)) ∷ m12 (r + 7) ∷ []

nthN : Nat → List Nat → Nat
nthN _       []       = 0
nthN zero    (x ∷ xs) = x
nthN (suc n) (x ∷ xs) = nthN n xs

-- types 135, 153, 351, 315 as member positions 0 (root), 1 (third), 2 (fifth)
types : List (List Nat)
types = (0 ∷ 1 ∷ 2 ∷ []) ∷ (0 ∷ 2 ∷ 1 ∷ []) ∷ (1 ∷ 2 ∷ 0 ∷ []) ∷ (1 ∷ 0 ∷ 2 ∷ []) ∷ []

typeCode : List Nat → Nat
typeCode (0 ∷ 1 ∷ 2 ∷ []) = 135
typeCode (0 ∷ 2 ∷ 1 ∷ []) = 153
typeCode (1 ∷ 2 ∷ 0 ∷ []) = 351
typeCode _                = 315

image3 : Tr → Nat → Bool → List Nat
image3 P r major = triad r (not major)
image3 L r true  = triad (r + 4) false
image3 L r false = triad (r + 8) true
image3 R r true  = triad (r + 9) false
image3 R r false = triad (r + 3) true

fromPcs : List Nat → St
fromPcs (lo ∷ mi ∷ up ∷ _) = st lo (sub12 mi lo) (sub12 up lo)
fromPcs _                  = st 0 0 0

-- the pitch class of x that is not in y (the moving voice's old pitch class)
missing : List Nat → List Nat → Nat
missing []       ys = 0
missing (x ∷ xs) ys = if mem x ys then missing xs ys else x

swap : Nat → Nat → Nat → Nat
swap old new q = if q ==ⁿ old then new else q

realize : Tr → Nat → Bool → List Nat → Real
realize tr r major ty =
  real tr major (typeCode ty) (fromPcs src) (fromPcs (map (swap (missing x y) (missing y x)) src))
  where
    x = triad r major
    y = image3 tr r major
    src = map (λ i → nthN i x) ty

realizations : List Real
realizations = concatMap (λ r → concatMap (λ major → concatMap (λ tr → map (realize tr r major) types)
                 (P ∷ L ∷ R ∷ [])) (true ∷ false ∷ [])) z12

targetIn : Real → Bool
targetIn (real _ _ _ _ t) = inMask t

admittedR : Table → Real → Bool
admittedR tbl (real _ _ _ s t) = admitted tbl s t

isTr : Tr → Real → Bool
isTr P (real P _ _ _ _) = true
isTr L (real L _ _ _ _) = true
isTr R (real R _ _ _ _) = true
isTr _ _                = false

pMinor351 : Real → Bool
pMinor351 (real P false ty _ _) = ty ==ⁿ 351
pMinor351 _                     = false

countR : (Real → Bool) → List Real → Nat
countR q rs = count q rs

-- realizations, in the mask, admitted, admitted but not (P, minor, 351)
prop68With : Table → List Nat
prop68With tbl = countR (λ _ → true) realizations
               ∷ countR targetIn realizations
               ∷ countR (admittedR tbl) realizations
               ∷ countR (λ z → admittedR tbl z && not (pMinor351 z)) realizations ∷ []

-- Fact 7: Prop. 6.8, 288 realizations, 192 in the mask, 12 admitted, all (P, minor, 351)
prop68 : prop68With (buildTable table8) ≡ 288 ∷ 192 ∷ 12 ∷ 0 ∷ []
prop68 = refl

------------------------------------------------------------------------
-- The hold-fibre relaxation (spec D6; ours, not the paper's): a pair
-- that stands still is held, the moving pairs are maximized jointly among
-- the states that keep the held pairs fixed, and a step where no pair moves
-- is admitted


stillIn : St → St → Pair → Bool
stillIn s t α = eqPt (proj α s) (proj α t)

filterPair : (Pair → Bool) → List Pair → List Pair
filterPair q []       = []
filterPair q (α ∷ αs) = if q α then α ∷ filterPair q αs else filterPair q αs

keeps : St → List Pair → St → Bool
keeps s still t = all (stillIn s t) still

holdWith : St → St → List Pair → List Pair → Bool
holdWith s t still []     = true
holdWith s t still moving =
  memSt t (unionOf (filterL (keeps s still) states) (product (map (pairPreds s) moving)))

holdAt0 : St → St → Bool
holdAt0 s t = holdWith s t (filterPair (stillIn s t) (active s)) (filterPair (λ α → not (stillIn s t α)) (active s))

-- translate the step to lower voice 0
holdAdmitted : St → St → Bool
holdAdmitted (st a b c) t = inMask t && holdAt0 (st 0 b c) (shift (12 - a) t)

holdR : Real → Bool
holdR (real _ _ _ s t) = holdAdmitted s t

refusedAs : Tr → Bool → Nat → Real → Bool
refusedAs tr mj ty z@(real tr' mj' ty' _ _) =
  targetIn z && not (holdR z) && isTr tr z && (mj ⇔ᵇ mj') && (ty ==ⁿ ty')

-- admitted, admitted P, L and R, refused (P, major, 351), refused (L, minor, 135)
holdSummary : List Nat
holdSummary = countR holdR realizations
            ∷ countR (λ z → holdR z && isTr P z) realizations
            ∷ countR (λ z → holdR z && isTr L z) realizations
            ∷ countR (λ z → holdR z && isTr R z) realizations
            ∷ countR (refusedAs P true 351) realizations
            ∷ countR (refusedAs L false 135) realizations ∷ []

-- Fact 8: hold-fibre admits 168 of the 192 (P 84, L 36, R 48) and refuses
-- 12 of (P, major, 351) and 12 of (L, minor, 135), which with the 168 makes
-- up the 192
hold-fibre : holdSummary ≡ 168 ∷ 84 ∷ 36 ∷ 48 ∷ 12 ∷ 12 ∷ []
hold-fibre = refl

------------------------------------------------------------------------
-- The hold relaxation (spec D12; ours, not the paper's): as hold-fibre, but
-- the moving pairs are maximized jointly among all states in the mask

holdAllWith : St → St → List Pair → Bool
holdAllWith s t []     = true
holdAllWith s t moving = memSt t (unionOf states (product (map (pairPreds s) moving)))

holdAllAdmitted : St → St → Bool
holdAllAdmitted (st a b c) t = inMask t && holdAllWith s0 t0 (filterPair (λ α → not (stillIn s0 t0 α)) (active s0))
  where
    s0 = st 0 b c
    t0 = shift (12 - a) t

holdAllR : Real → Bool
holdAllR (real _ _ _ s t) = holdAllAdmitted s t

------------------------------------------------------------------------
-- The reflexive relaxation (spec D13; ours, not the paper's): the V1
-- baseline with held pairs skipped. Each moving active pair's target lies in
-- some image of its maximizing symmetries; there is no joint maximization.

reflAt0 : St → St → Bool
reflAt0 s t = all (λ α → stillIn s t α || any (λ q → q t) (pairPreds s α)) (active s)

reflAdmitted : St → St → Bool
reflAdmitted (st a b c) t = inMask t && reflAt0 (st 0 b c) (shift (12 - a) t)

reflR : Real → Bool
reflR (real _ _ _ s t) = reflAdmitted s t

refusedBy : (Real → Bool) → Tr → Bool → Nat → Real → Bool
refusedBy q tr mj ty z@(real tr' mj' ty' _ _) =
  targetIn z && not (q z) && isTr tr z && (mj ⇔ᵇ mj') && (ty ==ⁿ ty')

-- admitted, admitted P, L and R, refused (P, major, 351), refused (L, minor, 135)
summaryOf : (Real → Bool) → List Nat
summaryOf q = countR q realizations
            ∷ countR (λ z → q z && isTr P z) realizations
            ∷ countR (λ z → q z && isTr L z) realizations
            ∷ countR (λ z → q z && isTr R z) realizations
            ∷ countR (refusedBy q P true 351) realizations
            ∷ countR (refusedBy q L false 135) realizations ∷ []

-- Fact 9: hold admits 144 of the 192 (P 60, L 36, R 48); it refuses the 12
-- of (P, major, 351) and the 12 of (L, minor, 135), and 24 more
hold : summaryOf holdAllR ≡ 144 ∷ 60 ∷ 36 ∷ 48 ∷ 12 ∷ 12 ∷ []
hold = refl

-- Fact 10: reflexive admits 180 of the 192 (P 96, L 36, R 48), refusing only
-- the 12 of (L, minor, 135)
reflexive : summaryOf reflR ≡ 180 ∷ 96 ∷ 36 ∷ 48 ∷ 0 ∷ 12 ∷ []
reflexive = refl

-- Fact 11: E G C to E♭ G C, P from the major triad in type 351, is refused by
-- the joint maximization under strict and hold and admitted by reflexive
major-p351-hold : holdAllAdmitted (st 4 3 8) (st 3 4 9) ≡ false
major-p351-hold = refl

major-p351-reflexive : reflAdmitted (st 4 3 8) (st 3 4 9) ≡ true
major-p351-reflexive = refl

-- Fact 12: from the 26 sources with lower voice 0, reflexive admits 5,547 steps
sources0 : List St
sources0 = map (λ { (pt b c) → st 0 b c }) table8

reflEdges : Nat
reflEdges = length (concatMap (λ s → filterL (reflAdmitted s) states) sources0)

reflexive-edges : reflEdges ≡ 5547
reflexive-edges = refl
