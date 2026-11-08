(ns scratchpad.views.taps
  "The tap panel: a bar at the bottom of every page that shows values sent
  with tap>, from anywhere - the REPL, your code, or the editors on the
  site:

      (tap> {:sample \"S001\" :reads [1200 3400]})

  Nested data can be folded and unfolded, or shown as pretty-printed text.
  (In Codespaces, tapped values also go to shadow-cljs's own Inspect page,
  on port 9630.)"
  (:require [cljs.pprint :as pp]
            [clojure.string :as str]
            [reagent.core :as r]))

(def ^:private max-taps
  "How many tapped values to keep. Older ones are dropped."
  200)

(def ^:private max-children
  "How many entries of a big collection to show when it's unfolded."
  100)

;; [{:id 1 :time #inst "..." :value ...}], newest first
(defonce taps (r/atom ()))
(defonce ^:private next-id (atom 0))
(defonce ^:private panel (r/atom {:open? false :unseen 0 :height 300}))

(defn- receive! [value]
  (swap! taps (fn [ts] (take max-taps (conj ts {:id (swap! next-id inc)
                                                 :time (js/Date.)
                                                 :value value}))))
  (when-not (:open? @panel)
    (swap! panel update :unseen inc)))

;; add-tap only once (not again on every hot reload), with a function that
;; calls the latest receive!
(defonce ^:private _listening
  (add-tap #(receive! %)))

(defn- open! []
  (swap! panel assoc :open? true :unseen 0))

(defn- close! []
  (swap! panel assoc :open? false))

;; ---------------------------------------------------------------------------
;; Showing a value
;; ---------------------------------------------------------------------------

(defn- short-str
  "A one-line printed version of `v`, cut off after about `n` characters."
  [v n]
  (let [s (binding [*print-length* 20 *print-level* 4] (pr-str v))]
    (if (> (count s) n) (str (subs s 0 (dec n)) "…") s)))

(defn pprint-str
  "`v` pretty-printed, as a string. Long (or endless) sequences are cut
  off, so this always finishes."
  [v]
  (binding [*print-length* max-children *print-level* 10]
    (str/trimr (with-out-str (pp/pprint v)))))

(def ^:private colors
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
;; The panel
;; ---------------------------------------------------------------------------

(def ^:private mono
  {:font-family "ui-monospace, SFMono-Regular, Menlo, Consolas, monospace"
   :font-size "13px"
   :line-height "1.45"})

(defn- small-button [opts label]
  [:button (merge {:style {:font-size "12px" :padding "2px 8px" :cursor "pointer"
                           :border "1px solid #d0d7de" :border-radius "4px"
                           :background "white" :color "#24292f"}}
                  opts)
   label])

(defn- time-str [^js d]
  (.toLocaleTimeString d [] #js {:hour "2-digit" :minute "2-digit" :second "2-digit"}))

(defn- tap-entry
  "One tapped value. Form-2, so whether it's shown as text is remembered."
  [_tap]
  (let [as-text? (r/atom false)]
    (fn [{:keys [id time value]}]
      [:div {:style {:padding "8px 12px" :border-bottom "1px solid #eaeef2"}}
       [:div {:style {:display "flex" :gap "8px" :align-items "center"
                      :font-size "12px" :color (:muted colors) :margin-bottom "4px"}}
        [:span "#" id " · " (time-str time)]
        [:span {:style {:flex 1}}]
        [small-button {:on-click #(swap! as-text? not)
                       :title "Show it folded, or as pretty-printed text"}
         (if @as-text? "fold" "text")]
        [small-button {:on-click (fn [] (swap! taps (fn [ts] (remove #(= id (:id %)) ts))))
                       :title "Remove this one"}
         "×"]]
       [:div {:style mono}
        (if @as-text?
          [:pre {:style (merge mono {:margin 0 :white-space "pre-wrap"})} (pprint-str value)]
          [value-view value])]])))

(defn- start-resize!
  "Dragging the panel's top edge changes its height."
  [^js e]
  (.preventDefault e)
  (let [start-y (.-clientY e)
        start-h (:height @panel)
        max-h (- (.-innerHeight js/window) 80)
        move (fn [^js e]
               (swap! panel assoc :height
                      (-> (+ start-h (- start-y (.-clientY e)))
                          (max 120)
                          (min max-h))))
        up (fn up [_]
             (.removeEventListener js/window "pointermove" move)
             (.removeEventListener js/window "pointerup" up))]
    (.addEventListener js/window "pointermove" move)
    (.addEventListener js/window "pointerup" up)))

(defn- open-panel []
  (let [ts @taps]
    [:div {:style {:position "fixed" :left 0 :right 0 :bottom 0
                   :height (str (:height @panel) "px")
                   :z-index 1001
                   :display "flex" :flex-direction "column"
                   :background "#ffffff"
                   :border-top "1px solid #d0d7de"
                   :box-shadow "0 -4px 16px rgba(31, 35, 40, 0.15)"}}
     ;; the top edge: drag it to make the panel taller or shorter
     [:div {:on-pointer-down start-resize!
            :title "Drag to resize"
            :style {:height "6px" :cursor "ns-resize" :flex-shrink 0
                    :background "#eaeef2"}}]
     [:div {:style {:display "flex" :gap "8px" :align-items "center"
                    :padding "6px 12px" :border-bottom "1px solid #d0d7de"
                    :background "#f6f8fa" :flex-shrink 0}}
      [:strong {:style {:font-size "14px"}} "Tapped values"]
      [:span {:style {:font-size "12px" :color (:muted colors)}}
       (count ts) (if (= 1 (count ts)) " value" " values") ", newest first"]
      [:span {:style {:flex 1}}]
      [small-button {:on-click #(reset! taps ())} "Clear"]
      [small-button {:on-click close!} "Close"]]
     [:div {:style {:overflow-y "auto" :flex 1}}
      (if (empty? ts)
        [:div {:style {:padding "16px" :color (:muted colors) :line-height "1.6"}}
         "Nothing tapped yet. Anywhere you can run code - the REPL, the "
         "Playground, the editors on the reference pages - try"
         [:pre {:style (merge mono {:margin "8px 0" :color "#24292f"})}
          "(tap> {:sample \"S001\" :reads [1200 3400 5600]})"]
         "and it shows up here. " [:code "tap>"] " returns " [:code "true"]
         ", so you can also drop it into the middle of your code without "
         "changing what it does: " [:code "(doto value tap>)"] " taps a value "
         "and passes it straight through."]
        (for [t ts]
          ^{:key (:id t)} [tap-entry t]))]]))

(defn- closed-button []
  (let [{:keys [unseen]} @panel]
    [:button {:on-click open!
              :title "Show the values sent with (tap> ...)"
              :style (merge mono
                            {:position "fixed" :right "12px" :bottom "12px"
                             :z-index 1001
                             :padding "4px 10px"
                             :border-radius "14px"
                             :cursor "pointer"
                             :border (if (pos? unseen) "1px solid #0969da" "1px solid #d0d7de")
                             :background (if (pos? unseen) "#0969da" "#f6f8fa")
                             :color (if (pos? unseen) "white" "#57606a")
                             :box-shadow "0 1px 3px rgba(31, 35, 40, 0.12)"})}
     "tap>" (when (pos? unseen) (str " " unseen " new"))]))

(defn panel-view
  "The tap panel: a small button in the bottom corner, that opens into a
  panel along the bottom of the window."
  []
  (if (:open? @panel)
    [open-panel]
    [closed-button]))
