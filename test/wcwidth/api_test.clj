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
            [clojure.string :as s]
            [wcwidth.api    :refer [code-point->string code-points->string string->code-points
                                    wcwidth wcswidth display-width remove-ansi grapheme-clusters-impl]]))

(println "\n☔️ Running tests on Clojure" (clojure-version) "/ JVM" (System/getProperty "java.version") (str "(" (System/getProperty "java.vm.name") " v" (System/getProperty "java.vm.version") ")"))
(println "☔️ Unicode grapheme cluster detection using" (s/upper-case (name grapheme-clusters-impl)))

(def code-point-clown-emoji          0x1F921)   ; 🤡
(def code-point-globe-asia           0x1F30F)   ; 🌏
(def code-point-combining-example    0x1D177)
(def code-point-non-printing-example 0x0094)
(def code-point-medium-white-circle  0x26AA)    ; ⚪️ - this one is tricky as UTR#11 doesn't define a width for it - it's in the "Miscellaenous symbols" category, rather than the emoji category

(def invalid-grapheme-with-non-printing (code-points->string [0x1F3F3 0xFE0F 0x200D 0x26A7 0x001B]))  ; Messed up partial trans flag with ASCII ESC on the end

(deftest test-code-point->string
  (testing "nil"
    (is (nil? (code-point->string nil))))

  (testing "ASCII code points"
    (is (=  " " (code-point->string 0x0020)))
    (is (=  "#" (code-point->string 0x0023)))
    (is (=  "6" (code-point->string 0x0036)))
    (is (=  "A" (code-point->string 0x0041))))

  (testing "Unicode code points"
    (is (= "🤡" (code-point->string code-point-clown-emoji)))))

(deftest test-code-points->string
  (testing "nil and empty"
    (is (nil? (code-points->string nil)))
    (is (= "" (code-points->string []))))

  (testing "ASCII code point"
    (is (=  " " (code-points->string [0x0020])))
    (is (=  "#" (code-points->string [0x0023])))
    (is (=  "6" (code-points->string [0x0036])))
    (is (=  "A" (code-points->string [0x0041]))))

  (testing "Unicode code point"
    (is (= "🤡" (code-points->string [code-point-clown-emoji]))))

  (testing "Sequence of code points"
    (is (= "Hello, 🌏!" (code-points->string [\H \e \l \l \o \, \space code-point-globe-asia \!])))))

(deftest test-string->code-points
  (testing "nil and empty"
    (is (nil? (string->code-points nil)))
    (is (= [] (string->code-points ""))))

  (testing "Other CharSequence inputs"
    (is (= []       (string->code-points (StringBuilder.))))
    (is (= [0x0041] (string->code-points (StringBuilder. "A")))))

  (testing "ASCII code point"
    (is (= [0x0020] (string->code-points " ")))
    (is (= [0x0023] (string->code-points "#" )))
    (is (= [0x0036] (string->code-points "6")))
    (is (= [0x0041] (string->code-points "A"))))

  (testing "Unicode code point"
    (is (= [code-point-clown-emoji] (string->code-points "🤡"))))

  (testing "Sequence of code points"
    (is (= [(int \H) (int \e) (int \l) (int \l) (int \o) (int \,) (int \space) code-point-globe-asia (int \!)]
           (string->code-points "Hello, 🌏!")))))

(deftest test-roundtripping
  (testing "Roundtripping of string->code-points and code-points->string"
    (doall
      (for [test [nil "" " " "\t" "\n" "Hello, world!" "Hello, 🌏!" "पीटर मोंक्सो" "彼得·蒙克斯"]]
        (is (= test (code-points->string (string->code-points test))))))))

(deftest test-wcwidth
  (testing "nil"
    (is (zero? (wcwidth nil))))

  (testing "ASCII codes"
    (is (=  0   (wcwidth 0x0000)))    ; NUL
    (is (= -1   (wcwidth 0x007F)))    ; DEL
    (is (=  1   (wcwidth 0x0020)))    ; space
    (is (=  1   (wcwidth 0x0023)))    ; #
    (is (=  1   (wcwidth 0x0036)))    ; 6
    (is (=  1   (wcwidth 0x0041)))    ; A
    (is (= '(1) (distinct (map wcwidth (range 0x21 0x7E))))))   ; All of the printable ASCII characters

  (testing "ASCII from Clojure character literals"
    (is (= 1 (wcwidth \space)))
    (is (= 1 (wcwidth \#)))
    (is (= 1 (wcwidth \6)))
    (is (= 1 (wcwidth \A))))

  (testing "Unicode - non-printing"
    (is (= -1 (wcwidth 0x0008)))    ; BS
    (is (= -1 (wcwidth 0x001B)))    ; ESC
    (is (= -1 (wcwidth 0x008A)))
    (is (= -1 (wcwidth 0x0099))))

  ; These tests primarily exercise the binary search of the combining characters structure
  (testing "Unicode - zero width"
    (is (zero? (wcwidth 0x0F35)))   ; Very close to middle of combining characters
    (is (zero? (wcwidth 0x0F37)))   ; Very close to middle of combining characters
    (is (zero? (wcwidth 0x0311)))   ; Lowest block of combining characters
    (is (zero? (wcwidth 0xE0100)))  ; Highest block of combining characters
    (is (zero? (wcwidth 0x0B01)))   ; Random entries in combining characters from here on
    (is (zero? (wcwidth 0x1DC4)))
    (is (zero? (wcwidth 0x0AE2)))
    (is (zero? (wcwidth 0x0AE3)))
    (is (zero? (wcwidth 0xA825)))
    (is (zero? (wcwidth 0xA826))))

  (testing "Unicode - single width"
    (is (= 1 (wcwidth \©)))
    (is (= 1 (wcwidth \█)))
    (is (= 1 (wcwidth 0x10400))))   ; 𐐀

  (testing "Unicode - double width"
    (is (= 2 (wcwidth code-point-clown-emoji)))
    (is (= 2 (wcwidth 0x26AA)))     ; ⚪️
    (is (= 2 (wcwidth 0x26AB)))     ; ⚫️
    (is (= 2 (wcwidth 0x1F7E2)))    ; 🟢
    (is (= 2 (wcwidth 0x2B1B)))     ; ⬛️
    (is (= 2 (wcwidth 0x2B1C)))     ; ⬜️
    (is (= 2 (wcwidth 0x1F7E7)))    ; 🟧
    (is (= 2 (wcwidth 0x2B50)))     ; ⭐️
    (is (= 2 (wcwidth 0x1F6E1)))))  ; 🛡

(deftest test-wcswidth
  (testing "nil and empty"
    (is (zero? (wcswidth nil)))
    (is (zero? (wcswidth ""))))

  (testing "Other CharSequence inputs"
    (is (zero? (wcswidth (StringBuilder.))))
    (is (= 3   (wcswidth (StringBuilder. "foo")))))

  (testing "ASCII-only strings"
    (is (=  3 (wcswidth "foo")))
    (is (= 12 (wcswidth "hello, world"))))

  (testing "Unicode - all single width"
    (is (= 28 (wcswidth "Copyright © Peter Monks 2022")))
    (is (=  1 (wcswidth (code-point->string  0x00E9))))        ; é
    (is (=  1 (wcswidth (code-points->string [\e 0x0341])))))  ; Also é, but using combining code points

  (testing "Unicode - graphemes with multiple code points"
    (is (= 4 (wcswidth (code-points->string [0x1F44D 0x1F44D 0x1F3FB]))))               ; 👍👍🏻 - note skin tone is controlled via a zero-width combining code points
    (is (= 2 (wcswidth (code-points->string [0x1F3F3 0xFE0F 0x200D 0x26A7 0xFE0F])))))  ; 🏳️‍⚧️ - note trans flag is 5 combined code points stored in 6 JVM chars

  (testing "Unicode - mixed widths"
    (is (=  9 (wcswidth "Ẓ̌á̲l͔̝̞̄̑͌g̖̘̘̔̔͢͞͝o̪̔T̢̙̫̈̍͞e̬͈͕͌̏͑x̺̍ṭ̓̓ͅ")))
    (is (=  9 (wcswidth "पीटर मोंक्सो")))  ; Note: Indic scripts (like this one) don't have formally defined display widths
    (is (= 11 (wcswidth "彼得·蒙克斯")))
    (is (= 15 (wcswidth "🔥🗡🍩👩🏻‍🚀⏰💃🏼🔦👍🏻")))
    (is (=  9 (wcswidth (str "hello, " (code-point->string code-point-clown-emoji)))))
    (is (= -1 (wcswidth invalid-grapheme-with-non-printing)))
    (is (= -1 (wcswidth (str "hello, world" (code-point->string code-point-non-printing-example)))))
    (is (= -1 (wcswidth (str (code-points->string [27 91 57 50 109]) "Ẓ̌á̲l͔̝̞̄̑͌g̖̘̘̔̔͢͞͝o̪̔T̢̙̫̈̍͞e̬͈͕͌̏͑x̺̍ṭ̓̓ͅ"))))))  ; ANSI fg colour bright green, ZalgoText

(deftest test-remove-ansi
  (testing "nil, empty, blank"
    (is (nil?            (remove-ansi nil)))
    (is (= ""            (remove-ansi "")))
    (is (= "  \t \n \r " (remove-ansi "  \t \n \r "))))

  (testing "no ANSI sequences"
    (is (= "hello, world"                (remove-ansi "hello, world")))
    (is (= "hello, world"                (remove-ansi (StringBuilder. "hello, world"))))
    (is (= (code-points->string [27])    (remove-ansi (code-points->string [27]))))      ; "Naked" ESC
    (is (= (code-points->string [27 59]) (remove-ansi (code-points->string [27 59])))))  ; ESC

  (testing "ANSI sequence"
    (is (= "0123456789"   (remove-ansi (code-points->string [27 91 57 50 109 48 49 50 51 52 53 54 55 56 57 27 91 109])))  ; ANSI fg colour bright green, ASCI digits 0-9, ANSI fg colour reset
    (is (= "Hello World!" (remove-ansi (code-points->string [27 91 51 49 109 72 101 108 27 91 51 49 109 27 91 52 55 109 108 111 27 91 109 27 91 109 32 27 91 49 109 27 91 51 51 109 87 111 114 27 91 109 27 91 49 109 108 100 33 27 91 109])))))))  ; "Hello World!" with various inline formatting (FG & BG colours, attributes)

(deftest test-display-width
  (testing "nil and empty"
    (is (zero? (display-width nil)))
    (is (zero? (display-width ""))))

  (testing "Other CharSequence inputs"
    (is (zero? (display-width (StringBuilder.))))
    (is (= 3   (display-width (StringBuilder. "foo")))))

  (testing "ASCII-only strings"
    (is (=  3 (display-width "foo")))
    (is (= 12 (display-width "hello, world"))))

  (testing "Unicode - all single width"
    (is (= 28 (display-width "Copyright © Peter Monks 2022"))))

  (testing "Unicode - graphemes with multiple code points"
    (is (= 4 (display-width (code-points->string [0x1F44D 0x1F44D 0x1F3FB]))))               ; 👍👍🏻 - note skin tone is controlled via a zero-width combining character
    (is (= 2 (display-width (code-points->string [0x1F3F3 0xFE0F 0x200D 0x26A7 0xFE0F])))))  ; 🏳️‍⚧️ - note trans flag is 5 combined code points stored in 6 JVM chars

  (testing "Unicode - mixed widths"
    (is (=  9 (display-width "Ẓ̌á̲l͔̝̞̄̑͌g̖̘̘̔̔͢͞͝o̪̔T̢̙̫̈̍͞e̬͈͕͌̏͑x̺̍ṭ̓̓ͅ")))
    (is (=  9 (display-width "पीटर मोंक्सो")))  ; Note: Indic scripts (like this one) don't have formally defined display widths
    (is (= 11 (display-width "彼得·蒙克斯")))
    (is (= 15 (display-width "🔥🗡🍩👩🏻‍🚀⏰💃🏼🔦👍🏻")))
    (is (=  2 (display-width invalid-grapheme-with-non-printing)))
    (is (= 12 (display-width (str "hello, world" (code-point->string code-point-non-printing-example))))))

  (testing "ANSI escape sequences"
    (let [string-with-ansi (code-points->string [27 91 57 50 109 48 49 50 51 52 53 54 55 56 57 27 91 109])]  ; ANSI fg colour bright green, ASCI digits 0-9, ANSI fg colour reset
      (is (= 10 (display-width string-with-ansi)))
      (is (= 16 (display-width string-with-ansi {:ignore-ansi? true}))))
    (let [string-with-ansi (code-points->string [27 91 51 49 109 72 101 108 27 91 51 49 109 27 91 52 55 109 108 111 27 91 109 27 91 109 32 27 91 49 109 27 91 51 51 109 87 111 114 27 91 109 27 91 49 109 108 100 33 27 91 109])]  ; "Hello World!" with various inline formatting (FG & BG colours, attributes)
      (is (= 12 (display-width string-with-ansi)))
      (is (= 42 (display-width string-with-ansi {:ignore-ansi? true}))))
    (is (= 9 (display-width (str (code-points->string [27 91 57 50 109]) "Ẓ̌á̲l͔̝̞̄̑͌g̖̘̘̔̔͢͞͝o̪̔T̢̙̫̈̍͞e̬͈͕͌̏͑x̺̍ṭ̓̓ͅ"))))))  ; ANSI fg colour bright green, ZalgoText
