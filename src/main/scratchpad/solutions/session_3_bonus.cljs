(ns scratchpad.solutions.session-3-bonus
  "Reference solutions for src/main/scratchpad/sessions/session_3_bonus.cljs.

  Just one way to do it - if yours works, it's right!"
  (:require ["ag-charts-react" :refer [AgCharts]]
            ["ag-grid-community" :refer [themeBalham]]
            ["ag-grid-react" :refer [AgGridReact]]
            [cljs.pprint :as pp]
            [reagent.core :as r]
            [scratchpad.components :as c]))

(def grid-options
  "Options for every AG Grid table in this file (the same as in
  session_3.cljs)."
  {:theme themeBalham
   :rowSelection {:mode "multiRow"}
   :selectionColumnDef {:pinned "left"}
   :defaultColDef {:editable true :filter true}
   :enableCellTextSelection true
   :ensureDomOrder true
   ;; columns share the table's width (these tables are side by side with
   ;; other things, so there isn't much room)
   :autoSizeStrategy {:type "fitGridWidth"}})

(defn get-selected-rows
  "The selected rows, from one of AG Grid's events, as maps."
  [event]
  (mapv #(js->clj % :keywordize-keys true)
        (.getSelectedRows (.-api event))))

;; All 96 reports from the JSON file. This file loads them itself (it's
;; the same as load-mutations! in session_3.cljs, already written), so the
;; bonus exercises work whether or not you've finished the main ones.
(defonce all-mutations (r/atom []))

(defn load-all!
  "Fetch the reports into all-mutations (once)."
  []
  (when (empty? @all-mutations)
    (-> (js/fetch "data/session-3/mutations.json")
        (.then #(.json %))
        (.then #(reset! all-mutations (js->clj % :keywordize-keys true)))
        (.catch #(js/console.error "Couldn't load the mutations:" %)))))


;; ===========================================================================
;; 1. A Pie Chart, Linked to a Table
;;
;; Tick reports in the table, and a pie chart shows how they're shared out
;; between genes. With nothing ticked, it shows them all.
;; ===========================================================================

(defn reports-per-gene
  "How many of `mutations` there are for each gene:
    ({:gene \"rpoB\" :reports 25} ...)"
  [mutations]
  (for [[gene n] (frequencies (map :gene mutations))]
    {:gene gene :reports n}))

(defn gene-pie
  "A pie chart of `mutations`, one slice per gene."
  [mutations]
  [:div {:style {:height "300px"}}
   [:> AgCharts
    {:style {:height "100%"}
     :options {:title {:text "Reports per gene"}
               :subtitle {:text (str (count mutations) " reports")}
               :data (reports-per-gene mutations)
               :series [{:type "pie" :angleKey "reports"
                         :legendItemKey "gene" :calloutLabelKey "gene"}]}}]])

(defn ticked-table
  "The reports, with a tickbox on each. Calls `on-change` with the ticked
  rows whenever they change."
  [mutations on-change]
  [:div {:style {:height "300px"}}
   [:> AgGridReact
    (merge grid-options
           {:columnDefs [{:field "gene" :headerName "Gene" :pinned "left"}
                         {:field "mutation" :headerName "Mutation"}
                         {:field "location" :headerName "Region"}
                         {:field "date" :headerName "Date"}]
            :rowData mutations
            :getRowId (fn [params] (str (.. params -data -id)))
            :onSelectionChanged (fn [event] (on-change (get-selected-rows event)))})]])

(defn pie-dashboard
  "The table and the pie chart, side by side."
  []
  (r/with-let [_ (load-all!)
               ticked (r/atom [])]
    [:div {:style {:display "grid" :grid-template-columns "3fr 2fr" :gap "12px"}}
     [ticked-table @all-mutations (fn [rows] (reset! ticked rows))]
     [gene-pie (if (seq @ticked) @ticked @all-mutations)]]))

(comment
  (count @all-mutations)
  (frequencies (map :gene @all-mutations))
  (reports-per-gene (take 10 @all-mutations))
  )


;; ===========================================================================
;; 2. A Trend Over Time
;;
;; Is a mutation becoming more common? A line chart of its frequency over
;; time, with a line for each region.
;; ===========================================================================

(defn trend-series
  "The reports of `mutation` (like \"C-15T\"), as a line for each region:
    ({:location \"BC\" :data [{:date #inst \"2026-03-20\" :frequency 42} ...]} ...)
  Each line's :data is in date order, with the date as a real date (a time
  axis needs one): (js/Date. \"2026-03-20\")"
  [mutations mutation]
  (->> mutations
       (filter #(= mutation (:mutation %)))
       (group-by :location)
       (sort-by key)
       (map (fn [[location reports]]
              {:location location
               :data (for [r (sort-by :date reports)]
                       {:date (js/Date. (:date r)) :frequency (:frequency r)})}))))

(defn trend-chart
  "A line chart of `mutation`'s frequency over time."
  [mutations mutation]
  [:div {:style {:height "300px"}}
   [:> AgCharts
    {:style {:height "100%"}
     :options {:title {:text (str mutation " over time")}
               ;; one line series per region, each with its own :data
               :series (for [{:keys [location data]} (trend-series mutations mutation)]
                         {:type "line" :title location :data data
                          :xKey "date" :yKey "frequency" :yName "%"})
               :axes {:x {:type "time"}
                      :y {:type "number" :title {:text "% of resistant isolates"}}}}}]])

(defn trend-explorer
  "A dropdown of mutations, and the chosen one's trend."
  []
  (r/with-let [_ (load-all!)
               chosen (r/atom "C-15T")]
    [:div
     [:label "Mutation: "
      [:select {:value @chosen :on-change #(reset! chosen (.. % -target -value))}
       (for [m (sort (distinct (map :mutation @all-mutations)))]
         ^{:key m} [:option {:value m} m])]]
     [trend-chart @all-mutations @chosen]]))

(comment
  (filter #(= "C-15T" (:mutation %)) @all-mutations)
  (trend-series @all-mutations "C-15T")
  (js/Date. "2026-03-20")
  )


;; ===========================================================================
;; 3. Make the Numbers Stand Out
;;
;; In a big table, color draws the eye to what matters. Color each
;; frequency by how high it is, and show the drug when hovering over a
;; mutation.
;; ===========================================================================

(defn frequency-color
  "A background color for a frequency: \"#ffd8d3\" (light red) for 50 or
  more, \"#fff1c2\" (light yellow) for 20 or more, and nil below that."
  [frequency]
  (cond
    (>= frequency 50) "#ffd8d3"
    (>= frequency 20) "#fff1c2"
    :else nil))

(defn frequency-columns
  "Columns for a table of reports, with colored frequencies."
  []
  [{:field "gene" :headerName "Gene" :pinned "left"}
   {:field "mutation" :headerName "Mutation" :tooltipField "drug"}
   {:field "frequency" :headerName "%"
    :cellStyle (fn [params]
                 (when-let [color (frequency-color (.-value params))]
                   #js {:backgroundColor color}))}
   {:field "location" :headerName "Region"}
   {:field "date" :headerName "Date"}])

(defn colored-table
  "The reports, with colored frequencies."
  []
  (r/with-let [_ (load-all!)]
    [:div {:style {:height "300px"}}
     ;; a fresh grid when the columns change: AG Grid applies some column
     ;; options (like tooltips) only when it first draws a column
     ^{:key (pr-str (frequency-columns))}
     [:> AgGridReact
      (merge grid-options
             {:columnDefs (frequency-columns)
              :rowData @all-mutations})]]))

(comment
  (frequency-color 78)
  (frequency-color 31)
  (frequency-color 9)
  ;; Call your :cellStyle function like AG Grid would:
  ((:cellStyle (nth (frequency-columns) 2)) #js {:value 78})
  )
