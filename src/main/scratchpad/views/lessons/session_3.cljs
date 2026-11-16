(ns scratchpad.views.lessons.session-3
  "Session 3 lesson slides: Building Your Web Application."
  (:require ["ag-charts-react" :refer [AgCharts]]
            ["ag-grid-community" :refer [themeBalham]]
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

(def ^:private data-url "data/session-3/mutations.json")

(def ^:private demo-mutations
  [{:id 1 :gene "rpoB" :mutation "S450L" :frequency 78 :location "BC"}
   {:id 2 :gene "katG" :mutation "S315T" :frequency 60 :location "AB"}
   {:id 3 :gene "inhA" :mutation "C-15T" :frequency 42 :location "BC"}
   {:id 4 :gene "rpoB" :mutation "D435V" :frequency 15 :location "ON"}
   {:id 5 :gene "katG" :mutation "S315T" :frequency 66 :location "BC"}
   {:id 6 :gene "rpoB" :mutation "H445Y" :frequency 9 :location "AB"}])

(def ^:private small-grid
  {:theme themeBalham
   :defaultColDef {:flex 1}})

(defn- fetch-demo
  "Fetches the real JSON file - slowed down, so you can see it loading."
  []
  (r/with-let [state (r/atom {:status :not-loaded})
               fetch! (fn []
                        (reset! state {:status :loading})
                        (-> (js/Promise. (fn [done] (js/setTimeout done 800)))
                            (.then #(js/fetch data-url))
                            (.then (fn [^js response]
                                     (if (.-ok response)
                                       (.json response)
                                       (throw (js/Error. (str "HTTP " (.-status response)))))))
                            (.then #(reset! state {:status :loaded
                                                   :data (js->clj % :keywordize-keys true)}))
                            (.catch #(reset! state {:status :error :error (ex-message %)}))))]
    (let [{:keys [status data error]} @state]
      [:div {:style {:font-size "1.6cqw"}}
       [c/row {:align "center"}
        [c/button {:on-click fetch! :style {:padding "4px 12px" :font-size "1.5cqw"}}
         "Fetch the data"]
        [:span {:style {:color "#57606a"}}
         (case status
           :not-loaded "Not loaded yet"
           :loading "⏳ Loading…"
           :error (str "⚠️ " error)
           :loaded (str "✓ " (count data) " reports"))]]
       (when (seq data)
         [:ul {:style {:margin "0.8cqw 0 0 0"}}
          (for [m (take 4 data)]
            ^{:key (:id m)}
            [:li (:gene m) " - " (:mutation m)])])])))

(defn- wireframe-box [label height]
  [:div {:style {:border "2px dashed #8c959f" :border-radius "6px"
                 :height height :display "flex" :align-items "center"
                 :justify-content "center" :color "#57606a"}}
   label])

(defn- wireframe-demo []
  [:div {:style {:display "flex" :flex-direction "column" :gap "0.8cqw" :font-size "1.5cqw"}}
   [:header {:style {:background-color "#004a87" :color "white"
                     :padding "0.6cqw 1cqw" :border-radius "6px"}}
    "Genomics Dashboard"]
   [:section {:style {:display "flex" :gap "1.5cqw"}}
    [:label "Date range: " [:input {:type "date" :style {:font-size "1.3cqw"}}]]
    [:label "Region: " [:select {:style {:font-size "1.3cqw"}}
                        [:option "BC"] [:option "AB"] [:option "ON"]]]]
   [:section {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "0.8cqw"}}
    [wireframe-box "Table goes here" "8cqw"]
    [wireframe-box "Charts go here" "8cqw"]]])

(defn- genes-grid
  "Its own component, that doesn't read the selection - so selecting a row
  doesn't redraw it (which would clear the selection)."
  [genes on-select]
  [:> AgGridReact
   (merge small-grid
          {:columnDefs [{:field "gene" :headerName "Gene"}]
           :rowData genes
           :rowSelection {:mode "singleRow" :checkboxes false :enableClickSelection true}
           :onRowClicked (fn [^js e] (on-select (.. e -data -gene)))})])

(defn- linked-tables-demo []
  (r/with-let [selected (r/atom "rpoB")
               genes (vec (for [g (distinct (map :gene demo-mutations))] {:gene g}))
               select! #(reset! selected %)]
    [:div {:style {:display "grid" :grid-template-columns "1fr 2fr" :gap "1cqw"}}
     [:div {:style {:height "15cqw" :min-width 0}}
      [genes-grid genes select!]]
     [:div {:style {:height "15cqw" :min-width 0}}
      [:> AgGridReact
       (merge small-grid
              {:columnDefs [{:field "mutation"} {:field "frequency"} {:field "location"}]
               :rowData (filterv #(= @selected (:gene %)) demo-mutations)})]]]))

(defn- static-chart-demo []
  [:div {:style {:height "17cqw"}}
   [:> AgCharts {:style {:height "100%"}
                 ;; small, to fit on a slide (charts are at least 300px by default)
                 :options {:minWidth 100 :minHeight 100
                           :title {:text "Mutation frequencies"}
                           :data [{:gene "rpoB" :frequency 78}
                                  {:gene "katG" :frequency 60}]
                           :series [{:type "bar" :xKey "gene" :yKey "frequency"}]}}]])

(defn- picked-grid [on-change]
  [:> AgGridReact
   (merge small-grid
          {:columnDefs [{:field "gene"} {:field "mutation"} {:field "frequency"}]
           :rowData demo-mutations
           :rowSelection {:mode "multiRow"}
           :onSelectionChanged
           (fn [^js e]
             (on-change (mapv #(js->clj % :keywordize-keys true)
                              (.getSelectedRows (.-api e)))))})])

(defn- dynamic-chart-demo []
  (r/with-let [chart-data (r/atom [])
               update-chart #(reset! chart-data %)]
    [:div {:style {:display "grid" :grid-template-columns "3fr 2fr" :gap "1cqw"}}
     [:div {:style {:height "16cqw" :min-width 0}}
      [picked-grid update-chart]]
     [:div {:style {:height "16cqw" :min-width 0}}
      (if (seq @chart-data)
        [:> AgCharts {:style {:height "100%"}
                      :options {:minWidth 100 :minHeight 100
                                :data @chart-data
                                :series [{:type "bar" :xKey "mutation" :yKey "frequency"}]}}]
        [:div {:style {:height "100%" :box-sizing "border-box"
                       :display "flex" :align-items "center" :justify-content "center"
                       :text-align "center" :padding "1cqw"
                       :color "#57606a" :font-size "1.5cqw" :border "1px dashed #d0d7de"
                       :border-radius "6px"}}
         "← Tick some rows, and the chart shows them"])]]))

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
      [:img {:src "images/logo-bacterium.svg" :alt "" :style {:height "12cqw"}}]
      [:h1 {:style {:font-size "5cqw" :color "#004a87" :margin "2cqw 0 1cqw 0"}}
       "Session 3: Building Your Web Application"]
      [:p {:style {:font-size "2.4cqw" :color "#57606a" :margin 0}}
       "Web Development Workshop"]]]}

   {:title "Today"
    :body
    [[:p "From a table on a page to a small application:"]
     [c/bullet-list
      [:span [:strong "Data"] " - preparing it, and making it available to your site"]
      [:span [:strong "Designing the interface"] " - components, feedback, and putting data first"]
      [:span [:strong "Linked tables"] " - pick a row in one, see the details in another"]
      [:span [:strong "Charts"] " - with AG Charts, linked to your tables"]
      [:span [:strong "Deploying"] " - putting your site on GitHub Pages"]
      [:span [:strong "Security"] " - what to think about before sharing genomic data"]]]}

   ;; ----------------------------------------------------------------- data
   {:title "Where data comes from"
    :body
    [[two-columns
      [:div
       [:strong "Backend and frontend"]
       [c/bullet-list
        [:span "The " [:strong "backend"] " provides the data: files, databases, APIs"]
        [:span "The " [:strong "frontend"] " - your ClojureScript app, in the browser - fetches it and shows it"]]
       [:strong "Static and dynamic"]
       [c/bullet-list
        [:span [:strong "Static"] ": files served as they are"]
        [:span [:strong "Dynamic"] ": made or looked up on request, by an API"]]]
      [:div
       [:strong "Formats"]
       [c/bullet-list
        [:span [:strong "JSON"] " - the most common, and the easiest in the browser"]
        [:span [:strong "CSV"] " - spreadsheets and pipelines (the Data Explorer reads these)"]
        [:span [:strong "XML"] " - older systems"]]
       [c/callout {:style {:margin-top "1.5cqw"}}
        "Today: a static JSON file. No server to write - and the same "
        "approach works for an API later."]]]]}

   {:title "Serving a static file"
    :body
    [[two-columns
      [:div
       [:p "Anything in " [:code "resources/public"] " is served by shadow-cljs's dev server. "
        "This project already has:"]
       [code ";; shadow-cljs.edn
{:dev-http {8000 \"resources/public\"}
 ...}"]
       [:p "So a file at"]
       [code "resources/public/data/session-3/mutations.json"]
       [:p "is at " [:code "http://localhost:8000/data/session-3/mutations.json"]]
       [small "In Codespaces, open port 8000 from the Ports tab and add "
        [:code "/data/session-3/mutations.json"] " to the address."]]
      [:div
       [example-box "mutations.json"
        [code "[
  {\"id\": 1, \"gene\": \"rpoB\",
   \"mutation\": \"S450L\",
   \"drug\": \"rifampicin\",
   \"frequency\": 78,
   \"location\": \"BC\",
   \"date\": \"2026-03-04\"},
  ...
]"]
        [small "Drug-resistance mutations in tuberculosis: "
         [:code "frequency"] " is the % of resistant isolates that carry it. "
         "Made-up numbers, real genes."]]]]]}

   {:title "Fetching it"
    :body
    [[two-columns
      [:div
       [code "(defonce app-state (r/atom []))

(defn fetch-data []
  (-> (js/fetch \"data/session-3/mutations.json\")
      (.then #(.json %))
      (.then #(js->clj % :keywordize-keys true))
      (.then #(reset! app-state %))
      (.catch #(js/console.error
                \"Error fetching data\" %))))"]
       [small [:code "js/fetch"] " returns a " [:strong "promise"] ": a value that arrives later. "
        [:code ".then"] " says what to do with it when it does."]]
      [:div
       [c/bullet-list
        [:span [:code "(.json %)"] " reads the response as JSON - JavaScript data"]
        [:span [:code "js->clj"] " turns that into ClojureScript maps and vectors, so "
         [:code "(:gene entry)"] " works"]
        [:span [:code "reset!"] " stores it - and everything that reads " [:code "app-state"] " redraws"]]
       [live [fetch-demo]]
       [small "(Slowed down a little, so you can see it loading. The Fetching Data page "
        "has more - including cljs-http and core.async.)"]]]]}

   {:title "Showing it"
    :body
    [[two-columns
      [code "(defn display-data []
  [:ul
   (for [entry @app-state]
     ^{:key (:id entry)}
     [:li (str (:gene entry) \" - \"
               (:mutation entry))])])"]
      [:div
       [c/bullet-list
        "When fetch-data stores the data, the list redraws by itself"
        [:span [:code "^{:key ...}"] " gives React a name for each item, so it can tell them apart when the list changes"]
        "The component doesn't know or care where the data came from"]]]
     [example-box "The flow"
      [:div {:style {:display "flex" :gap "1.5cqw" :align-items "center" :flex-wrap "wrap"}}
       [c/tag "page loads"] "→" [c/tag "fetch-data"] "→" [c/tag "mutations.json"] "→"
       [c/tag "reset! app-state"] "→" [c/tag "the list redraws"]]]]}

   {:title "Try it: more fields"
    :body
    [[two-columns
      [:div
       [:p "The JSON file has more than gene and mutation. Show more of it:"]
       [code "\"rpoB - S450L (BC, 2026-03-04)\""]
       [:p "Then try adding a field of your own to the JSON file - like "
        [:code "\"sampleSource\""] " - and showing it too."]]
      [c/callout
       "This is exercise 2 of the homework. In the in-browser version you can't "
       "edit the JSON file, but in Codespaces you can: it's "
       [:code "resources/public/data/session-3/mutations.json"]
       ". Reload the page after changing it - the dev server doesn't watch data files."]]]}

   {:title "Takeaways: data"
    :body
    [[c/bullet-list
      "A static JSON file is the simplest way to give your site structured data"
      [:span "The frontend fetches it with " [:code "js/fetch"] ", and converts it with " [:code "js->clj"]]
      "Store it in an atom, and Reagent redraws everything that uses it"
      "The same workflow works for CSV, or an API: only where the data comes from changes"]]}

   ;; -------------------------------------------------------------- design
   {:title "Designing the interface"
    :body
    [[two-columns
      [:div
       [:strong "Modularity"]
       [c/bullet-list
        "Build the page from independent components: a table, a chart, a filter"
        "Each one takes its data as arguments - easy to reuse, and to test at the REPL"]
       [:strong "Consistency"]
       [c/bullet-list
        "Predictable layouts; the same colors and fonts everywhere"
        [:span "Shared settings help - like " [:code "grid-options"] " in the homework"]]]
      [:div
       [:strong "Feedback"]
       [c/bullet-list
        "Show that data is loading"
        "Say clearly when something went wrong - and what"
        "Confirm that an action worked"]
       [:strong "Data first"]
       [c/bullet-list
        "Readability over decoration"
        "Color for categories and thresholds"
        "Tooltips for the details of a dense table"]]]]}

   {:title "Plan for loading and errors"
    :body
    [[two-columns
      [code "(defonce db
  (r/atom {:status :not-loaded}))

(defn status-message []
  (let [{:keys [status error]} @db]
    (case status
      :not-loaded \"Not loaded yet.\"
      :loading    \"Loading…\"
      :error      [c/callout {:kind :error} error]
      :loaded     \"Loaded!\")))"]
      [:div
       [c/bullet-list
        [:span "Keep the data's " [:strong "status"] " in the app state, next to the data"]
        "Every view can then show the right thing"
        [:span "A missing file isn't an error to " [:code "fetch"] " - check "
         [:code "(.-ok response)"] " yourself"]]
       [c/callout {:kind :info :style {:margin-top "1.5cqw"}}
        "Adding this from the start is much easier than adding it later."]]]]}

   {:title "Designing a genomic dashboard"
    :body
    [[two-columns
      [:div
       [:p [:strong "Scenario: "] "a dashboard of antimicrobial-resistance (AMR) mutations."]
       [c/bullet-list
        [:span [:strong "Header"] ": title, and links to other pages"]
        [:span [:strong "Filters"] ": region, and a date range"]
        [:span [:strong "Data table"] ": the mutations and their frequencies"]
        [:span [:strong "Charts"] ": frequency trends (a line chart), and how mutations are shared between genes (bars)"]]]
      [:div {:style {:display "flex" :flex-direction "column" :gap "0.8cqw"
                     :font-size "1.6cqw" :font-family "ui-monospace, monospace"}}
       (for [[part what] [["Header" "navigation, branding"]
                          ["Filters" "dropdowns, search, dates"]
                          ["Data table" "mutation frequencies"]
                          ["Charts" "trends and distributions"]]]
         ^{:key part}
         [:div {:style {:display "flex" :gap "1cqw" :align-items "center"}}
          [:div {:style {:border "2px solid #004a87" :border-radius "4px"
                         :padding "0.6cqw 1cqw" :min-width "12cqw" :color "#004a87"}}
           part]
          [:span {:style {:color "#57606a"}} "← " what]])]]]}

   {:title "Wireframes in hiccup"
    :body
    [[two-columns
      [code "(defn dashboard []
  [:div
   [:header \"Genomics Dashboard\"]
   [:section
    [:label \"Date range: \"
     [:input {:type \"date\"}]]
    [:label \"Region: \"
     [:select [:option \"BC\"]
              [:option \"AB\"]]]]
   [:section
    [:div \"Table goes here\"]
    [:div \"Charts go here\"]]])"]
      [:div
       [live [wireframe-demo]]
       [c/bullet-list
        "Lay out the structure before connecting any data"
        "Placeholders are fine - swap in the real components one at a time"
        "It's already a working page: see it, share it, change it"]]]]}

   {:title "Try it: a wireframe"
    :body
    [[two-columns
      [:div
       [:p "Sketch - on paper, or in code - a page for a genomic data app:"]
       [c/bullet-list
        "A header, with a title and navigation"
        "Filters: region, time"
        "Space for a table and charts"]
       [:p "How does your design put the data first?"]]
      [example-box "Takeaways"
       [c/bullet-list
        "Break the UI into reusable parts"
        "Plan for loading and error states from the start"
        "Design visuals that help people understand the data - not ones that overwhelm"]]]
     [small "Homework exercise 3 is a wireframe, which exercise 6 fills in with the real thing."]]}

   ;; ------------------------------------------------------- linked tables
   {:title "Linked tables"
    :body
    [[two-columns
      [:div
       [:p [:strong "Master-detail: "] "pick a row in one table, and another shows its details."]
       [c/bullet-list
        "Pick a gene → see every report of its mutations"
        "Pick a run → see its samples (Session 2's dashboard)"]]
      [:div
       [:p [:strong "How: "] "the selection lives in an atom."]
       [c/bullet-list
        "The first table writes to it when a row is picked"
        "The second table reads from it - and redraws when it changes"
        "Nothing else needs to know how they're connected"]]]]}

   {:title "A table to start from"
    :body
    [[two-columns
      [code "(defn mutations-table [rows]
  [:> AgGridReact
   {:rowData rows
    :columnDefs [{:field \"gene\"}
                 {:field \"mutation\"}
                 {:field \"frequency\"}]
    :defaultColDef {:filter true}}]])"]
      [c/bullet-list
       "Columns are sortable already: click a header"
       [:span [:code ":defaultColDef {:filter true}"] " adds a filter to every column"]
       [:span "Remember " [:code "[:> ...]"] " to use a React component from Reagent, and a "
        "height for the grid's box"]]]]}

   {:title "Linking two tables"
    :body
    [[two-columns
      [code "(defonce selected-gene (r/atom nil))

(defn genes-table [genes]
  [:> AgGridReact
   {:rowData genes
    :columnDefs [{:field \"gene\"}]
    :onRowClicked
    (fn [e]
      (reset! selected-gene
              (.. e -data -gene)))}])

(defn reports-table [mutations]
  [:> AgGridReact
   {:rowData (filter #(= @selected-gene
                         (:gene %))
                     mutations)
    :columnDefs [...]}])"]
      [:div
       [live [linked-tables-demo]]
       [c/bullet-list
        [:span "AG Grid passes " [:code ":onRowClicked"] " an event; " [:code "(.. e -data)"] " is the row"]
        [:span [:strong "Keep them apart: "] "the genes table must not read " [:code "@selected-gene"]
         " - if it did, every click would redraw it, and it would forget the selection"]]]]]}

   ;; --------------------------------------------------------------- charts
   {:title "AG Charts"
    :body
    [[two-columns
      [:div
       [:p "From the makers of AG Grid: a JavaScript charts library."]
       [c/bullet-list
        "Bar, line, pie, scatter, histogram, and many more"
        "Data is a vector of maps - like a grid's rows"
        "Change the data, and the chart animates to it"]]
      [:div
       [:p "With Reagent and AG Grid:"]
       [c/bullet-list
        "A chart is a component, like a table"
        "Give it data from an atom, and it follows the atom"
        "Link it to a table's selection, or its filters"]]]
     [small "The Data Explorer's \"Make chart\" button writes AG Charts code for you - a good way "
      "to see how different charts are put together."]]}

   {:title "A basic chart"
    :body
    [[two-columns
      [code "(:require [\"ag-charts-react\"
           :refer [AgCharts]])

(defn frequency-chart []
  [:> AgCharts
   {:options
    {:title {:text \"Mutation frequencies\"}
     :data [{:gene \"rpoB\" :frequency 78}
            {:gene \"katG\" :frequency 60}]
     :series [{:type \"bar\"
               :xKey \"gene\"
               :yKey \"frequency\"}]}}])"]
      [:div
       [live [static-chart-demo]]
       [c/bullet-list
        [:span [:code ":data"] ": the rows"]
        [:span [:code ":series"] ": what to draw from them - " [:code ":xKey"] " and "
         [:code ":yKey"] " name the fields"]]]]]}

   {:title "A chart linked to a table"
    :body
    [[two-columns
      [code "(defonce chart-data (r/atom []))

(defn update-chart [selected-rows]
  (reset! chart-data selected-rows))

;; in the table's options:
:onSelectionChanged
(fn [e]
  (update-chart
   (mapv #(js->clj % :keywordize-keys true)
         (.getSelectedRows (.-api e)))))

(defn dynamic-chart []
  [:> AgCharts
   {:options
    {:data @chart-data
     :series [{:type \"bar\"
               :xKey \"mutation\"
               :yKey \"frequency\"}]}}])"]
      [:div
       [live [dynamic-chart-demo]]
       [c/bullet-list
        "The table writes the ticked rows into an atom"
        "The chart reads the atom, and redraws when it changes"
        "Same pattern as linked tables - an atom in the middle"]]]]}

   {:title "Takeaways: tables and charts"
    :body
    [[c/bullet-list
      "AG Grid and a Reagent atom make master-detail views easy"
      "AG Charts reads its data from the same atoms as your tables"
      "Together: interactive dashboards for genomic data"]
     [c/callout {:style {:margin-top "2cqw"}}
      "Homework exercises 4-6 build exactly this: linked tables, a chart that follows them, "
      "and a region filter - with the whole dataset."]]}

   ;; ------------------------------------------------------------ deploying
   {:title "Deploying with GitHub Pages"
    :body
    [[two-columns
      [:div
       [:strong "1. Build it"]
       [code "npx shadow-cljs release app"]
       [small "An optimized build in " [:code "resources/public/js"] ". With "
        [:code "resources/public"] "'s other files - " [:code "index.html"] ", CSS, data - "
        "that's the whole site: just static files."]
       [:strong {:style {:display "block" :margin-top "1.2cqw"}} "2. Publish it"]
       [c/bullet-list
        "Push the files to a GitHub repository"
        "Settings → Pages: choose where the files are (a branch, or GitHub Actions)"
        [:span "The site goes live at " [:code "https://username.github.io/repo"]]]]
      [:div
       [example-box "This site does it automatically"
        [:p {:style {:margin "0 0 0.6cqw 0"}}
         "A GitHub Actions workflow, " [:code ".github/workflows/publish.yml"] ", builds and "
         "publishes it on every push. It also makes a single HTML file of the whole site - "
         "for a shared drive, or an email."]]
       [c/callout {:kind :warning :style {:margin-top "1.5cqw" :font-size "1.6cqw"}}
        [:strong "Before deploying: "]
        "talk to your organization's IT and security teams about their requirements - "
        "where data can be hosted, who can see it, and how it's handled. "
        "A GitHub Pages site is public."]]]]}

   {:title "Authentication and security"
    :body
    [[two-columns
      [:div
       [:strong "Why it matters for genomics"]
       [c/bullet-list
        [:span [:strong "Access control"] ": only authorized people should see the data"]
        [:span [:strong "Sensitive data"] ": genomic data can link to personal health information"]
        [:span [:strong "Compliance"] ": privacy laws and policies (PHIPA, HIPAA, ...) set the rules"]]]
      [:div
       [:strong "The core ideas"]
       [c/bullet-list
        [:span [:strong "Authentication"] ": who is this? (logging in)"]
        [:span [:strong "Authorization"] ": what may they see and do? (roles, permissions)"]
        [:span [:strong "Restricting features"] ": hide or disable what someone isn't allowed to use"]]]]
     [c/callout {:kind :warning :style {:margin-top "1.5cqw"}}
      [:strong "A static site can't keep secrets. "]
      "Everything it serves - code and data files - can be downloaded by anyone who can open it. "
      "Hiding a button isn't security: protecting data needs a server that checks who's asking. "
      "Always work within your organization's IT and security policies."]]}

   ;; ------------------------------------------------------------- wrap-up
   {:title "Homework, and where to learn more"
    :body
    [[two-columns
      [:div
       [:strong "Homework"]
       [:p "Six exercises that build an AMR mutation dashboard: load a JSON file, "
        "show it, sketch the layout, link two tables and a chart, and put it all together. "
        "Bonus: a pie chart linked to a table, a trend over time, and colors that make "
        "the numbers stand out."]
       [c/link {:href "#/session-3/homework"} "Go to the homework →"]]
      [:div
       [:strong "Resources"]
       [c/bullet-list
        [c/link {:href "https://www.ag-grid.com/react-data-grid/"} "AG Grid documentation"]
        [c/link {:href "https://www.ag-grid.com/charts/react/quick-start/"} "AG Charts documentation"]
        [c/link {:href "https://reagent-project.github.io/"} "Reagent"]
        [c/link {:href "https://shadow-cljs.github.io/docs/UsersGuide.html"} "shadow-cljs User's Guide"]
        [c/link {:href "https://docs.github.com/en/pages"} "GitHub Pages"]]]]]}

   {:title "A tour of real sites"
    :body
    [[two-columns
      [:div
       [:p "Let's look at how these ideas come together in real public-health genomics sites:"]
       [c/bullet-list
        "How they load and organize their data"
        "Their layout: headers, filters, tables, charts"
        "How tables and charts are linked"
        "How they handle loading and errors"]]
      [example-box "A public demo"
       [c/link {:href "https://github.com/dfornika/genomics-qc-site-demo"} "genomics-qc-site-demo"]
       [small "A sequencing QC site built with the tools from this workshop."]]]]}])

(defn page []
  [c/page {:max-width "1200px"}
   [slides/viewer "session-3" slides]])
