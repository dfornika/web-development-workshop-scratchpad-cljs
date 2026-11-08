(ns scratchpad.sessions.session-1
  "Session 1 homework: genomics-themed mini projects.

  Each exercise has:
    - a function for you to write (look for TODO)
    - a (comment ...) block with things to try at the REPL

  Your results show up live on the Session 1 page as you save.

  Get started by loading this file into the REPL:
    Command Palette (Ctrl+Shift+P) ->
    \"Calva: Load/Evaluate Current File and its Requires/Dependencies\"
    (shortcut: Ctrl+Alt+C Enter)
  Then evaluate forms in the (comment ...) blocks with Alt+Enter."
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [scratchpad.components :as c]))


;; ===========================================================================
;; 1. Gene Viewer
;;
;; Create a function that takes a map like the one below and displays the
;; gene info using styled HTML components.
;; ===========================================================================

(def example-gene
  {:gene-name "KPC-3" :start 21331 :end 22598})

(defn gene-viewer
  "Display information about `gene`, a map like `example-gene`."
  [gene]
  ;; TODO: replace this with your own hiccup. Some ideas:
  ;;   - a c/card with the gene name as its :title
  ;;   - c/label-value for the start and end positions
  ;;   - work out the gene's length from :start and :end
  [c/todo "gene-viewer"])

(comment
  example-gene
  (:gene-name example-gene)
  (:start example-gene)
  (- (:end example-gene) (:start example-gene))

  ;; Your component is just a function that returns data - try calling it:
  (gene-viewer example-gene)
  ;; Components inside it (like c/card) show up as #object[...] - c/hiccup
  ;; shows their names instead, and c/expand turns them into plain HTML
  ;; elements, the way Reagent does when it draws the page:
  (c/hiccup (gene-viewer example-gene))
  (c/expand (gene-viewer example-gene))
  )


;; ===========================================================================
;; 2. Sequence Analyzer
;;
;; Write a function that takes a string of nucleotides (e.g. "ATGCGTACG")
;; and counts how many times each base appears.
;; ===========================================================================

(defn count-bases
  "Count how many times each base appears in `dna`, a string like \"ATGCGTACG\"."
  [dna]
  ;; TODO: return something like {"A" 2, "T" 2, "G" 3, "C" 2}
  nil)

(comment
  ;; A string is a sequence of characters:
  (seq "ATGC")
  (first "ATGC")

  ;; Some core functions that might help - what does each one do?
  (count "ATGCGTACG")
  (filter #(= % \G) "ATGCGTACG")
  (frequencies [:a :b :a :c :a])

  (count-bases "ATGCGTACG")

  ;; Bonus: what about lowercase, like "atgc"? (str/upper-case might help)
  (count-bases "atgc")
  )


;; ===========================================================================
;; 3. GC Content Calculator
;;
;; Create a small input field where users can paste a DNA sequence, and
;; display its GC content when they click a button.
;; ===========================================================================

(defn gc-content
  "The percentage (0-100) of bases in `dna` that are G or C."
  [dna]
  ;; TODO: count the G's and C's, divide by the length, multiply by 100.
  ;; Careful: what should happen for an empty string?
  nil)

(defn gc-calculator
  "A text box to paste a sequence into, and a button to calculate its GC content."
  []
  ;; `r/with-let` creates `sequence` and `result` once, when the component
  ;; first appears, and keeps them between re-renders. They're Reagent atoms:
  ;; read them with @, change them with reset! or swap!.
  (r/with-let [sequence (r/atom "")
               result (r/atom nil)]
    [c/column {:gap "8px"}
     [c/text-area {:value @sequence
                   :on-change (fn [text] (reset! sequence text))
                   :placeholder "Paste a DNA sequence here"}]
     ;; TODO: add a c/button whose :on-click calculates (gc-content @sequence)
     ;;       and stores it with (reset! result ...)
     ;; TODO: show @result, e.g. with c/stat
     [c/todo "the button and the result"]]))

(comment
  (gc-content "GGCC")      ; should be 100
  (gc-content "ATAT")      ; should be 0
  (gc-content "ATGCGTACG") ; should be about 55.6
  (gc-content "")

  ;; Rounding: (.toFixed 55.5555 1) gives "55.6"
  (.toFixed 55.5555 1)
  )


;; ===========================================================================
;; 4. Antimicrobial Resistance Marker Tag
;;
;; Given a list of genes, display markers beside known resistance genes
;; from a hardcoded list.
;; ===========================================================================

(def known-resistance-genes
  #{"blaKPC" "blaNDM" "blaOXA-48" "mecA" "vanA"})

(def example-genes
  ["dnaA" "blaKPC" "recA" "mecA" "rpoD" "vanA" "16S rRNA"])

(defn resistance-gene?
  "true if `gene-name` is in `known-resistance-genes`."
  [gene-name]
  ;; TODO
  false)

(defn gene-list-with-markers
  "Show each gene in `genes`, with a marker beside the known resistance genes."
  [genes]
  ;; TODO: one line per gene, with a c/tag next to the resistance genes.
  ;; `for` is handy for turning a list of data into a list of hiccup:
  ;;   [:ul (for [gene genes] ^{:key gene} [:li gene])]
  ;; (the ^{:key ...} helps React keep track of the items in a list)
  [c/todo "gene-list-with-markers"])

(comment
  (contains? known-resistance-genes "mecA")
  (contains? known-resistance-genes "dnaA")
  (resistance-gene? "mecA")

  (filter resistance-gene? example-genes)
  (for [gene example-genes] [:li gene])

  ;; Stretch goal: real gene names often have a variant number, like
  ;; "blaKPC-3". Can you mark those too? (str/starts-with? might help)
  (str/starts-with? "blaKPC-3" "blaKPC")
  )
