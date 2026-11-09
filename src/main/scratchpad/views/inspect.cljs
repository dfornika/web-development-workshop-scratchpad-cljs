(ns scratchpad.views.inspect
  "Showing a value in the tap panel: nested maps and collections that can
  be folded and unfolded, or pretty-printed text. Used by its Taps and
  History tabs (scratchpad.views.taps, scratchpad.views.history)."
  (:require [cljs.pprint :as pp]
            [clojure.data :as data]
            [clojure.string :as str]))

(def ^:private max-children
  "How many entries of a big collection to show when it's unfolded."
  100)

(defn short-str
  "A one-line printed version of `v`, cut off after about `n` characters."
  [v n]
  (let [s (binding [*print-length* 20 *print-level* 4] (pr-str v))]
    (if (> (count s) n) (str (subs s 0 (dec n)) "…") s)))

(defn pprint-str
  "`v` pretty-printed, as a string. Long (or endless) sequences are cut
  off after `max-length` items (default 100), so this always finishes."
  ([v] (pprint-str v max-children))
  ([v max-length]
   (binding [*print-length* max-length *print-level* 10]
     (str/trimr (with-out-str (pp/pprint v))))))

(defn copy!
  "Copies `v`, pretty-printed, to the clipboard. Calls `done` with true if
  it worked. (Copied in full, up to 10,000 items per sequence.)"
  [v done]
  (try
    (-> (.writeText (.-clipboard js/navigator) (pprint-str v 10000))
        (.then #(done true) #(done false)))
    (catch :default _ (done false))))

(def colors
  {:string "#0a7d2c" :number "#0550ae" :keyword "#8250df" :nil "#8c959f"
   :symbol "#953800" :other "#24292f" :muted "#57606a"})

(defn- scalar [v]
  [:span {:style {:color (cond (string? v) (:string colors)
                               (number? v) (:number colors)
                               (keyword? v) (:keyword colors)
                               (boolean? v) (:number colors)
                               (nil? v) (:nil colors)
                               (symbol? v) (:symbol colors)
                               :else (:other colors))
                  :white-space "pre-wrap"
                  :word-break "break-word"}}
   (short-str v 500)])

(defn- folding-kind
  "What kind of collection `v` is, for the folding view - or nil, for
  values shown as they print."
  [v]
  (cond
    (map? v) :map
    (record? v) :map
    (vector? v) :vector
    (set? v) :set
    (seq? v) :seq
    (satisfies? IDeref v) :ref
    :else nil))

(declare value-view)

(defn- entries-of
  "The first `max-children` entries of collection `v` as [label value]
  pairs, plus how many there were (or :many, for more than that)."
  [kind v]
  (let [items (take (inc max-children) v)
        n (count items)
        shown (take max-children items)]
    [(case kind
       :map (map (fn [[k x]] [k x]) shown)
       (:vector :seq) (map-indexed vector shown)
       :set (map (fn [x] [nil x]) shown))
     (if (> n max-children)
       (if (counted? v) (count v) :many)
       n)]))

(defn- size-label [kind n]
  (let [noun (if (= kind :map) "key" "item")]
    (if (= n :many)
      (str "more than " max-children " " noun "s")
      (str n " " noun (when (not= n 1) "s")))))

(defn- collection-view [kind v depth]
  (let [[entries n] (entries-of kind v)
        preview (short-str v 80)]
    (if (and (pos? depth) (<= (count preview) 50) (not= n :many))
      ;; small enough to read on one line
      [scalar v]
      [:details {:open (< depth 1)
                 :style {:display "inline-block" :vertical-align "top"}}
       [:summary {:style {:cursor "pointer"}}
        [:span {:style {:color (:other colors)}} preview]
        [:span {:style {:color (:muted colors) :margin-left "8px" :font-size "12px"}}
         (size-label kind n)]]
       [:div {:style {:padding-left "14px" :border-left "1px solid #d0d7de"
                      :margin "2px 0 2px 4px"}}
        (for [[i [label x]] (map-indexed vector entries)]
          ^{:key i}
          [:div {:style {:display "flex" :gap "8px" :padding "1px 0"}}
           (when-not (= kind :set)
             [:span {:style {:color (if (= kind :map) (:keyword colors) (:muted colors))
                             :flex-shrink 0}}
              (if (= kind :map) (short-str label 60) label)])
           [:div {:style {:min-width 0}} [value-view x (inc depth)]]])
        (when (or (= n :many) (> n max-children))
          [:div {:style {:color (:muted colors)}}
           "… " (if (= n :many) "and more" (str (- n max-children) " more"))])]])))

(defn value-view
  "Shows `v`, with nested maps and collections that can be folded."
  ([v] (value-view v 0))
  ([v depth]
   (let [kind (folding-kind v)]
     (cond
       (nil? kind) [scalar v]
       (= kind :ref) [:span [:span {:style {:color (:muted colors)}} "atom, holding: "]
                      [value-view @v depth]]
       (> depth 12) [scalar v]
       :else [collection-view kind v depth]))))

;; ---------------------------------------------------------------------------
;; Small pieces the tabs share
;; ---------------------------------------------------------------------------

(def mono
  {:font-family "ui-monospace, SFMono-Regular, Menlo, Consolas, monospace"
   :font-size "13px"
   :line-height "1.45"})

(defn small-button [opts label]
  [:button (merge {:style {:font-size "12px" :padding "2px 8px" :cursor "pointer"
                           :border "1px solid #d0d7de" :border-radius "4px"
                           :background "white" :color "#24292f"}}
                  opts)
   label])

(defn time-str [^js d]
  (.toLocaleTimeString d [] #js {:hour "2-digit" :minute "2-digit" :second "2-digit"}))


;; ---------------------------------------------------------------------------
;; What's different between two values
;; ---------------------------------------------------------------------------

(defn- diff-column [title subtitle v]
  [:div {:style {:flex "1 1 220px" :min-width 0}}
   [:div {:style {:font-size "12px" :font-weight "bold" :margin-bottom "2px"}} title]
   [:div {:style {:font-size "12px" :color (:muted colors) :margin-bottom "4px"}} subtitle]
   [:div {:style (merge mono {:background "#f6f8fa" :border-radius "4px" :padding "6px 8px"})}
    (if (nil? v)
      [:span {:style {:color (:muted colors)}} "nothing"]
      [value-view v])]])

(defn diff-view
  "What's different between `a` and `b`, using clojure.data/diff: the parts
  only in `a`, the parts only in `b` and (if `both?`) the parts they share.
  `a-label` and `b-label` name them."
  [a b {:keys [a-label b-label both?]}]
  (let [[only-a only-b in-both] (data/diff a b)]
    (if (and (nil? only-a) (nil? only-b))
      [:div {:style {:color (:muted colors) :font-size "13px"}}
       "No difference: they're equal."]
      [:div
       [:div {:style {:display "flex" :gap "12px" :flex-wrap "wrap"}}
        [diff-column (str "Only in " a-label) "removed or changed" only-a]
        [diff-column (str "Only in " b-label) "added or changed" only-b]
        (when both?
          [diff-column "In both" "the same in each" in-both])]])))
