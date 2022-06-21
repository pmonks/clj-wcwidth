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
            [clojure.string :as s]
            [wcwidth.api    :as wcw]))

(def code-point-clown-emoji          0x1F921)   ; 🤡
(def code-point-combining-example    0x1D177)
(def code-point-non-printing-example 0x0094)

(deftest test-codepoint-to-string
  (testing "ASCII codepoints"
    (is (=  " " (wcw/codepoint-to-string 0x0020)))    ; space
    (is (=  "#" (wcw/codepoint-to-string 0x0023)))    ; #
    (is (=  "6" (wcw/codepoint-to-string 0x0036)))    ; 6
    (is (=  "A" (wcw/codepoint-to-string 0x0041))))   ; A

  (testing "Unicode codepoints"
    (is (= "🤡" (wcw/codepoint-to-string code-point-clown-emoji)))))

(deftest test-wcwidth
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
    (is (= 1 (wcw/wcwidth \█))))

  (testing "Unicode - double width")
    (is (= 2 (wcw/wcwidth code-point-clown-emoji))))

(deftest test-wcswidth
  (testing "ASCII-only strings"
    (is (=  3 (wcw/wcswidth "foo")))
    (is (= 12 (wcw/wcswidth "hello, world")))
    (is (= 28 (wcw/wcswidth "Copyright © Peter Monks 2022")))
    (is (= 10 (wcw/wcswidth "पीटर मोंक्सो")))
    (is (= 11 (wcw/wcswidth "彼得·蒙克斯")))
    (is (=  9 (wcw/wcswidth (str "hello, " (wcw/codepoint-to-string code-point-clown-emoji)))))
    (is (= -1 (wcw/wcswidth (str "hello, world" (wcw/codepoint-to-string code-point-non-printing-example)))))))

(deftest test-wcswidth2
  (testing "ASCII-only strings"
    (is (=  3 (wcw/wcswidth2 "foo")))
    (is (= 12 (wcw/wcswidth2 "hello, world")))
    (is (= 28 (wcw/wcswidth2 "Copyright © Peter Monks 2022")))
    (is (= 10 (wcw/wcswidth2 "पीटर मोंक्सो")))
    (is (= 11 (wcw/wcswidth2 "彼得·蒙克斯")))
    (is (=  9 (wcw/wcswidth2 (str "hello, " (wcw/codepoint-to-string code-point-clown-emoji)))))
    (is (= 12 (wcw/wcswidth2 (str "hello, world" (wcw/codepoint-to-string code-point-non-printing-example)))))))
