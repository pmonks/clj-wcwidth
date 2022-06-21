| | | |
|---:|:---:|:---:|
| [**main**](https://github.com/pmonks/clj-wcwidth/tree/main) | [![CI](https://github.com/pmonks/clj-wcwidth/workflows/CI/badge.svg?branch=main)](https://github.com/pmonks/clj-wcwidth/actions?query=workflow%3ACI+branch%3Amain) | [![Dependencies](https://github.com/pmonks/clj-wcwidth/workflows/dependencies/badge.svg?branch=main)](https://github.com/pmonks/clj-wcwidth/actions?query=workflow%3Adependencies+branch%3Amain) |
| [**dev**](https://github.com/pmonks/clj-wcwidth/tree/dev) | [![CI](https://github.com/pmonks/clj-wcwidth/workflows/CI/badge.svg?branch=dev)](https://github.com/pmonks/clj-wcwidth/actions?query=workflow%3ACI+branch%3Adev) | [![Dependencies](https://github.com/pmonks/clj-wcwidth/workflows/dependencies/badge.svg?branch=dev)](https://github.com/pmonks/clj-wcwidth/actions?query=workflow%3Adependencies+branch%3Adev) |

[![Latest Version](https://img.shields.io/clojars/v/com.github.pmonks/wcwidth)](https://clojars.org/com.github.pmonks/wcwidth/) [![Open Issues](https://img.shields.io/github/issues/pmonks/wcwidth.svg)](https://github.com/pmonks/clj-wcwidth/issues) [![License](https://img.shields.io/github/license/pmonks/wcwidth.svg)](https://github.com/pmonks/clj-wcwidth/blob/main/LICENSE)

# wcwidth

Pure Clojure implementations of the [`wcwidth`](https://man7.org/linux/man-pages/man3/wcwidth.3.html) and [`wcswidth`](https://man7.org/linux/man-pages/man3/wcswidth.3.html) POSIX functions.

## Why?

When printing Unicode characters to a fixed-width display device (e.g. a terminal), every Unicode code point has a well-defined "column width".  This was originally standardised in [Unicode Technical Report #11](https://unicode.org/reports/tr11-5/), and implemented as the POSIX functions `wcwidth` and `wcswidth` soon after.

Java doesn't provide these functions however, so applications that need to know these widths (e.g. for terminal screen formatting purposes) are left to their own devices.  While there are Java libraries that have implemented this themselves (notably [JLine](https://github.com/jline/jline3/blob/master/terminal/src/main/java/org/jline/utils/WCWidth.java)), pulling in a large dependency when one only uses a very small part of it is sometimes overkill.   This library provides a pure, zero-dependency Clojure implementation of the rules described in UTR-11 (and updated for recent Unicode versions), to avoid having to do that.

## Installation

`wcwidth` is available as a Maven artifact from [Clojars](https://clojars.org/com.github.pmonks/wcwidth).

### Trying it Out

#### Clojure CLI

```shell
$ clojure -Sdeps '{:deps {com.github.pmonks/wcwidth {:mvn/version "#.#.#"}}}'  # Where #.#.# is replaced with an actual version number (see badge above)
```

#### Leiningen

```shell
$ lein try com.github.pmonks/wcwidth
```

#### Simple REPL Session

```clojure
(require '[wcwidth.api :as wcw] :reload-all)

(wcw/wcwidth \A)
(wcw/wcwidth \©)
(wcw/wcwidth 0x001B)   ; ASCII ESC
(wcw/wcwidth 0x1F921)  ; 🤡

(wcw/wcwidth "hello, world")
(wcw/wcwidth "hello, 🤡")
(wcw/wcwidth (str "hello, world" (wcw/codepoint-to-string 0x0084)))   ; non-printing char

; Showing the difference between the POSIX wcswidth behaviour and the (more useful for Clojure, but non-POSIX) wcswidth2 behaviour
(wcw/wcwidth2 (str "hello, world" (wcw/codepoint-to-string 0x0084)))   ; non-printing char
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
