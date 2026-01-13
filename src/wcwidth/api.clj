;
; Copyright © 2022 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns wcwidth.api
  "The public API of [`clj-wcwidth`](https://github.com/pmonks/clj-wcwidth)."
  (:require [clojure.string :as s]))

(defn code-point->string
  "Returns the `String` representation of any Unicode `code-point`<sup>†</sup>,
  or `nil` when `code-point` is `nil`.

  One of the ways this is useful is because Clojure/Java `String` literals only
  support escape sequences (i.e. `\"\\uXXXX\"`) for code points in the basic
  plane; code points in the supplementary planes must be manually converted into
  their [UTF-16 surrogate pair](https://en.wikipedia.org/wiki/UTF-16#Code_points_from_U+010000_to_U+10FFFF),
  and then each UTF-16 code unit in the pair escaped separately (tedious and
  error prone).

  <sup>†</sup>a `char` or `int`, but `int` is usually the better choice, because
  of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  [code-point]
  (when code-point
    (s/join (java.lang.Character/toChars (int code-point)))))
;    (java.lang.Character/toString code-point)))  ; Java 11+

(defn ^:deprecated code-point-to-string
  "Deprecated. Use [[code-point->string]] instead."
  [code-point]
  (code-point->string code-point))

(defn code-points->string
  "Returns a `String` made up of all of the given Unicode
  `code-points`<sup>†</sup>, or `nil` when `code-points` is `nil`.

  <sup>†</sup>a sequence of `char`s or `int`s, but `int`s are usually the better
  choice, because of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  [code-points]
  (when code-points
    (s/join (map code-point->string code-points))))

(defn ^:deprecated code-points-to-string
  "Deprecated. Use [[code-points->string]] instead."
  [code-points]
  (code-points->string code-points))

(defn string->code-points
  "Returns all of the Unicode code points in `s` (a `String`), as a sequence of
  `int`s, or `nil` when `s` is `nil`."
  [^String s]
  (when s
    (sequence (.toArray (.codePoints s)))))

(defn ^:deprecated string-to-code-points
  "Deprecated. Use [[string->code-points]] instead."
  [s]
  (string->code-points s))

; Dynamically choose an implementation for `grapheme-clusters`
(try
  (Class/forName "com.ibm.icu.text.BreakIterator")
  (load "break_iterator_icu4j")
  (catch ClassNotFoundException _
    (load "break_iterator_jdk")))

; Copied from https://github.com/jline/jline3/blob/master/terminal/src/main/java/org/jline/utils/WCWidth.java#L104 2025-08-20
; That code is BSD-3-Clause
(def ^:private combining-char-ranges
  [[0x0300 0x036F]   [0x0483 0x0486]   [0x0488 0x0489]
   [0x0591 0x05BD]   [0x05BF 0x05BF]   [0x05C1 0x05C2]
   [0x05C4 0x05C5]   [0x05C7 0x05C7]   [0x0600 0x0603]
   [0x0610 0x0615]   [0x064B 0x065E]   [0x0670 0x0670]
   [0x06D6 0x06E4]   [0x06E7 0x06E8]   [0x06EA 0x06ED]
   [0x070F 0x070F]   [0x0711 0x0711]   [0x0730 0x074A]
   [0x07A6 0x07B0]   [0x07EB 0x07F3]   [0x0901 0x0902]
   [0x093C 0x093C]   [0x0941 0x0948]   [0x094D 0x094D]
   [0x0951 0x0954]   [0x0962 0x0963]   [0x0981 0x0981]
   [0x09BC 0x09BC]   [0x09C1 0x09C4]   [0x09CD 0x09CD]
   [0x09E2 0x09E3]   [0x0A01 0x0A02]   [0x0A3C 0x0A3C]
   [0x0A41 0x0A42]   [0x0A47 0x0A48]   [0x0A4B 0x0A4D]
   [0x0A70 0x0A71]   [0x0A81 0x0A82]   [0x0ABC 0x0ABC]
   [0x0AC1 0x0AC5]   [0x0AC7 0x0AC8]   [0x0ACD 0x0ACD]
   [0x0AE2 0x0AE3]   [0x0B01 0x0B01]   [0x0B3C 0x0B3C]
   [0x0B3F 0x0B3F]   [0x0B41 0x0B43]   [0x0B4D 0x0B4D]
   [0x0B56 0x0B56]   [0x0B82 0x0B82]   [0x0BC0 0x0BC0]
   [0x0BCD 0x0BCD]   [0x0C3E 0x0C40]   [0x0C46 0x0C48]
   [0x0C4A 0x0C4D]   [0x0C55 0x0C56]   [0x0CBC 0x0CBC]
   [0x0CBF 0x0CBF]   [0x0CC6 0x0CC6]   [0x0CCC 0x0CCD]
   [0x0CE2 0x0CE3]   [0x0D41 0x0D43]   [0x0D4D 0x0D4D]
   [0x0DCA 0x0DCA]   [0x0DD2 0x0DD4]   [0x0DD6 0x0DD6]
   [0x0E31 0x0E31]   [0x0E34 0x0E3A]   [0x0E47 0x0E4E]
   [0x0EB1 0x0EB1]   [0x0EB4 0x0EB9]   [0x0EBB 0x0EBC]
   [0x0EC8 0x0ECD]   [0x0F18 0x0F19]   [0x0F35 0x0F35]
   [0x0F37 0x0F37]   [0x0F39 0x0F39]   [0x0F71 0x0F7E]
   [0x0F80 0x0F84]   [0x0F86 0x0F87]   [0x0F90 0x0F97]
   [0x0F99 0x0FBC]   [0x0FC6 0x0FC6]   [0x102D 0x1030]
   [0x1032 0x1032]   [0x1036 0x1037]   [0x1039 0x1039]
   [0x1058 0x1059]   [0x1160 0x11FF]   [0x135F 0x135F]
   [0x1712 0x1714]   [0x1732 0x1734]   [0x1752 0x1753]
   [0x1772 0x1773]   [0x17B4 0x17B5]   [0x17B7 0x17BD]
   [0x17C6 0x17C6]   [0x17C9 0x17D3]   [0x17DD 0x17DD]
   [0x180B 0x180D]   [0x18A9 0x18A9]   [0x1920 0x1922]
   [0x1927 0x1928]   [0x1932 0x1932]   [0x1939 0x193B]
   [0x1A17 0x1A18]   [0x1B00 0x1B03]   [0x1B34 0x1B34]
   [0x1B36 0x1B3A]   [0x1B3C 0x1B3C]   [0x1B42 0x1B42]
   [0x1B6B 0x1B73]   [0x1DC0 0x1DCA]   [0x1DFE 0x1DFF]
   [0x200B 0x200F]   [0x202A 0x202E]   [0x2060 0x2063]
   [0x206A 0x206F]   [0x20D0 0x20EF]   [0x302A 0x302F]
   [0x3099 0x309A]   [0xA806 0xA806]   [0xA80B 0xA80B]
   [0xA825 0xA826]   [0xFB1E 0xFB1E]   [0xFE00 0xFE0F]
   [0xFE20 0xFE23]   [0xFEFF 0xFEFF]   [0xFFF9 0xFFFB]
   [0x10A01 0x10A03] [0x10A05 0x10A06] [0x10A0C 0x10A0F]
   [0x10A38 0x10A3A] [0x10A3F 0x10A3F] [0x1D167 0x1D169]
   [0x1D173 0x1D182] [0x1D185 0x1D18B] [0x1D1AA 0x1D1AD]
   [0x1D242 0x1D244] [0x1F3FB 0x1F3FF] [0xE0001 0xE0001]
   [0xE0020 0xE007F] [0xE0100 0xE01EF]])

(defn- char-range-comparator
  "A comparator for comparing a code point (within a singleton vector) against a
  single range from combining-char-ranges."
  [a b]
  ; We have to do these shenanigans as java.util.Collections/binarySearch assumes we're comparing identical types
  ; (which we're not, in this case, at least conceptually), and hence will hand them to us in any order.
  (let [multiplier (if (= 1 (count a)) 1         -1)
        code-point (if (= 1 (count a)) (first a) (first b))
        range      (if (= 2 (count a)) a         b)
        lower      (first  range)
        upper      (second range)]
    (cond
      (< code-point lower)  (* -1 multiplier)
      (> code-point upper)  (*  1 multiplier)
      :else                 0)))

(defn null?
  "Is `code-point`<sup>†</sup> a [null character](https://en.wikipedia.org/wiki/Null_character)?

  <sup>†</sup>a `char` or `int`, but `int` is usually the better choice, because
  of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  [code-point]
  (boolean
    (when code-point
      (= 0x0000 (int code-point)))))

(defn non-printing?
  "Is `code-point`<sup>†</sup> a [non-printing character](https://en.wikipedia.org/wiki/Unicode_control_characters)?

  <sup>†</sup>a `char` or `int`, but `int` is usually the better choice, because
  of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  [code-point]
  (boolean
    (when code-point
      (let [cp (int code-point)]
        (or (< cp 0x0020)
            (and (>= cp 0x007F)
                 (<  cp 0x00A0)))))))

(defn combining?
  "Is `code-point`<sup>†</sup> a [combining character](https://en.wikipedia.org/wiki/Combining_character)?

  <sup>†</sup>a `char` or `int`, but `int` is usually the better choice, because
  of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  [code-point]
  (boolean
    (when code-point
      (>= (java.util.Collections/binarySearch combining-char-ranges [(int code-point)] char-range-comparator) 0))))

(defn wide?
  "Is `code-point`<sup>†</sup> in the [East Asian Wide (W), East Asian Full-width
  (F), or other wide character (e.g. emoji) category](https://en.wikipedia.org/wiki/Wide_character)?

  <sup>†</sup>a `char` or `int`, but `int` is usually the better choice, because
  of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  [code-point]
  (boolean
    (when code-point
      (let [cp (int code-point)]
        (and (>= cp 0x1100)
             (or (<= cp 0x115F)                               ; Hangul Jamo init. consonants
                 (=  cp 0x2329)                               ; Left pointing angle bracket
                 (=  cp 0x232A)                               ; Right pointing angle bracket
                 (and (not= cp 0x303F)                        ; CJK ... Yi
                      (>= cp 0x2E80)  (<= cp 0xA4CF))
                 (and (>= cp 0xAC00)  (<= cp 0xD7A3))         ; Hangul Syllables
                 (and (>= cp 0xF900)  (<= cp 0xFAFF))         ; CJK Compatibility Ideographs
                 (and (>= cp 0xFE10)  (<= cp 0xFE19))         ; Vertical forms
                 (and (>= cp 0xFE30)  (<= cp 0xFE6F))         ; CJK Compatibility Forms
                 (and (>= cp 0xFF00)  (<= cp 0xFF60))         ; Fullwidth Forms
                 (and (>= cp 0xFFE0)  (<= cp 0xFFE6))         ; Fullwidth Forms
                 (and (>= cp 0x2B1B)  (<= cp 0x2B1C))         ; Black and white large squares
                 (= cp 0x2B50)                                ; White medium star
                 (= cp 0x2B55)                                ; Heavy large circle
                 (and (>= cp 0x2600)  (<= cp 0x27BF))         ; Miscellaneous symbols
                 (and (>= cp 0x1F000) (<= cp 0x1FEEE)         ; Emoji
                      (not (combining? cp)))
                 (and (>= cp 0x20000) (<= cp 0x2FFFD))        ; CJK Unified Ideographs Extension B
                 (and (>= cp 0x30000) (<= cp 0x3FFFD))))))))  ; CJK Symbols and Punctuation

(defn wcwidth
  "Returns the number of columns needed to represent the `code-point`
  <sup>†</sup>, based on these rules:

  * Printable: `0`, `1`, or `2`
  * Null character, or `nil`: `0`
  * Non-printing: `-1`

  <sup>†</sup>a `char` or `int`, but `int` is usually the better choice, because
  of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  [code-point]
  (if code-point
    (let [cp (int code-point)]
      (cond
        (null?         cp)  0
        (non-printing? cp) -1
        (combining?    cp)  0
        (wide?         cp)  2
        :else               1))
    0))

(defn- grapheme-cluster-widths
  "Returns a sequence of the [[wcwidth]]s of the grapheme clusters in `s` (a
  `String`)."
  [^String s]
  #_{:clj-kondo/ignore [:unresolved-symbol]}
  (when-let [gcs (grapheme-clusters s)]
    (map #(min 2 (reduce + (map wcwidth (string->code-points %)))) gcs)))

(defn wcswidth
  "Returns the number of columns needed to represent `s` (a `String`). If a
  non-printing code point occurs in `s`, `-1` is returned (as defined in POSIX).

  Returns `0` when `s` is `nil`."
  [^String s]
  (if-let [gcws (grapheme-cluster-widths s)]
    (if (some #{-1} gcws)
      -1
      (reduce + gcws))
    0))

(def re-ansi
  "A regular expression for matching ANSI escape sequences in a larger text.
  Adapted from from [ECMA-48](https://www.ecma-international.org/publications-and-standards/standards/ecma-48/)."
  #"(?:\x1b\x5b|\x9b)[\x30-\x3f]*[\x20-\x2f]*[\x40-\x7e]")

(defn remove-ansi
  "Strips all ANSI escape sequences from `s` (a `String`).  Returns `nil` if `s`
  is `nil`."
  [^String s]
  (when s
    (s/replace s re-ansi "")))

(defn display-width
  "Returns the number of columns needed to display `s` (a `String`), but
  deviates from POSIX [[wcswidth]] behaviour in these ways:

  * non-printing characters are considered zero width (instead of causing the
    entire result to be `-1`)
  * ANSI escape sequences are (by default, but configurable) also considered
    zero width

  For most use cases, this function is more useful than [[wcswidth]], despite
  not adhering to POSIX.

  Returns `0` when `s` is `nil`."
  ([^String s] (display-width s nil))
  ([^String s & {:keys [ignore-ansi?] :or {ignore-ansi? false}}]
   (if-let [s (if ignore-ansi? s (remove-ansi s))]
     (reduce + (remove #(<= % 0) (grapheme-cluster-widths s)))
     0)))
