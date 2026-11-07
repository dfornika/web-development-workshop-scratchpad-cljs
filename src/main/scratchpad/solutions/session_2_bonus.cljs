(ns scratchpad.solutions.session-2-bonus
  "Reference solutions for src/main/scratchpad/sessions/session_2_bonus.cljs.

  Just one way to do it - if yours works, it's right!"
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


;; 1. Filters, Sorting and Pages -----------------------------------------------

(defn big-samples-grid
  "A grid of `samples` with filters, sorting and pagination."
  [samples]
  [:div {:style {:height "420px"}}
   [:> AgGridReact
    {:columnDefs sample-columns
     :rowData samples
     :defaultColDef {:filter true :sortable true}
     :pagination true
     :paginationPageSize 10
     :paginationPageSizeSelector [10 20 50]}]])


;; 2. Export to CSV ------------------------------------------------------------

(defn exportable-grid
  "A grid of `samples`, with a button above it that exports them as CSV."
  [samples]
  (r/with-let [grid-api (atom nil)]
    [:div
     [c/button {:style {:margin-bottom "8px"}
                :on-click #(some-> @grid-api (.exportDataAsCsv #js {:fileName "samples.csv"}))}
      "Export CSV"]
     [:div {:style {:height "300px"}}
      [:> AgGridReact
       {:columnDefs sample-columns
        :rowData samples
        :onGridReady (fn [event] (reset! grid-api (.-api event)))}]]]))


;; 3. Tooltips -----------------------------------------------------------------

(defn tooltip-columns
  "The sample columns, with tooltips."
  []
  (for [column sample-columns]
    (cond-> (assoc column :headerTooltip (str "Each sample's " (.toLowerCase (:headerName column))))
      (= "sampleId" (:field column)) (assoc :tooltipField "collectionDate"))))


;; 4. Fix It From the Table ----------------------------------------------------

(defonce samples-db
  (r/atom (vec (take 6 example-samples))))

(defn toggle-status!
  "Flip the QC status of the sample with ID `sample-id`, in samples-db."
  [sample-id]
  (swap! samples-db
         (fn [samples]
           (mapv (fn [sample]
                   (if (= sample-id (:sampleId sample))
                     (update sample :qcStatus #(if (= "PASS" %) "FAIL" "PASS"))
                     sample))
                 samples))))

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
     :getRowId (fn [params] (.. params -data -sampleId))}]])
