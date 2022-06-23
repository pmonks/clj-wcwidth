;
; Copyright © 2022 Peter Monks
;
; Licensed under the Apache License Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;
; SPDX-License-Identifier: Apache-2.0
;

(ns wcwidth.api
  (:require [clojure.string :as s]))

(defn code-point-to-string
  "Returns the string representation of any Unicode code point†.

This is useful because Clojure/Java string literals only support UTF-16 escape
sequences for code points, which involves manual conversion of all supplementary
code points into pairs of escapes.

†a character or integer, but note that Java/Clojure characters are limited to
the Unicode basic plane (first 0xFFFF code points) for historical reasons"
  [code-point]
  (when code-point
    (s/join (java.lang.Character/toChars (int code-point)))))
;    (java.lang.Character/toString code-point)))  ; Java 11+

(defn code-points-to-string
  "Returns a string made up of all of the given code points†

†a sequence of characters or integers, but note that Java/Clojure characters are
limited to the Unicode basic plane (first 0xFFFF code points) for historical
reasons"
  [code-points]
  (when code-points
    (s/join (map #(code-point-to-string (int %)) code-points))))

(defn string-to-code-points
  "Returns all of the Unicode code points in s, as a sequence of integers."
  [^String s]
  (when s
    (let [a (.toArray (.codePoints s))]
      (when a (vec a)))))   ; Note: seq nil-puns empty sequences, and vec "empty-sequence-puns" nil, so we don't have a core fn to do exactly what we want!

; Copied directly from https://github.com/jline/jline3/blob/master/terminal/src/main/java/org/jline/utils/WCWidth.java#L80 as of 2021-11-14
; That code is BSD-3-Clause.
(def ^:private combining-char-ranges
  [[0x0300  0x036F]  [0x0483  0x0486]  [0x0488  0x0489]  [0x0591  0x05BD]  [0x05BF  0x05BF]  [0x05C1  0x05C2]
   [0x05C4  0x05C5]  [0x05C7  0x05C7]  [0x0600  0x0603]  [0x0610  0x0615]  [0x064B  0x065E]  [0x0670  0x0670]
   [0x06D6  0x06E4]  [0x06E7  0x06E8]  [0x06EA  0x06ED]  [0x070F  0x070F]  [0x0711  0x0711]  [0x0730  0x074A]
   [0x07A6  0x07B0]  [0x07EB  0x07F3]  [0x0901  0x0902]  [0x093C  0x093C]  [0x0941  0x0948]  [0x094D  0x094D]
   [0x0951  0x0954]  [0x0962  0x0963]  [0x0981  0x0981]  [0x09BC  0x09BC]  [0x09C1  0x09C4]  [0x09CD  0x09CD]
   [0x09E2  0x09E3]  [0x0A01  0x0A02]  [0x0A3C  0x0A3C]  [0x0A41  0x0A42]  [0x0A47  0x0A48]  [0x0A4B  0x0A4D]
   [0x0A70  0x0A71]  [0x0A81  0x0A82]  [0x0ABC  0x0ABC]  [0x0AC1  0x0AC5]  [0x0AC7  0x0AC8]  [0x0ACD  0x0ACD]
   [0x0AE2  0x0AE3]  [0x0B01  0x0B01]  [0x0B3C  0x0B3C]  [0x0B3F  0x0B3F]  [0x0B41  0x0B43]  [0x0B4D  0x0B4D]
   [0x0B56  0x0B56]  [0x0B82  0x0B82]  [0x0BC0  0x0BC0]  [0x0BCD  0x0BCD]  [0x0C3E  0x0C40]  [0x0C46  0x0C48]
   [0x0C4A  0x0C4D]  [0x0C55  0x0C56]  [0x0CBC  0x0CBC]  [0x0CBF  0x0CBF]  [0x0CC6  0x0CC6]  [0x0CCC  0x0CCD]
   [0x0CE2  0x0CE3]  [0x0D41  0x0D43]  [0x0D4D  0x0D4D]  [0x0DCA  0x0DCA]  [0x0DD2  0x0DD4]  [0x0DD6  0x0DD6]
   [0x0E31  0x0E31]  [0x0E34  0x0E3A]  [0x0E47  0x0E4E]  [0x0EB1  0x0EB1]  [0x0EB4  0x0EB9]  [0x0EBB  0x0EBC]
   [0x0EC8  0x0ECD]  [0x0F18  0x0F19]  [0x0F35  0x0F35]  [0x0F37  0x0F37]  [0x0F39  0x0F39]  [0x0F71  0x0F7E]
   [0x0F80  0x0F84]  [0x0F86  0x0F87]  [0x0F90  0x0F97]  [0x0F99  0x0FBC]  [0x0FC6  0x0FC6]  [0x102D  0x1030]
   [0x1032  0x1032]  [0x1036  0x1037]  [0x1039  0x1039]  [0x1058  0x1059]  [0x1160  0x11FF]  [0x135F  0x135F]
   [0x1712  0x1714]  [0x1732  0x1734]  [0x1752  0x1753]  [0x1772  0x1773]  [0x17B4  0x17B5]  [0x17B7  0x17BD]
   [0x17C6  0x17C6]  [0x17C9  0x17D3]  [0x17DD  0x17DD]  [0x180B  0x180D]  [0x18A9  0x18A9]  [0x1920  0x1922]
   [0x1927  0x1928]  [0x1932  0x1932]  [0x1939  0x193B]  [0x1A17  0x1A18]  [0x1B00  0x1B03]  [0x1B34  0x1B34]
   [0x1B36  0x1B3A]  [0x1B3C  0x1B3C]  [0x1B42  0x1B42]  [0x1B6B  0x1B73]  [0x1DC0  0x1DCA]  [0x1DFE  0x1DFF]
   [0x200B  0x200F]  [0x202A  0x202E]  [0x2060  0x2063]  [0x206A  0x206F]  [0x20D0  0x20EF]  [0x302A  0x302F]
   [0x3099  0x309A]  [0xA806  0xA806]  [0xA80B  0xA80B]  [0xA825  0xA826]  [0xFB1E  0xFB1E]  [0xFE00  0xFE0F]
   [0xFE20  0xFE23]  [0xFEFF  0xFEFF]  [0xFFF9  0xFFFB]  [0x10A01 0x10A03] [0x10A05 0x10A06] [0x10A0C 0x10A0F]
   [0x10A38 0x10A3A] [0x10A3F 0x10A3F] [0x1D167 0x1D169] [0x1D173 0x1D182] [0x1D185 0x1D18B] [0x1D1AA 0x1D1AD]
   [0x1D242 0x1D244] [0x1F3FB 0x1F3FF] [0xE0001 0xE0001] [0xE0020 0xE007F] [0xE0100 0xE01EF]])

(defn- compare-combining-char
  "A comparator for comparing a code-point (within a singleton vector) against a
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
  "Is code-point† a null character?

†a character or integer, but note that Java/Clojure characters are limited to
the Unicode basic plane (first 0xFFFF code points) for historical reasons"
  [code-point]
  (when code-point
    (= 0x0000 (int code-point))))

(defn non-printing?
  "Is code-point† a non-printing character?

†a character or integer, but note that Java/Clojure characters are limited to
the Unicode basic plane (first 0xFFFF code points) for historical reasons"
  [code-point]
  (when code-point
    (let [cp (int code-point)]
      (or (< cp 0x0020)
          (and (>= cp 0x007F)
               (<  cp 0x00A0))))))

(defn combining?
  "Is code-point† a combining character?

†a character or integer, but note that Java/Clojure characters are limited to
the Unicode basic plane (first 0xFFFF code points) for historical reasons"
  [code-point]
  (when code-point
    (>= (java.util.Collections/binarySearch combining-char-ranges [(int code-point)] compare-combining-char) 0)))

(defn wide?
  "Is code-point† in the East Asian Wide (W) or East Asian Full-width (F) category?

†a character or integer, but note that Java/Clojure characters are limited to
the Unicode basic plane (first 0xFFFF code points) for historical reasons"
  [code-point]
  (when code-point
    (let [cp (int code-point)]
      (and (>= cp 0x1100)
           (or (<= cp 0x115F)                              ; Hangul Jamo init. consonants
               (=  cp 0x2329)
               (=  cp 0x232A)
               (and (not= cp 0x303F)                       ; CJK ... Yi
                    (>= cp 0x2E80)  (<= cp 0xA4CF))
               (and (>= cp 0xAC00)  (<= cp 0xD7A3))        ; Hangul Syllables
               (and (>= cp 0xF900)  (<= cp 0xFAFF))        ; CJK Compatibility Ideographs
               (and (>= cp 0xFE10)  (<= cp 0xFE19))        ; Vertical forms
               (and (>= cp 0xFE30)  (<= cp 0xFE6F))        ; CJK Compatibility Forms
               (and (>= cp 0xFF00)  (<= cp 0xFF60))        ; Fullwidth Forms
               (and (>= cp 0xFFE0)  (<= cp 0xFFE6))
               (and (>= cp 0x1F000) (<= cp 0x1FEEE))       ; Emoji
               (and (>= cp 0x20000) (<= cp 0x2FFFD))       ; CJK Unified Ideographs Extension B
               (and (>= cp 0x30000) (<= cp 0x3FFFD)))))))  ; CJK Symbols and Punctuation

(defn wcwidth
  "Returns the number of columns needed to represent the code-point†. If
code-point is a printable character, the value is at least 0. If code-point is
a null character, the value is 0. Otherwise, -1 is returned (the code-point is
non-printing).

†a character or integer, but note that Java/Clojure characters are limited to
the Unicode basic plane (first 0xFFFF code points) for historical reasons"
  [code-point]
  (when code-point
    (let [cp (int code-point)]
      (cond
        (null? cp)          0
        (non-printing? cp) -1
        (combining? cp)     0
        (wide? cp)          2
        :else               1))))

(defn- widths
  "Returns a sequence of all of the widths of the Characters in String s."
  [s]
  (map wcwidth (string-to-code-points s)))

(defn wcswidth
  "Returns the number of columns needed to represent String s. If a non-printing
character occurs among these characters, -1 is returned."
  [s]
  (when s
    (let [ws (widths s)]
      (if (some #{-1} ws)
        -1
        (reduce + ws)))))

(defn display-width
  "Returns the number of columns needed to display String s, ignoring non-printing
characters. For Clojure, this is generally more useful than wcswidth."
  [s]
  (when s
    (reduce + (remove #(<= % 0) (widths s)))))
