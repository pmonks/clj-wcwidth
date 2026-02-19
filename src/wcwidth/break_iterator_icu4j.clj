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


(def ^:no-doc grapheme-clusters-impl
  "Which implementation is in use for finding grapheme clusters?  A keyword
  with one of these values:

  * `:icu4j`
  * `:jdk`"
  :icu4j)


(defn grapheme-clusters
  "Returns the [Unicode grapheme clusters](https://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries)
  (what we tend to think of as \"characters\") in `cs` (a `CharSequence`) as a
  sequence of `String`s, or `nil` when `cs` is `nil`.

  Notes:

  * Will use [ICU4J's `BreakIterator`](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/BreakIterator.html)
    class when available on the classpath, falling back on the [JDK's lower
    quality `BreakIterator`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/BreakIterator.html)
    class otherwise"
  [^CharSequence cs]
  (when cs
    (let [bi (doto (com.ibm.icu.text.BreakIterator/getCharacterInstance)
                   (.setText cs))]   ; ICU4J's BreakIterator _does_ support CharSequences directly (unlike the JDK's)
      (loop [start  0
             end    (.next bi)
             result []]
        (if (= end com.ibm.icu.text.BreakIterator/DONE)
          result
          (recur end (.next bi) (conj result (.toString (.subSequence cs start end)))))))))  ; We can't use clojure.core/subs here, since it doesn't support CharSequences - see https://ask.clojure.org/index.php/14889/clojure-core-subs-should-use-type-hints-charsequence-string
