(ns scratchpad.views.gallery
  "The Components page: every component in scratchpad.components, with an
  example you can copy."
  (:require-macros [scratchpad.macros :refer [example]])
  (:require [reagent.core :as r]
            [scratchpad.components :as c]
            [scratchpad.ui :as ui]
            [shadow.resource :as rc]))

(defn- say-hi []
  (js/alert "Hi!"))

(def sections
  [{:title "Layout"
    :examples
    [(example "page" "Centers a page's main content, with a maximum width. Usually the outermost component."
              [c/page {:max-width "240px"
                       :style {:background-color "#e6f0fa"}}
               [c/paragraph "Centered, and never wider than 240px."]])
     (example "card" "A bordered box, with an optional title."
              [c/card {:title "KPC-3"}
               [c/paragraph "A carbapenemase gene."]])
     (example "row" "Puts things side by side."
              [c/row {:gap "8px"}
               [c/card "One"]
               [c/card "Two"]
               [c/card "Three"]])
     (example "column" "Stacks things on top of each other."
              [c/column {:gap "8px"}
               [c/card "Top"]
               [c/card "Bottom"]])
     (example "grid" "A grid of equal-width columns."
              [c/grid {:columns 2 :gap "8px"}
               [c/card "A"] [c/card "B"]
               [c/card "C"] [c/card "D"]])
     (example "section" "A titled part of a page."
              [c/section {:title "Results"}
               [c/paragraph "Everything looks good."]])
     (example "divider" "A horizontal line."
              [:div
               [c/paragraph "Above"]
               [c/divider]
               [c/paragraph "Below"]])
     (example "footer" "Goes at the bottom of the page."
              [c/footer "Made with ClojureScript"])]}

   {:title "Content"
    :examples
    [(example "header" "A title and a logo."
              [c/header {:title "My Lab"
                         :title-color "darkgreen"
                         :height-px 32}])
     (example "heading" "A heading - :level 1 is biggest."
              [c/heading {:level 3 :color "#004a87"}
               "Sequencing results"])
     (example "paragraph" "A paragraph of text."
              [c/paragraph {:style {:font-size "18px"}}
               "Welcome to our learning app!"])
     (example "image" "An image, with an optional caption."
              [c/image {:src "images/gel.svg"
                        :width "240px"
                        :caption "Gel electrophoresis"}])
     (example "link" "Links to other sites open in a new tab."
              [c/link {:href "https://reagent-project.github.io/"}
               "Reagent docs"])
     (example "bullet-list" "Each child becomes a list item."
              [c/bullet-list
               "Extract DNA"
               "Sequence"
               "Analyze"])
     (example "callout" "A colored box for messages."
              [c/callout {:kind :warning}
               "Low coverage on 2 samples"])
     (example "code-block" "Shows code or text as-is."
              [c/code-block "(+ 1 2 3)"])]}

   {:title "Controls"
    :examples
    [(example "button" "Runs a function when clicked."
              [c/row
               [c/button {:on-click say-hi} "Say hi"]
               [c/button {:variant :secondary} "Secondary"]
               [c/button {:disabled? true} "Disabled"]])
     (example "text-input" "A one-line text box. Type in this one!"
              [(fn []
                 (r/with-let [text (r/atom "")]
                   [:div
                    [c/text-input {:value @text
                                   :on-change (fn [new-text] (reset! text new-text))
                                   :placeholder "Type here..."}]
                    [c/paragraph "You typed: " @text]]))])
     (example "text-area" "A multi-line text box. Works like text-input."
              [c/text-area {:placeholder "Paste a sequence..."
                            :rows 3}])
     (example "checkbox" "Calls :on-change with true or false."
              [(fn []
                 (r/with-let [checked? (r/atom false)]
                   [c/checkbox {:checked? @checked?
                                :on-change (fn [value] (reset! checked? value))}
                    (if @checked? "Checked!" "Not checked")]))])]}

   {:title "Data & genomics"
    :examples
    [(example "tag" "A small colored label."
              [c/row {:gap "4px"}
               [c/tag "neutral"]
               [c/tag {:color "#ffcecb"} "resistant"]
               [c/tag {:color "#aceebb"} "susceptible"]])
     (example "stat" "One number, big."
              [c/stat {:label "Mean coverage"
                       :value 87.4
                       :unit "x"}])
     (example "label-value" "A label and a value."
              [:div
               [c/label-value "Start" 21331]
               [c/label-value "End" 22598]])
     (example "dna-sequence" "A colored DNA sequence."
              [c/dna-sequence "ATGCGTACGTTAGC"])
     (example "region-bar" "Where a region sits in a longer sequence."
              [c/region-bar {:start 21331
                             :end 22598
                             :total 50000}])]}])

;; ---------------------------------------------------------------------------
;; Component source code, for the Source tab
;; ---------------------------------------------------------------------------

;; The text of components.cljs, embedded at build time. shadow-cljs watches
;; the file, so the Source tab updates when a component is changed.
(def ^:private components-source (rc/inline "scratchpad/components.cljs"))

(defn- top-level-forms
  "Split ClojureScript source text into its top-level (...) forms, as
  strings. Brackets inside strings, comments and character literals like
  \\( don't count."
  [text]
  (loop [i 0, depth 0, start nil, in-string? false, forms []]
    (if (>= i (count text))
      forms
      (let [ch (nth text i)]
        (cond
          in-string?
          (case ch
            "\\" (recur (+ i 2) depth start true forms)
            "\"" (recur (inc i) depth start false forms)
            (recur (inc i) depth start true forms))

          (= ch "\"") (recur (inc i) depth start true forms)
          ;; character literal, e.g. \( - skip the next character
          (= ch "\\") (recur (+ i 2) depth start false forms)
          ;; comment: skip to the end of the line
          (= ch ";") (let [eol (.indexOf text "\n" i)]
                       (recur (if (neg? eol) (count text) eol) depth start false forms))

          (contains? #{"(" "[" "{"} ch)
          (recur (inc i) (inc depth) (if (zero? depth) i start) false forms)

          (contains? #{")" "]" "}"} ch)
          (let [depth (dec depth)]
            (if (and (zero? depth) start)
              (recur (inc i) 0 nil false (conj forms (subs text start (inc i))))
              (recur (inc i) depth start false forms)))

          :else (recur (inc i) depth start false forms))))))

(def ^:private component-sources
  "A map of component name -> the source of its (defn ...), e.g. \"card\"."
  (into {}
        (for [form (top-level-forms components-source)
              :let [[_ name] (re-find #"^\(defn\s+([^\s\[]+)" form)]
              :when name]
          [name form])))

;; ---------------------------------------------------------------------------
;; One example card
;; ---------------------------------------------------------------------------

(defn- copy-button [text]
  (r/with-let [copied? (r/atom false)]
    [c/button {:variant :secondary
               :style {:padding "2px 8px" :font-size "12px"}
               :on-click (fn []
                           (.writeText js/navigator.clipboard text)
                           (reset! copied? true)
                           (js/setTimeout #(reset! copied? false) 1500))}
     (if @copied? "Copied!" "Copy")]))

(def ^:private tab-list
  [[:usage "Usage" "How to use it - copy this into your own page."]
   [:source "Source" (str "How it's defined, in src/main/scratchpad/components.cljs. "
                          "(Most components start with split-props, which separates the "
                          "optional map of options from the children.)")]
   [:html "HTML" "The HTML that the usage example turns into."]])

(defn- tab-bar [current on-select copy-text]
  [:div {:style {:display "flex"
                 :align-items "flex-end"
                 :border-bottom "1px solid #d0d7de"
                 :margin-bottom "6px"}}
   (for [[id label] tab-list]
     ^{:key id}
     [:button {:on-click #(on-select id)
               :style {:border "none"
                       :background "none"
                       :padding "6px 12px"
                       :margin-bottom "-1px"
                       :cursor "pointer"
                       :font-size "13px"
                       :color (if (= id current) "#004a87" "#57606a")
                       :font-weight (if (= id current) "bold" "normal")
                       :border-bottom (if (= id current)
                                        "2px solid #004a87"
                                        "2px solid transparent")}}
      label])
   [:div {:style {:margin-left "auto" :padding-bottom "4px"}}
    [copy-button copy-text]]])

(defn- example-card [{:keys [title description code view]}]
  (r/with-let [tab (r/atom :usage)]
    (let [text (case @tab
                 :usage code
                 :source (get component-sources title
                              (str ";; Couldn't find (defn " title " ...) in components.cljs"))
                 :html (c/html view))]
      [c/card
       [:code {:style {:font-size "16px" :font-weight "bold"}} title]
       [c/paragraph {:style {:color "#57606a" :margin "4px 0 12px 0"}} description]
       [:div {:style {:padding "12px"
                      :margin-bottom "12px"
                      :border "1px dashed #d0d7de"
                      :border-radius "6px"}}
        [ui/error-boundary view]]
       [tab-bar @tab #(reset! tab %) text]
       [:div {:style {:font-size "12px" :color "#57606a" :margin-bottom "6px"}}
        (some (fn [[id _ caption]] (when (= id @tab) caption)) tab-list)]
       [c/code-block {:style {:max-height "360px" :overflow "auto"}}
        text]])))

(defn- anchor-id [title]
  (ui/anchor-id "gallery" title))

(def ^:private toc-entries
  (for [{:keys [title examples]} sections
        entry (cons {:id (anchor-id title) :label title :level 1}
                    (for [{ex-title :title} examples]
                      {:id (anchor-id ex-title)
                       :label [:code {:style {:font-size "13px"}} ex-title]
                       :level 2}))]
    entry))

(defn page []
  [c/page {:max-width "1400px"}
   [ui/with-table-of-contents {:top-id "gallery-top" :entries toc-entries}
    [c/heading {:level 1} "Components"]
    [c/paragraph
     "Ready-made pieces for your pages. Copy an example into your own file "
     "(like " [:code "playground.cljs"] "). It needs "
     [:code "[scratchpad.components :as c]"] " in the " [:code ":require"]
     " part of the " [:code "ns"] " form at the top - playground.cljs already has it."]
    [c/paragraph
     "Want to see how one works, or change it? They're all in "
     [:code "src/main/scratchpad/components.cljs"] ". Under each example, the "
     [:strong "Usage"] " tab shows how to use it, " [:strong "Source"]
     " shows how it's built, and " [:strong "HTML"] " shows the HTML it turns into "
     "(you can also try " [:code "(c/print-html [c/card \"Hi\"])"] " at the REPL)."]
    (for [{:keys [title examples]} sections]
      ^{:key title}
      [ui/anchor (anchor-id title)
       [c/section {:title title}
        (into [c/grid {:columns 2}]
              (for [ex examples]
                ^{:key (:title ex)}
                [ui/anchor (anchor-id (:title ex))
                 [example-card ex]]))]])]])
