;
; Copyright © 2022 Peter Monks
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;
; SPDX-License-Identifier: Apache-2.0
;

(ns wcwidth.api-test
  (:require [clojure.test   :refer [deftest testing is]]
            [wcwidth.api    :as wcw]))

(def code-point-clown-emoji          0x1F921)   ; 🤡
(def code-point-globe-asia           0x1F30F)   ; 🌏
(def code-point-combining-example    0x1D177)
(def code-point-non-printing-example 0x0094)

(deftest test-code-point-to-string
  (testing "nil"
    (is (nil? (wcw/code-point-to-string nil))))

  (testing "ASCII code points"
    (is (=  " " (wcw/code-point-to-string 0x0020)))
    (is (=  "#" (wcw/code-point-to-string 0x0023)))
    (is (=  "6" (wcw/code-point-to-string 0x0036)))
    (is (=  "A" (wcw/code-point-to-string 0x0041))))

  (testing "Unicode code points"
    (is (= "🤡" (wcw/code-point-to-string code-point-clown-emoji)))))

(deftest test-code-points-to-string
  (testing "nil and empty"
    (is (nil? (wcw/code-points-to-string nil)))
    (is (= "" (wcw/code-points-to-string []))))

  (testing "ASCII code point"
    (is (=  " " (wcw/code-points-to-string [0x0020])))
    (is (=  "#" (wcw/code-points-to-string [0x0023])))
    (is (=  "6" (wcw/code-points-to-string [0x0036])))
    (is (=  "A" (wcw/code-points-to-string [0x0041]))))

  (testing "Unicode code point"
    (is (= "🤡" (wcw/code-points-to-string [code-point-clown-emoji]))))

  (testing "Sequence of code points"
    (is (= "Hello, 🌏!" (wcw/code-points-to-string [\H \e \l \l \o \, \space code-point-globe-asia \!])))))

(deftest test-string-to-code-points
  (testing "nil and empty"
    (is (nil? (wcw/string-to-code-points nil)))
    (is (= [] (wcw/string-to-code-points ""))))

  (testing "ASCII code point"
    (is (= [0x0020] (wcw/string-to-code-points " ")))
    (is (= [0x0023] (wcw/string-to-code-points "#" )))
    (is (= [0x0036] (wcw/string-to-code-points "6")))
    (is (= [0x0041] (wcw/string-to-code-points "A"))))

  (testing "Unicode code point"
    (is (= [code-point-clown-emoji] (wcw/string-to-code-points "🤡"))))

  (testing "Sequence of code points"
    (is (= [(int \H) (int \e) (int \l) (int \l) (int \o) (int \,) (int \space) code-point-globe-asia (int \!)]
           (wcw/string-to-code-points "Hello, 🌏!")))))

(deftest test-roundtripping
  (testing "Roundtripping of string-to-code-points and code-points-to-string"
    (doall
      (for [test [nil "" " " "\t" "\n" "Hello, world!" "Hello, 🌏!" "पीटर मोंक्सो" "彼得·蒙克斯"]]
        (is (= test (wcw/code-points-to-string (wcw/string-to-code-points test))))))))

(deftest test-wcwidth
  (testing "nil"
    (is (nil? (wcw/wcwidth nil))))

  (testing "ASCII codes"
    (is (zero?  (wcw/wcwidth 0x0000)))    ; NUL
    (is (= -1   (wcw/wcwidth 0x007F)))    ; DEL
    (is (=  1   (wcw/wcwidth 0x0020)))    ; space
    (is (=  1   (wcw/wcwidth 0x0023)))    ; #
    (is (=  1   (wcw/wcwidth 0x0036)))    ; 6
    (is (=  1   (wcw/wcwidth 0x0041)))    ; A
    (is (= '(1) (distinct (map wcw/wcwidth (range 0x21 0x7E))))))   ; All of the printable ASCII characters

  (testing "ASCII from Clojure character literals"
    (is (= 1 (wcw/wcwidth \space)))
    (is (= 1 (wcw/wcwidth \#)))
    (is (= 1 (wcw/wcwidth \6)))
    (is (= 1 (wcw/wcwidth \A))))

  (testing "Unicode - non-printing"
    (is (= -1 (wcw/wcwidth 0x0008)))    ; BS
    (is (= -1 (wcw/wcwidth 0x001B)))    ; ESC
    (is (= -1 (wcw/wcwidth 0x008A)))
    (is (= -1 (wcw/wcwidth 0x0099))))

  ; These tests primarily exercise the binary search of the combining characters structure
  (testing "Unicode - zero width"
    (is (zero? (wcw/wcwidth 0x0F35)))   ; Very close to middle of combining characters
    (is (zero? (wcw/wcwidth 0x0F37)))   ; Very close to middle of combining characters
    (is (zero? (wcw/wcwidth 0x0311)))   ; Lowest block of combining characters
    (is (zero? (wcw/wcwidth 0xE0100)))  ; Highest block of combining characters
    (is (zero? (wcw/wcwidth 0x0B01)))   ; Random entries in combining characters from here on
    (is (zero? (wcw/wcwidth 0x1DC4)))
    (is (zero? (wcw/wcwidth 0x0AE2)))
    (is (zero? (wcw/wcwidth 0x0AE3)))
    (is (zero? (wcw/wcwidth 0xA825)))
    (is (zero? (wcw/wcwidth 0xA826))))

  (testing "Unicode - single width"
    (is (= 1 (wcw/wcwidth \©)))
    (is (= 1 (wcw/wcwidth \█)))
    (is (= 1 (wcw/wcwidth 0x10400))))   ; 𐐀

  (testing "Unicode - double width")
    (is (= 2 (wcw/wcwidth code-point-clown-emoji))))

(deftest test-wcswidth
  (testing "nil and empty"
    (is (nil?  (wcw/wcswidth nil)))
    (is (zero? (wcw/wcswidth ""))))

  (testing "ASCII-only strings"
    (is (=  3 (wcw/wcswidth "foo")))
    (is (= 12 (wcw/wcswidth "hello, world"))))

  (testing "Unicode - all single width"
    (is (= 28 (wcw/wcswidth "Copyright © Peter Monks 2022"))))

  (testing "Unicode - mixed widths"
    (is (= 10 (wcw/wcswidth "पीटर मोंक्सो")))
    (is (= 11 (wcw/wcswidth "彼得·蒙克斯")))
    (is (=  9 (wcw/wcswidth (str "hello, " (wcw/code-point-to-string code-point-clown-emoji)))))
    (is (= -1 (wcw/wcswidth (str "hello, world" (wcw/code-point-to-string code-point-non-printing-example)))))))

(deftest test-display-width
  (testing "nil and empty"
    (is (nil?  (wcw/display-width nil)))
    (is (zero? (wcw/display-width ""))))

  (testing "ASCII-only strings"
    (is (=  3 (wcw/display-width "foo")))
    (is (= 12 (wcw/display-width "hello, world"))))

  (testing "Unicode - all single width"
    (is (= 28 (wcw/display-width "Copyright © Peter Monks 2022"))))

  (testing "Unicode - mixed widths"
    (is (= 10 (wcw/display-width "पीटर मोंक्सो")))
    (is (= 11 (wcw/display-width "彼得·蒙克斯")))
    (is (=  9 (wcw/display-width (str "hello, " (wcw/code-point-to-string code-point-clown-emoji)))))
    (is (= 12 (wcw/display-width (str "hello, world" (wcw/code-point-to-string code-point-non-printing-example)))))))
