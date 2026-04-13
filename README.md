# clj-wcwidth

[![CI](https://github.com/pmonks/clj-wcwidth/actions/workflows/ci.yml/badge.svg?branch=dev)](https://github.com/pmonks/clj-wcwidth/actions?query=workflow%3ACI+branch%3Adev)
[![Dependencies](https://github.com/pmonks/clj-wcwidth/actions/workflows/dependencies.yml/badge.svg?branch=dev)](https://github.com/pmonks/clj-wcwidth/actions?query=workflow%3Adependencies+branch%3Adev)
<br/>
[![Latest Version](https://img.shields.io/clojars/v/com.github.pmonks/clj-wcwidth)](https://clojars.org/com.github.pmonks/clj-wcwidth/)
[![Open Issues](https://img.shields.io/github/issues/pmonks/clj-wcwidth.svg)](https://github.com/pmonks/clj-wcwidth/issues)
[![License](https://img.shields.io/github/license/pmonks/clj-wcwidth.svg)](https://github.com/pmonks/clj-wcwidth/blob/release/LICENSE)
![Maintained](https://badges.ws/badge/?label=maintained&value=yes,+at+author's+discretion)

Pure Clojure implementations of the `wcwidth` and `wcswidth` POSIX functions, plus some other, more useful non-POSIX functions related to this use case.

## Why?

When Unicode text is sent to a Unicode-capable fixed-width device (e.g. a terminal, monospaced printer, etc.), the "characters" that make up that text each have a well-defined "notional width" of either 0, 1, or 2 columns (where a typical ASCII character takes up 1 column).  This is standardised in [Unicode Technical Report #11](https://www.unicode.org/reports/tr11/), and implemented as the POSIX C functions [`wcwidth`](https://manpages.org/wcwidth) and [`wcswidth`](https://manpages.org/wcswidth).  The JVM doesn't provide these functions however, so applications that need to know these display widths (e.g. for terminal output formatting purposes) are left to their own devices.  While there are Java libraries that have implemented this (notably [JLine](https://github.com/jline/jline3/blob/master/terminal/src/main/java/org/jline/utils/WCWidth.java)), pulling in a large dependency when one only uses a very small part of it is sometimes overkill.

`clj-wcwidth` provides a small, zero-dependency-by-default, pure Clojure implementation of this functionality (and more).

### More, you say?

This library addresses various inconveniences in both POSIX and JLine:

* The POSIX `wcswidth` function returns `-1` if a string contains any non-printing characters.  In practice this means that Unicode text needs to be pre-processed before being passed to this function.
* JLine only provides an equivalent of `wcwidth` (the POSIX function that returns the display width of a single code point), but what we think of as a "character" is actually a ["Unicode grapheme cluster"](https://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries) and critically, [many grapheme clusters (especially emoji) are made up of _multiple_ code points](https://emojipedia.org/emoji-zwj-sequence).
* Neither POSIX nor JLine take [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) into account, yet these sequences are zero width on an ANSI-capable device.

## How does it work?

UTR11 defines display widths for most Unicode code points, but a code point is _not_ necessarily the same thing as a grapheme cluster (a "character").  The way this library (and [others like it](https://docs.rs/unicode-display-width/latest/unicode_display_width/)) function is to break strings up into their grapheme clusters, determine the width of each cluster based on the display width rules of the code point(s) that comprise that cluster (i.e. using the rules in UTR11), then sum up the cluster widths to arrive at the string's overall display width.

At the grapheme cluster level this manifests in several ways, including:

* A 1:1 correspondence e.g. the grapheme cluster `a` is defined by a single code point ([`U+0061`](https://www.compart.com/en/unicode/U+0061)), and takes up 1 display column.
* A 1:2 correspondence e.g. the grapheme cluster `☕️` is defined by a single code point ([`U+2615`](https://www.compart.com/en/unicode/U+2615)), and takes up 2 display columns.
* An N:1 correspondence e.g. the grapheme cluster `é` is defined by 2 code points ([`U+0065`](https://www.compart.com/en/unicode/U+0065) and [`U+0341`](https://www.compart.com/en/unicode/U+0341)), but only takes up 1 display column.
* An N:2 correspondence e.g. the grapheme cluster `🏳️‍⚧️` is defined by 5 code points ([`U+1F3F3`](https://www.compart.com/en/unicode/U+1F3F3), [`U+FE0F`](https://www.compart.com/en/unicode/U+FE0F), [`U+200D`](https://www.compart.com/en/unicode/U+200D), [`U+26A7`](https://www.compart.com/en/unicode/U+26A7), and [`U+FE0F`](https://www.compart.com/en/unicode/U+FE0F)), and takes up 2 display columns.

> [!CAUTION]  
> There is a common misconception that the JVM's `char` and `Character` types represent a Unicode code point, but that is _not_ the case.  Instead, due to an [epicly shortsighted decision by Sun in the early 2000s](https://www.oracle.com/technical-resources/articles/javase/supplementary.html), they represent a [UTF-16 "code unit"](https://en.wikipedia.org/wiki/UTF-16#Description), a footgun that spawns bugs throughout JVM / Clojure code when surrogate pairs aren't properly handled during processing of sequences of `char`s (including strings).  This is why, for example, calling [`count`](https://clojuredocs.org/clojure.core/count) on the single code point string `"🌏"` returns 2, instead of the expected 1 - the (single) code point (`U+1F30F`) cannot be represented by a single JVM `char`, and so is instead represented as two `char`s containing the equivalent UTF-16 surrogate pair (`[0xD83C, 0xDF0F]`).

> [!NOTE]  
> This library fundamentally depends on being able to break strings into grapheme clusters, [which evolves with each version of the Unicode specification](https://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries).  The JVM provides this capability via the [`java.text.BreakIterator` class](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/text/BreakIterator.html), but unfortunately the implementation of this class tends to lag behind the latest version of the Unicode specification, especially in JVM versions prior to 20.  For that reason, this library will check at runtime whether the ICU4J library is on the classpath, and if so use [its superior implementation of the `BreakIterator` class](https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/com/ibm/icu/text/BreakIterator.html) instead of the JDK's.  This gives downstream users of the library the ability to choose whether to consume this library in a lightweight, zero-dependency, "best effort of the JVM" form (the default), or whether to introduce the large (14MB) ICU4J dependency in order to ensure correct behaviour across a wider range of JVM versions and Unicode inputs.  Note that this switching behaviour is automatic and requires no effort on the part of code that uses this library - it's achieved using ["classpath sniffing" at runtime](https://github.com/pmonks/clj-wcwidth/blob/release/src/wcwidth/api.clj#L71-L76).

## Installation

`clj-wcwidth` is available as a Maven artifact from [Clojars](https://clojars.org/com.github.pmonks/clj-wcwidth).

## Usage

[API documentation is available here](https://pmonks.github.io/clj-wcwidth/wcwidth.api.html).  [The unit tests](https://github.com/pmonks/clj-wcwidth/blob/release/test/wcwidth/api_test.clj) provide comprehensive usage examples.

I'm also active on [the Clojure Discord server](https://discord.gg/discljord) if you'd like to chat.

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

(def jerome (wcw/code-points->string [\J \e 0x0341 \r \o 0x0302 \m \e]))  ; Jérôme, using combining diacritics
(wcw/display-width jerome)
; ==> 6
(count jerome)
; ==> 8

(def deseret-capital-long-i (wcw/code-point->string 0x10400))  ; 𐐀
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

(def transgender-flag (wcw/code-points->string [0x1F3F3 0xFE0F 0x200D 0x26A7 0xFE0F]))  ; 🏳️‍⚧️
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
