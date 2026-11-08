(ns scratchpad.data
  "The CSV file loaded on the Data Explorer page.

  The code the Data Explorer writes for you gets the file's rows with

      (data/csv-rows)

  Your file is read by your browser and stays there: nothing is uploaded."
  (:require [clojure.string :as str]
            [reagent.core :as r]))

;; The loaded file: {:file-name "samples.csv" :headers [...] :rows [[...] ...]}
(defonce current (r/atom nil))

;; ---------------------------------------------------------------------------
;; Reading CSV (and TSV) text
;; ---------------------------------------------------------------------------

(defn- guess-delimiter
  "Comma, tab or semicolon - whichever appears most in the first line."
  [text]
  (let [first-line (first (str/split-lines text))]
    (->> [\, \tab \;]
         (apply max-key #(count (filter #{%} first-line))))))

(defn- parse-records
  "Splits CSV text into records (vectors of strings). Handles quoted
  values, which can contain the delimiter, line breaks, and doubled
  quotes (\"\")."
  [text delimiter]
  ;; Each match is one value, then what follows it: the delimiter, a line
  ;; break, or the end of the text. The y (\"sticky\") flag makes each match
  ;; start exactly where the last one ended.
  (let [d (if (= delimiter \tab) "\\t" (str delimiter))
        re (js/RegExp. (str "(\"(?:[^\"]|\"\")*\"|[^" d "\\r\\n]*)(" d "|\\r\\n|\\n|\\r|$)") "y")
        n (count text)]
    (loop [record (transient [])
           records (transient [])]
      (let [m (.exec re text)
            raw (aget m 1)
            value (if (and (> (count raw) 1)
                           (str/starts-with? raw "\"")
                           (str/ends-with? raw "\""))
                    (str/replace (subs raw 1 (dec (count raw))) "\"\"" "\"")
                    raw)
            record (conj! record value)
            separator (aget m 2)]
        (cond
          (= separator (str delimiter)) (recur record records)
          (or (= separator "") (>= (.-lastIndex re) n))
          (persistent! (conj! records (persistent! record)))
          :else (recur (transient []) (conj! records (persistent! record))))))))

(defn- blank-record? [record]
  (every? str/blank? record))

(defn- clean-headers
  "Header names, with blank ones named (column 3) and repeated ones
  numbered (sample, sample (2)), so every column has its own name."
  [headers]
  (loop [[h & more] headers
         i 1
         seen #{}
         result []]
    (if (nil? h)
      result
      (let [base (if (str/blank? h) (str "column " i) (str/trim h))
            name (loop [k 2 candidate base]
                   (if (seen candidate)
                     (recur (inc k) (str base " (" k ")"))
                     candidate))]
        (recur more (inc i) (conj seen name) (conj result name))))))

(defn parse-csv
  "Reads CSV (or tab-separated) text into
  {:headers [\"sample_id\" ...] :rows [[\"S001\" ...] ...]}. The first line
  is the column names. Every row gets exactly one value per column."
  [text]
  (let [text (str/replace text #"^﻿" "")   ; a marker some programs add
        records (remove blank-record? (parse-records text (guess-delimiter text)))
        headers (clean-headers (first records))
        width (count headers)]
    {:headers headers
     :rows (mapv (fn [record]
                   (mapv #(str/trim (or (get record %) "")) (range width)))
                 (rest records))}))

;; ---------------------------------------------------------------------------
;; For your code
;; ---------------------------------------------------------------------------

(defn file-name
  "The name of the loaded file."
  []
  (:file-name @current))

(defn csv-rows
  "The rows of the loaded file, each one a map from column name to text:
    [{\"sample_id\" \"S001\", \"total_reads\" \"1200\", ...} ...]"
  []
  (let [{:keys [headers rows]} @current]
    (mapv #(zipmap headers %) rows)))
