(ns scratchpad.views.session-2
  "The Session 2 Homework page. It shows the live results of the functions in
  src/main/scratchpad/sessions/session_2.cljs and session_2_bonus.cljs -
  edit those files, not this one."
  (:require ["ag-grid-react" :refer [AgGridReact]]
            [scratchpad.components :as c]
            [scratchpad.sessions.session-2 :as s2]
            [scratchpad.sessions.session-2-bonus :as bonus]
            [scratchpad.ui :as ui]
            [scratchpad.views.homework :as hw]))

(def files
  {:s2 {:name "session_2.cljs"
        :path "src/main/scratchpad/sessions/session_2.cljs"
        :ns "scratchpad.sessions.session-2"}
   :bonus {:name "session_2_bonus.cljs"
           :path "src/main/scratchpad/sessions/session_2_bonus.cljs"
           :ns "scratchpad.sessions.session-2-bonus"}})

(defn compiled-lookup
  "Finds a homework function (or value) in the compiled code. Each branch
  reads the var when it's called, so a function re-defined at the REPL shows
  up the next time the page is drawn."
  [file sym]
  (case file
    :s2 (case sym
          example-samples s2/example-samples
          samples-grid s2/samples-grid
          qc-color s2/qc-color
          qc-status-column s2/qc-status-column
          format-percent s2/format-percent
          format-bases s2/format-bases
          details-button s2/details-button
          sample-details s2/sample-details
          libraries-for-run s2/libraries-for-run
          qc-dashboard s2/qc-dashboard
          nil)
    :bonus (case sym
             example-samples bonus/example-samples
             big-samples-grid bonus/big-samples-grid
             exportable-grid bonus/exportable-grid
             sample-columns bonus/sample-columns
             tooltip-columns bonus/tooltip-columns
             editable-grid bonus/editable-grid
             nil)
    nil))

;; ---------------------------------------------------------------------------
;; Grids the page builds around the pieces you write
;; ---------------------------------------------------------------------------

(defn- grid [columns rows]
  [:div {:style {:height "250px"}}
   ;; A fresh grid whenever the columns change: AG Grid applies some column
   ;; options (like tooltips) only when it first draws a column, so without
   ;; this, edits to those wouldn't show up until the page was reloaded.
   ^{:key (pr-str columns)}
   [:> AgGridReact {:columnDefs columns :rowData rows}]])

(defn- status-grid
  "Exercise 2: a grid using your qc-status-column."
  [{:keys [lookup]}]
  (let [column-fn (lookup :s2 'qc-status-column)]
    [grid [{:headerName "Sample ID" :field "sampleId"}
           (column-fn)]
     (lookup :s2 'example-samples)]))

(defn- formatted-grid
  "Exercise 3: a grid that formats its numbers with your functions."
  [{:keys [lookup]}]
  (let [format-percent (lookup :s2 'format-percent)
        format-bases (lookup :s2 'format-bases)]
    [grid [{:headerName "Sample ID" :field "sampleId"}
           {:headerName "% Q30" :field "percentQ30"
            :valueFormatter (fn [params] (format-percent (.-value params)))}
           {:headerName "Total bases" :field "totalBases"
            :valueFormatter (fn [params] (format-bases (.-value params)))}]
     (lookup :s2 'example-samples)]))

(defn- details-grid
  "Exercise 4: a grid with your details-button in each row."
  [{:keys [lookup]}]
  [grid [{:headerName "Sample ID" :field "sampleId"}
         {:headerName "Lineage" :field "lineage"}
         {:headerName "" :cellRenderer (lookup :s2 'details-button)}]
   (lookup :s2 'example-samples)])

(defn- tooltip-grid
  "Bonus 3: a grid using your tooltip-columns."
  [{:keys [lookup]}]
  [grid ((lookup :bonus 'tooltip-columns))
   (take 5 (lookup :bonus 'example-samples))])

;; ---------------------------------------------------------------------------
;; The page
;; ---------------------------------------------------------------------------

(defn page
  "Options:
     :lookup       how to find the homework functions (default: compiled-lookup)
     :in-browser?  true in the in-browser version, where the files are edited on the page"
  ([] (page {}))
  ([{:keys [lookup in-browser?] :or {lookup compiled-lookup}}]
   (let [ctx {:lookup lookup :files files :in-browser? in-browser?}
         edit (partial hw/edit ctx)]
     [c/page
      [c/heading {:level 1} "Session 2: Homework"]
      [hw/intro ctx :s2]

      [c/section {:title "Tables with AG Grid"}
       [ui/exercise {:title "1. Your First Grid"
                     :task "Show the example samples in an AG Grid table, with columns for the sample ID, lineage, collection date and QC status."
                     :edit (edit :s2 "samples-grid")}
        [hw/component ctx :s2 'samples-grid (lookup :s2 'example-samples)]]

       [ui/exercise {:title "2. Color-coded QC Status"
                     :task "Show QC statuses in color: green for PASS, orange for WARN, red for FAIL."
                     :edit (edit :s2 "qc-color and qc-status-column")}
        [c/column {:gap "8px"}
         [hw/call ctx :s2 'qc-color "PASS"]
         [hw/call ctx :s2 'qc-color "FAIL"]
         [status-grid ctx]]]

       [ui/exercise {:title "3. Friendly Numbers"
                     :task "Format percentages and base counts so they're easy to read."
                     :edit (edit :s2 "format-percent and format-bases")}
        [c/column {:gap "8px"}
         [hw/call ctx :s2 'format-percent 97.25]
         [hw/call ctx :s2 'format-bases 1200000000]
         [formatted-grid ctx]]]

       [ui/exercise {:title "4. A Details Button"
                     :task "Add a View button to each row. Clicking it shows that sample's details underneath the table."
                     :edit (edit :s2 "details-button and sample-details")}
        [c/column {:gap "8px"}
         [details-grid ctx]
         [hw/component ctx :s2 'sample-details (lookup :s2 'example-samples)]]]

       [ui/exercise {:title "5. A Mini QC Dashboard"
                     :task "Two tables side by side: sequencing runs on the left, and the QC results of the selected run on the right - all driven by one app-db atom."
                     :edit (edit :s2 "libraries-for-run, select-run! and qc-dashboard")}
        [c/column {:gap "8px"}
         [hw/call ctx :s2 'libraries-for-run
          [{:runId "RUN_001" :libraryId "LIB-001"} {:runId "RUN_002" :libraryId "LIB-004"}]
          "RUN_001"]
         [hw/component ctx :s2 'qc-dashboard]]]]

      [c/section {:title "Bonus: more AG Grid features"}
       [ui/exercise {:title "1. Filters, Sorting and Pages"
                     :task "With 40 samples, let people filter and sort every column, and show 10 rows per page."
                     :edit (edit :bonus "big-samples-grid")}
        [hw/component ctx :bonus 'big-samples-grid (lookup :bonus 'example-samples)]]

       [ui/exercise {:title "2. Export to CSV"
                     :task "Add a button that downloads the table as a CSV file."
                     :edit (edit :bonus "exportable-grid")}
        [hw/component ctx :bonus 'exportable-grid (take 10 (lookup :bonus 'example-samples))]]

       [ui/exercise {:title "3. Tooltips"
                     :task "Show more information when hovering over a header or a cell."
                     :edit (edit :bonus "tooltip-columns")}
        [tooltip-grid ctx]]

       [ui/exercise {:title "4. Fix It From the Table"
                     :task "Put a button in each row that flips its QC status between PASS and FAIL, and watch the table update."
                     :edit (edit :bonus "toggle-status!")}
        [hw/component ctx :bonus 'editable-grid]]]])))
