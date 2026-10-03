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
  (:require [clojure.string :as s]
            [wcwidth.ucd    :as ucd]))

(defmacro ^:private safe-int
  "nil-safe version of clojure.core/int"
  [x]
  `(when ~x
     (int ~x)))

(defn code-point->string
  "Returns the `String` representation of any Unicode `code-point`<sup>†</sup>,
  or `nil` when `code-point` is `nil`.

  One of the ways this is useful is because Clojure/Java `String` literals only
  support escape sequences (i.e. `\"\\uXXXX\"`) for code points in the basic
  plane; code points in the supplementary planes must be manually converted into
  their [UTF-16 surrogate pair](https://en.wikipedia.org/wiki/UTF-16#Code_points_from_U+010000_to_U+10FFFF),
  and then each UTF-16 code unit in the pair escaped separately (which is
  tedious and error prone).

  <sup>†</sup>a `char` or `int`, but `int` is usually the better choice, because
  of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  ^String [code-point]
  (when-let [cp (safe-int code-point)]
    (s/join (java.lang.Character/toChars cp))))
;    (java.lang.Character/toString cp)))  ; Java 11+ only

(defn code-points->string
  "Returns a `String` made up of all of the given Unicode
  `code-points`<sup>†</sup>, or `nil` when `code-points` is `nil`.

  <sup>†</sup>a sequence of `char`s or `int`s, but `int`s are usually the better
  choice, because of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  ^String [code-points]
  (when code-points
    (s/join (map code-point->string code-points))))

(defn string->code-points
  "Returns all of the Unicode code points in `cs` (a `CharSequence`), as a
  sequence of `int`s, or `nil` when `cs` is `nil`."
  [^CharSequence cs]
  (when cs
    (sequence (.toArray (.codePoints cs)))))

(defn print-code-point
  "Prints `code-point` in normative Unicode notation (`U+codepoint`) to stdout,
  returning `nil`.  This is primarily intended to be a convenience at the REPL."
  [code-point]
  (when-let [cp (safe-int code-point)]
    (println (format "U+%04X" cp))))

(defn print-code-points
  "Prints the code points in `cs` (a `CharSequence`) in normative Unicode
  notation (`U+codepoint`) to stdout, returning `nil`.  This is primarily
  intended to be a convenience at the REPL."
  [^CharSequence cs]
  (println (s/join " "
                   (map (partial format "U+%04X")
                        (string->code-points cs)))))

; Dynamically choose an implementation for `grapheme-clusters`
(try
  (Class/forName "com.ibm.icu.text.BreakIterator")
  (load "break_iterator_icu4j")
  (catch ClassNotFoundException _
    (load "break_iterator_jdk")))

(defn null?
  "Is `code-point`<sup>†</sup> a [null character](https://en.wikipedia.org/wiki/Null_character)?

  <sup>†</sup>a `char` or `int`, but `int` is usually the better choice, because
  of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  ^Boolean [code-point]
  (boolean
    (when-let [cp (safe-int code-point)]
      (= 0x0000 cp))))

(defn non-printing?
  "Is `code-point`<sup>†</sup> a [non-printing (control) character](https://en.wikipedia.org/wiki/Unicode_control_characters)?

  <sup>†</sup>a `char` or `int`, but `int` is usually the better choice, because
  of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  ^Boolean [code-point]
  (ucd/in-ranges? ucd/control-ranges code-point))

(defn combining?
  "Is `code-point`<sup>†</sup> a [combining character](https://en.wikipedia.org/wiki/Combining_character)?

  <sup>†</sup>a `char` or `int`, but `int` is usually the better choice, because
  of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  ^Boolean [code-point]
  (ucd/in-ranges? ucd/combining-class-ranges code-point))

(defn wide?
  "Is `code-point`<sup>†</sup> in the [East Asian Wide (W), East Asian Full-width
  (F), or other wide character (e.g. emoji) category](https://en.wikipedia.org/wiki/Wide_character)?

  Note that many emoji are comprised of multiple code points, and this function
  cannot detect those.

  <sup>†</sup>a `char` or `int`, but `int` is usually the better choice, because
  of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  ^Boolean [code-point]
  (ucd/in-ranges? ucd/wide-ranges code-point))

(defn wcwidth
  "Returns the number of columns needed to represent the `code-point`
  <sup>†</sup>, based on these rules:

  * Printable: `0`, `1`, or `2`
  * Null character, or `nil`: `0`
  * Non-printing: `-1`

  <sup>†</sup>a `char` or `int`, but `int` is usually the better choice, because
  of [historical limitations with Java's `char` type](https://www.oracle.com/technical-resources/articles/javase/supplementary.html)"
  ^long [code-point]
  (if-let [cp (safe-int code-point)]
    (cond
      (null?         cp)  0
      (non-printing? cp) -1
      (wide?         cp)  2
      (combining?    cp)  0
      :else               1)
    0))

(defn- grapheme-cluster-width
  "Returns the width of the single grapheme cluster identified by `gc` (a
  `CharSequence`)."
  [^CharSequence gc]
  (when-let [cps (string->code-points gc)]
    (if (ucd/is-zwj-emoji-sequence? cps)
      2
      (min 2 (reduce + (map wcwidth cps))))))

(defn- grapheme-cluster-widths
  "Returns a sequence of the widths of the grapheme clusters in `cs` (a
  `CharSequence`)."
  [^CharSequence cs]
  (when-let [gcs (grapheme-clusters cs)]
    (map grapheme-cluster-width gcs)))

(defn wcswidth
  "Returns the number of columns needed to represent `cs` (a `CharSequence`). If
  a non-printing code point occurs in `cs`, `-1` is returned (as defined in
  POSIX).

  Returns `0` when `cs` is `nil`."
  ^long [^CharSequence cs]
  (if-let [gcws (grapheme-cluster-widths cs)]
    (if (some #{-1} gcws)
      -1
      (reduce + gcws))
    0))

(def ^:no-doc re-ansi
  "A regular expression for matching ANSI escape sequences in a larger text.
  Adapted from [ECMA-48](https://www.ecma-international.org/publications-and-standards/standards/ecma-48/)."
  #"(?:\x1b\x5b|\x9b)[\x30-\x3f]*[\x20-\x2f]*[\x40-\x7e]")

(defn ^:no-doc remove-ansi
  "Strips all ANSI escape sequences from `cs` (a `CharSequence`).  Returns `nil`
  if `cs` is `nil`."
  ^String [^CharSequence cs]
  (when cs
    (s/replace cs re-ansi "")))

(defn display-width
  "Returns the number of columns needed to display `cs` (a `CharSequence`), but
  deviates from POSIX [[wcswidth]] behaviour in these ways:

  * non-printing characters are considered zero width (instead of causing the
    entire result to be `-1`)
  * ANSI escape sequences are (by default, but configurable) also considered
    zero width

  For most use cases, this function is more useful than [[wcswidth]], despite
  not adhering to POSIX.

  Returns `0` when `cs` is `nil`."
  (^Long [^CharSequence cs] (display-width cs nil))
  (^Long [^CharSequence cs & {:keys [ignore-ansi?] :or {ignore-ansi? false}}]
   (if-let [s (if ignore-ansi? cs (remove-ansi cs))]
     (reduce + (remove neg? (grapheme-cluster-widths s)))
     0)))


;;
;; Deprecated functions to be removed in the next major release
;;

(defn ^:deprecated ^:no-doc code-point-to-string
  "Deprecated. Use [[code-point->string]] instead."
  [code-point]
  (code-point->string code-point))

(defn ^:deprecated ^:no-doc code-points-to-string
  "Deprecated. Use [[code-points->string]] instead."
  [code-points]
  (code-points->string code-points))

(defn ^:deprecated ^:no-doc string-to-code-points
  "Deprecated. Use [[string->code-points]] instead."
  [s]
  (string->code-points s))
