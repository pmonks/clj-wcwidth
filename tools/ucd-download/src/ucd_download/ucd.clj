;
; Copyright © 2026 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns ucd-download.ucd
  "UCD data download functions."
  (:require [clojure.string  :as s]
            [clojure.java.io :as io]
            [urlocal.api     :as url]))

;;
;; UTILITY FNS
;;

(defn hex-string->long
  "Turns a hex string (as found in UCD files representing codepoints) into a
  Long."
  ^Long [^CharSequence s]
  (when-not (s/blank? s)
    (Integer/parseInt s 16)))

(defn ^:private download-ucd-data
  "Downloads and performs basic parsing of any standard text file from the
  [Unicode Character Database](https://unicode.org/Public/UCD/latest/ucd/),
  returning a sequence of vectors containing each field (as a String) in the
  file.  Does not attempt to process field values (since that's specific to each
  text file)."
  [u]
  (with-open [is (url/input-stream u)
              r  (io/reader is)]
    (some->> r
             line-seq
             (map #(s/replace % #"\s*#.*\Z" ""))  ; Remove comments
             (filter (complement s/blank?))       ; Remove blank lines
             (map #(vec (s/split % #"\s*;\s*")))  ; Split fields within each line
             doall)))                             ; De-lazy before leaving with-open

(defn ^:private download-ranged-ucd-data
  "Retrieves and parses any standard text file from the [Unicode Character Database](https://unicode.org/Public/UCD/latest/ucd/),
  returning a sequence of vectors containing each field (as a String) from the
  file.  Assumes the first field is a codepoint range (as either one or two hex
  strings, separated by '..'), and converts them into a vector of Longs.  Sorts
  the list in codepoint range order."
  [u]
  (some->> (download-ucd-data u)
           (map #(let [rng (vec (map hex-string->long (s/split (first %) #"\.\.")))]  ; Split first field into its range end-members, then convert each of them to a long
                   (cons rng (rest %))))
           (sort-by (comp first first))))

(defn optimise-ranges
  "Optimises a sequence of ranges (tuples of 1 or 2 integers), by identifying
  overlapping or contiguous ranges and collapsing them into a single range
  bracketing both.

  Note: `ranges` must already be sorted (by the first element in the range, if
  a tuple of 2 integers)."
  [ranges]
  (when (seq ranges)
    (loop [[f s & r] ranges
           result    []]
      (cond
        ; Base cases
        (nil? f) result
        (nil? s) (conj result f)

        ; Recursive cases
        (<= (first f) (first s) (inc (last f)))  ; Ranges overlap or are contiguous, so combine them and push the combined range back onto r
          (recur (cons [(first f) (max (last f) (last s))] r) result)

        :else  ; Ranges are _not_ contiguous, so add f to the result
          (recur (cons s r) (conj result f))))))


;;
;; UCD VERSION
;;

; We could use any of the UCD text file URIs for this purpose, but we retrieving this one anyway (and urlocal means we don't download it more than once)
(def ^:private unicode-version-uri "https://unicode.org/Public/UCD/latest/ucd/extracted/DerivedGeneralCategory.txt")

(defn unicode-version
  "Retrieve the Unicode version from the first line of any text file in the UCD."
  []
  (with-open [is (url/input-stream unicode-version-uri)
              r  (io/reader is)]
    (some->> r
             line-seq
             first
             (re-find #"\d+(?:\.\d+)*")
             doall)))


;;
;; UCD GENERAL CATEGORIES
;;

(def ^:private ucd-general-categories-uri "https://unicode.org/Public/UCD/latest/ucd/extracted/DerivedGeneralCategory.txt")

; See https://www.unicode.org/reports/tr44/#General_Category_Values
(def ^:private ucd-general-category-code->keyword {
  ; Letters
  "Lu" :uppercase-letter
  "Ll" :lowercase-letter
  "Lt" :titlecase-letter
  "Lm" :modifier-letter
  "Lo" :other-letter
  ; Marks
  "Mn" :nonspacing-mark
  "Mc" :spacing-mark
  "Me" :enclosing-mark
  ; Numbers
  "Nd" :decimal-number
  "Nl" :letter-number
  "No" :other-number
  ; Punctuation
  "Pc" :connector-punctuation
  "Pd" :dash-punctuation
  "Ps" :open-punctuation
  "Pe" :close-punctuation
  "Pi" :initial-punctuation
  "Pf" :final-punctuation
  "Po" :other-punctuation
  ; Symbols
  "Sm" :math-symbol
  "Sc" :currency-symbol
  "Sk" :modifier-symbol
  "So" :other-symbol
  ; Separators
  "Zs" :space-separator
  "Zl" :line-separator
  "Zp" :paragraph-separator
  ; Other
  "Cc" :control
  "Cf" :format
  "Cs" :surrogate
  "Co" :private-use
  "Cn" :unassigned})

(defn general-categories
  "Retrieves the [general category data from the UCD](https://unicode.org/Public/UCD/latest/ucd/extracted/DerivedGeneralCategory.txt)."
  []
  (some->> (download-ranged-ucd-data ucd-general-categories-uri)
           (map #(let [[rng general-category] %]
                   [rng (ucd-general-category-code->keyword general-category)]))
           (filter #(not (= :unassigned (second %))))  ; Remove explicitly unassigned codepoints (since that's the default for any given codepoint not present in this list anyway)
           vec))


;;
;; UCD EAST ASIAN WIDTHS
;;

(def ^:private ucd-east-asian-widths-uri "https://unicode.org/Public/UCD/latest/ucd/EastAsianWidth.txt")

; See https://www.unicode.org/reports/tr11/#Definitions
(def ^:private east-asian-width-code->keyword {
  "F"  :east-asian-full-width
  "H"  :east-asian-half-width
  "W"  :east-asian-wide
  "Na" :east-asian-narrow
  "A"  :ambiguous
  "N"  :neutral})

(defn east-asian-widths
  "Retrieves the [East Asian width data from the UCD](https://unicode.org/Public/UCD/latest/ucd/EastAsianWidth.txt)."
  []
  (some->> (download-ranged-ucd-data ucd-east-asian-widths-uri)
           (map #(let [[rng combining-class] %]
                   [rng (east-asian-width-code->keyword combining-class)]))
           vec))


;;
;; UCD EMOJI
;;

(def ^:private ucd-emoji-sequences-uri     "https://www.unicode.org/Public/latest/emoji/emoji-sequences.txt")
(def ^:private ucd-emoji-zwj-sequences-uri "https://www.unicode.org/Public/latest/emoji/emoji-zwj-sequences.txt")

(defn- emoji-sequences
  "Retrieves the [emoji data from the UCD](https://www.unicode.org/Public/latest/emoji/emoji-sequences.txt)."
  []
  (download-ucd-data ucd-emoji-sequences-uri))  ; Can't use per-codepoint or ranged here, since the emoji-sequences data is a hodgepodge of ranges, codepoints, and sequences of codepoints

(defn- emoji-zwj-sequences
  "Retrieves the [emoji data from the UCD](https://www.unicode.org/Public/latest/emoji/emoji-sequences.txt)."
  []
  (download-ucd-data ucd-emoji-zwj-sequences-uri))  ; Can't use per-codepoint or ranged here, since the emoji-zwj-sequences data is sequences of codepoints

(defn single-codepoint-emoji
  "Retrieves all [single codepoint emoji from the UCD](https://www.unicode.org/Public/latest/emoji/emoji-sequences.txt)."
  []
  (some->> (emoji-sequences)
           (map #(when-not (re-matches #"\p{XDigit}{4,5}(?:\s+\p{XDigit}{4,5})+" (first %))  ; Ignore sequences
                   (let [rng (vec (map hex-string->long (s/split (first %) #"\.\.")))]
                     (vec (cons rng (rest %))))))
           (filter some?)
           (sort-by (comp first first))
           vec))

(defn codepoint-sequence-emoji
  "Retrieves all codepoint sequence emoji from the UCD.  This includes both
  'simple' multi-codepoint emoji and ZWJ emoji."
  []
  (some->> (concat (map #(when (re-matches #"\p{XDigit}{4,5}(?:\s+\p{XDigit}{4,5})+" (first %))  ; Ignore everything except sequences
                           (let [sq (vec (map hex-string->long (s/split (first %) #"\s+")))]
                             (vec (cons sq (rest %)))))
                        (emoji-sequences))
                   (map #(let [sq (vec (map hex-string->long (s/split (first %) #"\s+")))]
                           (vec (cons sq (rest %))))
                        (emoji-zwj-sequences)))
           (filter some?)
           (sort-by first)
           vec))
