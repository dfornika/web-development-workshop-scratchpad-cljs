(ns scratchpad.views.gallery
  "The Components page: every component in scratchpad.components, with an
  example you can copy."
  (:require-macros [scratchpad.macros :refer [example]])
  (:require [reagent.core :as r]
            [scratchpad.components :as c]
            [scratchpad.ui :as ui]))

(defn- say-hi []
  (js/alert "Hi!"))

(def sections
  [{:title "Layout"
    :examples
    [(example "card" "A bordered box, with an optional title."
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

(defn- copy-button [code]
  (r/with-let [copied? (r/atom false)]
    [c/button {:variant :secondary
               :style {:padding "2px 8px" :font-size "12px"}
               :on-click (fn []
                           (.writeText js/navigator.clipboard code)
                           (reset! copied? true)
                           (js/setTimeout #(reset! copied? false) 1500))}
     (if @copied? "Copied!" "Copy")]))

(defn- small-label [text]
  [:div {:style {:font-size "12px" :color "#57606a" :margin "8px 0 4px 0"}} text])

(defn- example-card [{:keys [title description code view]}]
  (r/with-let [show-html? (r/atom false)]
    [c/card
     [c/row {:align "center" :style {:justify-content "space-between"}}
      [:code {:style {:font-size "16px" :font-weight "bold"}} title]
      [c/row {:gap "4px"}
       [c/button {:variant :secondary
                  :style {:padding "2px 8px" :font-size "12px"}
                  :on-click (fn [] (swap! show-html? not))}
        (if @show-html? "Hide HTML" "Show HTML")]
       [copy-button code]]]
     [c/paragraph {:style {:color "#57606a" :margin "4px 0 12px 0"}} description]
     [:div {:style {:padding "12px"
                    :margin-bottom "12px"
                    :border "1px dashed #d0d7de"
                    :border-radius "6px"}}
      [ui/error-boundary view]]
     (if @show-html?
       [:<>
        [small-label "Hiccup (ClojureScript)"]
        [c/code-block code]
        [small-label "HTML it turns into"]
        [c/code-block (c/html view)]]
       [c/code-block code])]))

(defn page []
  [c/page {:max-width "1200px"}
   [c/heading {:level 1} "Components"]
   [c/paragraph
    "Ready-made pieces for your pages. Copy an example into your own file "
    "(like " [:code "playground.cljs"] "). It needs "
    [:code "[scratchpad.components :as c]"] " in the " [:code ":require"]
    " part of the " [:code "ns"] " form at the top - playground.cljs already has it."]
   [c/paragraph
    "Want to see how one works, or change it? They're all in "
    [:code "src/main/scratchpad/components.cljs"] ". Click " [:strong "Show HTML"]
    " to see the HTML an example turns into - or try "
    [:code "(c/print-html [c/card \"Hi\"])"] " at the REPL."]
   (for [{:keys [title examples]} sections]
     ^{:key title}
     [c/section {:title title}
      (into [c/grid {:columns 2}]
            (for [ex examples]
              ^{:key (:title ex)} [example-card ex]))])])
