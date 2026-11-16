(ns scratchpad.sessions.session-3-bonus
  "Session 3 bonus homework: more charts, and making data stand out.

  Same as session_3.cljs: write the functions marked TODO, try things out in
  the (comment ...) blocks, and watch the Homework page update as you save.

  Remember to load this file into the REPL first (Ctrl+Alt+C Enter).

  AG Charts' documentation has every chart type and option:
  https://www.ag-grid.com/charts/react/quick-start/"
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
  ;; TODO (hint: frequencies, then map over its [gene n] pairs)
  [])

(defn gene-pie
  "A pie chart of `mutations`, one slice per gene."
  [mutations]
  [:div {:style {:height "300px"}}
   [:> AgCharts
    {:style {:height "100%"}
     :options {:title {:text "Reports per gene"}
               :subtitle {:text (str (count mutations) " reports")}
               :data (reports-per-gene mutations)
               ;; TODO: a pie series: {:type "pie" :angleKey "reports"
               ;; :legendItemKey "gene" :calloutLabelKey "gene"}
               :series []}}]])

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
     ;; TODO: pass ticked-table a function that stores the ticked rows in
     ;; `ticked` - and give gene-pie the ticked rows, or all of them if
     ;; none are ticked (hint: (if (seq @ticked) ...)).
     [ticked-table @all-mutations (fn [rows] nil)]
     [gene-pie @all-mutations]]))

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
  ;; TODO:
  ;;   1. keep the reports of `mutation`
  ;;   2. group them by :location
  ;;   3. turn each [location reports] into a map like the one above:
  ;;      sort the reports by :date, and make each one {:date ... :frequency ...}
  [])

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
  ;; TODO (hint: cond)
  nil)

(defn frequency-columns
  "Columns for a table of reports, with colored frequencies."
  []
  [{:field "gene" :headerName "Gene" :pinned "left"}
   ;; TODO: show the drug when hovering over a mutation: add
   ;;   :tooltipField "drug"
   {:field "mutation" :headerName "Mutation"}
   ;; TODO: add a :cellStyle function that colors the background with
   ;; frequency-color. It gets `params`, and should return a JavaScript
   ;; object - #js {:backgroundColor ...} - or nil.
   {:field "frequency" :headerName "%"}
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
