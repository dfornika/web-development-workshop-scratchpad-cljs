(ns scratchpad.views.reference
  "Building blocks for the reference pages (ClojureScript Basics, JS Interop):
  syntax-colored code, examples you can edit and run, simple tables, and
  the page layout with its table of contents.

  The examples are run by SCI (the interpreter the in-browser version
  uses), so what's on the page is exactly the code that ran."
  (:require [cljs.pprint :refer [pprint]]
            [clojure.string :as str]
            [reagent.core :as r]
            [scratchpad.components :as c]
            [scratchpad.ui :as ui]
            [scratchpad.web.editor :as editor]
            [scratchpad.web.sci :as sci]))

(defn snippet
  "Syntax-colored code."
  [text]
  ^{:key text}
  [editor/editor {:initial-doc text
                  :read-only? true
                  :line-numbers? false
                  :font-size "13px"
                  :style {:margin "8px 0"
                          :border "1px solid #d0d7de"
                          :border-radius "6px"
                          :overflow "hidden"}}])

(defn- show-value
  "A value as text, spread over several lines if it's long."
  [value]
  ;; *print-length* keeps endless sequences, like (range), from printing forever
  (binding [*print-length* 50]
    (let [one-line (pr-str value)]
      (cond
        (<= (count one-line) 70)
        one-line

        ;; a long list of plain values, like (range): wrap it like text
        (and (sequential? value) (not-any? coll? (take 50 value)))
        (->> (str/split one-line #" ")
             (reduce (fn [lines word]
                       (let [line (peek lines)]
                         (if (and line (<= (+ (count line) 1 (count word)) 70))
                           (conj (pop lines) (str line " " word))
                           (conj lines word))))
                     [])
             (str/join "\n;;    "))

        ;; anything else: pretty-printed, lined up after the ";; => "
        :else
        (str/replace (str/trimr (with-out-str (pprint value))) "\n" "\n;;    ")))))

(defn- friendly-error [message]
  (str message
       (cond
         (re-find #"EOF while reading" message) " - is a closing bracket missing?"
         (re-find #"Unmatched delimiter" message) " - is there an extra closing bracket?"
         :else "")))

(defn- run-code
  "Run `code` in the SCI namespace `ns`: {:ok? true :text \"...\"} or
  {:ok? false :text \"error message\"}."
  [code ns]
  (let [{:keys [ok? value error]} (sci/eval-value code ns)]
    (if ok?
      {:ok? true :text (show-value value)}
      {:ok? false :text (friendly-error error)})))

(defn- small-button [opts label]
  [c/button (merge {:variant :secondary
                    :style {:padding "1px 8px" :font-size "12px"}}
                   opts)
   label])

(defn example
  "An example you can edit and run: `code` in an editor, with the result of
  running it underneath. `ns` is a delay holding the SCI namespace to run it
  in (made with sci/create-ns!), so pages can set up data for their examples.

  Ctrl+Enter, Alt+Enter or the Run button runs it - not every keystroke,
  since half-typed code like (range) or (js/alert ...) could misbehave."
  [ns code]
  (r/with-let [result (r/atom (run-code code @ns))
               edited? (r/atom false)   ; different from the original?
               stale? (r/atom false)    ; changed since it was last run?
               view-ref (atom nil)
               current-text #(or (some-> ^js @view-ref .-state .-doc .toString) code)
               run! (fn []
                      (reset! result (run-code (current-text) @ns))
                      (reset! stale? false))
               reset-example! (fn []
                                (some-> @view-ref (editor/set-doc! code))
                                (reset! result (run-code code @ns))
                                (reset! edited? false)
                                (reset! stale? false))]
    (let [{:keys [ok? text]} @result]
      [:div {:style {:margin "8px 0"}}
       [editor/editor {:initial-doc code
                       :view-ref view-ref
                       :line-numbers? false
                       :font-size "13px"
                       :on-change (fn [new-text]
                                    (reset! edited? (not= new-text code))
                                    (reset! stale? true))
                       :on-eval (fn [_] (run!))
                       :on-load (fn [_] (run!))
                       :style {:border "1px solid #d0d7de"
                               :border-radius "6px 6px 0 0"}}]
       [:div {:style {:display "flex"
                      :align-items "flex-start"
                      :gap "8px"
                      :padding "6px 8px 6px 10px"
                      :border "1px solid #d0d7de"
                      :border-top "none"
                      :border-radius "0 0 6px 6px"
                      :background-color "#f6f8fa"
                      :font-family "ui-monospace, Menlo, Consolas, monospace"
                      :font-size "13px"}}
        [:pre {:style {:flex "1 1 auto"
                       :margin 0
                       :white-space "pre-wrap"
                       :overflow-wrap "anywhere"
                       :font-family "inherit"
                       :color (if ok? "#57606a" "#cf222e")
                       :opacity (if @stale? 0.5 1)}}
         (if ok? (str ";; => " text) (str ";; Error: " text))]
        (when @stale?
          [:span {:style {:color "#57606a" :font-family "Arial, sans-serif" :font-size "12px"
                          :white-space "nowrap" :padding-top "2px"}}
           "Ctrl+Enter to run"])
        (when @edited?
          [small-button {:on-click reset-example!} "Reset"])
        [small-button {:variant :primary :on-click run!} "▶ Run"]]])))

(def ^:private cell {:padding "6px 12px"
                     :border-bottom "1px solid #d0d7de"
                     :text-align "left"
                     :vertical-align "top"})

(defn table
  "A simple table: a list of `headers`, and `rows` (each a list of cells)."
  [headers rows]
  [:div {:style {:overflow-x "auto"}}
   [:table {:style {:border-collapse "collapse" :width "100%" :background-color "white"}}
    [:thead
     (into [:tr] (for [h headers] ^{:key h} [:th {:style (merge cell {:background-color "#f6f8fa"})} h]))]
    [:tbody
     (for [[i row] (map-indexed vector rows)]
       ;; with-meta, because ^{:key ...} on an (into ...) wouldn't reach the vector
       (with-meta (into [:tr] (for [value row] [:td {:style cell} value]))
         {:key i}))]]])

(defn mono
  "Inline code that doesn't break across lines."
  [text]
  [:code {:style {:white-space "nowrap"}} text])

(defn page
  "A reference page: a heading, an intro, and `sections` - each a map of
  {:title \"...\" :content component} - with a table of contents. `id` is
  used to make the DOM ids of the sections."
  [{:keys [id title intro sections]}]
  (let [section-id #(ui/anchor-id id %)]
    [c/page {:max-width "1200px"}
     [ui/with-table-of-contents {:top-id (str id "-top")
                                 :entries (for [s sections]
                                            {:id (section-id (:title s))
                                             :label (:title s)
                                             :level 1})}
      [c/heading {:level 1} title]
      (into [c/callout] intro)
      (for [{:keys [title content]} sections]
        ^{:key title}
        [ui/anchor (section-id title)
         [c/section {:title title}
          [content]]])]]))
