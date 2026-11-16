(ns scratchpad.solutions.session-3
  "Reference solutions for src/main/scratchpad/sessions/session_3.cljs.

  These are just one way to do it - if yours works, it's right!
  The function names match the homework file, so you can compare them side
  by side, or copy a function across to see it on the Session 3 page.

  To try them at the REPL: load this file (Ctrl+Alt+C Enter), then evaluate
  the forms in the (comment ...) blocks."
  (:require ["ag-charts-react" :refer [AgCharts]]
            ["ag-grid-community" :refer [themeBalham]]
            ["ag-grid-react" :refer [AgGridReact]]
            [cljs.pprint :as pp]
            [reagent.core :as r]
            [scratchpad.components :as c]))

;; ===========================================================================
;; Example data
;;
;; A few reports of mutations that make tuberculosis resistant to a drug.
;; :frequency is the percentage of resistant isolates carrying the mutation.
;; (Made-up numbers - but real genes and mutations.)
;;
;; The whole dataset - 96 reports - is in a JSON file, which exercise 1
;; loads: resources/public/data/session-3/mutations.json
;; ===========================================================================

(def example-mutations
  [{:id 1  :gene "rpoB" :mutation "S450L" :drug "rifampicin"       :frequency 78 :location "BC" :date "2026-03-04"}
   {:id 2  :gene "katG" :mutation "S315T" :drug "isoniazid"        :frequency 60 :location "AB" :date "2026-03-11"}
   {:id 3  :gene "inhA" :mutation "C-15T" :drug "isoniazid"        :frequency 42 :location "BC" :date "2026-03-20"}
   {:id 4  :gene "rpoB" :mutation "D435V" :drug "rifampicin"       :frequency 15 :location "ON" :date "2026-04-02"}
   {:id 5  :gene "embB" :mutation "M306V" :drug "ethambutol"       :frequency 31 :location "SK" :date "2026-04-09"}
   {:id 6  :gene "rpoB" :mutation "S450L" :drug "rifampicin"       :frequency 71 :location "ON" :date "2026-04-15"}
   {:id 7  :gene "gyrA" :mutation "D94G"  :drug "fluoroquinolones" :frequency 22 :location "MB" :date "2026-05-06"}
   {:id 8  :gene "katG" :mutation "S315T" :drug "isoniazid"        :frequency 66 :location "BC" :date "2026-05-13"}
   {:id 9  :gene "rpoB" :mutation "H445Y" :drug "rifampicin"       :frequency 9  :location "AB" :date "2026-05-27"}
   {:id 10 :gene "gyrA" :mutation "A90V"  :drug "fluoroquinolones" :frequency 14 :location "ON" :date "2026-06-03"}])


;; ===========================================================================
;; The app database, and settings for every table
;; ===========================================================================

;; Everything the dashboard shows comes from here.
(defonce db
  (r/atom {:status :not-loaded   ; then :loading, and :loaded or :error
           :mutations []         ; the reports, once they're loaded
           :error nil            ; what went wrong, if loading failed
           :selected-gene nil    ; the gene picked in the genes table
           :region "All"}))      ; the region picked in the dashboard's filter

(def grid-options
  "Options for every AG Grid table in this file (the same as in Session 2).
  Merge your own on top:

      [:> AgGridReact (merge grid-options {:columnDefs columns :rowData rows})]"
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

(defn average
  "The average of some numbers, to one decimal place: [1 2 4] -> 2.3"
  [xs]
  (when (seq xs)
    (/ (js/Math.round (* 10 (/ (reduce + xs) (count xs)))) 10)))


;; ===========================================================================
;; 1. Load the Data
;;
;; The data lives in a JSON file next to the page:
;;   resources/public/data/session-3/mutations.json
;; Anything in resources/public is served by the dev server, so the page
;; can fetch it from "data/session-3/mutations.json". (Try opening that
;; address in your browser, after the app's address.)
;; ===========================================================================

(def data-url "data/session-3/mutations.json")

(defn load-mutations!
  "Fetch the mutation reports from `url` (data-url, if you don't give one),
  and keep them in db. While they're loading, db's :status is :loading;
  then it's :loaded or :error."
  ([] (load-mutations! data-url))
  ([url]
   (swap! db assoc :status :loading :error nil)
   (-> (js/fetch url)
       (.then (fn [response]
                ;; fetch only fails by itself when the network does. A
                ;; missing file is still a response - with .-ok false - so
                ;; we check, and fail ourselves.
                (if (.-ok response)
                  (.json response)   ; read the body as JSON (another promise)
                  (throw (js/Error. (str "couldn't load " url " (" (.-status response) ")"))))))
       (.then (fn [data]
                (swap! db assoc
                       :mutations (js->clj data :keywordize-keys true)
                       :status :loaded)))
       (.catch (fn [error]
                 (swap! db assoc :status :error :error (ex-message error)))))))

(defn status-message
  "What the dashboard says about the data: that it's loading, that it
  couldn't be loaded (and why), or how many reports there are."
  []
  (let [{:keys [status mutations error]} @db]
    (case status
      :not-loaded [c/paragraph "Not loaded yet."]
      :loading [c/paragraph "Loading…"]
      :error [c/callout {:kind :error} [:strong "Couldn't load the data: "] error]
      :loaded [c/paragraph "Loaded " (count mutations) " mutation reports."])))

(comment
  (load-mutations!)
  @db
  (:status @db)
  (count (:mutations @db))
  (first (:mutations @db))

  ;; A file that isn't there - what does status-message show?
  (load-mutations! "data/session-3/nope.json")

  ;; Start again
  (swap! db assoc :status :not-loaded :mutations [] :error nil)

  ;; js->clj turns JavaScript data into ClojureScript data:
  (js->clj #js [#js {:gene "rpoB"}] :keywordize-keys true)

  ;; Promises: .then runs a function when the value arrives
  (-> (js/fetch data-url)
      (.then #(.json %))
      (.then #(js/console.log "loaded" (.-length %))))
  )


;; ===========================================================================
;; 2. Show It
;;
;; A list of the reports, one line each:  rpoB - S450L (BC, 2026-03-04)  78%
;; ===========================================================================

(defn mutation-label
  "One line about a report, e.g. \"rpoB - S450L (BC, 2026-03-04)\"."
  [m]
  (str (:gene m) " - " (:mutation m) " (" (:location m) ", " (:date m) ")"))

(defn mutation-list
  "A list of `mutations`, one line each."
  [mutations]
  [:ul {:style {:margin 0 :padding-left "20px" :line-height "1.6"}}
   ;; ^{:key ...} gives React a name for each item, so it can tell them
   ;; apart when the list changes
   (for [m mutations]
     ^{:key (:id m)}
     [:li (mutation-label m) [:strong " " (:frequency m) "%"]])])

(comment
  (mutation-label (first example-mutations))
  (map mutation-label (take 3 example-mutations))
  (c/print-html [mutation-list (take 2 example-mutations)])
  )


;; ===========================================================================
;; 3. Sketch the Dashboard
;;
;; Before wiring up any data, lay out where everything goes - a wireframe.
;; The dashboard will have: a header, filters (a region and a date), and a
;; table and a chart, side by side.
;; ===========================================================================

(defn placeholder
  "A dashed box saying what will go there."
  [label height]
  [:div {:style {:border "2px dashed #8c959f"
                 :border-radius "6px"
                 :height height
                 :display "flex"
                 :align-items "center"
                 :justify-content "center"
                 :color "#57606a"}}
   label])

(defn dashboard-wireframe
  "The dashboard's layout, with placeholders where the real parts will go."
  []
  [:div {:style {:display "flex" :flex-direction "column" :gap "12px"}}
   [:header {:style {:background-color "#004a87" :color "white"
                     :padding "10px 16px" :border-radius "6px"}}
    [:strong "AMR Mutation Dashboard"]]
   [:div {:style {:display "flex" :gap "16px"}}
    [:label "Region: " [:select [:option "All"] [:option "BC"] [:option "ON"]]]
    [:label "From: " [:input {:type "date"}]]]
   [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "12px"}}
    [placeholder "Table goes here" "200px"]
    [placeholder "Chart goes here" "200px"]]])

(comment
  (c/hiccup (dashboard-wireframe))
  (c/print-html [placeholder "Table" "100px"])
  )


;; ===========================================================================
;; 4. Linked Tables
;;
;; Two tables: a summary of each gene on the left, and - when you pick a
;; gene - its reports on the right. Picking a gene stores it in db's
;; :selected-gene; the reports table reads it from there.
;; ===========================================================================

(defn gene-summary
  "One row for each gene in `mutations`, most reports first:
    ({:gene \"rpoB\" :drug \"rifampicin\" :reports 4 :averageFrequency 43.3} ...)"
  [mutations]
  (->> (group-by :gene mutations)
       (map (fn [[gene reports]]
              {:gene gene
               :drug (:drug (first reports))
               :reports (count reports)
               :averageFrequency (average (map :frequency reports))}))
       (sort-by :reports >)))

(defn mutations-for-gene
  "The reports in `mutations` for `gene`."
  [mutations gene]
  (filterv #(= gene (:gene %)) mutations))

(defn select-gene!
  "Record that `gene` is selected, in db's :selected-gene."
  [gene]
  (swap! db assoc :selected-gene gene))

(defn genes-table
  "The summary of each gene. Picking one selects it.

  (It doesn't read db itself: if it did, it would be redrawn every time the
  selection changed - and AG Grid would forget the selection.)"
  [mutations]
  [:div {:style {:height "260px"}}
   [:> AgGridReact
    (merge grid-options
           {:columnDefs [{:field "gene" :headerName "Gene" :pinned "left"}
                         {:field "drug" :headerName "Drug"}
                         {:field "reports" :headerName "Reports"}
                         {:field "averageFrequency" :headerName "Avg %"}]
            :rowData (gene-summary mutations)
            ;; lets AG Grid recognize each row across redraws
            :getRowId (fn [params] (.. params -data -gene))
            ;; one gene at a time - tick it, or click anywhere on its row
            :rowSelection {:mode "singleRow" :enableClickSelection true}
            :onSelectionChanged (fn [event]
                                  (select-gene! (:gene (first (get-selected-rows event)))))})]])

(defn reports-table
  "The reports in `rows`."
  [rows]
  [:div {:style {:height "260px"}}
   [:> AgGridReact
    (merge grid-options
           {:columnDefs [{:field "mutation" :headerName "Mutation"}
                         {:field "frequency" :headerName "%"}
                         {:field "location" :headerName "Region"}
                         {:field "date" :headerName "Date"}]
            :rowData rows})]])

(defn linked-tables
  "The genes table, and the selected gene's reports beside it."
  [mutations]
  (let [gene (:selected-gene @db)]
    [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "12px"}}
     [genes-table mutations]
     (if gene
       [:div
        [:div {:style {:font-weight "bold" :margin-bottom "4px"}} "Reports for " gene]
        [reports-table (mutations-for-gene mutations gene)]]
       [c/callout "Pick a gene in the table to see its reports."])]))

(comment
  (group-by :gene (take 4 example-mutations))
  (gene-summary example-mutations)
  (pp/print-table (gene-summary example-mutations))
  (mutations-for-gene example-mutations "katG")

  (select-gene! "rpoB")
  (:selected-gene @db)
  (select-gene! nil)
  )


;; ===========================================================================
;; 5. A Linked Chart
;;
;; A bar chart of the selected gene's mutations: how common each one is.
;; Pick another gene in the table, and the chart follows.
;; ===========================================================================

(defn mutation-frequencies
  "The average frequency of each mutation in `rows`, most common first:
    ({:mutation \"S450L\" :frequency 74.5} ...)"
  [rows]
  (->> (group-by :mutation rows)
       (map (fn [[mutation reports]]
              {:mutation mutation
               :frequency (average (map :frequency reports))}))
       (sort-by :frequency >)))

(defn frequency-chart
  "A bar chart of the mutations in `rows` (the reports for one gene)."
  [rows gene]
  (if (empty? rows)
    [c/callout "Pick a gene to see its mutations."]
    [:div {:style {:height "300px"}}
     [:> AgCharts
      {:style {:height "100%"}
       :options {:title {:text (str "Mutations in " gene)}
                 :data (mutation-frequencies rows)
                 :series [{:type "bar"
                           :xKey "mutation" :xName "Mutation"
                           :yKey "frequency" :yName "Average %"}]}}]]))

(defn gene-chart
  "The chart of whichever gene is selected."
  [mutations]
  (let [gene (:selected-gene @db)]
    [frequency-chart (mutations-for-gene mutations gene) gene]))

(comment
  (mutation-frequencies (mutations-for-gene example-mutations "rpoB"))

  ;; The chart redraws whenever db's :selected-gene changes:
  (select-gene! "katG")
  (select-gene! "rpoB")
  )


;; ===========================================================================
;; 6. The Dashboard
;;
;; Put it all together, with the whole dataset from the JSON file: the
;; status of the data, a region filter, the linked tables, and the chart.
;; ===========================================================================

(defn filter-by-region
  "The reports in `mutations` from `region` - or all of them, if `region`
  is \"All\"."
  [mutations region]
  (if (= "All" region)
    mutations
    (filterv #(= region (:location %)) mutations)))

(defn region-filter
  "A dropdown of the regions in `mutations`, that sets db's :region."
  [mutations]
  [:label "Region: "
   [:select {:value (:region @db)
             :on-change #(swap! db assoc :region (.. % -target -value))}
    (for [region (cons "All" (sort (distinct (map :location mutations))))]
      ^{:key region} [:option {:value region} region])]])

(defn dashboard
  "The whole dashboard."
  []
  ;; r/with-let runs this once, when the dashboard is first shown: it starts
  ;; loading the data if nothing has yet
  (r/with-let [_ (when (= :not-loaded (:status @db)) (load-mutations!))]
    (let [{:keys [mutations region]} @db]
      [:div {:style {:display "flex" :flex-direction "column" :gap "12px"}}
       [:header {:style {:background-color "#004a87" :color "white"
                         :padding "10px 16px" :border-radius "6px"}}
        [:strong "AMR Mutation Dashboard"]]
       [status-message]
       (when (seq mutations)
         (let [shown (filter-by-region mutations region)]
           [:<>
            [region-filter mutations]
            [linked-tables shown]
            [gene-chart shown]]))])))

(comment
  (filter-by-region example-mutations "BC")
  (filter-by-region example-mutations "All")
  (swap! db assoc :region "ON")

  ;; Which regions are there, and how many reports from each?
  (frequencies (map :location (:mutations @db)))
  )
