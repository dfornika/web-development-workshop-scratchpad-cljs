(ns scratchpad.views.taps
  "The tap panel: a bar at the bottom of every page that shows values sent
  with tap>, from anywhere - the REPL, your code, or the editors on the
  site:

      (tap> {:sample \"S001\" :reads [1200 3400]})

  Nested data can be folded and unfolded, or shown as pretty-printed text,
  and two tapped values can be compared. (In Codespaces, tapped values
  also go to shadow-cljs's own Inspect page, on port 9630.)

  Its History tab (scratchpad.views.history) steps back through the
  values an atom has held."
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [scratchpad.views.history :as history]
            [scratchpad.views.inspect :as inspect]))

(def ^:private max-taps
  "How many tapped values to keep. Older ones are dropped."
  200)

;; [{:id 1 :time #inst "..." :source "scratchpad.playground" :value ...}],
;; newest first
(defonce taps (r/atom ()))
(defonce ^:private next-id (atom 0))
(defonce ^:private panel (r/atom {:open? false :unseen 0 :height 300 :tab :taps}))
;; the ids of the (up to two) taps picked to compare, in the order picked
(defonce ^:private comparing (r/atom []))


;; ---------------------------------------------------------------------------
;; Where a value was tapped from
;;
;; tap> only gets the value, not where it was called. So when tap> is
;; called, *exec-tap-fn* (below) works out the namespace straight away -
;; before tap> hands the value to the taps, a moment later - and passes it
;; along in *source*.
;; ---------------------------------------------------------------------------

(def ^:dynamic *source* nil)

(def current-sci-ns
  "For code run by SCI (the editors on the site): a function that gives the
  namespace SCI is running code in, or nil. Set by scratchpad.web.sci."
  nil)

(def ^:private not-yours
  ;; namespaces that call tap> on your behalf, or that your code is called from
  ["cljs." "cljs-http." "clojure." "goog." "shadow." "reagent." "sci."
   "module$" "scratchpad.components" "scratchpad.views.taps" "scratchpad.web."])

(defn- source-from-stack
  "In Codespaces (a development build), every namespace is its own file,
  like .../cljs-runtime/scratchpad.sessions.session_1.js. The first one in
  the stack trace that isn't library code is the one that called tap>.
  Code evaluated at the REPL has no file of its own: that's \"REPL\"."
  []
  (let [stack (str (.-stack (js/Error.)))
        names (for [[_ file] (re-seq #"cljs-runtime/([^/:?#\s()]+?)\.js" stack)]
                (str/replace file "_" "-"))]
    (or (first (remove (fn [n] (some #(str/starts-with? n %) not-yours)) names))
        (when (str/includes? stack "shadow.cljs.devtools.client") "REPL"))))

(defn- tap-source []
  (or (when current-sci-ns (current-sci-ns))
      (source-from-stack)))

;; Like the standard one (run the taps a moment later, so tap> returns
;; straight away), plus the source. Not defonce, so a reload uses the latest.
(set! *exec-tap-fn*
      (fn [f]
        (let [source (try (tap-source) (catch :default _ nil))]
          (boolean (js/setTimeout #(binding [*source* source] (f)) 0)))))

(defn- receive! [value]
  (swap! taps (fn [ts] (take max-taps (conj ts {:id (swap! next-id inc)
                                                 :time (js/Date.)
                                                 :source *source*
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
;; The Taps tab
;; ---------------------------------------------------------------------------

(def ^:private colors inspect/colors)
(def ^:private mono inspect/mono)
(def ^:private small-button inspect/small-button)

(defn- toggle-compare!
  "Picks tap `id` to compare (or un-picks it). Picking a third drops the
  first one picked."
  [id]
  (swap! comparing (fn [ids]
                     (if (some #{id} ids)
                       (vec (remove #{id} ids))
                       (vec (take-last 2 (conj ids id)))))))

(defn- compare-box
  "The two picked taps, side by side: what's only in each, and what they
  share."
  [ts]
  (let [ids (sort @comparing)
        by-id (into {} (map (juxt :id identity)) ts)
        [a b] (map by-id ids)]
    (when (and a b)
      [:div {:style {:padding "10px 12px" :border-bottom "2px solid #0969da"
                     :background "#f6fbff"}}
       [:div {:style {:display "flex" :gap "8px" :align-items "center" :margin-bottom "8px"}}
        [:strong {:style {:font-size "13px"}}
         "Comparing #" (:id a) " with #" (:id b)]
        [:span {:style {:font-size "12px" :color (:muted colors)}}
         "using clojure.data/diff"]
        [:span {:style {:flex 1}}]
        [small-button {:on-click #(reset! comparing [])} "Done"]]
       [inspect/diff-view (:value a) (:value b)
        {:a-label (str "#" (:id a)) :b-label (str "#" (:id b)) :both? true}]])))

(defn- tap-entry
  "One tapped value. Form-2, so whether it's shown as text is remembered."
  [_tap]
  (let [as-text? (r/atom false)
        copied (r/atom nil)]   ; nil, :ok or :failed - shown for a moment
    (fn [{:keys [id time source value]}]
      (let [picked? (some #{id} @comparing)]
        [:div {:style {:padding "8px 12px" :border-bottom "1px solid #eaeef2"
                       :border-left (if picked? "3px solid #0969da" "3px solid transparent")}}
         [:div {:style {:display "flex" :gap "8px" :align-items "center"
                        :font-size "12px" :color (:muted colors) :margin-bottom "4px"}}
          [:span "#" id " · " (inspect/time-str time)
           (when source
             [:<> " · from " [:code {:style {:color "#24292f"}} source]])]
          [:span {:style {:flex 1}}]
          [small-button {:on-click #(swap! as-text? not)
                         :title "Show it folded, or as pretty-printed text"}
           (if @as-text? "fold" "text")]
          [small-button {:on-click #(toggle-compare! id)
                         :title "Pick two values to see what's different between them"
                         :style {:font-size "12px" :padding "2px 8px" :cursor "pointer"
                                 :border "1px solid #d0d7de" :border-radius "4px"
                                 :background (if picked? "#0969da" "white")
                                 :color (if picked? "white" "#24292f")}}
           (if picked? "Comparing" "Compare")]
          [small-button {:on-click (fn []
                                     (inspect/copy! value (fn [ok?]
                                                            (reset! copied (if ok? :ok :failed))
                                                            (js/setTimeout #(reset! copied nil) 1500))))
                         :title "Copy it to the clipboard, as pretty-printed text"}
           (case @copied :ok "Copied ✓" :failed "Couldn't copy" "Copy")]
          [small-button {:on-click (fn []
                                     (swap! comparing (fn [ids] (vec (remove #{id} ids))))
                                     (swap! taps (fn [ts] (remove #(= id (:id %)) ts))))
                         :title "Remove this one"}
           "×"]]
         [:div {:style mono}
          (if @as-text?
            [:pre {:style (merge mono {:margin 0 :white-space "pre-wrap"})} (inspect/pprint-str value)]
            [inspect/value-view value])]]))))

(defn- taps-tab [ts]
  (if (empty? ts)
    [:div {:style {:padding "16px" :color (:muted colors) :line-height "1.6"}}
     "Nothing tapped yet. Anywhere you can run code - the REPL, the "
     "Playground, the editors on the reference pages - try"
     [:pre {:style (merge mono {:margin "8px 0" :color "#24292f"})}
      "(tap> {:sample \"S001\" :reads [1200 3400 5600]})"]
     "and it shows up here. To tap a value in the middle of a "
     [:code "->"] " or " [:code "->>"] " pipeline, use " [:code "c/tap->"]
     " - it taps the value and passes it straight on:"
     [:pre {:style (merge mono {:margin "8px 0" :color "#24292f"})}
      "(->> samples (filter :passed) c/tap-> (map :reads))"]]
    [:<>
     [compare-box ts]
     (when (= 1 (count @comparing))
       [:div {:style {:padding "6px 12px" :font-size "12px" :color (:muted colors)
                      :background "#f6fbff" :border-bottom "1px solid #eaeef2"}}
        "Pick one more value to compare it with."])
     (for [t ts]
       ^{:key (:id t)} [tap-entry t])]))

;; ---------------------------------------------------------------------------
;; The panel
;; ---------------------------------------------------------------------------

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

(defn- tab-button [tab label]
  (let [current? (= tab (:tab @panel))]
    [:button {:on-click #(swap! panel assoc :tab tab)
              :style {:font-size "14px" :padding "4px 10px" :cursor "pointer"
                      :border "none" :background "none"
                      :font-weight (if current? "bold" "normal")
                      :color (if current? "#24292f" (:muted colors))
                      :border-bottom (if current? "2px solid #0969da" "2px solid transparent")}}
     label]))

(defn- open-panel []
  (let [ts @taps
        tab (:tab @panel)]
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
                    :padding "2px 12px" :border-bottom "1px solid #d0d7de"
                    :background "#f6f8fa" :flex-shrink 0}}
      [tab-button :taps "Tapped values"]
      [tab-button :history "History"]
      (when (= tab :taps)
        [:span {:style {:font-size "12px" :color (:muted colors)}}
         (count ts) (if (= 1 (count ts)) " value" " values") ", newest first"])
      [:span {:style {:flex 1}}]
      (when (= tab :taps)
        [small-button {:on-click #(do (reset! taps ()) (reset! comparing []))} "Clear"])
      [small-button {:on-click close!} "Close"]]
     [:div {:style {:overflow-y "auto" :flex 1}}
      (case tab
        :history [history/tab]
        [taps-tab ts])]]))

(defn- closed-button []
  (let [{:keys [unseen]} @panel]
    [:button {:on-click open!
              :title "Show the values sent with (tap> ...), and the History of your atoms"
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
