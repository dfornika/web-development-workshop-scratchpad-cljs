(ns scratchpad.solutions.session-2
  "Reference solutions for src/main/scratchpad/sessions/session_2.cljs.

  These are just one way to do it - if yours works, it's right!
  The function names match the homework file, so you can compare them side
  by side, or copy a function across to see it on the Session 2 page.

  To try them at the REPL: load this file (Ctrl+Alt+C Enter), then evaluate
  the forms in the (comment ...) blocks."
  (:require ["ag-grid-community" :refer [themeBalham]]
            ["ag-grid-react" :refer [AgGridReact]]
            [reagent.core :as r]
            [scratchpad.components :as c]))

(def example-samples
  [{:sampleId "S001" :lineage "XBB.1.5" :collectionDate "2026-06-01" :qcStatus "PASS" :percentQ30 97.25 :totalBases 1200000000}
   {:sampleId "S002" :lineage "EG.5"    :collectionDate "2026-06-03" :qcStatus "FAIL" :percentQ30 71.5  :totalBases 180000000}
   {:sampleId "S003" :lineage "JN.1"    :collectionDate "2026-06-04" :qcStatus "PASS" :percentQ30 93.1  :totalBases 950000000}
   {:sampleId "S004" :lineage "XBB.1.5" :collectionDate "2026-06-09" :qcStatus "WARN" :percentQ30 84.0  :totalBases 610000000}
   {:sampleId "S005" :lineage "BA.2.86" :collectionDate "2026-06-12" :qcStatus "PASS" :percentQ30 95.6  :totalBases 1450000000}])


(def grid-options
  "Options for every AG Grid table in this file (the same as in the
  homework file). Merge your own on top:

      [:> AgGridReact (merge grid-options {:columnDefs columns :rowData rows})]"
  {;; a compact look that fits more rows on screen
   :theme themeBalham
   ;; a checkbox on each row (and one at the top to tick them all), kept in
   ;; view on the left when the table scrolls sideways
   :rowSelection {:mode "multiRow"}
   :selectionColumnDef {:pinned "left"}
   ;; every cell can be edited - double-click one. (Edits change what the
   ;; table shows, not your data.)
   :defaultColDef {:editable true}
   ;; the text in cells can be selected and copied
   :enableCellTextSelection true
   :ensureDomOrder true})

(defn get-selected-rows
  "The selected rows, from one of AG Grid's events - like the one it gives
  :onSelectionChanged. AG Grid has them as JavaScript objects; js->clj
  turns them back into maps."
  [event]
  (mapv #(js->clj % :keywordize-keys true)
        (.getSelectedRows (.-api event))))


;; 1. Your First Grid ----------------------------------------------------------

(def sample-columns
  [{:headerName "Sample ID" :field "sampleId" :pinned "left"}
   {:headerName "Lineage" :field "lineage"}
   {:headerName "Collection Date" :field "collectionDate"}
   {:headerName "QC Status" :field "qcStatus"}])

(defn samples-grid
  "An AG Grid table of `samples`, a vector of maps like `example-samples`."
  [samples]
  [:> AgGridReact (merge grid-options
                         {:columnDefs sample-columns
                          :rowData samples
                          ;; as tall as its rows, so it doesn't need a height
                          :domLayout "autoHeight"})])


;; 2. Color-coded QC Status ----------------------------------------------------

(defn qc-color
  "The color to show a QC status in."
  [status]
  (case status
    "PASS" "green"
    "WARN" "orange"
    "FAIL" "red"
    nil))

(defn qc-status-column
  "An AG Grid column definition for the qcStatus field, colored with qc-color."
  []
  {:headerName "QC Status"
   :field "qcStatus"
   :cellStyle (fn [params]
                (when-let [color (qc-color (.-value params))]
                  #js {:color color :fontWeight "bold"}))})


;; 3. Friendly Numbers ---------------------------------------------------------

(defn format-percent
  "A percentage, to one decimal place: 97.25 -> \"97.3 %\""
  [n]
  (str (.toFixed n 1) " %"))

(defn format-bases
  "A number of bases, in gigabases: 1200000000 -> \"1.20 Gb\""
  [n]
  (str (.toFixed (/ n 1e9) 2) " Gb"))


;; 4. A Details Button ---------------------------------------------------------

(defonce selected-sample-id (r/atom nil))

(defn details-button
  "An AG Grid cell renderer: a button that selects this row's sample."
  [params]
  (let [id (.. params -data -sampleId)]
    (r/as-element
     [c/button {:variant :secondary
                :style {:padding "2px 10px" :font-size "12px"}
                :on-click #(reset! selected-sample-id id)}
      "View"])))

(defn sample-details
  "Shows the details of the sample whose ID is in selected-sample-id."
  [samples]
  (if-let [sample (first (filter #(= @selected-sample-id (:sampleId %)) samples))]
    [c/card {:title (:sampleId sample)}
     [c/label-value "Lineage" (:lineage sample)]
     [c/label-value "Collected" (:collectionDate sample)]
     [c/label-value "QC status" [:span {:style {:color (qc-color (:qcStatus sample))}}
                                 (:qcStatus sample)]]
     [c/label-value "% Q30" (format-percent (:percentQ30 sample))]
     [c/label-value "Total bases" (format-bases (:totalBases sample))]]
    [c/paragraph "Click View on a row to see its details."]))


;; 5. A Mini QC Dashboard ------------------------------------------------------

(defonce db
  (r/atom
   {:runs [{:runId "RUN_001" :date "2026-06-02" :instrument "Sequencer A"}
           {:runId "RUN_002" :date "2026-06-09" :instrument "Sequencer B"}
           {:runId "RUN_003" :date "2026-06-16" :instrument "Sequencer A"}]
    :libraries [{:runId "RUN_001" :libraryId "LIB-001" :species "Escherichia coli" :percentQ30 96.2 :depth 85.1 :qcStatus "PASS"}
                {:runId "RUN_001" :libraryId "LIB-002" :species "Salmonella enterica" :percentQ30 94.8 :depth 61.0 :qcStatus "PASS"}
                {:runId "RUN_001" :libraryId "LIB-003" :species "Staphylococcus aureus" :percentQ30 78.3 :depth 12.4 :qcStatus "FAIL"}
                {:runId "RUN_002" :libraryId "LIB-004" :species "Mycobacterium tuberculosis" :percentQ30 97.3 :depth 32.5 :qcStatus "PASS"}
                {:runId "RUN_002" :libraryId "LIB-005" :species "Escherichia coli" :percentQ30 88.9 :depth 24.0 :qcStatus "WARN"}
                {:runId "RUN_003" :libraryId "LIB-006" :species "Klebsiella pneumoniae" :percentQ30 95.1 :depth 70.2 :qcStatus "PASS"}]
    :selected-run-id nil}))

(defn libraries-for-run
  "The libraries in `libraries` that belong to the run `run-id`."
  [libraries run-id]
  (filterv #(= run-id (:runId %)) libraries))

(defn select-run!
  "Record that the run `run-id` is selected, in db's :selected-run-id."
  [run-id]
  (swap! db assoc :selected-run-id run-id))

(defn runs-table
  "The sequencing runs. Clicking one selects it."
  [runs]
  [:div {:style {:height "220px"}}
   [:> AgGridReact
    (merge grid-options
           {:columnDefs [{:field "runId" :headerName "Run" :pinned "left"}
                         {:field "date" :headerName "Date"}]
            :rowData runs
            :getRowId (fn [params] (.. params -data -runId))
            :rowSelection {:mode "singleRow" :enableClickSelection true}
            :onSelectionChanged (fn [event]
                                  (when-let [row (first (get-selected-rows event))]
                                    (select-run! (:runId row))))})]])

(defn libraries-table
  "The QC results of `libraries`."
  [libraries]
  [:div {:style {:height "220px"}}
   [:> AgGridReact
    (merge grid-options
           {:columnDefs [{:field "libraryId" :headerName "Library" :pinned "left"}
                         {:field "species" :headerName "Species"}
                         {:field "percentQ30" :headerName "% Q30"
                          :valueFormatter (fn [params] (format-percent (.-value params)))}
                         {:field "depth" :headerName "Depth"}
                         (qc-status-column)]
            :rowData libraries
            :getRowId (fn [params] (.. params -data -libraryId))})]])

(defn qc-dashboard
  "The runs table on the left, and the QC results of the selected run on the right."
  []
  (let [{:keys [runs libraries selected-run-id]} @db]
    [:div {:style {:display "grid" :grid-template-columns "1fr 2fr" :gap "16px"}}
     ;; runs-table is its own component, and only gets `runs` - so selecting
     ;; a run doesn't redraw it (or clear its selection)
     [runs-table runs]
     (if selected-run-id
       [libraries-table (libraries-for-run libraries selected-run-id)]
       [c/callout "Select a run to see its QC results."])]))

(comment
  (libraries-for-run (:libraries @db) "RUN_001")
  (select-run! "RUN_002")
  @db
  )
