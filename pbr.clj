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

(def lib 'com.github.pmonks/clj-wcwidth)

#_{:clj-kondo/ignore [:unresolved-namespace]}
(def version (format "1.0.%s" (b/git-count-revs nil)))

(defn set-opts
  [opts]
  (assoc opts
         :lib          lib
         :version      version
         :write-pom    true
         :validate-pom true
         :pom          {:description      "Pure Clojure implementations of wcwidth/wcswidth."
                        :url              "https://github.com/pmonks/clj-wcwidth"
                        :licenses         [:license   {:name "Apache License 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}]
                        :developers       [:developer {:id "pmonks" :name "Peter Monks" :email "pmonks+wcwidth@gmail.com"}]
                        :scm              {:url "https://github.com/pmonks/wcwidth" :connection "scm:git:git://github.com/pmonks/wcwidth.git" :developer-connection "scm:git:ssh://git@github.com/pmonks/wcwidth.git"}
                        :issue-management {:system "github" :url "https://github.com/pmonks/wcwidth/issues"}}))
