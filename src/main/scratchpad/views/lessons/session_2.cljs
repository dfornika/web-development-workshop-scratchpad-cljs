(ns scratchpad.views.lessons.session-2
  "Session 2 lesson slides: Web Development with Reagent & AG Grid."
  (:require ["ag-grid-community" :refer [themeBalham]]
            ["ag-grid-react" :refer [AgGridReact]]
            [reagent.core :as r]
            [scratchpad.components :as c]
            [scratchpad.views.slides :as slides :refer [code two-columns]]))

(defn- small [& children]
  (into [:div {:style {:font-size "1.6cqw" :color "#57606a"}}] children))

(defn- example-box [title & children]
  [:div {:style {:border "1px solid #d0d7de"
                 :border-radius "8px"
                 :padding "1.4cqw 1.8cqw"
                 :background-color "#f6f8fa"}}
   [:div {:style {:font-weight "bold" :margin-bottom "0.6cqw"}} title]
   (into [:div {:style {:font-size "1.7cqw"}}] children)])

(defn- live [& children]
  (into [:div {:style {:border "2px dashed #54aeff"
                       :border-radius "8px"
                       :padding "1cqw"
                       :position "relative"}}
         [:div {:style {:position "absolute" :top "-1.2cqw" :right "1cqw"
                        :background-color "white" :padding "0 0.6cqw"
                        :font-size "1.3cqw" :color "#0969da"}}
          "live"]]
        children))

;; ---------------------------------------------------------------------------
;; Live examples shown on the slides
;; ---------------------------------------------------------------------------

(def ^:private demo-samples
  [{:sampleId "S001" :lineage "XBB.1.5" :collectionDate "2026-06-01" :qcStatus "PASS"}
   {:sampleId "S002" :lineage "EG.5" :collectionDate "2026-06-03" :qcStatus "FAIL"}
   {:sampleId "S003" :lineage "JN.1" :collectionDate "2026-06-04" :qcStatus "PASS"}])

(defn- counter-demo []
  (r/with-let [clicks (r/atom 0)]
    [c/row {:align "center"}
     [c/button {:on-click #(swap! clicks inc)} "Click me"]
     [:span "Clicked " @clicks " times"]]))

(defn- first-grid-demo []
  [:div {:style {:height "15cqw"}}
   [:> AgGridReact {:columnDefs [{:headerName "Sample ID" :field "sampleId"}
                                 {:headerName "Lineage" :field "lineage"}
                                 {:headerName "Date" :field "collectionDate"}]
                    :rowData demo-samples}]])

(defn- styled-grid-demo []
  [:div {:style {:height "15cqw"}}
   [:> AgGridReact {:columnDefs [{:headerName "Sample ID" :field "sampleId"}
                                 {:headerName "QC Status" :field "qcStatus"
                                  :cellStyle (fn [params]
                                               (case (.-value params)
                                                 "PASS" #js {:color "green"}
                                                 "FAIL" #js {:color "red"}
                                                 nil))}]
                    :rowData demo-samples}]])

(defn- button-grid-demo []
  (r/with-let [viewing (r/atom nil)]
    [:div
     [:div {:style {:height "15cqw"}}
      [:> AgGridReact {:columnDefs [{:headerName "Sample ID" :field "sampleId"}
                                    {:headerName "Details"
                                     ;; ^js: this demo is compiled, and :advanced builds
                                     ;; need it to keep the name `sampleId`
                                     :cellRenderer (fn [^js params]
                                                     (r/as-element
                                                      [c/button {:style {:padding "2px 10px" :font-size "12px"}
                                                                 :on-click #(reset! viewing (.. params -data -sampleId))}
                                                       "View"]))}]
                       :rowData demo-samples}]]
     [:div {:style {:font-size "1.6cqw" :margin-top "0.6cqw"}}
      "Viewing: " (or @viewing "nothing yet")]]))

(defn- balham-demo []
  [:div {:style {:height "13cqw"}}
   [:> AgGridReact {:theme themeBalham
                    :columnDefs [{:headerName "Sample ID" :field "sampleId"}
                                 {:headerName "Lineage" :field "lineage"}]
                    :rowData demo-samples}]])

;; ---------------------------------------------------------------------------
;; The slides
;; ---------------------------------------------------------------------------

(def slides
  [;; ---------------------------------------------------------------- intro
   {:body
    [[:div {:style {:display "flex"
                    :flex-direction "column"
                    :justify-content "center"
                    :align-items "center"
                    :text-align "center"
                    :min-height "40cqw"}}
      [:img {:src "images/logo-molecule.svg" :alt "" :style {:height "12cqw"}}]
      [:h1 {:style {:font-size "5cqw" :color "#004a87" :margin "2cqw 0 1cqw 0"}}
       "Session 2: Reagent & AG Grid"]
      [:p {:style {:font-size "2.4cqw" :color "#57606a" :margin 0}}
       "Web Development Workshop"]]]}

   {:title "Today"
    :body
    [[:p "Building on Session 1, we'll look at:"]
     [c/bullet-list
      [:span [:strong "Reagent and React"] " - how they fit together"]
      [:span [:strong "React's virtual DOM"] " - and why it suits functional programming"]
      [:span [:strong "Hiccup"] " - HTML as ClojureScript data (a quick recap)"]
      [:span [:strong "App state"] " - one atom that the whole app reads from and writes to"]
      [:span [:strong "AG Grid"] " - interactive tables: sorting, filtering, formatting, buttons in cells"]]]}

   ;; -------------------------------------------------------- reagent/react
   {:title "Reagent and React"
    :body
    [[two-columns
      [:div
       [c/bullet-list
        [:span [:strong "React"] " is a JavaScript library for building user interfaces from components and state."]
        [:span [:strong "Reagent"] " is a small ClojureScript layer over React: you write components as functions that return hiccup."]
        [:span "Reagent turns your hiccup into React elements, and works with any React component - like AG Grid's."]
        [:span "It encourages a functional, declarative style: describe what the page should look like, not how to change it."]]]
      [:div
       [example-box "A component"
        [code "(defn hello []
  [:h1 \"Welcome to Reagent!\"])"]]
       [:div {:style {:height "1.5cqw"}}]
       [example-box "Drawing it on the page"
        [code "(defonce root
  (rdomc/create-root
    (.getElementById js/document \"app\")))

(rdomc/render @root [hello])"]
        [small "This is what " [:code "src/main/scratchpad/main.cljs"] " does, using "
         [:code "reagent.dom.client"] "."]]]]]}

   {:title "React's virtual DOM"
    :body
    [[two-columns
      [:div
       [:strong "How it works"]
       [c/bullet-list
        "React keeps a lightweight copy of the page in memory: the virtual DOM."
        "When state changes, React works out what's different - a \"diff\"."
        "Then it updates only those parts of the real page."]]
      [:div
       [:strong "Why it suits ClojureScript"]
       [c/bullet-list
        "Immutable data is quick to compare: if it's the same object, nothing changed."
        "Pure functions + declarative code = predictable diffs and fewer bugs."
        "Functional programming and the virtual DOM complement each other perfectly."]]]
     [c/callout {:style {:margin-top "2cqw"}}
      "You'll see the effect in AG Grid: change the data, and only the changed cells update."]]}

   {:title "Hiccup recap: HTML as data"
    :body
    [[two-columns
      [code "[:div
 [:h2 \"QC Dashboard\"]
 [:p {:style {:color \"blue\"}}
  \"Welcome to our site!\"]]"]
      [code {:language :html}
       "<div>
  <h2>QC Dashboard</h2>
  <p style=\"color:blue\">
    Welcome to our site!
  </p>
</div>"]]
     [c/bullet-list
      "Vectors are elements"
      "Maps are attributes"
      "Nesting is the element hierarchy"]
     [small "Hiccup is declarative, composable and readable - and because it's just data, "
      "you can build it with map, filter and friends."]]}

   ;; ---------------------------------------------------------------- state
   {:title "State: Reagent atoms"
    :body
    [[two-columns
      [:div
       [code "(defn counter []
  (r/with-let [clicks (r/atom 0)]
    [c/row
     [c/button {:on-click #(swap! clicks inc)}
      \"Click me\"]
     [:span \"Clicked \" @clicks \" times\"]]))"]
       [live [counter-demo]]]
      [c/bullet-list
       [:span [:code "r/atom"] " holds a value that can change."]
       [:span "A component that reads it with " [:code "@"] " redraws when it changes."]
       [:span [:code "swap!"] " and " [:code "reset!"] " change it."]
       [:span [:code "r/with-let"] " keeps the atom between redraws."]]]]}

   {:title "One atom for the whole app"
    :body
    [[:p "Bigger apps keep " [:em "all"] " their state in one Reagent atom - an "
      [:strong "app database"] ", often called " [:code "db"] ":"]
     [code "(defonce db
  (r/atom {:runs [...]                 ; everything loaded
           :libraries [...]
           :selected-run-id nil}))     ; what the user picked"]
     [two-columns
      [example-box "Components read from it"
       [code "(:runs @db)"]
       [small "...and redraw when it changes."]]
      [example-box "Events write to it"
       [code "(swap! db assoc :selected-run-id \"RUN_002\")"]
       [small "Clicks, loaded data, timers..."]]]
     [small "This site has one too: " [:code "scratchpad.state/db"] " - try "
      [:code "(swap! db assoc :message \"Hi\")"] " in the Playground."]]}

   {:title "Example: a QC dashboard"
    :body
    [[two-columns
      [:div
       [:p "A Reagent app that tracks sequencing QC:"]
       [c/bullet-list
        "A header"
        "A table of sequencing runs on the left"
        "The selected run's library QC results on the right"
        "Everything - loaded data and selections - in db"]
       [small "A bigger version: "
        [c/link {:href "https://github.com/dfornika/genomics-qc-site-demo"} "genomics-qc-site-demo"]
        " on GitHub."]]
      [:div
       [example-box "The layout"
        [code "[:div {:style
       {:display \"grid\"
        :grid-template-columns \"1fr 3fr\"}}
 [runs-table]
 [library-qc-table]]"]]
       [:div {:style {:height "1.5cqw"}}]
       [example-box "The state"
        [code "{:runs [...]
 :selected-run-id \"RUN_002\"
 :libraries [...]}"]]]]]}

   {:title "How events change state"
    :body
    [[code "(defn select-run! [run-id]
  (swap! db assoc :selected-run-id run-id))

[:> AgGridReact
 {:rowData (:runs @db)
  :onSelectionChanged
  (fn [event]
    (let [row (first (.getSelectedRows (.-api event)))]
      (select-run! (.-runId row))))}]"]
     [c/bullet-list
      "The user clicks a run in the table."
      [:span "The grid calls " [:code ":onSelectionChanged"] ", which updates " [:code "db"] "."]
      "Everything that reads :selected-run-id redraws - the QC table shows the new run."]]}

   {:title "Try it: change the app from the REPL"
    :body
    [[:p "In the homework's " [:code "(comment ...)"] " block, evaluate these one at a time and watch the dashboard:"]
     [code ";; add a run that doesn't exist anywhere else
(swap! db update :runs conj
       {:runId \"TEST_RUN\" :date \"2026-07-01\"})

;; ...give it a library...
(swap! db update :libraries conj
       {:runId \"TEST_RUN\" :libraryId \"LIB-TEST\"
        :species \"Vibrio cholerae\" :percentQ30 91.0 :qcStatus \"PASS\"})

;; ...and select it
(swap! db assoc :selected-run-id \"TEST_RUN\")"]
     [small "Then try changing a QC status, or a Q30 value, and watch the table update. "
      [:code "@db"] " shows you everything."]]}

   ;; -------------------------------------------------------------- AG Grid
   {:title "AG Grid"
    :body
    [[two-columns
      [:div
       [:p "A powerful JavaScript table library, for large datasets:"]
       [c/bullet-list
        "Sorting and filtering"
        "Formatting values, and coloring cells by their value"
        "Buttons and other components inside cells"
        "Pagination, tooltips, CSV export..."]]
      [:div
       [:p "With Reagent:"]
       [c/bullet-list
        "Columns and rows are plain ClojureScript data"
        "AG Grid takes care of the layout, rendering and interaction"
        "Reagent atoms keep the table up to date"]]]
     [c/callout {:style {:margin-top "1.5cqw"}}
      "It's already installed in this project, and ready to use."]]}

   {:title "Your first grid"
    :body
    [[two-columns
      [code "(:require [\"ag-grid-react\" :refer [AgGridReact]])

(def columns
  [{:headerName \"Sample ID\" :field \"sampleId\"}
   {:headerName \"Lineage\" :field \"lineage\"}
   {:headerName \"Date\" :field \"collectionDate\"}])

(def rows
  [{:sampleId \"S001\" :lineage \"XBB.1.5\"
    :collectionDate \"2026-06-01\"}
   ...])

[:div {:style {:height \"250px\"}}
 [:> AgGridReact {:columnDefs columns
                  :rowData rows}]]"]
      [:div
       [live [first-grid-demo]]
       [c/bullet-list
        [:span [:code ":columnDefs"] ": what each column shows"]
        [:span [:code ":rowData"] ": the rows - a vector of maps"]
        [:span [:code "[:> ...]"] ": how Reagent uses a React component"]
        "The grid needs a height - or :domLayout \"autoHeight\""]]]]}

   {:title "Talking to AG Grid: JavaScript objects"
    :body
    [[:p "AG Grid is a JavaScript library. When it calls your functions, it passes "
      [:strong "JavaScript objects"] ", and it wants JavaScript objects back:"]
     [code "(.-value params)            ; read a property: params.value
(.. params -data -sampleId)  ; params.data.sampleId
#js {:color \"green\"}        ; make a JavaScript object
(js->clj obj :keywordize-keys true)  ; JavaScript object -> ClojureScript map"]
     [c/bullet-list
      [:span "Your row maps become JavaScript objects too - so use camelCase keys like "
       [:code ":sampleId"] ". " [:code "(.-sample-id row)"] " would look for "
       [:code "sample_id"] "!"]
      [:span "To put hiccup inside a cell, turn it into a React element with "
       [:code "r/as-element"] "."]]]}

   {:title "Color-coded cells"
    :body
    [[two-columns
      [code "{:headerName \"QC Status\"
 :field \"qcStatus\"
 :cellStyle
 (fn [params]
   (case (.-value params)
     \"PASS\" #js {:color \"green\"}
     \"FAIL\" #js {:color \"red\"}
     nil))}"]
      [:div
       [live [styled-grid-demo]]
       [c/bullet-list
        [:span [:code ":cellStyle"] " is called for each cell in the column"]
        [:span "It gets " [:code "params"] " - " [:code "(.-value params)"] " is the cell's value"]
        "It returns a style, as a JavaScript object - or nil"]]]]}

   {:title "Formatting values"
    :body
    [[two-columns
      [code "{:headerName \"% Q30\"
 :field \"percentQ30\"
 :valueFormatter
 (fn [params]
   (str (.toFixed (.-value params) 1) \" %\"))}"]
      [c/bullet-list
       [:span [:code ":valueFormatter"] " changes how a value is " [:em "shown"] " - 97.25 becomes \"97.3 %\""]
       "The data itself doesn't change, so sorting and filtering still use the real numbers"
       "Great for dates, percentages, big numbers..."]]]}

   {:title "Buttons in cells"
    :body
    [[two-columns
      [code "{:headerName \"Details\"
 :cellRenderer
 (fn [params]
   (r/as-element
    [c/button
     {:on-click
      #(reset! viewing
               (.. params -data -sampleId))}
     \"View\"]))}"]
      [:div
       [live [button-grid-demo]]
       [c/bullet-list
        [:span [:code ":cellRenderer"] " draws a cell with your own component"]
        [:span [:code "(.. params -data)"] " is the whole row"]
        [:span [:code "r/as-element"] " turns your hiccup into what React needs"]]]]]}

   {:title "Selecting rows"
    :body
    [[code "[:> AgGridReact
 {:rowData (:runs @db)
  :getRowId (fn [params] (.. params -data -runId))
  :rowSelection {:mode \"singleRow\"
                 :checkboxes false
                 :enableClickSelection true}
  :onSelectionChanged (fn [event] ...)}]"]
     [c/callout {:kind :warning :style {:margin-top "1cqw"}}
      [:strong "Watch out: "]
      "if the component that draws the grid also reads the selection from "
      [:code "db"] ", every click redraws the grid with \"new\" rows - and AG Grid "
      "forgets the selection. Two fixes: give the grid its own component, and give "
      "it " [:code ":getRowId"] " so it recognizes rows across redraws."]]}

   {:title "More AG Grid features"
    :body
    [[two-columns
      [code ";; every column filterable and sortable
:defaultColDef {:filter true :sortable true}

;; 10 rows per page
:pagination true
:paginationPageSize 10

;; tooltips
{:field \"sampleId\"
 :headerTooltip \"The sample's ID\"
 :tooltipField \"collectionDate\"}"]
      [code ";; the grid's API - its remote control
:onGridReady
(fn [event] (reset! api (.-api event)))

;; ...e.g. to export the table as CSV
[c/button
 {:on-click #(.exportDataAsCsv @api)}
 \"Export CSV\"]"]]
     [small "Every option is in the "
      [c/link {:href "https://www.ag-grid.com/react-data-grid/"} "AG Grid documentation"] "."]]}

   {:title "Themes"
    :body
    [[two-columns
      [:div
       [:p "AG Grid styles itself - no CSS files needed. The default theme is "
        [:strong "Quartz"] ". For a different look, pass a " [:code ":theme"] ":"]
       [code "(:require [\"ag-grid-community\"
           :refer [themeBalham]])

[:> AgGridReact {:theme themeBalham ...}]"]
       [small "Others: " [:code "themeAlpine"] ", " [:code "themeMaterial"] ". Themes can also be "
        "customized - see \"Theming\" in the AG Grid docs."]]
      [:div
       [live [balham-demo]]
       [c/callout {:kind :warning :style {:margin-top "1.5cqw" :font-size "1.6cqw"}}
        "Older AG Grid examples load CSS files and use classes like "
        [:code "ag-theme-alpine"] ". Don't mix that with themes like these - AG Grid will warn you."]]]]}

   ;; ------------------------------------------------------------- wrap-up
   {:title "Summary"
    :body
    [[:div {:style {:display "grid"
                    :grid-template-columns "repeat(3, minmax(0, 1fr))"
                    :gap "2cqw"}}
      [example-box "Reagent and React"
       [c/bullet-list
        "Components are functions that return hiccup"
        "The virtual DOM updates only what changed"]]
      [example-box "App state"
       [c/bullet-list
        "Reagent atoms redraw what reads them"
        "One db atom for the whole app"
        "Events update it with swap!"]]
      [example-box "AG Grid"
       [c/bullet-list
        "Columns and rows as data"
        "Styles, formatters and renderers"
        "Selection, filtering, pages, export"]]]]}

   {:title "Homework, and where to learn more"
    :body
    [[two-columns
      [:div
       [:strong "Homework"]
       [:p "Five exercises that build up to a mini QC dashboard, plus four bonus "
        "challenges: filters and pages, CSV export, tooltips, and editing data from the table."]
       [c/link {:href "#/session-2/homework"} "Go to the homework →"]]
      [:div
       [:strong "Resources"]
       [c/bullet-list
        [c/link {:href "https://www.ag-grid.com/react-data-grid/"} "AG Grid documentation (React)"]
        [c/link {:href "https://www.ag-grid.com/example/"} "AG Grid demo"]
        [c/link {:href "https://reagent-project.github.io/"} "Reagent"]
        [c/link {:href "https://github.com/reagent-project/reagent/blob/master/doc/InteropWithReact.md"} "Reagent: interop with React"]
        [c/link {:href "https://github.com/dfornika/genomics-qc-site-demo"} "genomics-qc-site-demo"]]]]]}])

(defn page []
  [c/page {:max-width "1200px"}
   [slides/viewer "session-2" slides]])
