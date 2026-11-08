(ns scratchpad.views.reference
  "Building blocks for the reference pages (ClojureScript Basics, JS Interop):
  syntax-colored code, examples you can edit and run, simple tables, and
  the page layout with its table of contents.

  The examples are run by SCI (the interpreter the in-browser version
  uses), so what's on the page is exactly the code that ran."
  (:require [cljs.core.async :as async]
            [cljs.core.async.impl.protocols :as async-protocols]
            [cljs.pprint :refer [pprint]]
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

(defn- format-value
  "A value as text, spread over several lines (each starting with
  `line-start`) if it's long."
  [value line-start]
  ;; *print-length* keeps endless sequences, like (range), from printing forever
  (binding [*print-length* (or *print-length* 50)]
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
             (str/join line-start))

        ;; anything else: pretty-printed
        :else
        (str/replace (str/trimr (with-out-str (pprint value))) "\n" line-start)))))

(defn show-value
  "A value as text for an example's result: long ones are spread over
  several lines, lined up after the \";; => \"."
  [value]
  (format-value value "\n;;    "))

(defn pretty
  "A value as text, pretty-printed over several lines if it's long."
  [value]
  (format-value value "\n"))

(defn- friendly-error [message]
  (str message
       (cond
         (re-find #"EOF while reading" message) " - is a closing bracket missing?"
         (re-find #"Unmatched delimiter" message) " - is there an extra closing bracket?"
         :else "")))

(defonce run-count
  ;; Goes up every time an example runs, so things that show what the
  ;; examples made (like a preview of a component) know to redraw.
  (r/atom 0))

(defn- channel? [v]
  (satisfies? async-protocols/ReadPort v))

(defn- promise? [v]
  (instance? js/Promise v))

(defn- run-code
  "Run `code` in the SCI namespace `ns`, putting what happens in the atom
  `result`: {:ok? true :text \"...\"} or {:ok? false :text \"error\"}.

  If the code gives a channel (from go, or http/get) or a promise (from
  js/fetch, or an ^:async function), what it delivers later is added."
  [code ns result]
  (let [{:keys [ok? value error]} (sci/eval-value code ns)
        ;; only the latest run may update the result
        token (js-obj)
        later! (fn [text ok?]
                 (when (identical? token (:token @result))
                   (swap! result assoc :ok? ok? :text text)))]
    (swap! run-count inc)
    (cond
      (not ok?)
      (reset! result {:ok? false :text (friendly-error error) :token token})

      (channel? value)
      (do (reset! result {:ok? true :text "a channel - waiting for a value..." :token token})
          (async/take! value
                       (fn [v]
                         (swap! run-count inc)
                         (later! (if (nil? v)
                                   "a channel, which closed without a value"
                                   (str "a channel, which gave:\n;;    " (show-value v)))
                                 true))))

      (promise? value)
      (do (reset! result {:ok? true :text "a promise - waiting for it to finish..." :token token})
          (.then ^js value
                 (fn [v]
                   (swap! run-count inc)
                   (later! (str "a promise, which gave:\n;;    " (show-value v)) true))
                 (fn [e]
                   (later! (str "a promise, which failed: " (or (ex-message e) e)) false))))

      :else
      (reset! result {:ok? true :text (show-value value) :token token}))))

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
  since half-typed code like (range) or (js/alert ...) could misbehave.

  Options:
    :run-at-start?  run it when the page is drawn (default true). Turn it
                    off for examples that do something, like fetching data,
                    so they only happen when asked."
  ([ns code] (example ns code {}))
  ([ns code {:keys [run-at-start?] :or {run-at-start? true}}]
   (r/with-let [result (r/atom (if run-at-start?
                                 nil
                                 {:ok? true :text "(not run yet - press Ctrl+Enter or Run)"}))
                _ (when run-at-start? (run-code code @ns result))
                edited? (r/atom false)   ; different from the original?
                stale? (r/atom false)    ; changed since it was last run?
                view-ref (atom nil)
                current-text #(or (some-> ^js @view-ref .-state .-doc .toString) code)
                run! (fn []
                       (run-code (current-text) @ns result)
                       (reset! stale? false))
                reset-example! (fn []
                                 (some-> @view-ref (editor/set-doc! code))
                                 (if run-at-start?
                                   (run-code code @ns result)
                                   (reset! result {:ok? true :text "(not run yet - press Ctrl+Enter or Run)"}))
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
         [small-button {:variant :primary :on-click run!} "▶ Run"]]]))))

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
