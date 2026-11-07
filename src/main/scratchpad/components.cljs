(ns scratchpad.components
  "A small library of ready-made components.

  Every component works like a plain HTML element in hiccup: an optional
  map of options first, then any children.

      [c/card {:title \"Hello\"}
       [c/paragraph \"Some text\"]]

  Most components also accept a `:style` option, which is merged into
  their default styles, so you can tweak how they look.

  Browse them all (with copy-able examples) on the Components page.

  To see the HTML any hiccup turns into, use `c/print-html` (or `c/html`
  for a string) at the REPL:

      (c/print-html [c/card {:title \"Hello\"} \"Some text\"])
  To use them in your own file, add this to your `ns` form:

      (:require [scratchpad.components :as c])"
  (:require [clojure.string :as str]
            [reagent.dom.server :as rdom-server]
            [scratchpad.state :refer [db]]))

(defn- split-props
  "Split a component's arguments into [options-map children]."
  [args]
  (if (map? (first args))
    [(first args) (rest args)]
    [{} args]))


;; ---------------------------------------------------------------------------
;; Layout
;; ---------------------------------------------------------------------------

(defn page
  "A centered container with a maximum width, for the main page content.

  Options: `:max-width` (default \"960px\"), `:style`"
  [& args]
  (let [[{:keys [max-width style] :or {max-width "960px"}} children] (split-props args)]
    (into [:main {:style (merge {:max-width max-width
                                 :margin "0 auto"
                                 :padding "16px"}
                                style)}]
          children)))

(defn row
  "Lays out its children side by side, wrapping onto new lines if needed.

  Options: `:gap` (default \"16px\"), `:align` (e.g. \"center\"), `:style`"
  [& args]
  (let [[{:keys [gap align style] :or {gap "16px"}} children] (split-props args)]
    (into [:div {:style (merge {:display "flex"
                                :flex-wrap "wrap"
                                :gap gap
                                :align-items align}
                               style)}]
          children)))

(defn column
  "Stacks its children on top of each other.

  Options: `:gap` (default \"16px\"), `:style`"
  [& args]
  (let [[{:keys [gap style] :or {gap "16px"}} children] (split-props args)]
    (into [:div {:style (merge {:display "flex"
                                :flex-direction "column"
                                :gap gap}
                               style)}]
          children)))

(defn grid
  "Arranges its children in a grid of equal-width columns.

  Options: `:columns` (default 3), `:gap` (default \"16px\"), `:style`"
  [& args]
  (let [[{:keys [columns gap style] :or {columns 3 gap "16px"}} children] (split-props args)]
    (into [:div {:style (merge {:display "grid"
                                ;; minmax(0, 1fr) rather than 1fr, so wide content (like long code)
                                ;; scrolls inside its cell instead of stretching the grid
                                :grid-template-columns (str "repeat(" columns ", minmax(0, 1fr))")
                                :gap gap}
                               style)}]
          children)))

(defn card
  "A bordered box, with an optional title.

  Options: `:title`, `:style`"
  [& args]
  (let [[{:keys [title style]} children] (split-props args)]
    [:div {:style (merge {:border "1px solid #d0d7de"
                          :border-radius "8px"
                          :padding "16px"
                          :background-color "white"}
                         style)}
     (when title
       [:h3 {:style {:margin "0 0 8px 0"}} title])
     (into [:<>] children)]))

(defn section
  "A titled section of a page.

  Options: `:title`, `:style`"
  [& args]
  (let [[{:keys [title style]} children] (split-props args)]
    [:section {:style (merge {:margin "24px 0"} style)}
     (when title
       [:h2 {:style {:border-bottom "2px solid #004a87"
                     :padding-bottom "4px"}}
        title])
     (into [:<>] children)]))

(defn divider
  "A horizontal line."
  []
  [:hr {:style {:border "none"
                :border-top "1px solid #d0d7de"
                :margin "16px 0"}}])

(defn footer
  "A footer for the bottom of the page.

  Options: `:style`"
  [& args]
  (let [[{:keys [style]} children] (split-props args)]
    (into [:footer {:style (merge {:margin-top "32px"
                                   :padding "16px"
                                   :border-top "1px solid #d0d7de"
                                   :color "#57606a"
                                   :font-size "14px"
                                   :text-align "center"}
                                  style)}]
          children)))


;; ---------------------------------------------------------------------------
;; Content
;; ---------------------------------------------------------------------------

(defn header
  "A page header with a title on the left and a logo on the right.

  Options: `:title`, `:title-color`, `:height-px`, `:logo-src`, `:style`"
  [& args]
  (let [[{:keys [title title-color height-px logo-src style]
          :or {title "Lab Website"
               title-color "#004a87"
               height-px 48
               logo-src "images/logo-dna.svg"}}
         _children] (split-props args)]
    [:header {:style (merge {:display "flex"
                             :justify-content "space-between"
                             :align-items "center"
                             :padding "8px 16px"}
                            style)}
     [:h1 {:style {:color title-color :margin "0px"}}
      title]
     [:img {:src logo-src
            :alt "logo"
            :style {:height (str height-px "px")}}]]))

(defn heading
  "A heading. `:level` 1 is the biggest, 4 the smallest.

  Options: `:level` (default 2), `:color`, `:style`"
  [& args]
  (let [[{:keys [level color style] :or {level 2}} children] (split-props args)
        tag (get {1 :h1 2 :h2 3 :h3 4 :h4} level :h2)]
    (into [tag {:style (merge {:color color} style)}]
          children)))

(defn paragraph
  "A paragraph of text.

  Options: `:style`"
  [& args]
  (let [[{:keys [style]} children] (split-props args)]
    (into [:p {:style (merge {:line-height "1.5"} style)}]
          children)))

(defn image
  "An image, with an optional caption underneath.

  Options: `:src`, `:alt`, `:width`, `:height`, `:caption`, `:style`"
  [& args]
  (let [[{:keys [src alt width height caption style]} _children] (split-props args)]
    [:figure {:style (merge {:margin "0"} style)}
     [:img {:src src
            :alt (or alt caption "")
            :style {:width width
                    :height height
                    :max-width "100%"
                    :border-radius "4px"}}]
     (when caption
       [:figcaption {:style {:font-size "14px" :color "#57606a"}}
        caption])]))

(defn link
  "A link. Links to other websites open in a new tab.

  Options: `:href`, `:style`"
  [& args]
  (let [[{:keys [href style]} children] (split-props args)
        external? (str/starts-with? (str href) "http")]
    (into [:a {:href href
               :target (when external? "_blank")
               :rel (when external? "noopener noreferrer")
               :style (merge {:color "#0969da"} style)}]
          children)))

(defn bullet-list
  "A bulleted list. Each child becomes one item in the list.

  Options: `:numbered?` (true for a numbered list), `:style`"
  [& args]
  (let [[{:keys [numbered? style]} children] (split-props args)]
    (into [(if numbered? :ol :ul) {:style style}]
          (map-indexed (fn [i child] ^{:key i} [:li child])
                       children))))

(def ^:private callout-colors
  {:info    {:background-color "#ddf4ff" :border-color "#54aeff"}
   :success {:background-color "#dafbe1" :border-color "#4ac26b"}
   :warning {:background-color "#fff8c5" :border-color "#d4a72c"}
   :error   {:background-color "#ffebe9" :border-color "#ff8182"}})

(defn callout
  "A colored box to make a message stand out.

  Options: `:kind` (one of :info :success :warning :error, default :info), `:style`"
  [& args]
  (let [[{:keys [kind style] :or {kind :info}} children] (split-props args)]
    (into [:div {:style (merge {:padding "12px 16px"
                                :border-left "4px solid"
                                :border-radius "4px"}
                               (get callout-colors kind (:info callout-colors))
                               style)}]
          children)))

(defn code-block
  "Shows code (or any text) in a fixed-width font, keeping its line breaks.

  Options: `:style`"
  [& args]
  (let [[{:keys [style]} children] (split-props args)]
    [:pre {:style (merge {:background-color "#f6f8fa"
                          :padding "12px"
                          :border-radius "6px"
                          :overflow-x "auto"
                          :font-size "13px"
                          :margin "0"}
                         style)}
     (into [:code] children)]))


;; ---------------------------------------------------------------------------
;; Controls (things you can click or type into)
;; ---------------------------------------------------------------------------

(defn button
  "A button. `:on-click` is a function called when the button is clicked.

  Options: `:on-click`, `:variant` (:primary or :secondary), `:disabled?`, `:style`"
  [& args]
  (let [[{:keys [on-click variant disabled? style] :or {variant :primary}} children] (split-props args)]
    (into [:button {:on-click on-click
                    :disabled disabled?
                    :style (merge {:padding "8px 16px"
                                   :border-radius "6px"
                                   :font-size "14px"
                                   :cursor (if disabled? "not-allowed" "pointer")
                                   :opacity (when disabled? 0.5)}
                                  (if (= variant :secondary)
                                    {:background-color "white"
                                     :color "#004a87"
                                     :border "1px solid #004a87"}
                                    {:background-color "#004a87"
                                     :color "white"
                                     :border "1px solid #004a87"})
                                  style)}]
          children)))

(defn text-input
  "A one-line text box.

  `:on-change` is called with the new text every time it changes, so you
  can store it in an atom: `:on-change (fn [text] (reset! my-atom text))`

  Options: `:value`, `:on-change`, `:placeholder`, `:style`"
  [& args]
  (let [[{:keys [value on-change placeholder style]} _children] (split-props args)]
    [:input {:type "text"
             :value (or value "")
             :placeholder placeholder
             :on-change (fn [event] (when on-change (on-change (.. event -target -value))))
             :style (merge {:padding "8px"
                            :font-size "14px"
                            :border "1px solid #d0d7de"
                            :border-radius "6px"}
                           style)}]))

(defn text-area
  "A multi-line text box. Works just like `text-input`.

  Options: `:value`, `:on-change`, `:placeholder`, `:rows` (default 4), `:style`"
  [& args]
  (let [[{:keys [value on-change placeholder rows style] :or {rows 4}} _children] (split-props args)]
    [:textarea {:value (or value "")
                :placeholder placeholder
                :rows rows
                :on-change (fn [event] (when on-change (on-change (.. event -target -value))))
                :style (merge {:padding "8px"
                               :font-size "14px"
                               :font-family "monospace"
                               :border "1px solid #d0d7de"
                               :border-radius "6px"
                               :width "100%"
                               :box-sizing "border-box"}
                              style)}]))

(defn checkbox
  "A checkbox with a label.

  `:on-change` is called with true or false when it's clicked.

  Options: `:checked?`, `:on-change`, `:style`"
  [& args]
  (let [[{:keys [checked? on-change style]} children] (split-props args)]
    [:label {:style (merge {:display "inline-flex"
                            :align-items "center"
                            :gap "6px"
                            :cursor "pointer"}
                           style)}
     [:input {:type "checkbox"
              :checked (boolean checked?)
              :on-change (fn [event] (when on-change (on-change (.. event -target -checked))))}]
     (into [:<>] children)]))


;; ---------------------------------------------------------------------------
;; Data & genomics
;; ---------------------------------------------------------------------------

(defn tag
  "A small colored label, like a sticker.

  Options: `:color` (background color, default light grey), `:style`"
  [& args]
  (let [[{:keys [color style] :or {color "#eaeef2"}} children] (split-props args)]
    (into [:span {:style (merge {:display "inline-block"
                                 :padding "2px 8px"
                                 :border-radius "999px"
                                 :background-color color
                                 :font-size "12px"
                                 :font-weight "bold"}
                                style)}]
          children)))

(defn stat
  "Shows one number, big, with a label.

  Options: `:label`, `:value`, `:unit`, `:style`"
  [& args]
  (let [[{:keys [label value unit style]} _children] (split-props args)]
    [:div {:style (merge {:display "inline-block"
                          :padding "8px 16px"
                          :border "1px solid #d0d7de"
                          :border-radius "8px"}
                         style)}
     [:div {:style {:font-size "12px" :color "#57606a" :text-transform "uppercase"}}
      label]
     [:div {:style {:font-size "28px" :font-weight "bold"}}
      value
      (when unit
        [:span {:style {:font-size "16px" :margin-left "4px"}} unit])]]))

(defn label-value
  "A label and a value on one line, e.g. \"Start: 21331\"."
  [label value]
  [:div {:style {:margin "4px 0"}}
   [:span {:style {:color "#57606a" :margin-right "8px"}} label ":"]
   [:span {:style {:font-weight "bold"}} value]])

(def ^:private base-colors
  {"A" "#2da44e" "T" "#cf222e" "G" "#bf8700" "C" "#0969da"})

(defn dna-sequence
  "Shows a DNA sequence in a fixed-width font, with each base colored."
  [sequence]
  [:div {:style {:font-family "monospace"
                 :font-size "16px"
                 :word-break "break-all"
                 :letter-spacing "1px"}}
   (map-indexed (fn [i base]
                  ^{:key i}
                  [:span {:style {:color (get base-colors (str/upper-case base) "#57606a")}}
                   base])
                sequence)])

(defn region-bar
  "Draws where a region (like a gene) sits inside a longer sequence.

  Options: `:start`, `:end`, `:total` (length of the whole sequence),
  `:color`, `:style`"
  [& args]
  (let [[{:keys [start end total color style] :or {color "#004a87"}} _children] (split-props args)
        percent (fn [n] (str (* 100 (/ n total)) "%"))]
    [:div {:style (merge {:position "relative"
                          :height "16px"
                          :background-color "#eaeef2"
                          :border-radius "4px"}
                         style)}
     [:div {:style {:position "absolute"
                    :left (percent start)
                    :width (percent (- end start))
                    :height "100%"
                    :background-color color
                    :border-radius "4px"}}]]))


;; ---------------------------------------------------------------------------
;; Tools: see the HTML behind your hiccup
;; ---------------------------------------------------------------------------

(def ^:private void-elements
  "Elements that never have children or a closing tag."
  #{"area" "base" "br" "col" "embed" "hr" "img" "input" "link" "meta" "source" "track" "wbr"})

(defn- escape-html [s]
  (-> s
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- format-node
  "Turn a DOM node into indented HTML lines."
  [node depth]
  (let [indent (apply str (repeat depth "  "))]
    (case (.-nodeType node)
      ;; text: skip whitespace-only text between tags
      3 (let [text (str/trim (.-textContent node))]
          (when (seq text)
            [(str indent (escape-html text))]))
      ;; element
      1 (let [tag (str/lower-case (.-tagName node))
              attrs (apply str (for [attr (array-seq (.-attributes node))]
                                 (str " " (.-name attr) "=\"" (str/replace (.-value attr) "\"" "&quot;") "\"")))
              open (str indent "<" tag attrs ">")
              close (str "</" tag ">")
              children (array-seq (.-childNodes node))]
          (cond
            (contains? void-elements tag)
            [open]

            ;; only text inside (or nothing): keep it on one line, e.g. <h3>KPC-3</h3>
            (every? #(= 3 (.-nodeType %)) children)
            [(str open (escape-html (str/trim (.-textContent node))) close)]

            :else
            (concat [open]
                    (mapcat #(format-node % (inc depth)) children)
                    [(str indent close)])))
      ;; anything else (e.g. comments): skip it
      nil)))

(defn html
  "Returns the HTML for some hiccup, as a nicely indented string.

      (c/html [c/card {:title \"Hi\"} \"There\"])

  Works with plain hiccup like [:p \"text\"] and with components."
  [hiccup]
  (let [markup (rdom-server/render-to-static-markup hiccup)
        template (.createElement js/document "template")]
    (set! (.-innerHTML template) markup)
    ;; React adds <link rel="preload"> tags for images to speed up page
    ;; loads. They're not part of your component, so leave them out.
    (doseq [preload (array-seq (.querySelectorAll (.-content template) "link[rel=preload]"))]
      (.remove preload))
    (str/join "\n" (mapcat #(format-node % 0)
                           (array-seq (.. template -content -childNodes))))))

(defn print-html
  "Prints the HTML for some hiccup. Easier to read at the REPL than `html`,
  since the line breaks show up properly. The output appears in Calva's
  REPL output window.

      (c/print-html [c/card {:title \"Hi\"} \"There\"])"
  [hiccup]
  (println (html hiccup)))


;; ---------------------------------------------------------------------------
;; Misc
;; ---------------------------------------------------------------------------

(defn todo
  "A placeholder for something that hasn't been written yet."
  [what]
  [:div {:style {:padding "12px"
                 :border "2px dashed #d0d7de"
                 :border-radius "6px"
                 :color "#57606a"}}
   "🚧 Not done yet: " what])

(defn config-view
  "Read-only view of the config loaded from config.json."
  []
  [:div
   [:textarea {:style {:font-family "monospace"}
               :read-only true
               :value (str (:config @db))}]])
