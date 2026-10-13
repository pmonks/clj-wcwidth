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

(def ^:private control-set         (read-edn-from-classpath "ucd-controls.edn"))           ; Set of codepoints (longs)
(def ^:private combining-class-set (read-edn-from-classpath "ucd-combining-classes.edn"))  ; Set of codepoints (longs)
(def ^:private wide-ranges         (read-edn-from-classpath "ucd-wide-ranges.edn"))        ; Sequence of ranges, in order
(def ^:private emoji-sequences-set (read-edn-from-classpath "ucd-emoji-sequences.edn"))    ; Set of sequences of codepoints (longs)

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

(defn- in-ranges?
  "Is the given code-point in any of the given ranges?"
  ^Boolean [ranges code-point]
  (boolean
    (when code-point
      (>= (java.util.Collections/binarySearch ranges [(int code-point)] ranges-comparator) 0))))

(def is-control?
  "Is the given codepoint a control 'character'?"
  (partial contains? control-set))

(def is-combining-class?
  "Is the given codepoint a combining class 'character'?"
  (partial contains? combining-class-set))

(def is-wide?
  "Is the given codepoint wide?"
  (partial in-ranges? wide-ranges))

(def is-zwj-emoji-sequence?
  "Is the given sequence of codepoints a ZWJ emoji?"
  (partial contains? emoji-sequences-set))
