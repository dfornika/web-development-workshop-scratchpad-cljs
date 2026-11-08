(ns scratchpad.views.reference
  "Building blocks for the reference pages (ClojureScript Basics, JS Interop):
  syntax-colored code, examples with live results, simple tables, and the
  page layout with its table of contents."
  (:require [cljs.pprint :refer [pprint]]
            [clojure.string :as str]
            [scratchpad.components :as c]
            [scratchpad.ui :as ui]
            [scratchpad.web.editor :as editor]))

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

(defn example
  "Syntax-colored `code`, followed by the result of calling `f` - which
  should run the same code, so the result shown is the real one."
  [code f]
  (let [result (f)
        one-line (pr-str result)]
    [snippet (str code "\n;; => "
                  (if (<= (count one-line) 70)
                    one-line
                    ;; long results are spread over several lines, lined up
                    ;; after the ";; => "
                    (str/replace (str/trimr (with-out-str (pprint result)))
                                 "\n" "\n;;    ")))]))

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
