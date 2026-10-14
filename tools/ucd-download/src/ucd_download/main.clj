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

; Note: don't change these without also updating wcwidth.ucd to match
(def controls-edn-filename          "controls.edn")
(def combining-classes-edn-filename "combining-classes.edn")
(def wides-edn-filename             "wide-ranges.edn")
(def emoji-sequences-edn-filename   "emoji-sequences.edn")

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
  [f unicode-version now data]
  (with-open [w (io/writer (io/file f))]
    (.write w "; GENERATED FILE, DO NOT EDIT!\n")
    (.write w (str "; Generated from Unicode v" unicode-version " at " now "\n"))
    (pp/pprint data w)
    (.flush w)))

(defn download-and-write-files
  "Downloads and writes the various EDN data files used by clj-wcwidth to the
  specified `output-directory`."
  [output-directory]
  (let [unicode-version    (ucd/unicode-version)
        now                (java.time.Instant/now)
        general-categories (ucd/general-categories)]
    ; Generate and write out controls codepoint set EDN file (we use a set as it's small - 65 codepoints as of Unicode v18.0.0)
    (write-edn-data (io/file output-directory controls-edn-filename)
                    unicode-version
                    now
                    (ucd/ranges->set (ucd/ranges-by-categories general-categories #{:control})))

    ; Generate and write out combining classes codepoint ranges EDN file - 4308 codepoints as of Unicode v18.0.0 (~175KB in memory as a set)
    (write-edn-data (io/file output-directory combining-classes-edn-filename)
                    unicode-version
                    now
                    (ucd/ranges->set (ucd/ranges-by-categories general-categories #{:nonspacing-mark :format :surrogate})))

    ; Generate and write out wide (east asian widths and single-codepoint emoji) codepoint ranges EDN file - 183866 codepoints as of Unicode v18.0.0, which is too large to put in a set (~7MB)
    (let [east-asian-width-ranges (map first (filter #(= :wide (second %)) (map #(let [[rng width-code] %] [rng (east-asian-width->simple-width width-code)]) (ucd/east-asian-widths))))
          emoji-ranges            (map first (ucd/single-codepoint-emoji))
          all-wides               (ucd/optimise-ranges (sort-by first (concat east-asian-width-ranges emoji-ranges)))]
      (write-edn-data (io/file output-directory wides-edn-filename)
                      unicode-version
                      now
                      all-wides))

    ; Generate and write out multi-codepoint emoji sequences set EDN file - 2770 sequences as of Unicode v18.0.0
    (write-edn-data (io/file output-directory emoji-sequences-edn-filename)
                    unicode-version
                    now
                    (set (map first (ucd/codepoint-sequence-emoji))))))  ; Important: these are NOT ranges!

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

    (print "ℹ️ Downloading latest UCD data and precompiling into EDN files in" (str output-directory-name "... "))
    (pi/animate! (download-and-write-files output-directory))
    (println "\nℹ️ Done")
    (exit 0)))
