;
; Copyright © 2026 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns wcwidth.ucd
  "Unicode Character Database (UCD) support namespace.  This is not part of the
  public API of clj-wcwidth and may change in any way, at any time, without
  notice."
  (:require [clojure.java.io :as io]
            [clojure.edn     :as edn]))

(defn- read-edn-from-classpath
  "Reads and parse an EDN file from the classpath, throwing on error."
  [^CharSequence res]
  (edn/read (java.io.PushbackReader. (io/reader (io/resource res)))))

(def combining-class-ranges (read-edn-from-classpath "ucd-combining-class-ranges.edn"))  ; Sequence of ranges, in order
(def control-ranges         (read-edn-from-classpath "ucd-control-ranges.edn"))          ; Sequence of ranges, in order
(def wide-ranges            (read-edn-from-classpath "ucd-wide-ranges.edn"))             ; Sequence of ranges, in order
(def emoji-sequences-set    (read-edn-from-classpath "ucd-emoji-sequences.edn"))         ; Set

(defn- range-comparator
  "Comparator for a value vs a single range."
  ^long [val rng]
  (let [lower (first rng)
        upper (last  rng)]
    (cond
      (<= lower val upper) 0
      (< val lower)        -1
      (> val upper)        1)))

(defn- ranges-comparator
  "A comparator for comparing a codepoint (within a singleton vector) against a
  range structure (e.g. loaded from an EDN file)."
  ^long [a b]
  (case [(count a) (count b)]
    [1 1] (compare (first a) (first b))
    [1 2] (range-comparator (first a) b)
    [2 1] (* -1 (range-comparator (first b) a))))

(defn in-ranges?
  "Is the given code-point in any of the given ranges?"
  ^Boolean [ranges code-point]
  (boolean
    (when code-point
      (>= (java.util.Collections/binarySearch ranges [(int code-point)] ranges-comparator) 0))))

(def is-zwj-emoji-sequence?
  "Is the given sequence of codepoints a ZWJ emoji?"
  (partial contains? emoji-sequences-set))
