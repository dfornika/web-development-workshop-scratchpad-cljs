(ns scratchpad.sessions.session-2-bonus
  "Session 2 bonus homework: more AG Grid features.

  Same as session_2.cljs: write the functions marked TODO, try things out in
  the (comment ...) blocks, and watch the Homework page update as you save.

  Remember to load this file into the REPL first (Ctrl+Alt+C Enter).

  The AG Grid documentation lists every option:
  https://www.ag-grid.com/react-data-grid/"
  (:require ["ag-grid-react" :refer [AgGridReact]]
            [reagent.core :as r]
            [scratchpad.components :as c]))

(def example-samples
  (vec (for [i (range 1 41)]
         {:sampleId (str "S" (.padStart (str i) 3 "0"))
          :lineage (nth ["XBB.1.5" "EG.5" "JN.1" "BA.2.86" "KP.2"] (mod i 5))
          :collectionDate (str "2026-06-" (.padStart (str (inc (mod i 28))) 2 "0"))
          :qcStatus (nth ["PASS" "PASS" "PASS" "WARN" "FAIL"] (mod (* i 7) 5))
          :percentQ30 (+ 70 (mod (* i 13) 29))})))

(def sample-columns
  [{:headerName "Sample ID" :field "sampleId"}
   {:headerName "Lineage" :field "lineage"}
   {:headerName "Collection Date" :field "collectionDate"}
   {:headerName "QC Status" :field "qcStatus"}
   {:headerName "% Q30" :field "percentQ30"}])


;; ===========================================================================
;; 1. Filters, Sorting and Pages
;;
;; There are 40 samples now - too many to see at once. Let people filter and
;; sort every column, and show 10 rows per page.
;; ===========================================================================

(defn big-samples-grid
  "A grid of `samples` with filters, sorting and pagination."
  [samples]
  [:div {:style {:height "420px"}}
   [:> AgGridReact
    {:columnDefs sample-columns
     :rowData samples
     ;; TODO: add these grid options (see the AG Grid docs for more):
     ;;   :defaultColDef {:filter true :sortable true}  - applies to every column
     ;;   :pagination true
     ;;   :paginationPageSize 10
     ;;   :paginationPageSizeSelector [10 20 50]
     }]])

(comment
  (count example-samples)
  (frequencies (map :qcStatus example-samples))
  (take 3 example-samples)
  )


;; ===========================================================================
;; 2. Export to CSV
;;
;; Add a button that downloads the table as a CSV file - for a spreadsheet.
;; ===========================================================================

(defn exportable-grid
  "A grid of `samples`, with a button above it that exports them as CSV."
  [samples]
  ;; `grid-api` will hold AG Grid's API object - its remote control - once
  ;; the grid is ready. (r/with-let keeps it between redraws.)
  (r/with-let [grid-api (atom nil)]
    [:div
     ;; TODO: add a c/button whose :on-click calls
     ;;   (.exportDataAsCsv @grid-api)
     [:div {:style {:height "300px"}}
      [:> AgGridReact
       {:columnDefs sample-columns
        :rowData samples
        ;; AG Grid calls this when it's ready, with an event holding its API
        :onGridReady (fn [event] (reset! grid-api (.-api event)))}]]]))

(comment
  ;; The API can do lots of things - look at the AG Grid docs for "Grid API".
  ;; e.g. (.exportDataAsCsv api #js {:fileName "samples.csv"})
  )


;; ===========================================================================
;; 3. Tooltips
;;
;; Show more information when the mouse hovers over a cell or a header.
;; ===========================================================================

(defn tooltip-columns
  "The sample columns, with tooltips."
  []
  ;; TODO: AG Grid shows a tooltip when a column has
  ;;   :headerTooltip "text"   - when hovering over the header
  ;;   :tooltipField "field"   - when hovering over a cell: that field's value
  ;; Add a :headerTooltip to each column, and a :tooltipField to the Sample ID
  ;; column that shows the collection date. (`map` and `assoc` might help.)
  ;; Tooltips appear after you hold the mouse still for a moment.
  ;;
  ;; Good to know: AG Grid applies some column options, like tooltips, only
  ;; when it first draws a column. This page redraws the grid for you when
  ;; the columns change - but in your own grids, if a change like this
  ;; doesn't show up, reload the page.
  sample-columns)

(comment
  (first (tooltip-columns))
  (map #(assoc % :headerTooltip (str "This is " (:headerName %))) sample-columns)
  )


;; ===========================================================================
;; 4. Fix It From the Table
;;
;; Put a button in each row that flips its QC status between "PASS" and
;; "FAIL" - and watch the table update straight away.
;; ===========================================================================

(defonce samples-db
  (r/atom (vec (take 6 example-samples))))

(defn toggle-status!
  "Flip the QC status of the sample with ID `sample-id`, in samples-db."
  [sample-id]
  ;; TODO: swap! samples-db, using mapv to change just the matching sample
  ;; ("PASS" -> "FAIL", anything else -> "PASS")
  nil)

(defn toggle-button
  "An AG Grid cell renderer: a button that toggles this row's QC status."
  [params]
  (r/as-element
   [c/button {:variant :secondary
              :style {:padding "2px 8px" :font-size "12px"}
              :on-click #(toggle-status! (.. params -data -sampleId))}
    "Toggle"]))

(defn editable-grid
  "The samples in samples-db, with a Toggle button on each row."
  []
  [:div {:style {:height "300px"}}
   [:> AgGridReact
    {;; the Toggle button first, so it's always in view
     :columnDefs (into [{:headerName "" :cellRenderer toggle-button :width 110}]
                       sample-columns)
     :rowData @samples-db
     ;; keeps each row's identity when samples-db changes
     :getRowId (fn [params] (.. params -data -sampleId))}]])

(comment
  @samples-db
  (toggle-status! "S001")
  (map :qcStatus @samples-db)
  (reset! samples-db (vec (take 6 example-samples)))
  )
