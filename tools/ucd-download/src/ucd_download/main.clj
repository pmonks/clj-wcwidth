;
; Copyright © 2026 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns ucd-download.main
  "Entry point for the UCD download command line utility."
  (:require [clojure.string         :as s]
            [clojure.pprint         :as pp]
            [clojure.java.io        :as io]
            [progress.indeterminate :as pi]
            [ucd-download.ucd       :as ucd]))


(def controls-edn-filename          "ucd-control-ranges.edn")
(def combining-classes-edn-filename "ucd-combining-class-ranges.edn")
(def wides-edn-filename             "ucd-wide-ranges.edn")
(def emoji-sequences-edn-filename   "ucd-emoji-sequences.edn")

; Simplified versions of the various east asian width codes, suitable for our purposes
(def east-asian-width->simple-width {
  :east-asian-full-width :wide
  :east-asian-half-width :narrow
  :east-asian-wide       :wide
  :east-asian-narrow     :narrow
  :ambiguous             :narrow  ; Note: assumes non-east-asian context - see https://www.unicode.org/reports/tr11/#Ambiguous for more details
  :neutral               :narrow})

(defn exit
  "Exits the program after printing the given messages (as per println), and
  returns the given status code."
  ([]            (exit 0 nil))
  ([status-code] (exit status-code nil))
  ([status-code & messages]
   (let [message (s/join " " messages)]
     (when-not (s/blank? message)
       (if (= 0 status-code)
         (println message)
         (binding [*out* *err*]
           (println message)))
       (flush))
     (shutdown-agents)
     (System/exit status-code))))

(defn write-edn-data
  "Writes `data` (pretty printed) to EDN file `f`, with a comment at the top
  containing the generation date and the `unicode-version`."
  [f unicode-version data]
  (with-open [w (io/writer (io/file f))]
    (.write w "; GENERATED FILE, DO NOT EDIT!\n")
    (.write w "; Generated from Unicode v")
    (.write w unicode-version)
    (.write w " at ")
    (.write w (str (java.time.Instant/now)))
    (.write w "\n")
    (pp/pprint data w)))

(defn -main
  "Entry point for the UCD loader CLI tool."
  [& args]
  (when (or (not= 1 (count args))
            (s/blank? (first args)))
    (exit -1 "Please provide an output directory on the command line."))

  (let [output-directory-name (first args)
        output-directory      (io/file output-directory-name)]
    (when-not (and (.exists output-directory)
                   (.isDirectory output-directory)
                   (.canWrite output-directory))
      (exit -1 "Output directory" output-directory-name "does not exist, is not a directory, or is not writable."))

    (print "ℹ️ Downloading latest UCD data and precompiling into EDN files in" output-directory-name "")

    (pi/animate!
      (let [unicode-version    (ucd/unicode-version)
            general-categories (ucd/general-categories)]
        ; Generate and write out controls codepoint ranges EDN file
        (write-edn-data (io/file output-directory controls-edn-filename)
                        unicode-version
                        (vec (ucd/optimise-ranges (map first (filter #(some #{:control} [(second %)]) general-categories)))))

        ; Generate and write out combining classes codepoint ranges EDN file
        (write-edn-data (io/file output-directory combining-classes-edn-filename)
                        unicode-version
                        (vec (ucd/optimise-ranges (map first (filter #(some #{:nonspacing-mark :format :surrogate} [(second %)]) general-categories)))))

        ; Generate and write out wide (east asian widths and emoji) codepoint ranges EDN file
        (let [east-asian-width-ranges (map first (filter #(= :wide (second %)) (map #(let [[rng width-code] %] [rng (east-asian-width->simple-width width-code)]) (ucd/east-asian-widths))))
              emoji-ranges            (map first (ucd/single-codepoint-emoji))
              all-wides               (vec (ucd/optimise-ranges (sort-by first (concat east-asian-width-ranges emoji-ranges))))]
          (write-edn-data (io/file output-directory wides-edn-filename)
                          unicode-version
                          all-wides))

        ; Generate and write out ZWJ emoji sequences EDN file
        (write-edn-data (io/file output-directory emoji-sequences-edn-filename)
                        unicode-version
                        (set (map first (ucd/codepoint-sequence-emoji))))))  ; Important: these are NOT ranges, so can't be run through optimise-ranges!

    (println "\nℹ️ Done")))
