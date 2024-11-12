;
; Copyright © 2022 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

#_{:clj-kondo/ignore [:unresolved-namespace]}
(defn set-opts
  [opts]
  (assoc opts
         :lib          'com.github.pmonks/clj-wcwidth
         :version      (pbr/calculate-version 1 0)
         :prod-branch  "release"
         :write-pom    true
         :validate-pom true
         :pom          {:description      "Pure Clojure implementations of wcwidth/wcswidth."
                        :url              "https://github.com/pmonks/clj-wcwidth"
                        :licenses         [:license   {:name "MPL-2.0" :url "https://www.mozilla.org/en-US/MPL/2.0/"}]
                        :developers       [:developer {:id "pmonks" :name "Peter Monks" :email "pmonks+wcwidth@gmail.com"}]
                        :scm              {:url "https://github.com/pmonks/clj-wcwidth" :connection "scm:git:git://github.com/pmonks/clj-wcwidth.git" :developer-connection "scm:git:ssh://git@github.com/pmonks/clj-wcwidth.git"}
                        :issue-management {:system "github" :url "https://github.com/pmonks/clj-wcwidth/issues"}}
         :codox        {:metadata         {:doc/format :markdown}}))
