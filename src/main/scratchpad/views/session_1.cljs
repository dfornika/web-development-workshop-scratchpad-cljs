(ns scratchpad.views.session-1
  "The Session 1 page. It shows the live results of the functions in
  src/main/scratchpad/sessions/session_1.cljs and session_1_bonus.cljs -
  edit those files, not this one.

  The page gets those functions through a `lookup` function, so the same
  page works in Codespaces (compiled code) and in the in-browser version
  (code typed into the page, run by SCI)."
  (:require [scratchpad.components :as c]
            [scratchpad.sessions.session-1 :as s1]
            [scratchpad.sessions.session-1-bonus :as bonus]
            [scratchpad.ui :as ui]))

(def files
  {:s1 {:name "session_1.cljs"
        :path "src/main/scratchpad/sessions/session_1.cljs"
        :ns "scratchpad.sessions.session-1"}
   :bonus {:name "session_1_bonus.cljs"
           :path "src/main/scratchpad/sessions/session_1_bonus.cljs"
           :ns "scratchpad.sessions.session-1-bonus"}})

(defn compiled-lookup
  "Finds a homework function (or value) in the compiled code. Each branch
  reads the var when it's called, so a function re-defined at the REPL shows
  up the next time the page is drawn."
  [file sym]
  (case file
    :s1 (case sym
          example-gene s1/example-gene
          gene-viewer s1/gene-viewer
          count-bases s1/count-bases
          gc-content s1/gc-content
          gc-calculator s1/gc-calculator
          example-genes s1/example-genes
          resistance-gene? s1/resistance-gene?
          gene-list-with-markers s1/gene-list-with-markers
          nil)
    :bonus (case sym
             click-counter bonus/click-counter
             theme-switcher bonus/theme-switcher
             example-images bonus/example-images
             image-gallery bonus/image-gallery
             example-samples bonus/example-samples
             sample-table bonus/sample-table
             nil)
    nil))

(defn- missing [file sym]
  (str "Can't find " sym " in " (get-in files [file :name])
       " - has it been renamed or deleted?"))

(defn- show-component
  "Draws the homework component `sym` with `args`."
  [lookup file sym & args]
  (let [f (lookup file sym)]
    (if (fn? f)
      (into [f] args)
      [c/callout {:kind :warning} (missing file sym)])))

(defn- show-call
  "Shows a call like (count-bases \"ATG\") together with its result."
  [lookup file sym & args]
  [ui/call-result
   (pr-str (cons sym args))
   (fn []
     (let [f (lookup file sym)]
       (if (fn? f)
         (apply f args)
         (throw (ex-info (missing file sym) {})))))])

(defn page
  "Options:
     :lookup       how to find the homework functions (default: compiled-lookup)
     :in-browser?  true in the in-browser version, where the files are edited on the page"
  ([] (page {}))
  ([{:keys [lookup in-browser?] :or {lookup compiled-lookup}}]
   (let [edit (fn [file what]
                (if in-browser?
                  (str what " in " (get-in files [file :name]) " (on the left)")
                  (str what " in " (get-in files [file :path]))))]
     [c/page
      [c/heading {:level 1} "Session 1: Homework"]
      (if in-browser?
        [c/callout
         [:strong "How this works: "]
         "each panel shows the live result of a function you write in the files on "
         "the left. Work through the TODOs, try things out in the "
         [:code "(comment ...)"] " blocks with Alt+Enter, and press Ctrl+S to load "
         "the file and update these panels."]
        [c/callout
         [:strong "How this page works: "]
         "each panel below shows the live result of a function you write. Open "
         [:code (get-in files [:s1 :path])] ", load it into the REPL (Ctrl+Alt+C Enter), "
         "and work through the TODOs. Try things out in the " [:code "(comment ...)"]
         " blocks with Alt+Enter, and save the file to see this page update."])

      [c/section {:title "Genomics mini projects"}
       [ui/exercise {:title "1. Gene Viewer"
                     :task "Create a function that takes a map like {:gene-name \"KPC-3\" :start 21331 :end 22598} and displays gene info using styled HTML components."
                     :edit (edit :s1 "gene-viewer")}
        [show-component lookup :s1 'gene-viewer (lookup :s1 'example-gene)]]

       [ui/exercise {:title "2. Sequence Analyzer"
                     :task "Write a function that takes a string of nucleotides (e.g. \"ATGCGTACG\") and counts how many times each base appears."
                     :edit (edit :s1 "count-bases")}
        [c/column {:gap "8px"}
         [show-call lookup :s1 'count-bases "ATGCGTACG"]
         [show-call lookup :s1 'count-bases "AAAA"]
         [show-call lookup :s1 'count-bases ""]]]

       [ui/exercise {:title "3. GC Content Calculator"
                     :task "Create a small input field where users can paste a DNA sequence, and display its GC content when they click a button."
                     :edit (edit :s1 "gc-content and gc-calculator")}
        [c/column {:gap "8px"}
         [show-call lookup :s1 'gc-content "ATGCGTACG"]
         [show-component lookup :s1 'gc-calculator]]]

       [ui/exercise {:title "4. Antimicrobial Resistance Marker Tag"
                     :task "Given a list of genes, display markers beside known resistance genes from a hardcoded list (e.g. \"blaKPC\", \"mecA\")."
                     :edit (edit :s1 "resistance-gene? and gene-list-with-markers")}
        [c/column {:gap "8px"}
         [show-call lookup :s1 'resistance-gene? "mecA"]
         [show-call lookup :s1 'resistance-gene? "dnaA"]
         [show-component lookup :s1 'gene-list-with-markers (lookup :s1 'example-genes)]]]]

      [c/section {:title "Bonus: general ClojureScript challenges"}
       [ui/exercise {:title "1. Click Counter"
                     :task "Replace the alert button with a dynamic counter that updates on click."
                     :edit (edit :bonus "click-counter")}
        [show-component lookup :bonus 'click-counter]]

       [ui/exercise {:title "2. Theme Switcher"
                     :task "Add a light/dark mode toggle by changing background and font styles using a button."
                     :edit (edit :bonus "theme-switcher")}
        [show-component lookup :bonus 'theme-switcher]]

       [ui/exercise {:title "3. Image Gallery"
                     :task "Display multiple images (e.g. microscopic images, DNA gels) with titles and descriptions."
                     :edit (edit :bonus "image-gallery")}
        [show-component lookup :bonus 'image-gallery (lookup :bonus 'example-images)]]

       [ui/exercise {:title "4. Interactive Table"
                     :task "Use map to generate a table from a vector of sample metadata like sample ID, date, and status."
                     :edit (edit :bonus "sample-table")}
        [show-component lookup :bonus 'sample-table (lookup :bonus 'example-samples)]]]])))
