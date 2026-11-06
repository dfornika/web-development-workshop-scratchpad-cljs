(ns scratchpad.views.session-1
  "The Session 1 page. It shows the live results of the functions in
  src/main/scratchpad/sessions/session_1.cljs and session_1_bonus.cljs -
  edit those files, not this one."
  (:require-macros [scratchpad.macros :refer [show-call]])
  (:require [scratchpad.components :as c]
            [scratchpad.sessions.session-1 :as s1]
            [scratchpad.sessions.session-1-bonus :as bonus]
            [scratchpad.ui :as ui]))

(def ^:private homework-file "src/main/scratchpad/sessions/session_1.cljs")
(def ^:private bonus-file "src/main/scratchpad/sessions/session_1_bonus.cljs")

(defn- edit [file fn-name]
  (str fn-name " in " file))

(defn page []
  [c/page
   [c/heading {:level 1} "Session 1: Homework"]
   [c/callout
    [:strong "How this page works: "]
    "each panel below shows the live result of a function you write. Open "
    [:code homework-file] ", load it into the REPL (Ctrl+Alt+C Enter), "
    "and work through the TODOs. Try things out in the " [:code "(comment ...)"]
    " blocks with Alt+Enter, and save the file to see this page update."]

   [c/section {:title "Genomics mini projects"}
    [ui/exercise {:title "1. Gene Viewer"
                  :task "Create a function that takes a map like {:gene-name \"KPC-3\" :start 21331 :end 22598} and displays gene info using styled HTML components."
                  :edit (edit homework-file "gene-viewer")}
     [s1/gene-viewer s1/example-gene]]

    [ui/exercise {:title "2. Sequence Analyzer"
                  :task "Write a function that takes a string of nucleotides (e.g. \"ATGCGTACG\") and counts how many times each base appears."
                  :edit (edit homework-file "count-bases")}
     [c/column {:gap "8px"}
      (show-call (s1/count-bases "ATGCGTACG"))
      (show-call (s1/count-bases "AAAA"))
      (show-call (s1/count-bases ""))]]

    [ui/exercise {:title "3. GC Content Calculator"
                  :task "Create a small input field where users can paste a DNA sequence, and display its GC content when they click a button."
                  :edit (edit homework-file "gc-content and gc-calculator")}
     [c/column {:gap "8px"}
      (show-call (s1/gc-content "ATGCGTACG"))
      [s1/gc-calculator]]]

    [ui/exercise {:title "4. Antimicrobial Resistance Marker Tag"
                  :task "Given a list of genes, display markers beside known resistance genes from a hardcoded list (e.g. \"blaKPC\", \"mecA\")."
                  :edit (edit homework-file "resistance-gene? and gene-list-with-markers")}
     [c/column {:gap "8px"}
      (show-call (s1/resistance-gene? "mecA"))
      (show-call (s1/resistance-gene? "dnaA"))
      [s1/gene-list-with-markers s1/example-genes]]]]

   [c/section {:title "Bonus: general ClojureScript challenges"}
    [ui/exercise {:title "1. Click Counter"
                  :task "Replace the alert button with a dynamic counter that updates on click."
                  :edit (edit bonus-file "click-counter")}
     [bonus/click-counter]]

    [ui/exercise {:title "2. Theme Switcher"
                  :task "Add a light/dark mode toggle by changing background and font styles using a button."
                  :edit (edit bonus-file "theme-switcher")}
     [bonus/theme-switcher]]

    [ui/exercise {:title "3. Image Gallery"
                  :task "Display multiple images (e.g. microscopic images, DNA gels) with titles and descriptions."
                  :edit (edit bonus-file "image-gallery")}
     [bonus/image-gallery bonus/example-images]]

    [ui/exercise {:title "4. Interactive Table"
                  :task "Use map to generate a table from a vector of sample metadata like sample ID, date, and status."
                  :edit (edit bonus-file "sample-table")}
     [bonus/sample-table bonus/example-samples]]]])
