(ns scratchpad.views.session-3
  "The Session 3 Homework page. It shows the live results of the functions in
  src/main/scratchpad/sessions/session_3.cljs and session_3_bonus.cljs -
  edit those files, not this one."
  (:require [scratchpad.components :as c]
            [scratchpad.sessions.session-3 :as s3]
            [scratchpad.sessions.session-3-bonus :as bonus]
            [scratchpad.ui :as ui]
            [scratchpad.views.homework :as hw]))

(def files
  {:s3 {:name "session_3.cljs"
        :path "src/main/scratchpad/sessions/session_3.cljs"
        :ns "scratchpad.sessions.session-3"}
   :bonus {:name "session_3_bonus.cljs"
           :path "src/main/scratchpad/sessions/session_3_bonus.cljs"
           :ns "scratchpad.sessions.session-3-bonus"}})

(defn compiled-lookup
  "Finds a homework function (or value) in the compiled code. Each branch
  reads the var when it's called, so a function re-defined at the REPL shows
  up the next time the page is drawn."
  [file sym]
  (case file
    :s3 (case sym
          example-mutations s3/example-mutations
          db s3/db
          load-mutations! s3/load-mutations!
          status-message s3/status-message
          mutation-label s3/mutation-label
          mutation-list s3/mutation-list
          dashboard-wireframe s3/dashboard-wireframe
          gene-summary s3/gene-summary
          mutations-for-gene s3/mutations-for-gene
          linked-tables s3/linked-tables
          mutation-frequencies s3/mutation-frequencies
          gene-chart s3/gene-chart
          filter-by-region s3/filter-by-region
          dashboard s3/dashboard
          nil)
    :bonus (case sym
             reports-per-gene bonus/reports-per-gene
             pie-dashboard bonus/pie-dashboard
             trend-explorer bonus/trend-explorer
             frequency-color bonus/frequency-color
             colored-table bonus/colored-table
             nil)
    nil))

;; ---------------------------------------------------------------------------
;; Pieces the page adds around the ones you write
;; ---------------------------------------------------------------------------

(defn- load-buttons
  "Exercise 1: buttons that call your load-mutations!."
  [{:keys [lookup]}]
  (let [load! (lookup :s3 'load-mutations!)
        db (lookup :s3 'db)
        small {:style {:padding "4px 10px" :font-size "13px"}}]
    (if-not (and (fn? load!) (satisfies? IDeref db))
      [c/callout {:kind :warning} "Can't find load-mutations! or db in session_3.cljs."]
      [c/row {:gap "8px"}
       [c/button (merge small {:on-click #(load!)}) "Load the data"]
       [c/button (merge small {:variant :secondary
                               :on-click #(load! "data/session-3/nope.json")})
        "Try a missing file"]
       [c/button (merge small {:variant :secondary
                               :on-click #(swap! db assoc :status :not-loaded :mutations [] :error nil)})
        "Start again"]])))

(defn- loaded-preview
  "Exercise 1: the first few reports, once they're loaded."
  [{:keys [lookup]}]
  (let [db (lookup :s3 'db)
        mutations (when (satisfies? IDeref db) (:mutations @db))]
    (when (seq mutations)
      [c/code-block (c/table-str [:gene :mutation :frequency :location :date]
                                 (take 3 mutations))])))

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
         edit (partial hw/edit ctx)
         examples (lookup :s3 'example-mutations)]
     [c/page
      [c/heading {:level 1} "Session 3: Homework"]
      [hw/intro ctx :s3]

      [c/section {:title "Data, and a dashboard for it"}
       [ui/exercise {:title "1. Load the Data"
                     :task "Fetch the mutation reports from a JSON file, keep them in the app database, and say what's happening: loading, loaded, or what went wrong."
                     :edit (edit :s3 "load-mutations! and status-message")}
        [c/column {:gap "8px"}
         [load-buttons ctx]
         [hw/component ctx :s3 'status-message]
         [loaded-preview ctx]]]

       [ui/exercise {:title "2. Show It"
                     :task "List the reports, one line each, with the gene, mutation, region, date and frequency."
                     :edit (edit :s3 "mutation-label and mutation-list")}
        [c/column {:gap "8px"}
         [hw/call ctx :s3 'mutation-label (first examples)]
         [hw/component ctx :s3 'mutation-list (take 5 examples)]]]

       [ui/exercise {:title "3. Sketch the Dashboard"
                     :task "Lay out the dashboard before any data goes in: a header, filters, and room for a table and a chart."
                     :edit (edit :s3 "dashboard-wireframe")}
        [hw/component ctx :s3 'dashboard-wireframe]]

       [ui/exercise {:title "4. Linked Tables"
                     :task "A summary of each gene on the left. Picking one shows its reports on the right."
                     :edit (edit :s3 "gene-summary, mutations-for-gene, select-gene! and genes-table")}
        [c/column {:gap "8px"}
         [hw/call ctx :s3 'mutations-for-gene (take 4 examples) "katG"]
         [hw/component ctx :s3 'linked-tables examples]]]

       [ui/exercise {:title "5. A Linked Chart"
                     :task "A bar chart of the selected gene's mutations, that follows the selection in the table above."
                     :edit (edit :s3 "mutation-frequencies and frequency-chart")}
        [c/column {:gap "8px"}
         [hw/call ctx :s3 'mutation-frequencies
          [{:mutation "S450L" :frequency 78} {:mutation "D435V" :frequency 15} {:mutation "S450L" :frequency 71}]]
         [hw/component ctx :s3 'gene-chart examples]]]

       [ui/exercise {:title "6. The Dashboard"
                     :task "Put it together with all 96 reports from the JSON file: the status of the data, a region filter, the linked tables and the chart."
                     :edit (edit :s3 "filter-by-region and dashboard")}
        [c/column {:gap "8px"}
         [hw/call ctx :s3 'filter-by-region (take 4 examples) "BC"]
         [hw/component ctx :s3 'dashboard]]]]

      [c/section {:title "Bonus: more charts"}
       [ui/exercise {:title "1. A Pie Chart, Linked to a Table"
                     :task "Tick reports in the table, and a pie chart shows how they're shared between genes."
                     :edit (edit :bonus "reports-per-gene, gene-pie and pie-dashboard")}
        [hw/component ctx :bonus 'pie-dashboard]]

       [ui/exercise {:title "2. A Trend Over Time"
                     :task "A line chart of a mutation's frequency over time, with a line for each region. Is C-15T becoming more common?"
                     :edit (edit :bonus "trend-series")}
        [hw/component ctx :bonus 'trend-explorer]]

       [ui/exercise {:title "3. Make the Numbers Stand Out"
                     :task "Color each frequency by how high it is, and show the drug when hovering over a mutation."
                     :edit (edit :bonus "frequency-color and frequency-columns")}
        [c/column {:gap "8px"}
         [hw/call ctx :bonus 'frequency-color 78]
         [hw/component ctx :bonus 'colored-table]]]]])))
