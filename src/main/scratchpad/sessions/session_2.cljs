(ns scratchpad.sessions.session-2
  "Session 2 homework: tables with AG Grid, and a mini QC dashboard.

  Each exercise has:
    - a function for you to write (look for TODO)
    - a (comment ...) block with things to try at the REPL

  Your results show up live on the Session 2 Homework page as you save.

  Get started by loading this file into the REPL:
    Command Palette (Ctrl+Shift+P) ->
    \"Calva: Load/Evaluate Current File and its Requires/Dependencies\"
    (shortcut: Ctrl+Alt+C Enter)
  Then evaluate forms in the (comment ...) blocks with Alt+Enter."
  (:require ["ag-grid-react" :refer [AgGridReact]]
            [cljs.pprint :as pp]
            [reagent.core :as r]
            [scratchpad.components :as c]))

;; ===========================================================================
;; Example data
;;
;; AG Grid turns each row into a JavaScript object, so the keys here are
;; camelCase, like JavaScript's: :sampleId becomes the property sampleId,
;; which ClojureScript can read with (.-sampleId row).
;; ===========================================================================

(def example-samples
  [{:sampleId "S001" :lineage "XBB.1.5" :collectionDate "2026-06-01" :qcStatus "PASS" :percentQ30 97.25 :totalBases 1200000000}
   {:sampleId "S002" :lineage "EG.5"    :collectionDate "2026-06-03" :qcStatus "FAIL" :percentQ30 71.5  :totalBases 180000000}
   {:sampleId "S003" :lineage "JN.1"    :collectionDate "2026-06-04" :qcStatus "PASS" :percentQ30 93.1  :totalBases 950000000}
   {:sampleId "S004" :lineage "XBB.1.5" :collectionDate "2026-06-09" :qcStatus "WARN" :percentQ30 84.0  :totalBases 610000000}
   {:sampleId "S005" :lineage "BA.2.86" :collectionDate "2026-06-12" :qcStatus "PASS" :percentQ30 95.6  :totalBases 1450000000}])


;; ===========================================================================
;; 1. Your First Grid
;;
;; Show `samples` in an AG Grid table, with a column for the sample ID,
;; lineage, collection date and QC status.
;; ===========================================================================

(defn samples-grid
  "An AG Grid table of `samples`, a vector of maps like `example-samples`."
  [samples]
  ;; TODO: replace this with an AG Grid table. You'll need:
  ;;   - columns: a vector of maps like {:headerName "Sample ID" :field "sampleId"}
  ;;   - [:> AgGridReact {:columnDefs columns :rowData samples}]
  ;;     (:> is how Reagent uses a React component, like AgGridReact)
  ;;   - a box with a height around it, e.g. [:div {:style {:height "250px"}} ...]
  ;;     or :domLayout "autoHeight" to make the table as tall as its rows
  [c/todo "samples-grid"])

(comment
  (first example-samples)
  (map :sampleId example-samples)
  (keys (first example-samples))

  ;; Column definitions are plain data - build them however you like:
  (for [k [:sampleId :lineage]] {:field (name k)})
  )


;; ===========================================================================
;; 2. Color-coded QC Status
;;
;; Make the QC status column green for "PASS", orange for "WARN" and red
;; for "FAIL".
;; ===========================================================================

(defn qc-color
  "The color to show a QC status in: \"green\" for \"PASS\", \"orange\" for
  \"WARN\", \"red\" for \"FAIL\", and nil for anything else."
  [status]
  ;; TODO (hint: `case`)
  nil)

(defn qc-status-column
  "An AG Grid column definition for the qcStatus field, colored with qc-color."
  []
  ;; TODO: add a :cellStyle function. AG Grid calls it with a JavaScript
  ;; object, `params`, for each cell:
  ;;   - (.-value params) is the cell's value, e.g. "PASS"
  ;;   - it wants a JavaScript object back, like #js {:color "green"}
  ;;     (or nil for no special style)
  {:headerName "QC Status"
   :field "qcStatus"})

(comment
  (qc-color "PASS")
  (qc-color "FAIL")
  (qc-color "something else")

  ;; #js {...} makes a JavaScript object - which is what AG Grid wants
  #js {:color "green"}
  (.-value #js {:value "PASS"})

  ;; You can test your :cellStyle function by calling it like AG Grid would:
  ((:cellStyle (qc-status-column)) #js {:value "FAIL"})
  )


;; ===========================================================================
;; 3. Friendly Numbers
;;
;; Format the numbers in the table so they're easier to read.
;; ===========================================================================

(defn format-percent
  "A percentage, to one decimal place: 97.25 -> \"97.3 %\""
  [n]
  ;; TODO (hint: (.toFixed 97.25 1) gives "97.3")
  (str n))

(defn format-bases
  "A number of bases, in gigabases: 1200000000 -> \"1.20 Gb\""
  [n]
  ;; TODO: divide by a billion (1e9), and show two decimal places
  (str n))

(comment
  (format-percent 97.25)
  (format-bases 1200000000)
  (format-bases 180000000)
  (.toFixed 3.14159 2)
  (/ 1200000000 1e9)

  ;; The page uses your functions as AG Grid :valueFormatters, like this:
  (let [formatter (fn [params] (format-percent (.-value params)))]
    (formatter #js {:value 84}))
  )


;; ===========================================================================
;; 4. A Details Button
;;
;; Add a "View" button to each row. Clicking it shows that sample's details
;; underneath the table.
;; ===========================================================================

;; Which sample's details to show (its :sampleId), or nil.
(defonce selected-sample-id (r/atom nil))

(defn details-button
  "An AG Grid cell renderer: a button that selects this row's sample."
  [params]
  ;; TODO: AG Grid calls this for each row. (.. params -data -sampleId) is
  ;; the row's sample ID. Return a button that stores it in
  ;; selected-sample-id when clicked. AG Grid needs a React element, so
  ;; wrap your hiccup with r/as-element:
  ;;   (r/as-element [c/button {:on-click ...} "View"])
  (r/as-element [c/tag "TODO"]))

(defn sample-details
  "Shows the details of the sample whose ID is in selected-sample-id."
  [samples]
  ;; TODO: find the sample in `samples` whose :sampleId matches
  ;; @selected-sample-id, and show it - e.g. in a c/card with c/label-value
  (if-let [id @selected-sample-id]
    [c/paragraph "Selected: " id]
    [c/paragraph "Click View on a row to see its details."]))

(comment
  @selected-sample-id
  (reset! selected-sample-id "S003")
  (reset! selected-sample-id nil)

  ;; Finding one sample by its ID:
  (first (filter #(= "S003" (:sampleId %)) example-samples))
  )


;; ===========================================================================
;; 5. A Mini QC Dashboard
;;
;; Two tables, side by side: sequencing runs on the left, and the QC results
;; for the selected run on the right. Everything the dashboard shows comes
;; from one atom, `db` - an "app database".
;; ===========================================================================

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
  ;; TODO (hint: filter)
  [])

(defn select-run!
  "Record that the run `run-id` is selected, in db's :selected-run-id."
  [run-id]
  ;; TODO (hint: swap! and assoc)
  nil)

(defn runs-table
  "The sequencing runs. Clicking one selects it."
  [runs]
  [:div {:style {:height "220px"}}
   [:> AgGridReact
    {:columnDefs [{:field "runId" :headerName "Run"}
                  {:field "date" :headerName "Date"}]
     :rowData runs
     ;; lets AG Grid recognize each row, so it keeps the selection when
     ;; the page redraws
     :getRowId (fn [params] (.. params -data -runId))
     :rowSelection {:mode "singleRow" :checkboxes false :enableClickSelection true}
     ;; TODO: when the selection changes, call select-run! with the selected
     ;; run's ID. The selected rows are (.getSelectedRows (.-api event)) -
     ;; a JavaScript array of JavaScript objects.
     :onSelectionChanged (fn [event] nil)}]])

(defn qc-dashboard
  "The runs table on the left, and the QC results of the selected run on the right."
  []
  ;; TODO: lay out [runs-table ...] and a table of the selected run's
  ;; libraries side by side - e.g. a [:div] with
  ;;   {:style {:display "grid" :grid-template-columns "1fr 2fr" :gap "16px"}}
  ;; Read what you need from @db. (You've written most of the pieces!)
  (let [{:keys [runs]} @db]
    [runs-table runs]))

(comment
  @db
  (:runs @db)
  (:selected-run-id @db)

  (libraries-for-run (:libraries @db) "RUN_001")
  (select-run! "RUN_002")

  ;; The dashboard redraws whenever db changes. Try these and watch it:
  (swap! db assoc :selected-run-id "RUN_003")

  ;; Add a whole new run, with a library:
  (swap! db update :runs conj {:runId "TEST_RUN" :date "2026-07-01" :instrument "Sequencer C"})
  (swap! db update :libraries conj {:runId "TEST_RUN" :libraryId "LIB-TEST" :species "Vibrio cholerae"
                                    :percentQ30 91.0 :depth 40.0 :qcStatus "PASS"})
  (select-run! "TEST_RUN")

  ;; Change a QC status, and see the table update:
  (swap! db update :libraries
         (fn [libs] (mapv #(if (= "LIB-003" (:libraryId %)) (assoc % :qcStatus "PASS") %) libs)))

  ;; Turning a JavaScript object back into a ClojureScript map:
  (js->clj #js {:runId "RUN_001"} :keywordize-keys true)
  )
