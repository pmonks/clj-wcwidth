| | | |
|---:|:---:|:---:|
| [**main**](https://github.com/pmonks/clj-wcwidth/tree/main) | [![CI](https://github.com/pmonks/clj-wcwidth/workflows/CI/badge.svg?branch=main)](https://github.com/pmonks/clj-wcwidth/actions?query=workflow%3ACI+branch%3Amain) | [![Dependencies](https://github.com/pmonks/clj-wcwidth/workflows/dependencies/badge.svg?branch=main)](https://github.com/pmonks/clj-wcwidth/actions?query=workflow%3Adependencies+branch%3Amain) |
| [**dev**](https://github.com/pmonks/clj-wcwidth/tree/dev) | [![CI](https://github.com/pmonks/clj-wcwidth/workflows/CI/badge.svg?branch=dev)](https://github.com/pmonks/clj-wcwidth/actions?query=workflow%3ACI+branch%3Adev) | [![Dependencies](https://github.com/pmonks/clj-wcwidth/workflows/dependencies/badge.svg?branch=dev)](https://github.com/pmonks/clj-wcwidth/actions?query=workflow%3Adependencies+branch%3Adev) |

[![Latest Version](https://img.shields.io/clojars/v/com.github.pmonks/clj-wcwidth)](https://clojars.org/com.github.pmonks/clj-wcwidth/) [![Open Issues](https://img.shields.io/github/issues/pmonks/clj-wcwidth.svg)](https://github.com/pmonks/clj-wcwidth/issues) [![License](https://img.shields.io/github/license/pmonks/clj-wcwidth.svg)](https://github.com/pmonks/clj-wcwidth/blob/main/LICENSE)

# clj-wcwidth

Pure Clojure implementations of the [`wcwidth`](https://man7.org/linux/man-pages/man3/wcwidth.3.html) and [`wcswidth`](https://man7.org/linux/man-pages/man3/wcswidth.3.html) POSIX functions (plus some other useful Unicode functions).

## Why?

When printing Unicode characters to a fixed-width display device (e.g. a terminal), many Unicode code points have a well-defined "column width".  This has been standardised in [Unicode Technical Report #11](https://www.unicode.org/reports/tr11/), and implemented as the POSIX functions `wcwidth` and `wcswidth`.

Java doesn't provide these functions however, so applications that need to know these widths (e.g. for terminal screen formatting purposes) are left to their own devices.  While there are Java libraries that have implemented this themselves (notably [JLine](https://github.com/jline/jline3/blob/master/terminal/src/main/java/org/jline/utils/WCWidth.java)), pulling in a large dependency when one only uses a very small part of it is sometimes overkill.

This library provides a small, zero-dependency, pure Clojure implementation of the rules described in UTR-11 (and updated for recent Unicode versions), to avoid having to do that. It also goes further by (optionally) also taking [ANSI escape sequences](https://en.wikipedia.org/wiki/ANSI_escape_code) into account.

## Why not [`count`](https://clojuredocs.org/clojure.core/count)?

When supplied with a sequence of characters (normally a `String`, though also a Java `char[]`), `count` simply counts the number of Java `char`s in that sequence, which, due to a [historical oddity of the JVM](https://www.oracle.com/technical-resources/articles/javase/supplementary.html), is not necessarily the same thing as a Unicode code point (what we generally now think of as a "character"). Specifically, Java `char`s are a 16 bit "code unit" from UTF-16, and Unicode code points in the supplementary planes are represented by two such code units (and therefore as 2 `char`s on the JVM).

Furthermore, `count` doesn't account for non-printing and zero-width Unicode code points; it counts them as `char`s even though they take up zero width when printed.

## Installation

`clj-wcwidth` is available as a Maven artifact from [Clojars](https://clojars.org/com.github.pmonks/clj-wcwidth).

### Trying it Out

#### Clojure CLI

```shell
$ clojure -Sdeps '{:deps {com.github.pmonks/clj-wcwidth {:mvn/version "RELEASE"}}}'
```

#### Leiningen

```shell
$ lein try com.github.pmonks/clj-wcwidth
```

#### Simple REPL Session

```clojure
(require '[wcwidth.api :as wcw] :reload-all)

(wcw/wcwidth \A)
; ==> 1
(wcw/wcwidth \©)
; ==> 1
(wcw/wcwidth 0x0000)   ; ASCII NUL (zero width)
; ==> 0
(wcw/wcwidth 0x001B)   ; ASCII ESC (non printing)
; ==> -1
(wcw/wcwidth 0x1F921)  ; 🤡 (double width)
; ==> 2

(wcw/display-width "hello, world")  ; all single width
; ==> 12
(wcw/display-width "hello, 🌏")     ; mixed single and double width
; ==> 9

; Showing the difference between the POSIX wcswidth behaviour and the more
; useful in Clojure, but non-POSIX, display-width behaviour:
(let [example-string (str "hello, world" (wcw/code-point-to-string 0x0084))]   ; non-printing code point
  (wcw/display-width example-string)
  ; ==> 12
  (wcw/wcswidth example-string)
  ; ==> -1

  ; Also show why clojure.core/count is inappropriate for determining display width:
  (count example-string))
  ; ==> 13

; More examples showing why clojure.core/count is inappropriate for determining display width:
(let [example-string (wcw/code-point-to-string 0x10400)]  ; 𐐀
  (wcw/display-width example-string)
  ; ==> 1
  (count example-string))
  ; ==> 2

(let [example-string "👍👍🏻"]
  (wcw/display-width example-string)
  ; ==> 4
  (count example-string))
  ; ==> 6
```

## Usage

The functionality is provided by the `wcwidth.api` namespace.

Require it in the REPL:

```clojure
(require '[wcwidth.api :as wcw] :reload-all)
```

Require it in your application:

```clojure
(ns my-app.core
  (:require [wcwidth.api :as wcw]))
```

### API Documentation

[API documentation is available here](https://pmonks.github.io/clj-wcwidth/).  [The unit tests](https://github.com/pmonks/clj-wcwidth/blob/main/test/wcwidth/api_test.clj) provide comprehensive usage examples.

## Contributor Information

[Contributing Guidelines](https://github.com/pmonks/clj-wcwidth/blob/main/.github/CONTRIBUTING.md)

[Bug Tracker](https://github.com/pmonks/clj-wcwidth/issues)

[Code of Conduct](https://github.com/pmonks/clj-wcwidth/blob/main/.github/CODE_OF_CONDUCT.md)

### Developer Workflow

This project uses the [git-flow branching strategy](https://nvie.com/posts/a-successful-git-branching-model/), with the caveat that the permanent branches are called `main` and `dev`, and any changes to the `main` branch are considered a release and auto-deployed (JARs to Clojars, API docs to GitHub Pages, etc.).

For this reason, **all development must occur either in branch `dev`, or (preferably) in temporary branches off of `dev`.**  All PRs from forked repos must also be submitted against `dev`; the `main` branch is **only** updated from `dev` via PRs created by the core development team.  All other changes submitted to `main` will be rejected.

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

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
