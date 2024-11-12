;
; Copyright © 2022 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns wcwidth.api-test
  (:require [clojure.test   :refer [deftest testing is]]
            [wcwidth.api    :as wcw]))

(def code-point-clown-emoji          0x1F921)   ; 🤡
(def code-point-globe-asia           0x1F30F)   ; 🌏
(def code-point-combining-example    0x1D177)
(def code-point-non-printing-example 0x0094)
(def code-point-medium-white-circle  0x26AA)    ; ⚪️ - this one is tricky as UTR#11 doesn't define a width for it - it's in the "Miscellaenous symbols" category, rather than the emoji category

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

  (testing "Unicode - double width"
    (is (= 2 (wcw/wcwidth code-point-clown-emoji)))
    ; Note: these aren't aligned with UTR#11, but work better in practice
    (is (= 2 (wcw/wcwidth 0x26AA)))    ; ⚪️
    (is (= 2 (wcw/wcwidth 0x26AB)))    ; ⚫️
    (is (= 2 (wcw/wcwidth 0x1F7E2)))   ; 🟢
    (is (= 2 (wcw/wcwidth 0x2B1B)))    ; ⬛️
    (is (= 2 (wcw/wcwidth 0x2B1C)))    ; ⬜️
    (is (= 2 (wcw/wcwidth 0x1F7E7)))   ; 🟧
    (is (= 2 (wcw/wcwidth 0x2B50)))))  ; ⭐️

(deftest test-wcswidth
  (testing "nil and empty"
    (is (nil?  (wcw/wcswidth nil)))
    (is (zero? (wcw/wcswidth ""))))

  (testing "ASCII-only strings"
    (is (=  3 (wcw/wcswidth "foo")))
    (is (= 12 (wcw/wcswidth "hello, world"))))

  (testing "Unicode - all single width"
    (is (= 28 (wcw/wcswidth "Copyright © Peter Monks 2022"))))

  (testing "Unicode - all double width, with some non-printing as well"
    (is (= 4 (wcw/wcswidth (wcw/code-points-to-string [0x1F44D 0x1F44D 0x1F3FB])))))  ; 👍👍🏻 - note skin tone is controlled via a zero-width combining character

  (testing "Unicode - mixed widths"
    (is (= 10 (wcw/wcswidth "पीटर मोंक्सो")))
    (is (= 11 (wcw/wcswidth "彼得·蒙克斯")))
    (is (=  9 (wcw/wcswidth (str "hello, " (wcw/code-point-to-string code-point-clown-emoji)))))
    (is (= -1 (wcw/wcswidth (str "hello, world" (wcw/code-point-to-string code-point-non-printing-example)))))))

(deftest test-remove-ansi
  (testing "nil, empty, blank"
    (is (nil?            (wcw/remove-ansi nil)))
    (is (= ""            (wcw/remove-ansi "")))
    (is (= "  \t \n \r " (wcw/remove-ansi "  \t \n \r "))))
  (testing "no ANSI sequences"
    (is (= "hello, world"                      (wcw/remove-ansi "hello, world")))
    (is (= (wcw/code-points-to-string [27])    (wcw/remove-ansi (wcw/code-points-to-string [27]))))     ; "Naked" ESC
    (is (= (wcw/code-points-to-string [27 59]) (wcw/remove-ansi (wcw/code-points-to-string [27 59])))))  ; ESC;
  (testing "ANSI sequence"
    (is (= "0123456789"   (wcw/remove-ansi (wcw/code-points-to-string [27 91 57 50 109 48 49 50 51 52 53 54 55 56 57 27 91 109])))  ; ANSI fg colour bright green, ASCI digits 0-9, ANSI fg colour reset
    (is (= "Hello World!" (wcw/remove-ansi (wcw/code-points-to-string [27 91 51 49 109 72 101 108 27 91 51 49 109 27 91 52 55 109 108 111 27 91 109 27 91 109 32 27 91 49 109 27 91 51 51 109 87 111 114 27 91 109 27 91 49 109 108 100 33 27 91 109])))))))  ; "Hello World!" with various inline formatting (FG & BG colours, attributes)

(deftest test-display-width
  (testing "nil and empty"
    (is (nil?  (wcw/display-width nil)))
    (is (zero? (wcw/display-width ""))))

  (testing "ASCII-only strings"
    (is (=  3 (wcw/display-width "foo")))
    (is (= 12 (wcw/display-width "hello, world"))))

  (testing "Unicode - all single width"
    (is (= 28 (wcw/display-width "Copyright © Peter Monks 2022"))))

  (testing "Unicode - all double width, with some non-printing as well"
    (is (= 4 (wcw/wcswidth (wcw/code-points-to-string [0x1F44D 0x1F44D 0x1F3FB])))))  ; 👍👍🏻 - note skin tone is controlled via a zero-width combining character

  (testing "Unicode - mixed widths"
    (is (= 10 (wcw/display-width "पीटर मोंक्सो")))
    (is (= 11 (wcw/display-width "彼得·蒙克斯")))
    (is (=  9 (wcw/display-width (str "hello, " (wcw/code-point-to-string code-point-clown-emoji)))))
    (is (= 12 (wcw/display-width (str "hello, world" (wcw/code-point-to-string code-point-non-printing-example))))))

  (testing "ANSI escape sequences"
    (let [string-with-ansi (wcw/code-points-to-string [27 91 57 50 109 48 49 50 51 52 53 54 55 56 57 27 91 109])]  ; ANSI fg colour bright green, ASCI digits 0-9, ANSI fg colour reset
      (is (= 10 (wcw/display-width string-with-ansi)))
      (is (= 16 (wcw/display-width string-with-ansi {:ignore-ansi? true}))))
    (let [string-with-ansi (wcw/code-points-to-string [27 91 51 49 109 72 101 108 27 91 51 49 109 27 91 52 55 109 108 111 27 91 109 27 91 109 32 27 91 49 109 27 91 51 51 109 87 111 114 27 91 109 27 91 49 109 108 100 33 27 91 109])]  ; "Hello World!" with various inline formatting (FG & BG colours, attributes)
      (is (= 12 (wcw/display-width string-with-ansi)))
      (is (= 42 (wcw/display-width string-with-ansi {:ignore-ansi? true}))))))
