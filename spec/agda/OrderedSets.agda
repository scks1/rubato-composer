{-# OPTIONS --safe --without-K #-}

-- OrderedSets.agda: finite sets kept as lists in ascending order of a
-- three-way comparison, the way Rubato's AutoListMorphismMap keeps the
-- elements of a power denotator in a java.util.TreeMap.

module OrderedSets where

open import Prelude

isLt : Ordering → Bool
isLt lt = true
isLt _  = false

module _ {A : Set} (c : A → A → Ordering) where

  -- Adjacent elements are strictly increasing.
  data Asc : List A → Set where
    []    : Asc []
    [_]   : (x : A) → Asc (x ∷ [])
    _<∷_  : {x y : A} {ys : List A} → c x y ≡ lt → Asc (y ∷ ys) → Asc (x ∷ y ∷ ys)

  -- The boolean test the decision procedure uses.
  ascB : List A → Bool
  ascB []           = true
  ascB (x ∷ [])     = true
  ascB (x ∷ y ∷ ys) = isLt (c x y) && ascB (y ∷ ys)

  -- TreeMap.put: an element that compares equal to a stored one is
  -- dropped and the stored one stays.
  ins : Ordering → A → A → List A → List A
  insert : A → List A → List A
  ins lt x y ys = x ∷ y ∷ ys
  ins eq x y ys = y ∷ ys
  ins gt x y ys = y ∷ insert x ys
  insert x []       = x ∷ []
  insert x (y ∷ ys) = ins (c x y) x y ys

  -- PowerDenotator's constructors append the coordinates in order.
  fromListAcc : List A → List A → List A
  fromListAcc acc []       = acc
  fromListAcc acc (x ∷ xs) = fromListAcc (insert x acc) xs

  fromList : List A → List A
  fromList = fromListAcc []

  -- Flip symmetry: compare(b, a) is the negation of compare(a, b), the
  -- first condition of java.lang.Comparable's contract.
  FlipSym : Set
  FlipSym = (a b : A) → c b a ≡ flipO (c a b)

  TransLt : Set
  TransLt = (a b d : A) → c a b ≡ lt → c b d ≡ lt → c a d ≡ lt

  private
    flip-gt : (s : FlipSym) {a b : A} → c a b ≡ gt → c b a ≡ lt
    flip-gt s {a} {b} p with s a b
    ... | q rewrite p = q

    -- Inserting x behind a head y that is smaller than x keeps the list ascending.
    insert-behind : FlipSym → {x y : A} (ys : List A) →
                    c y x ≡ lt → Asc (y ∷ ys) → Asc (y ∷ insert x ys)
    insert-behind s {x} {y} []       yx _ = yx <∷ [ x ]
    insert-behind s {x} {y} (z ∷ zs) yx (yz <∷ a) with c x z in e
    ... | lt = yx <∷ (e <∷ a)
    ... | eq = yz <∷ a
    ... | gt = yz <∷ insert-behind s zs (flip-gt s e) a

  insert-asc : FlipSym → (x : A) (xs : List A) → Asc xs → Asc (insert x xs)
  insert-asc s x []       _ = [ x ]
  insert-asc s x (y ∷ ys) a with c x y in e
  ... | lt = e <∷ a
  ... | eq = a
  ... | gt = insert-behind s ys (flip-gt s e) a

  fromList-asc : FlipSym → (xs : List A) → Asc (fromList xs)
  fromList-asc s xs = go-asc [] xs []
    where
      go-asc : (acc xs : List A) → Asc acc → Asc (fromListAcc acc xs)
      go-asc acc []       a = a
      go-asc acc (x ∷ xs) a = go-asc (insert x acc) xs (insert-asc s x acc a)

  -- With a transitive order every later element is greater than the
  -- head, so no two elements of an ascending list compare equal.
  head-below-all : TransLt → {x : A} {xs : List A} → Asc (x ∷ xs) → All (λ y → c x y ≡ lt) xs
  head-below-all t [ _ ]                   = []
  head-below-all t {x} (_<∷_ {y = y} xy rest) = xy ∷ lift (head-below-all t rest)
    where
      lift : {ys : List A} → All (λ z → c y z ≡ lt) ys → All (λ z → c x z ≡ lt) ys
      lift []       = []
      lift (p ∷ ps) = t x y _ xy p ∷ lift ps

  lt≢eq : lt ≡ eq → ⊥
  lt≢eq ()

  no-duplicates : TransLt → {x : A} {xs : List A} → Asc (x ∷ xs) →
                  All (λ y → ¬ (c x y ≡ eq)) xs
  no-duplicates t {x} a = go (head-below-all t a)
    where
      lt-not-eq : {o : Ordering} → o ≡ lt → o ≡ eq → ⊥
      lt-not-eq refl q = lt≢eq q
      go : {ys : List A} → All (λ y → c x y ≡ lt) ys → All (λ y → ¬ (c x y ≡ eq)) ys
      go []       = []
      go (p ∷ ps) = lt-not-eq p ∷ go ps

  -- ascB decides Asc.
  ascB-sound-from : (x : A) (ys : List A) → ascB (x ∷ ys) ≡ true → Asc (x ∷ ys)
  ascB-sound-from x []       _ = [ x ]
  ascB-sound-from x (y ∷ ys) p with c x y in e
  ... | lt = e <∷ ascB-sound-from y ys p
  ... | eq = noAsc p
    where noAsc : false ≡ true → Asc (x ∷ y ∷ ys)
          noAsc ()
  ... | gt = noAsc p
    where noAsc : false ≡ true → Asc (x ∷ y ∷ ys)
          noAsc ()

  ascB-sound : (xs : List A) → ascB xs ≡ true → Asc xs
  ascB-sound []       _ = []
  ascB-sound (x ∷ ys) p = ascB-sound-from x ys p

  ascB-complete : {xs : List A} → Asc xs → ascB xs ≡ true
  ascB-complete []             = refl
  ascB-complete [ _ ]          = refl
  ascB-complete (_<∷_ {x} {y} p a) rewrite p = ascB-complete a
