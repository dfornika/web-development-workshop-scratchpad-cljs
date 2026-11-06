(ns scratchpad.solutions.session-1
  "Reference solutions for src/main/scratchpad/sessions/session_1.cljs.

  These are just one way to do it - if yours works, it's right!
  The function names match the homework file, so you can compare them side
  by side, or copy a function across to see it on the Session 1 page.

  To try them at the REPL: load this file (Ctrl+Alt+C Enter), then evaluate
  the forms in the (comment ...) blocks."
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [scratchpad.components :as c]))


;; 1. Gene Viewer ------------------------------------------------------------

(def example-gene
  {:gene-name "KPC-3" :start 21331 :end 22598})

(defn gene-length
  "Length of a gene in base pairs. Positions are inclusive, hence the inc."
  [{:keys [start end]}]
  (inc (- end start)))

(defn gene-viewer
  "Display information about `gene`, a map like `example-gene`."
  [gene]
  [c/card {:title (:gene-name gene)
           :style {:max-width "360px"}}
   [c/label-value "Start" (:start gene)]
   [c/label-value "End" (:end gene)]
   [c/label-value "Length" (str (gene-length gene) " bp")]])

(comment
  (gene-length example-gene)
  (gene-viewer example-gene)
  )


;; 2. Sequence Analyzer ------------------------------------------------------

(defn count-bases
  "Count how many times each base appears in `dna`, a string like \"ATGCGTACG\"."
  [dna]
  ;; `frequencies` counts each distinct item. Strings are sequences of
  ;; characters, so (map str ...) turns each character into a 1-letter string.
  (frequencies (map str (str/upper-case dna))))

(comment
  (count-bases "ATGCGTACG")
  (count-bases "atgc")
  (count-bases "")
  )


;; 3. GC Content Calculator --------------------------------------------------

(defn gc-content
  "The percentage (0-100) of bases in `dna` that are G or C."
  [dna]
  (let [bases (str/upper-case (str/trim dna))
        gc (count (filter #{\G \C} bases))]
    (if (zero? (count bases))
      0
      (* 100 (/ gc (count bases))))))

(defn gc-calculator
  "A text box to paste a sequence into, and a button to calculate its GC content."
  []
  (r/with-let [sequence (r/atom "")
               result (r/atom nil)]
    [c/column {:gap "8px"}
     [c/text-area {:value @sequence
                   :on-change (fn [text] (reset! sequence text))
                   :placeholder "Paste a DNA sequence here"}]
     [:div
      [c/button {:on-click (fn [] (reset! result (gc-content @sequence)))}
       "Calculate GC content"]]
     (when @result
       [c/stat {:label "GC content"
                :value (.toFixed @result 1)
                :unit "%"}])]))

(comment
  (gc-content "GGCC")
  (gc-content "ATAT")
  (gc-content "ATGCGTACG")
  (gc-content "")
  )


;; 4. Antimicrobial Resistance Marker Tag ------------------------------------

(def known-resistance-genes
  #{"blaKPC" "blaNDM" "blaOXA-48" "mecA" "vanA"})

(def example-genes
  ["dnaA" "blaKPC" "recA" "mecA" "rpoD" "vanA" "16S rRNA"])

(defn resistance-gene?
  "true if `gene-name` is in `known-resistance-genes`."
  [gene-name]
  (contains? known-resistance-genes gene-name))

(defn gene-list-with-markers
  "Show each gene in `genes`, with a marker beside the known resistance genes."
  [genes]
  [:ul {:style {:list-style "none" :padding 0}}
   (for [gene genes]
     ^{:key gene}
     [:li {:style {:margin "4px 0"}}
      [:span {:style {:font-family "monospace" :margin-right "8px"}} gene]
      (when (resistance-gene? gene)
        [c/tag {:color "#ffcecb"} "AMR"])])])

(comment
  (resistance-gene? "mecA")
  (filter resistance-gene? example-genes)

  ;; Stretch goal: also match variants like "blaKPC-3"
  (defn resistance-gene-or-variant? [gene-name]
    (boolean (some #(str/starts-with? gene-name %) known-resistance-genes)))
  (resistance-gene-or-variant? "blaKPC-3")
  )
