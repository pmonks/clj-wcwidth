# clj-wcwidth

[![CI](https://github.com/pmonks/clj-wcwidth/actions/workflows/ci.yml/badge.svg?branch=dev)](https://github.com/pmonks/clj-wcwidth/actions?query=workflow%3ACI+branch%3Adev)
[![Dependencies](https://github.com/pmonks/clj-wcwidth/actions/workflows/dependencies.yml/badge.svg?branch=dev)](https://github.com/pmonks/clj-wcwidth/actions?query=workflow%3Adependencies+branch%3Adev)
<br/>
[![Latest Version](https://img.shields.io/clojars/v/com.github.pmonks/clj-wcwidth)](https://clojars.org/com.github.pmonks/clj-wcwidth/)
[![Open Issues](https://img.shields.io/github/issues/pmonks/clj-wcwidth.svg)](https://github.com/pmonks/clj-wcwidth/issues)
[![License](https://img.shields.io/github/license/pmonks/clj-wcwidth.svg)](https://github.com/pmonks/clj-wcwidth/blob/release/LICENSE)
![Maintained](https://badges.ws/badge/?label=maintained&value=yes,+at+author's+discretion)

Pure Clojure implementations of the [`wcwidth`](https://man7.org/linux/man-pages/man3/wcwidth.3.html) and [`wcswidth`](https://man7.org/linux/man-pages/man3/wcswidth.3.html) POSIX functions (plus some other useful Unicode functions).

## Why?

When Unicode grapheme clusters ("characters") are sent to a fixed-width device (e.g. a terminal or monospaced editor), many have a well-defined "notional width", expressed in units of columns (where a typical ASCII character takes up 1 column).  This is partially standardised in [Unicode Technical Report #11](https://www.unicode.org/reports/tr11/), which is implemented as the POSIX functions `wcwidth` and `wcswidth`.

The JVM doesn't provide these functions however, so applications that need to know these widths (e.g. for terminal screen formatting purposes) are left to their own devices.  While there are Java libraries that have implemented this themselves (notably [ICU4J](https://unicode-org.github.io/icu/userguide/icu4j/) and [JLine](https://github.com/jline/jline3/blob/master/terminal/src/main/java/org/jline/utils/WCWidth.java)), pulling in a large dependency when one only uses a very small part of it is sometimes overkill.

This library provides a small, zero-dependency-by-default, pure Clojure implementation of this functionality and goes further by (optionally) also taking [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) into account (as these are also zero width on an ANSI-capable device).

## Why not [`count`](https://clojuredocs.org/clojure.core/count)?

When supplied with a sequence of textual data (i.e. a `String` or `char[]`), `count` simply counts the number of Java `char`s in that sequence, which is not the same thing as a Unicode grapheme cluster (since a Unicode grapheme cluster may be made up of multiple Unicode code points).  What's worse is that due to a [historical oddity of the JVM](https://www.oracle.com/technical-resources/articles/javase/supplementary.html), a Java `char` isn't even necessarily the same thing as a Unicode code point.  Specifically, Java `char`s are a 16 bit "code unit" from UTF-16, and Unicode code points in the supplementary planes are represented by 2 such code units (and therefore as 2 `char`s on the JVM).

Furthermore, `count` doesn't account for combining, non-printing, or zero-width Unicode code points; it counts them as `char`s regardless of whether they get displayed on Unicode-capable devices or not.  Similarly it has no awareness of the non-printing nature of ANSI escape codes on ANSI-capable devices.

## How does it work?

Technically, UTR11 defines display widths for every Unicode code point, which is _not_ necessarily the same thing as a grapheme cluster (a "character").  So the way this library (and [others like it](https://docs.rs/unicode-display-width/latest/unicode_display_width/)) function is to break strings up into their grapheme clusters, and then determine the width of each cluster based on the display width rules of the code point(s) that comprise that cluster, then add the cluster widths together to arrive at the string's overall display width.

In many cases this is a simple 1:1 correspondence - the [Latin character "a"](https://www.compart.com/en/unicode/U+0061), for example, is a single grapheme cluster (`a`) defined by a single code point (`U+0061`), and takes up a single display column.  At the other end of complexity, the [transgender flag emoji](https://emojipedia.org/transgender-flag#technical) is a single grapheme cluster (`🏳️‍⚧️`), defined by 5 code points (`U+1F3F3 U+FE0F U+200D U+26A7 U+FE0F`), and takes up 2 display columns.  It also (due to the historical Java issue mentioned above) takes up 6 (!) JVM `char`s, further complicating the situation for Clojure developers.

## A note about JVM Unicode suppport

This library fundamentally depends on being able to break strings into Unicode grapheme clusters, which the JVM supports via the [`java.text.BreakIterator` class](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/BreakIterator.html).  Unfortunately the implementation of this class tends to lag behind the latest Unicode specification, especially in JVM versions prior to 20 (see [JDK-8291660](https://bugs.openjdk.org/browse/JDK-8291660) for some specifics).

For that reason, this library will check at runtime whether the ICU4J library is on the classpath, and if so use [its implementation of the `BreakIterator` class](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/BreakIterator.html) instead of the JDK's.  This gives downstream users of the library the ability to choose whether to consume this library in a lightweight, zero-dependency "best effort" form, or whether to introduce the (large) ICU4J library and thereby ensure correct behaviour across a wider range of esoteric Unicode inputs.

Note that the unit tests are run using the ICU4J library only, since they are run on a CI matrix of JVM versions, and include some tests that are known to fail on JVM versions prior to v24.

## Installation

`clj-wcwidth` is available as a Maven artifact from [Clojars](https://clojars.org/com.github.pmonks/clj-wcwidth).

### API Documentation

[API documentation is available here](https://pmonks.github.io/clj-wcwidth/).  [The unit tests](https://github.com/pmonks/clj-wcwidth/blob/release/test/wcwidth/api_test.clj) provide comprehensive usage examples.

### Trying it Out

#### Clojure CLI

```shell
$ clj -Sdeps '{:deps {com.github.pmonks/clj-wcwidth {:mvn/version "RELEASE"}}}'
```

#### Leiningen

```shell
$ lein try com.github.pmonks/clj-wcwidth
```

#### deps-try

```shell
$ deps-try com.github.pmonks/clj-wcwidth
```

### Demo

```clojure
(require '[clojure.string :as s])
(require '[wcwidth.api :as wcw])

;; POSIX-compliant wcwidth / wcswidth

(def ascii-esc \u001B)

(wcw/wcwidth \A)
; ==> 1
(wcw/wcwidth \©)
; ==> 1
(wcw/wcwidth 0x0000)     ; ASCII NUL (zero width)
; ==> 0
(wcw/wcwidth ascii-esc)  ; ASCII ESC (non printing)
; ==> -1
(wcw/wcwidth 0x1F921)    ; 🤡 (double width)
; ==> 2

(wcw/wcswidth "hello, world")
; ==> 12
(wcw/wcswidth "hello, 🌏")
; ==> 9

;; wcswidth (POSIX) vs display-width (non-POSIX, but more practical)

(wcw/wcswidth (str "hello, " ascii-esc))
; ==> -1
(wcw/display-width (str "hello, " ascii-esc))
; ==> 7

;; ANSI escape code support

(def ansi-hide-cursor (str ascii-esc "[25l"))
(wcw/display-width (str "hello, " ansi-hide-cursor))
; ==> 7

;; Examples showing how clojure.core/count doesn't work for this use case

(def jerome (wcw/code-points-to-string [\J \e 0x0341 \r \o 0x0302 \m \e]))  ; Jérôme, using combining diacritics
(wcw/display-width jerome)
; ==> 6
(count jerome)
; ==> 8

(def deseret-capital-long-i (wcw/code-point-to-string 0x10400))  ; 𐐀
(wcw/display-width deseret-capital-long-i)
; ==> 1
(count deseret-capital-long-i)
; ==> 2

(def zalgo-text "Ẓ̌á̲l͔̝̞̄̑͌g̖̘̘̔̔͢͞͝o̪̔T̢̙̫̈̍͞e̬͈͕͌̏͑x̺̍ṭ̓̓ͅ")
(wcw/display-width zalgo-text)
; ==> 9
(count zalgo-text)
; ==> 44                    ; lol 🤡

(def lots-of-escapes (s/join (repeat 1000 ascii-esc)))
(wcw/display-width lots-of-escapes)
; ==> 0
(count lots-of-escapes)
; ==> 1000                  ; lol 🤡

(def transgender-flag (wcw/code-points-to-string [0x1F3F3 0xFE0F 0x200D 0x26A7 0xFE0F]))  ; 🏳️‍⚧️
(wcw/display-width transgender-flag)
; ==> 2
(count transgender-flag)
; ==> 6                     ; lol 🤡
```

## Contributor Information

[Contributing Guidelines](https://github.com/pmonks/clj-wcwidth/blob/release/.github/CONTRIBUTING.md)

[Bug Tracker](https://github.com/pmonks/clj-wcwidth/issues)

[Code of Conduct](https://github.com/pmonks/clj-wcwidth/blob/release/.github/CODE_OF_CONDUCT.md)

### Developer Workflow

This project uses the [git-flow branching strategy](https://nvie.com/posts/a-successful-git-branching-model/), with the caveat that the permanent branches are called `release` and `dev`.  Any changes to the `release` branch are considered a release and auto-deployed (JARs to Clojars, API docs to GitHub Pages, etc.).

For this reason, **all development must occur either in branch `dev`, or (preferably) in temporary branches off of `dev`.**  All PRs from forked repos must also be submitted against `dev`; the `release` branch is **only** updated from `dev` via PRs created by the core development team.  All other changes submitted to `release` will be rejected.

### Build Tasks

`wcwidth` uses [`tools.build`](https://clojure.org/guides/tools_build). You can get a list of available tasks by running:

```
clojure -A:deps -T:build help/doc
```

Of particular interest are:

* `clojure -T:build test` - run the unit tests
* `clojure -T:build lint` - run the linters (clj-kondo and eastwood)
* `clojure -T:build ci` - run the full CI suite (check for outdated dependencies, run the unit tests, run the linters)
* `clojure -T:build install` - build the JAR and install it locally (e.g. so you can test it with downstream code)

Please note that the `deploy` task is restricted to the core development team (and will not function if you run it yourself).

## License

Copyright © 2022 Peter Monks

Distributed under the [Mozilla Public License, version 2.0](https://www.mozilla.org/en-US/MPL/2.0/).

SPDX-License-Identifier: [`MPL-2.0`](https://spdx.org/licenses/MPL-2.0)
