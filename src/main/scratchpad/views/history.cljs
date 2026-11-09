(ns scratchpad.views.history
  "The History tab of the tap panel: every value an atom has held, so you
  can step back through them - and the page goes back with them.

      (c/track! \"selected\" selected)

  This is cheap because Clojure's data is immutable: a change makes a new
  value that shares almost everything with the old one, instead of
  changing it in place. So keeping every old value costs very little, and
  going back is just putting an old value back in the atom.

  The app state (scratchpad.state/db) is tracked from the start."
  (:require [reagent.core :as r]
            [scratchpad.state :as state]
            [scratchpad.views.inspect :as inspect]))

(def ^:private max-states
  "How many values to keep for each atom. Older ones are dropped."
  500)

;; {label {:atom a :ignore #{...} :order 1 :index 3
;;         :states [{:value ... :time #inst "..."} ...]}}
(defonce tracked (r/atom {}))
(defonce ^:private chosen (r/atom nil))   ; the label shown in the tab
(defonce ^:private next-order (atom 0))

;; True while going-to! puts an old value back, so that doesn't count as
;; a new change. (Watches run straight away, inside reset!.)
(defonce ^:private restoring? (atom false))

(defn- without-ignored [ignore v]
  (if (and (seq ignore) (map? v)) (apply dissoc v ignore) v))

(defn- record! [label v]
  (swap! tracked
         (fn [t]
           (if-let [{:keys [ignore states index]} (get t label)]
             (let [v (without-ignored ignore v)]
               (if (= v (:value (nth states index)))
                 t  ; nothing changed (or only the keys we ignore)
                 ;; anything after the step we're on is replaced: going
                 ;; back and then changing something starts a new branch
                 (let [states (conj (subvec states 0 (inc index))
                                    {:value v :time (js/Date.)})
                       states (if (> (count states) max-states)
                                (subvec states (- (count states) max-states))
                                states)]
                   (assoc t label (assoc (get t label)
                                         :states states
                                         :index (dec (count states)))))))
             t))))

(defn- watch-key [label]
  [::history label])

(defn untrack!
  "Stops keeping the history of the atom tracked as `label`."
  [label]
  (when-let [{a :atom} (get @tracked label)]
    (remove-watch a (watch-key label)))
  (swap! tracked dissoc label)
  nil)

(defn track!
  "Keeps every value atom `a` holds from now on, in the History tab of the
  tap panel, where you can step back through them. `label` names it there.

  Options:
    :ignore  keys of a map to leave out: changes to only those keys
             aren't recorded, and going back leaves them as they are now.

      (c/track! \"selected\" selected)
      (c/track! \"state\" state {:ignore #{:loading?}})

  Tracking the same label again starts over (with the new atom, if it's
  a different one)."
  ([a] (track! (str "atom " (inc @next-order)) a))
  ([label a] (track! label a {}))
  ([label a {:keys [ignore]}]
   (let [label (str label)
         ignore (set ignore)]
     (when-not (and (identical? a (get-in @tracked [label :atom]))
                    (= ignore (get-in @tracked [label :ignore])))
       (untrack! label)
       (swap! tracked assoc label
              {:atom a
               :ignore ignore
               :order (swap! next-order inc)
               :index 0
               :states [{:value (without-ignored ignore @a) :time (js/Date.)}]})
       (add-watch a (watch-key label)
                  (fn [_ _ _ new]
                    (when-not @restoring? (record! label new)))))
     (reset! chosen label)
     label)))

(defn go-to!
  "Puts the value from step `i` (counting from 0) back into the atom
  tracked as `label`."
  [label i]
  (when-let [{a :atom :keys [ignore states]} (get @tracked label)]
    (let [i (-> i (max 0) (min (dec (count states))))
          v (:value (nth states i))]
      (swap! tracked assoc-in [label :index] i)
      (reset! restoring? true)
      (try
        (reset! a (if (and (seq ignore) (map? v) (map? @a))
                    (merge (select-keys @a ignore) v)
                    v))
        (finally (reset! restoring? false))))))

(defn clear!
  "Forgets the history of `label`, keeping just the value it has now."
  [label]
  (when-let [{a :atom :keys [ignore]} (get @tracked label)]
    (swap! tracked update label assoc
           :index 0
           :states [{:value (without-ignored ignore @a) :time (js/Date.)}])))

;; The app state, from the start. Changing pages isn't recorded: going
;; back in time shouldn't take you to another page.
(defonce ^:private _db
  (track! "app state (db)" state/db {:ignore #{:view :subview :edition}}))

;; ---------------------------------------------------------------------------
;; The tab
;; ---------------------------------------------------------------------------

(def ^:private muted (:muted inspect/colors))

(defn- labels []
  (->> @tracked (sort-by (comp :order val)) (map key)))

(defn- picker [current]
  (let [ls (labels)]
    (when (> (count ls) 1)
      (into [:div {:style {:display "flex" :gap "6px" :flex-wrap "wrap" :margin-bottom "8px"}}]
            (for [l ls]
              [:button {:on-click #(reset! chosen l)
                        :style (merge inspect/mono
                                      {:font-size "12px" :padding "2px 10px" :cursor "pointer"
                                       :border-radius "12px"
                                       :border (if (= l current) "1px solid #0969da" "1px solid #d0d7de")
                                       :background (if (= l current) "#ddf4ff" "white")
                                       :color "#24292f"})}
               l])))))

(defn- controls [label {:keys [states index]}]
  (let [last-i (dec (count states))]
    [:div {:style {:display "flex" :gap "6px" :align-items "center" :flex-wrap "wrap"}}
     [inspect/small-button {:on-click #(go-to! label 0) :disabled (zero? index)
                            :title "The first value recorded"} "⏮"]
     [inspect/small-button {:on-click #(go-to! label (dec index)) :disabled (zero? index)
                            :title "One step back"} "◀ Back"]
     [:input {:type "range" :min 0 :max last-i :value index
              :disabled (zero? last-i)
              :on-change #(go-to! label (js/parseInt (.. % -target -value)))
              :style {:flex "1 1 160px" :min-width "120px"}}]
     [inspect/small-button {:on-click #(go-to! label (inc index)) :disabled (= index last-i)
                            :title "One step forward"} "Forward ▶"]
     [inspect/small-button {:on-click #(go-to! label last-i) :disabled (= index last-i)
                            :title "The latest value"} "⏭"]
     [:span {:style {:font-size "12px" :color muted :min-width "150px"}}
      "step " (inc index) " of " (count states) " · "
      (inspect/time-str (:time (nth states index)))]]))

(defn- step-view
  "What step `index` changed, and the whole value at that step."
  [_label _entry]
  (let [show-all? (r/atom false)]
    (fn [label {:keys [states index ignore]}]
      (let [now (:value (nth states index))
            before (when (pos? index) (:value (nth states (dec index))))]
        [:div {:style {:margin-top "10px"}}
         (when (< index (dec (count states)))
           [:div {:style {:background "#fff8c5" :border "1px solid #d4a72c"
                          :border-radius "6px" :padding "6px 10px" :margin-bottom "10px"
                          :font-size "13px" :line-height "1.5"}}
            [:strong "You've gone back in time. "]
            "The page shows the value from this step. Step forward to come back, "
            "or change something: that starts a new history from here, and the "
            "later steps are dropped."])
         [:div {:style {:font-size "13px" :font-weight "bold" :margin-bottom "6px"}}
          (if before
            (str "What changed in step " (inc index))
            "The first value recorded")]
         (if before
           [inspect/diff-view before now {:a-label (str "step " index)
                                          :b-label (str "step " (inc index))}]
           [:div {:style inspect/mono} [inspect/value-view now]])
         (when before
           [:div {:style {:margin-top "10px"}}
            [inspect/small-button {:on-click #(swap! show-all? not)}
             (if @show-all? "Hide the whole value" (str "Show the whole value at step " (inc index)))]
            (when @show-all?
              [:div {:style (merge inspect/mono {:margin-top "6px"})}
               [inspect/value-view now]])])
         [:div {:style {:margin-top "12px" :font-size "12px" :color muted :line-height "1.5"}}
          (when (seq ignore)
            [:<> "Changes to " (pr-str (vec (sort ignore))) " aren't recorded. "])
          "Keeping every step is cheap: each new value shares almost all of "
          "itself with the one before, because Clojure data never changes in place. "
          [:a {:href "#" :on-click (fn [e] (.preventDefault e) (clear! label))
               :style {:color muted}}
           "Forget this history"]]]))))

(defn tab
  "The History tab."
  []
  (let [ls (labels)
        current (if (some #{@chosen} ls) @chosen (first ls))
        entry (get @tracked current)]
    [:div {:style {:padding "10px 12px"}}
     [picker current]
     (if entry
       [:<>
        [controls current entry]
        ^{:key current} [step-view current entry]]
       [:div {:style {:color muted :line-height "1.6"}}
        "Nothing tracked. Try " [:code "(c/track! \"my-atom\" my-atom)"] "."])
     (when entry
       [:div {:style {:margin-top "12px" :font-size "12px" :color muted :line-height "1.6"}}
        "Track one of your own atoms with "
        [:code "(c/track! \"selected\" selected)"] "."])]))
