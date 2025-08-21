;
; Copyright © 2022 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(in-ns 'wcwidth.api)

#_{:clj-kondo/ignore [:redefined-var]}
(def grapheme-clusters-impl
  "Which implementation is in use for finding grapheme clusters?  A keyword
  with one of these values:

  * `:icu4j`
  * `:jdk`"
  :jdk)

#_{:clj-kondo/ignore [:redefined-var]}
(defn grapheme-clusters
  "Returns the Unicode grapheme clusters in `s`, as a sequence of `String`s.
  Returns `nil` when `s` is `nil`.

  Notes:

  * Will use [ICU4J's `BreakIterator`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/BreakIterator.html)
    class when available, falling back on the [JDK's `BreakIterator`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/BreakIterator.html)
    class otherwise"
  [^String s]
  (when s
    (let [bi (doto (java.text.BreakIterator/getCharacterInstance)
                   (.setText s))]
      (loop [start  0
             end    (.next bi)
             result []]
        (if (= end java.text.BreakIterator/DONE)
          result
          (recur end (.next bi) (conj result (subs s start end))))))))
