(ns scratchpad.views.data-explorer
  "The Data Explorer page: open a CSV file and get an AG Grid table of it,
  plus the ClojureScript code that makes the table - in an editor on the
  page, so you can change it and see the result straight away.

  The file is read by the browser and stays there: nothing is uploaded.

  How it works:
    1. scratchpad.data reads the CSV text into column names and rows.
    2. `column-types` guesses each column's type from its values.
    3. `generate-code` writes a namespace (my_data.cljs) that turns the
       text into those types and shows them in a grid.
    4. The workspace (the same one the in-browser Playground uses) runs
       that code with SCI, and shows its `page` on the right."
  (:require ["@codemirror/view" :refer [EditorView]]
            ["ag-grid-community" :refer [getGridApi]]
            [clojure.string :as str]
            [reagent.core :as r]
            [scratchpad.components :as c]
            [scratchpad.data :as data]
            [scratchpad.ui :as ui]
            [scratchpad.web.sci :as sci]
            [scratchpad.web.workspace :as workspace]
            [shadow.resource :as rc]))

(def ^:private example
  {:file-name "example-qc-samples.csv"
   :text (rc/inline "public/data/example-qc-samples.csv")})

(def ^:private max-file-bytes (* 50 1024 1024))

;; Files bigger than this aren't kept in the browser's storage (it only
;; holds a few MB), so they're gone after a reload.
(def ^:private max-saved-chars 2000000)

;; ---------------------------------------------------------------------------
;; Guessing each column's type
;; ---------------------------------------------------------------------------

;; Numbers - but not ones with leading zeros, like 007, which are usually IDs
(def ^:private number-re #"^-?(?:0|[1-9]\d*)?(?:\.\d+)?(?:[eE][-+]?\d+)?$")
(def ^:private date-re #"^(\d{4})-(\d{2})-(\d{2})$")
(def ^:private date-time-re #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:?\d{2})?$")

(defn- number-text? [s]
  (and (re-matches number-re s) (re-find #"\d" s)))

(defn- date-text? [s]
  (when-let [[_ y m d] (re-matches date-re s)]
    (let [[y m d] (map js/parseInt [y m d])
          date (js/Date. y (dec m) d)]
      (and (= (.getFullYear date) y) (= (.getMonth date) (dec m)) (= (.getDate date) d)))))

(defn- date-time-text? [s]
  (and (re-matches date-time-re s) (date-text? (subs s 0 10))))

(defn- boolean-text? [s]
  (#{"true" "false"} (str/lower-case s)))

(defn- column-type
  "The type of a column, from its values: :number, :boolean (or
  :boolean-any-case, for TRUE or True), :date, :date-time or :text. Empty
  cells don't count either way."
  [values]
  (let [values (remove str/blank? values)]
    (cond
      (empty? values) :text
      (every? number-text? values) :number
      (every? #{"true" "false"} values) :boolean
      (every? boolean-text? values) :boolean-any-case
      (every? date-text? values) :date
      (every? date-time-text? values) :date-time
      :else :text)))

(defn- column-types [{:keys [headers rows]}]
  (vec (for [i (range (count headers))]
         (column-type (map #(nth % i) rows)))))

;; ---------------------------------------------------------------------------
;; Writing the code
;; ---------------------------------------------------------------------------

(defn- field-name
  "A name for a column that works as a map key and an AG Grid field:
  \"sample_id\" -> \"sampleId\", \"QC status\" -> \"qcStatus\"."
  [header]
  (let [words (->> (str/split header #"[^A-Za-z0-9]+")
                   (remove str/blank?)
                   (map #(if (= % (str/upper-case %)) (str/lower-case %) %)))
        camel (apply str
                     (str/lower-case (subs (or (first words) "") 0 1))
                     (subs (or (first words) "") 1)
                     (for [w (rest words)]
                       (str (str/upper-case (subs w 0 1)) (subs w 1))))]
    (cond
      (str/blank? camel) "column"
      (re-find #"^\d" camel) (str "col" camel)
      :else camel)))

(defn- field-names
  "A different field name for every column."
  [headers]
  (loop [[h & more] headers
         seen #{}
         result []]
    (if (nil? h)
      result
      (let [base (field-name h)
            name (loop [k 2 candidate base]
                   (if (seen candidate) (recur (inc k) (str base k)) candidate))]
        (recur more (conj seen name) (conj result name))))))

(def ^:private type-info
  ;; how to turn the text into a value, AG Grid's name for the type, and a
  ;; description for the comments
  {:number {:parse #(str "(parse-double " % ")") :ag "number" :about "number"}
   :boolean {:parse #(str "(parse-boolean " % ")") :ag "boolean" :about "true/false"}
   ;; parse-boolean only reads lower case, so TRUE and True need lower-casing first
   :boolean-any-case {:parse #(str "(parse-boolean (str/lower-case " % "))") :ag "boolean" :about "true/false"}
   :date {:parse identity :ag "dateString" :about "date"}
   :date-time {:parse identity :ag "dateTimeString" :about "date & time"}
   :text {:parse identity :ag "text" :about "text"}})

(defn- aligned
  "Lines of code with a comment after each, the comments lined up."
  [pairs]
  (let [width (apply max (map (comp count first) pairs))]
    (for [[code comment] pairs]
      (str code (apply str (repeat (- width (count code)) " ")) "  ; " comment))))

;; ---------------------------------------------------------------------------
;; Writing the code for a chart
;; ---------------------------------------------------------------------------

(def chart-types
  "The charts the \"Make chart\" button can write. Each needs one or two
  columns (:x, and maybe :y) of the kinds listed."
  [{:id :count-bar :label "Bar: count of each value"
    :x {:label "of" :kinds #{:category}}}
   {:id :count-pie :label "Pie: share of each value"
    :x {:label "of" :kinds #{:category}}}
   {:id :histogram :label "Histogram: spread of a number"
    :x {:label "of" :kinds #{:number}}}
   {:id :average-bar :label "Bar: average per group"
    :y {:label "average of" :kinds #{:number}}
    :x {:label "for each" :kinds #{:category}}}
   {:id :scatter :label "Scatter: number against number"
    :x {:label "across" :kinds #{:number}}
    :y {:label "up" :kinds #{:number}}}
   {:id :count-over-time :label "Over time: rows per date"
    :x {:label "by" :kinds #{:date}}}
   {:id :over-time :label "Over time: daily average"
    :y {:label "average of" :kinds #{:number}}
    :x {:label "by" :kinds #{:date}}}])

(def ^:private chart-start
  (str ";; ===========================================================================\n"
       ";; 4. The chart - written by the \"Make chart\" button above the table.\n"
       ";;    Making another chart replaces this part, down to \"end of the chart\".\n"
       ";; ===========================================================================\n"))

(def ^:private chart-end
  ";; ============================ end of the chart ============================\n")

(defn- chart-component
  "The code for a `chart` component drawing `options` (code for a map,
  which uses `data`). `source` is the code for the rows it draws: \"@shown\"
  (the rows that pass the table's filters) or \"rows\" (all of them)."
  [source options]
  (str (if (= source "@shown")
         (str ";; It draws the rows in `shown` - the ones that pass the table's\n"
              ";; filters - so filter the table, and the chart follows.\n")
         (str ";; It draws all the rows. (Press \"Reset to original\" for newer code,\n"
              ";; where the chart follows the table's filters.)\n"))
       "(defn chart []\n"
       "  (let [data (chart-data " source ")]\n"
       "    [:> AgCharts\n"
       "     {:style {:height \"100%\"}\n"
       "      :options " options "}]))\n"))

(defn- subtitle [source]
  (when (= source "@shown")
    (str "\n                :subtitle {:text (str (count @shown) \" of \" (count rows) \" rows\")}")))

(defn- chart-body
  "The code for one kind of chart. `x` and `y` are {:field \"sampleId\"
  :label \"sample_id\"}."
  [{:keys [id]} x y source]
  (let [xf (:field x) xl (:label x) yf (:field y) yl (:label y)
        chart-component (partial chart-component source)
        sub (subtitle source)]
    (case id
      (:count-bar :count-pie)
      (str ";; How many rows have each " xl ", most common first:\n"
           ";;   ({:" xf " \"...\" :count 12} ...)\n"
           "(defn chart-data [rows]\n"
           "  (->> rows\n"
           "       (map :" xf ")\n"
           "       (frequencies)\n"
           "       (map (fn [[value n]] {:" xf " (str value) :count n}))\n"
           "       (sort-by :count >)))\n"
           "\n"
           (chart-component
            (str "{:title {:text " (pr-str (str "Rows by " xl)) "}" sub "\n"
                 "                :data data\n"
                 "                :series [" (if (= id :count-bar)
                                             (str "{:type \"bar\" :xKey " (pr-str xf) " :xName " (pr-str xl)
                                                  "\n                          :yKey \"count\" :yName \"Rows\"}")
                                             (str "{:type \"pie\" :angleKey \"count\" :angleName \"Rows\""
                                                  "\n                          :legendItemKey " (pr-str xf)
                                                  "\n                          :calloutLabelKey " (pr-str xf) "}"))
                 "]}")))

      :histogram
      (str ";; The rows that have a " xl ". AG Charts sorts them into bins itself.\n"
           "(defn chart-data [rows]\n"
           "  (filterv (comp number? :" xf ") rows))\n"
           "\n"
           (chart-component
            (str "{:title {:text " (pr-str (str "Spread of " xl)) "}" sub "\n"
                 "                :data data\n"
                 "                :series [{:type \"histogram\" :xKey " (pr-str xf) " :xName " (pr-str xl) "}]}")))

      :average-bar
      (str ";; The average " yl " for each " xl ":\n"
           ";;   ({:" xf " \"...\" :average 12.5} ...)\n"
           "(defn chart-data [rows]\n"
           "  (->> rows\n"
           "       (filter (comp number? :" yf "))\n"
           "       (group-by :" xf ")\n"
           "       (map (fn [[group group-rows]]\n"
           "              {:" xf " (str group)\n"
           "               :average (/ (reduce + (map :" yf " group-rows))\n"
           "                           (count group-rows))}))\n"
           "       (sort-by :" xf ")))\n"
           "\n"
           (chart-component
            (str "{:title {:text " (pr-str (str "Average " yl " by " xl)) "}" sub "\n"
                 "                :data data\n"
                 "                :series [{:type \"bar\" :xKey " (pr-str xf) " :xName " (pr-str xl)
                 "\n                          :yKey \"average\" :yName " (pr-str (str "Average " yl)) "}]}")))

      :scatter
      (str ";; The rows that have both a " xl " and a " yl "\n"
           "(defn chart-data [rows]\n"
           "  (filterv #(and (number? (:" xf " %)) (number? (:" yf " %))) rows))\n"
           "\n"
           (chart-component
            (str "{:title {:text " (pr-str (str yl " against " xl)) "}" sub "\n"
                 "                :data data\n"
                 "                :series [{:type \"scatter\" :xKey " (pr-str xf) " :xName " (pr-str xl)
                 "\n                          :yKey " (pr-str yf) " :yName " (pr-str yl) "}]\n"
                 "                :axes {:x {:type \"number\" :title {:text " (pr-str xl) "}}\n"
                 "                       :y {:type \"number\" :title {:text " (pr-str yl) "}}}}")))

      :count-over-time
      (str ";; How many rows there are for each day of " xl ", in date order:\n"
           ";;   ({:day \"2026-06-01\" :count 12} ...)\n"
           ";; (subs ... 0 10) is the day part of the date, like \"2026-06-01\".\n"
           "(defn chart-data [rows]\n"
           "  (->> rows\n"
           "       (map :" xf ")\n"
           "       (filter seq)          ; leave out empty ones\n"
           "       (map #(subs % 0 10))\n"
           "       (frequencies)\n"
           "       (sort-by first)\n"
           "       (map (fn [[day n]] {:day day :count n}))))\n"
           "\n"
           (chart-component
            (str "{:title {:text " (pr-str (str "Rows by " xl)) "}" sub "\n"
                 "                :data data\n"
                 "                :series [{:type \"bar\" :xKey \"day\" :xName " (pr-str xl)
                 "\n                          :yKey \"count\" :yName \"Rows\"}]}")))

      :over-time
      (str ";; The average " yl " for each day of " xl ", in date order:\n"
           ";;   ({:date #inst \"...\" :average 12.5} ...)\n"
           ";; (subs ... 0 10) is the day part of the date, like \"2026-06-01\". A time\n"
           ";; axis needs real dates, so js/Date. turns that text into one.\n"
           "(defn chart-data [rows]\n"
           "  (->> rows\n"
           "       (filter #(and (seq (:" xf " %)) (number? (:" yf " %))))\n"
           "       (group-by #(subs (:" xf " %) 0 10))\n"
           "       (sort-by first)\n"
           "       (map (fn [[day day-rows]]\n"
           "              {:date (js/Date. (str day \"T00:00\"))\n"
           "               :average (/ (reduce + (map :" yf " day-rows))\n"
           "                           (count day-rows))}))))\n"
           "\n"
           (chart-component
            (str "{:title {:text " (pr-str (str "Average " yl " by " xl)) "}" sub "\n"
                 "                :data data\n"
                 "                :series [{:type \"line\" :xKey \"date\" :xName " (pr-str xl)
                 "\n                          :yKey \"average\" :yName " (pr-str (str "Average " yl)) "}]\n"
                 "                :axes {:x {:type \"time\"} :y {:type \"number\"}}}"))))))

(defn chart-section
  "The code for the chart part of my_data.cljs: `chart-type` (one of
  chart-types) of the columns `x` and `y`, or no chart if it's nil. The
  chart draws `source` (see chart-component): \"@shown\", unless the code
  is from before there was a `shown`."
  ([chart-type x y] (chart-section chart-type x y "@shown"))
  ([chart-type x y source]
   (str chart-start
       "\n"
       (if chart-type
         (str (chart-body chart-type x y source)
              "\n"
              ";; Ideas: change \"bar\" to \"line\", add :fill \"#004a87\" to a series,\n"
              ";; or look at (chart-data " source ") at the REPL. More in AG Charts'\n"
              ";; documentation: https://www.ag-grid.com/charts/react/quick-start/\n")
         (str ";; No chart yet: choose one above the table and press \"Make chart\".\n"
              "(def chart nil)\n"))
       "\n"
       chart-end)))

(defn- with-chart-section
  "`code` with its chart part replaced by `section` (or, if it hasn't got
  one, with `section` added before the (comment ...) block at the end)."
  [code section]
  (let [start (str/index-of code chart-start)
        end (when start (str/index-of code chart-end start))]
    (cond
      end (str (subs code 0 start) section (subs code (+ end (count chart-end))))
      (str/last-index-of code "\n(comment")
      (let [i (inc (str/last-index-of code "\n(comment"))]
        (str (subs code 0 i) section "\n" (subs code i)))
      :else (str code "\n" section))))

(defn generate-code
  "The code for my_data.cljs: a table of `dataset` (from scratchpad.data)."
  [{:keys [file-name headers rows] :as dataset}]
  (let [types (column-types dataset)
        fields (field-names headers)
        columns (map vector headers fields types)
        uses-str? (some #{:boolean-any-case} types)
        row-lines (aligned
                   (for [[header field type] columns]
                     [(str ":" field " " ((get-in type-info [type :parse]) (str "(get row " (pr-str header) ")")))
                      (get-in type-info [type :about])]))
        ;; for the comment block: a column with a few repeated values, and a number column
        category (first (for [[i [_ field type]] (map-indexed vector columns)
                              :let [n (count (distinct (map #(nth % i) rows)))]
                              :when (and (= type :text) (<= 2 n 12) (< n (count rows)))]
                          field))
        number-field (first (for [[_ field type] columns :when (= type :number)] field))]
    (str
     "(ns scratchpad.my-data\n"
     "  \"A table of " file-name ", written by the Data Explorer.\n"
     "  Change anything you like, then press Ctrl+S to see the result.\"\n"
     "  (:require [\"ag-charts-react\" :refer [AgCharts]]\n"
     "            [\"ag-grid-community\" :refer [themeBalham]]\n"
     "            [\"ag-grid-react\" :refer [AgGridReact]]\n"
     "            [cljs.pprint :as pp]\n"
     (when uses-str? "            [clojure.string :as str]\n")
     "            [reagent.core :as r]\n"
     "            [scratchpad.components :as c]\n"
     "            [scratchpad.data :as data]))\n"
     "\n"
     ";; ---------------------------------------------------------------------------\n"
     ";; 1. The data\n"
     ";; ---------------------------------------------------------------------------\n"
     "\n"
     ";; The rows of " file-name " (" (count rows) " of them). Each row is a map\n"
     ";; from column name to text, like {" (pr-str (first headers)) " " (pr-str (get-in rows [0 0] "")) ", ...}\n"
     ";; Your file stays in this browser: nothing is uploaded.\n"
     "(def csv-rows (data/csv-rows))\n"
     "\n"
     ";; Everything in a CSV file is text. parse-row turns each row into a map\n"
     ";; with the right kind of value for each column. The kinds were guessed\n"
     ";; from the values in each column - if one is wrong, change it here and\n"
     ";; in `columns` below.\n"
     (when (some #{:date :date-time} types)
       (str ";; (Dates stay as text, like \"2026-06-01\" - AG Grid understands that\n"
            ";; format, and sorts and filters them as dates.)\n"))
     "(defn parse-row [row]\n"
     "  {" (str/join "\n   " row-lines) "\n"
     "  })\n"
     "\n"
     "(def rows (mapv parse-row csv-rows))\n"
     "\n"
     ";; ---------------------------------------------------------------------------\n"
     ";; 2. The columns\n"
     ";;\n"
     ";;   :field         which key of each row to show\n"
     ";;   :headerName    the title at the top of the column\n"
     ";;   :cellDataType  what kind of values it holds, so AG Grid can sort,\n"
     ";;                  filter and line them up properly: \"text\", \"number\",\n"
     ";;                  \"boolean\", \"dateString\" or \"dateTimeString\"\n"
     ";;   :pinned        \"left\" or \"right\" keeps the column in view when you\n"
     ";;                  scroll sideways\n"
     ";; ---------------------------------------------------------------------------\n"
     "\n"
     "(def columns\n"
     "  [" (str/join "\n   " (for [[i [header field type]] (map-indexed vector columns)]
                              (str "{:field " (pr-str field)
                                   " :headerName " (pr-str header)
                                   " :cellDataType " (pr-str (get-in type-info [type :ag]))
                                   ;; the first column stays put when scrolling sideways
                                   (when (zero? i) " :pinned \"left\"")
                                   "}")))
     "])\n"
     "\n"
     ";; ---------------------------------------------------------------------------\n"
     ";; 3. The page\n"
     ";;\n"
     ";; The Data Explorer shows `page` on the right - and `chart` (part 4), if\n"
     ";; there is one, above it.\n"
     ";; ---------------------------------------------------------------------------\n"
     "\n"
     ";; The rows you've ticked, as maps like the ones in `rows`. Tick some,\n"
     ";; then look at @selected at the REPL.\n"
     "(def selected (r/atom []))\n"
     "\n"
     "(defn get-selected-rows\n"
     "  \"The ticked rows, from one of AG Grid's events. AG Grid gives them as\n"
     "  JavaScript objects, so js->clj turns them back into maps.\"\n"
     "  [event]\n"
     "  (mapv #(js->clj % :keywordize-keys true)\n"
     "        (.getSelectedRows (.-api event))))\n"
     "\n"
     "(defn on-selection-changed\n"
     "  \"AG Grid calls this whenever the ticks change.\"\n"
     "  [event]\n"
     "  (->> event\n"
     "       get-selected-rows\n"
     "       (reset! selected)))\n"
     "\n"
     ";; The rows that have come in so far: all of them - unless you press\n"
     ";; Replay above the table, which brings them in a batch at a time, the\n"
     ";; way new results arrive week by week.\n"
     "(def arrived (r/atom rows))\n"
     "\n"
     ";; The rows that pass the table's filters, in the table's order. The\n"
     ";; chart (part 4) draws these.\n"
     "(def shown (r/atom rows))\n"
     "\n"
     "(defn get-shown-rows\n"
     "  \"The rows that pass the table's filters, from one of AG Grid's events,\n"
     "  as maps. They include any edits made in the table.\"\n"
     "  [event]\n"
     "  (let [result #js []]\n"
     "    (.forEachNodeAfterFilterAndSort (.-api event) #(.push result (.-data %)))\n"
     "    (mapv #(js->clj % :keywordize-keys true) result)))\n"
     "\n"
     "(defn on-rows-changed\n"
     "  \"AG Grid calls this whenever the rows it shows change: when you filter\n"
     "  or sort, when new rows arrive, and after an edit.\"\n"
     "  [event]\n"
     "  (->> event\n"
     "       get-shown-rows\n"
     "       (reset! shown)))\n"
     "\n"
     "(defn table\n"
     "  \"The AG Grid table. It's a component of its own that doesn't read\n"
     "  `selected` or `shown`, so ticking or filtering doesn't redraw it\n"
     "  (which would clear the ticks). It does read `arrived`, so it's\n"
     "  redrawn when new rows come in.\"\n"
     "  []\n"
     "  [:> AgGridReact\n"
     "   {:rowData @arrived\n"
     "    :columnDefs columns\n"
     "    ;; a compact look that fits more rows on screen (the default is\n"
     "    ;; themeQuartz; there's also themeAlpine and themeMaterial)\n"
     "    :theme themeBalham\n"
     "    ;; for every column: a filter in its menu, a quick one under its\n"
     "    ;; title, and cells you can edit (double-click one). Edits change\n"
     "    ;; what the table shows, not `rows`.\n"
     "    :defaultColDef {:filter true :floatingFilter true :editable true}\n"
     "    ;; let people select and copy the text in cells\n"
     "    :enableCellTextSelection true\n"
     "    :ensureDomOrder true\n"
     "    ;; make each column as wide as what's in it\n"
     "    :autoSizeStrategy {:type \"fitCellContents\"}\n"
     "    ;; a checkbox on each row (and one at the top to tick them all),\n"
     "    ;; kept in view on the left\n"
     "    :rowSelection {:mode \"multiRow\"}\n"
     "    :selectionColumnDef {:pinned \"left\"}\n"
     "    ;; whenever the ticks change. (Written #(...) rather than just\n"
     "    ;; on-selection-changed, so that if you change the function and\n"
     "    ;; evaluate it with Alt+Enter, the table uses the new one straight away.)\n"
     "    :onSelectionChanged #(on-selection-changed %)\n"
     "    ;; whenever the rows it shows change, and after an edit - so the\n"
     "    ;; chart can follow\n"
     "    :onModelUpdated #(on-rows-changed %)\n"
     "    :onCellValueChanged #(on-rows-changed %)}])\n"
     "\n"
     "(defn page []\n"
     "  [:div {:style {:height \"100%\" :display \"flex\" :flex-direction \"column\"}}\n"
     "   [:div {:style {:padding \"8px 12px\" :font-size \"14px\" :color \"#57606a\"}}\n"
     "    (count @arrived) \" rows from \" (data/file-name)\n"
     "    \" - \" (count @shown) \" shown\"\n"
     "    \" - \" (count @selected) \" selected\"]\n"
     "   [:div {:style {:flex \"1 1 0\" :min-height \"150px\"}}\n"
     "    [table]]])\n"
     "\n"
     (chart-section nil nil nil)
     "\n"
     "(comment\n"
     "  ;; Look at your data (Alt+Enter on each):\n"
     "  (count rows)\n"
     "  (first csv-rows)\n"
     "  (first rows)\n"
     "  (pp/print-table (take 5 rows))\n"
     "  (tap> (take 20 rows))   ; look at them in the tap panel (bottom right)\n"
     "  (->> rows (take 5) c/tap-> (map :" (second (first columns)) "))   ; tap mid-pipeline\n"
     (when category
       (str "  (frequencies (map :" category " rows))\n"))
     (when number-field
       (str "  (apply max (keep :" number-field " rows))\n"))
     "\n"
     "  ;; Tick some rows in the table, then:\n"
     "  @selected\n"
     "  (map :" (second (first columns)) " @selected)\n"
     "\n"
     "  ;; Filter the table (the boxes under the column titles), then:\n"
     "  (count @shown)\n"
     "\n"
     "  ;; Bring the rows in yourself - the Replay bar does the same, a batch\n"
     "  ;; at a time:\n"
     "  (reset! arrived (take 50 rows))\n"
     "  (swap! arrived concat (take 50 (drop 50 rows)))\n"
     "  (reset! arrived rows)\n"
     "\n"
     "  ;; Change what ticking does - evaluate this, then tick some rows, and\n"
     "  ;; watch the tap panel (bottom right). Load the file (Ctrl+S) to go back.\n"
     "  (defn on-selection-changed [event]\n"
     "    (->> event\n"
     "         get-selected-rows\n"
     "         c/tap->\n"
     "         (reset! selected)))\n"
     "\n"
     "  ;; Keep every value `selected` takes, and step back through them in the\n"
     "  ;; History tab of the tap panel. What reads @selected (the count above\n"
     "  ;; the table) goes back too; the ticks don't, as the table keeps those.\n"
     "  (c/track! \"selected\" selected)\n"
     "\n"
     "  ;; Ideas to try:\n"
     ";;   - hide a column:             {:field \"...\" :hide true}\n"
     ";;   - pin another column:        {:field \"...\" :pinned \"right\"}\n"
     ";;   - 25 rows at a time:         :pagination true :paginationPageSize 25\n"
     ";;   - color a column, format numbers, or add buttons: see Session 2's homework\n"
     "  )\n")))

;; ---------------------------------------------------------------------------
;; The loaded file, remembered in the browser
;; ---------------------------------------------------------------------------

(def ^:private storage-keys
  {:file-name "scratchpad/data-explorer-file-name"
   :text "scratchpad/data-explorer-csv"})

(def ^:private code-storage-key "scratchpad/my_data.cljs")

(defn- dataset [file-name text]
  (assoc (data/parse-csv text) :file-name file-name))

(defn- remember-file!
  "Keep the file in the browser's storage, if it fits. Returns true if it did."
  [file-name text]
  (try
    (if (<= (count text) max-saved-chars)
      (do (.setItem js/localStorage (:file-name storage-keys) file-name)
          (.setItem js/localStorage (:text storage-keys) text)
          true)
      (do (.removeItem js/localStorage (:file-name storage-keys))
          (.removeItem js/localStorage (:text storage-keys))
          false))
    (catch :default _ false)))

(defn- remembered-file []
  (try
    (let [file-name (.getItem js/localStorage (:file-name storage-keys))
          text (.getItem js/localStorage (:text storage-keys))]
      (when (and file-name text)
        {:file-name file-name :text text}))
    (catch :default _ nil)))

;; Start with the file from last time, or the example. If last time's file
;; was too big to remember, its code is forgotten too, so the code and the
;; data always match.
(defonce ^:private _start
  (let [{:keys [file-name text]} (or (remembered-file)
                                     (do (try (.removeItem js/localStorage code-storage-key)
                                              (catch :default _ nil))
                                         example))]
    (remember-file! file-name text)
    (reset! data/current (dataset file-name text))))

(defonce ^:private workspace
  (workspace/make
   {:id "data-explorer"
    :files [{:name "my_data.cljs"
             :path "my_data.cljs"
             ;; a function, so "Reset to original" writes the code for
             ;; whichever file is loaded
             :source #(generate-code @data/current)}]}))

(defonce ^:private status (r/atom nil))  ; {:kind :info/:warning :text "..."}

(defn- load-csv!
  "Read a CSV file's text, and write the code for it."
  [file-name text]
  (let [new-dataset (dataset file-name text)
        edited? (not= (workspace/current-file-text workspace 0)
                      (generate-code @data/current))]
    (cond
      (empty? (:headers new-dataset))
      (reset! status {:kind :warning :text (str file-name " looks empty.")})

      (or (not edited?)
          (js/confirm (str "Write new code for " file-name "? This replaces your "
                           "changes to my_data.cljs (download it first to keep them).")))
      (let [remembered? (remember-file! file-name text)]
        (reset! data/current new-dataset)
        (workspace/replace-text! workspace 0 (generate-code new-dataset))
        (reset! status
                (when-not remembered?
                  {:kind :warning
                   :text (str file-name " is too big to keep in the browser, so it "
                              "won't be here after you reload the page.")}))))))

(defn- open-file! [^js file]
  (when file
    (if (> (.-size file) max-file-bytes)
      (reset! status {:kind :warning
                      :text (str (.-name file) " is too big (the limit is 50 MB).")})
      (-> (.text file)
          (.then #(load-csv! (.-name file) %))
          (.catch #(reset! status {:kind :warning
                                   :text (str "Couldn't read " (.-name file) ".")}))))))

;; ---------------------------------------------------------------------------
;; The page
;; ---------------------------------------------------------------------------

(defn- toolbar []
  (r/with-let [input-ref (atom nil)]
    (let [{:keys [file-name headers rows]} @data/current]
      [:div {:style {:display "flex" :flex-direction "column" :gap "8px"}}
       [:div {:style {:display "flex" :flex-wrap "wrap" :align-items "center" :gap "8px"}}
        [:input {:type "file"
                 :accept ".csv,.tsv,.txt,text/csv,text/tab-separated-values"
                 :ref #(reset! input-ref %)
                 :style {:display "none"}
                 :on-change (fn [^js e]
                              (open-file! (aget (.. e -target -files) 0))
                              ;; so choosing the same file again still works
                              (set! (.. e -target -value) ""))}]
        [c/button {:style {:padding "6px 12px" :font-size "14px"}
                   :on-click #(some-> ^js @input-ref .click)}
         "Open a CSV file…"]
        [c/button {:variant :secondary
                   :style {:padding "6px 12px" :font-size "14px"}
                   :on-click #(load-csv! (:file-name example) (:text example))}
         "Use the example"]
        [:span {:style {:color "#57606a" :font-size "14px"}}
         "or drop a file anywhere on this page."]
        [:span {:style {:margin-left "auto" :font-size "14px"}}
         "📄 " [:strong file-name] " - " (count rows) " rows, " (count headers) " columns"]]
       [:div {:style {:font-size "13px" :color "#57606a"}}
        "🔒 Your file stays in this browser: nothing is uploaded. The code on the "
        "left makes the table - change it and press " [:strong "Ctrl+S"]
        " (Cmd+S on a Mac) to see the result, or " [:strong "Alt+Enter"]
        " to try out a form."]
       (when-let [{:keys [kind text]} @status]
         [c/callout {:kind kind :style {:font-size "14px" :padding "8px 12px"}} text])])))

(defn- run-page
  "Calls a component from the code (`page` or `chart`). Taking
  `_render-count` makes Reagent re-run this whenever the preview is redrawn."
  [component _render-count]
  (component))

;; ---------------------------------------------------------------------------
;; Making a chart
;; ---------------------------------------------------------------------------

(defn- value-kind [v]
  (cond
    (number? v) :number
    (boolean? v) :boolean
    (and (string? v) (re-find #"^\d{4}-\d{2}-\d{2}" v)) :date
    (string? v) :text))

(defn- analyse-columns
  "What each column of the code's `rows` holds, for the chart menus:
  [{:field \"qcStatus\" :label \"qc_status\" :kinds #{:category}} ...].
  Looks at the rows themselves, so it follows any changes made to
  parse-row. :category means a column with some repeated values, and up
  to 50 different ones."
  [rows columns]
  (let [fields (or (seq (for [{:keys [field headerName]} columns :when field]
                          [field (or headerName field)]))
                   (for [k (keys (first rows))] [(name k) (name k)]))]
    (vec (for [[field label] fields
               :let [k (keyword field)
                     values (remove #(or (nil? %) (= "" %)) (map k rows))
                     kinds (set (map value-kind values))
                     kind (when (= 1 (count kinds)) (first kinds))]
               :when kind]
           {:field field
            :label label
            :kinds (cond-> #{}
                     (= kind :number) (conj :number)
                     (= kind :date) (conj :date)
                     ;; up to 50 different values - but not all different,
                     ;; like IDs, which would just be one bar each
                     (and (#{:text :boolean :date} kind)
                          (let [n (count (distinct values))]
                            (and (<= n 50) (< n (count values)))))
                     (conj :category))}))))

;; Working that out takes a moment for big files, so it's only redone when
;; the rows change.
(defonce ^:private analysis (atom nil))

(defn- columns-info [rows columns]
  (let [[old-rows old-columns result] @analysis]
    (if (and (identical? rows old-rows) (identical? columns old-columns))
      result
      (let [result (analyse-columns rows columns)]
        (reset! analysis [rows columns result])
        result))))

(defn- candidates
  "The columns that can go in `slot` (:x or :y) of `chart-type`."
  [chart-type slot info]
  (when-let [{:keys [kinds]} (get chart-type slot)]
    (filter #(some kinds (:kinds %)) info)))

(defn- slots [chart-type]
  (if (#{:average-bar :over-time} (:id chart-type)) [:y :x] [:x :y]))

(defonce ^:private choice (r/atom {:type :count-bar}))

(defn- chosen
  "The chosen chart type and columns - or the first that fit, for any
  that haven't been chosen or don't fit."
  [info]
  (let [{:keys [type] :as picked} @choice
        chart-type (some #(when (= type (:id %)) %) chart-types)
        pick (fn [slot avoid]
               (let [options (candidates chart-type slot info)]
                 (or (some #(when (= (get picked slot) (:field %)) %) options)
                     (first (remove #(= avoid (:field %)) options))
                     (first options))))
        x (pick :x nil)
        y (when (:y chart-type) (pick :y (:field x)))]
    {:chart-type chart-type :x x :y y
     :ready? (and x (or y (not (:y chart-type))))}))

(defn- show-chart-code!
  "Scroll the editor to the chart part of the code."
  []
  (when-let [^js view @(:view-ref workspace)]
    (when-let [i (str/index-of (.. view -state -doc toString) chart-start)]
      (.dispatch view #js {:effects (.scrollIntoView EditorView i #js {:y "start"})}))))

(defn- write-chart! [chart-type x y]
  (let [code (workspace/current-file-text workspace 0)
        ;; code written before there was a `shown`: chart all the rows
        source (if (re-find #"\(def shown " code) "@shown" "rows")]
    (workspace/replace-text! workspace 0 (with-chart-section code (chart-section chart-type x y source)))
    (show-chart-code!)))

(def ^:private select-style
  {:padding "3px 4px" :font-size "13px" :max-width "220px"
   :border "1px solid #d0d7de" :border-radius "4px" :background-color "white"})

(defn- column-select [slot {:keys [chart-type] :as current} info]
  (let [options (candidates chart-type slot info)]
    [:<>
     [:span (get-in chart-type [slot :label])]
     [:select {:value (or (get-in current [slot :field]) "")
               :style select-style
               :on-change #(swap! choice assoc slot (.. % -target -value))}
      (for [{:keys [field label]} options]
        ^{:key field} [:option {:value field} label])]]))

(defn- chart-builder [rows columns has-chart?]
  (let [info (columns-info rows columns)
        {:keys [chart-type x y ready?] :as current} (chosen info)]
    [:div {:style {:display "flex" :flex-wrap "wrap" :align-items "center" :gap "6px"
                   :padding "6px 8px" :font-size "13px" :color "#57606a"
                   :border-bottom "1px solid #d0d7de" :background-color "#f6f8fa"}}
     [:span "📊"]
     [:select {:value (name (:id chart-type))
               :style select-style
               :on-change #(reset! choice {:type (keyword (.. % -target -value))})}
      (for [{:keys [id label] :as t} chart-types
            :let [possible? (every? #(seq (candidates t % info)) (keys (select-keys t [:x :y])))]]
        ^{:key id}
        [:option {:value (name id) :disabled (not possible?)}
         (str label (when-not possible? " (no suitable columns)"))])]
     (for [slot (slots chart-type) :when (get chart-type slot)]
       ^{:key slot} [column-select slot current info])
     [c/button {:style {:padding "3px 10px" :font-size "13px"}
                :disabled? (not ready?)
                :on-click #(write-chart! chart-type x y)}
      (if has-chart? "Replace chart" "Make chart")]
     (when has-chart?
       [c/button {:variant :secondary
                  :style {:padding "3px 10px" :font-size "13px"}
                  :on-click #(write-chart! nil nil nil)}
        "Remove chart"])]))

;; ---------------------------------------------------------------------------
;; Replay: bring the rows in a batch at a time
;;
;; The code's `arrived` atom holds the rows the table shows. Replay puts
;; them back a batch at a time - in the table's current order, either a
;; number of rows at a time or one value of a column (like a run ID) at a
;; time - so you can watch the chart change as results come in.
;; ---------------------------------------------------------------------------

;; {:batches [[row ...] ...] :step 2 :playing? true :render-count 3}
;; :batches is worked out when Play is first pressed; :render-count is
;; the code it was for - after loading the code again, it starts over.
(defonce ^:private replay (r/atom {}))
(defonce ^:private replay-options (r/atom {:by "" :size 20 :ms 1000}))
(defonce ^:private timer (atom nil))
(defonce ^:private preview-ref (atom nil))

(defn- grid-api
  "The AG Grid table's API, from the page."
  []
  (some-> ^js @preview-ref (.querySelector "[grid-id]") .-parentElement getGridApi))

(defn- compare-values [a b]
  (cond
    (= a b) 0
    (nil? a) 1                        ; empty ones last
    (nil? b) -1
    (= (type a) (type b)) (compare a b)
    :else (compare (str a) (str b))))

(defn- table-order
  "`rows` in the order the table sorts them (or as they are, if it isn't
  sorted). Filters are left out on purpose: they still apply as rows come in."
  [rows]
  (let [sorts (some->> (some-> ^js (grid-api) .getColumnState)
                       (filter #(.-sort ^js %))
                       (sort-by #(.-sortIndex ^js %))
                       (map (fn [^js c] [(keyword (.-colId c)) (.-sort c)])))]
    (if (seq sorts)
      (sort (fn [a b]
              (or (some (fn [[k direction]]
                          (let [c (compare-values (get a k) (get b k))]
                            (when-not (zero? c) (if (= direction "desc") (- c) c))))
                        sorts)
                  0))
            rows)
      rows)))

(defn- batches
  "The rows in batches: `size` at a time, or one value of column `by` at a
  time (in the order each value first comes up)."
  [rows by size]
  (if (str/blank? by)
    (mapv vec (partition-all size rows))
    (let [k (keyword by)
          groups (group-by k rows)]
      (mapv #(get groups %) (distinct (map k rows))))))

(defn- arrived-atom []
  (let [a (sci/resolve-var 'scratchpad.my-data/arrived)]
    (when (satisfies? IReset a) a)))

(defn- stop! []
  (some-> @timer js/clearInterval)
  (reset! timer nil)
  (swap! replay assoc :playing? false))

(defn- go-to-step!
  "Shows the first `step` batches in the table."
  [step]
  (let [{bs :batches} @replay]
    (when-let [a (and bs (arrived-atom))]
      (let [step (max 0 (min step (count bs)))]
        (swap! replay assoc :step step)
        (reset! a (into [] cat (take step bs)))))))

(defn- start!
  "Works out the batches (if it's a new replay) and plays them: one value
  of column `by` at a time, or (if it's \"\") a number of rows at a time."
  [by]
  (let [{:keys [size ms]} @replay-options
        {bs :batches :keys [step]} @replay
        rows (sci/resolve-var 'scratchpad.my-data/rows)
        fresh? (or (nil? bs) (>= step (count bs)))]
    (when (and (sequential? rows) (arrived-atom))
      (when fresh?
        (reset! replay {:batches (batches (table-order rows) by size)
                        :step 0
                        :render-count @sci/render-count})
        (go-to-step! 1))
      (some-> @timer js/clearInterval)
      (swap! replay assoc :playing? true)
      (reset! timer
              (js/setInterval
               (fn []
                 (let [{bs :batches :keys [step]} @replay]
                   (if (< step (count bs))
                     (go-to-step! (inc step))
                     (stop!))))
               ms)))))

(defn- finish!
  "Ends the replay: every row is back."
  []
  (stop!)
  (when-let [a (arrived-atom)]
    (reset! a (sci/resolve-var 'scratchpad.my-data/rows)))
  (reset! replay {}))

(defn- replay-bar [info]
  (let [{bs :batches :keys [step playing? render-count]} @replay
        {:keys [by size ms]} @replay-options
        ;; a column from another file, that this one hasn't got
        by (if (some #(= by (:field %)) info) by "")
        n @sci/render-count]
    ;; the code was loaded again: its `arrived` is new, so start over
    (when (and bs (not= render-count n))
      (js/setTimeout finish! 0))
    (if-not (arrived-atom)
      [:div {:style {:padding "4px 8px" :font-size "12px" :color "#57606a"
                     :border-bottom "1px solid #d0d7de" :background-color "#f6f8fa"}}
       "⏯ Replay needs newer code: press \"Reset to original\" to get it."]
      (let [active? (some? bs)
            batch-label (when (and active? (pos? step))
                          (let [b (nth bs (dec step))]
                            (if (str/blank? by)
                              (str (count b) " rows")
                              (str (get (first b) (keyword by)) " (" (count b) " rows)"))))]
        [:div {:style {:display "flex" :flex-wrap "wrap" :align-items "center" :gap "6px"
                       :padding "6px 8px" :font-size "13px" :color "#57606a"
                       :border-bottom "1px solid #d0d7de" :background-color "#f6f8fa"}}
         [:span {:title (str "Bring the rows in a batch at a time, in the table's order "
                             "(sort it first, if you like), and watch the chart change")}
          "⏯ Replay, a batch per"]
         [:select {:value ms :style select-style :disabled active?
                   :on-change #(swap! replay-options assoc :ms (js/parseInt (.. % -target -value)))}
          [:option {:value 500} "½ second"]
          [:option {:value 1000} "second"]
          [:option {:value 2000} "2 seconds"]]
         [:span "of"]
         [:select {:value by :style select-style :disabled active?
                   :on-change #(swap! replay-options assoc :by (.. % -target -value))}
          [:option {:value ""} (str size " rows")]
          (for [{:keys [field label]} info]
            ^{:key field} [:option {:value field} (str "each " label)])]
         (if playing?
           [c/button {:style {:padding "3px 10px" :font-size "13px"} :on-click stop!} "⏸ Pause"]
           [c/button {:style {:padding "3px 10px" :font-size "13px"} :on-click #(start! by)}
            (if (and active? (< step (count bs))) "▶ Play" (if active? "↺ Again" "▶ Play"))])
         (when active?
           [:<>
            [:input {:type "range" :min 0 :max (count bs) :value step
                     :title "Drag to go to any batch"
                     :on-change (fn [e]
                                  (stop!)
                                  (go-to-step! (js/parseInt (.. e -target -value))))
                     :style {:flex "1 1 120px" :min-width "100px"}}]
            [:span {:style {:white-space "nowrap"}}
             "batch " step " of " (count bs)
             (when batch-label [:<> ": " [:strong {:style {:color "#24292f"}} batch-label]])]
            [c/button {:variant :secondary :style {:padding "3px 10px" :font-size "13px"}
                       :on-click finish!}
             "Show all rows"]])]))))

;; ---------------------------------------------------------------------------
;; The chart above the table, with a divider between them
;; ---------------------------------------------------------------------------

(def ^:private split-key "scratchpad/data-explorer-chart-split")

(defn- clamp-split [percent] (max 15 (min 85 percent)))

(defonce ^:private chart-split
  (r/atom (try
            (let [v (js/parseFloat (.getItem js/localStorage split-key))]
              (if (js/isNaN v) 45 (clamp-split v)))
            (catch :default _ 45))))

(defn- fill
  "Makes `child` exactly fill a pane, so things inside it sized at 100%
  (like the grid and the chart) know how big to be."
  [style child]
  [:div {:style (merge {:position "relative" :min-height 0} style)}
   [:div {:style {:position "absolute" :inset 0 :overflow "auto"}}
    child]])

(defn- horizontal-divider
  "Drag to share the height between the chart and the table; double-click
  to reset."
  [container-ref]
  (r/with-let [dragging? (r/atom false)]
    [:div {:class "playground-divider"
           :title "Drag to resize. Double-click to reset."
           :style {:flex "0 0 12px" :cursor "row-resize" :display "flex"
                   :align-items "center" :touch-action "none"}
           :on-pointer-down (fn [^js e]
                              (.preventDefault e)
                              (.setPointerCapture (.-currentTarget e) (.-pointerId e))
                              (reset! dragging? true))
           :on-pointer-move (fn [^js e]
                              (when-let [^js el (and @dragging? @container-ref)]
                                (let [rect (.getBoundingClientRect el)]
                                  (reset! chart-split
                                          (clamp-split (* 100 (/ (- (.-clientY e) (.-top rect))
                                                                 (.-height rect))))))))
           :on-pointer-up (fn [_]
                            (reset! dragging? false)
                            (try (.setItem js/localStorage split-key (str @chart-split))
                                 (catch :default _ nil)))
           :on-lost-pointer-capture (fn [_] (reset! dragging? false))
           :on-double-click (fn [_] (reset! chart-split 45))}
     [:div {:class "playground-divider-line"
            :style {:height "3px" :width "100%" :border-radius "2px"
                    :background-color (if @dragging? "#54aeff" "#d0d7de")}}]]))

(defn- chart-and-table [chart table]
  (r/with-let [container-ref (atom nil)]
    (if chart
      [:div {:ref #(reset! container-ref %)
             :style {:flex "1 1 0" :min-height 0 :display "flex" :flex-direction "column"}}
       [fill {:flex (str "0 0 calc(" @chart-split "% - 6px)") :padding "8px 8px 0"} chart]
       [horizontal-divider container-ref]
       [fill {:flex "1 1 0"} table]]
      [fill {:flex "1 1 0"} table])))

(defn- preview []
  (let [n @sci/render-count
        page (sci/resolve-var 'scratchpad.my-data/page)
        chart (sci/resolve-var 'scratchpad.my-data/chart)
        rows (sci/resolve-var 'scratchpad.my-data/rows)
        columns (sci/resolve-var 'scratchpad.my-data/columns)]
    [:div {:ref #(reset! preview-ref %)
           :style {:height "100%" :display "flex" :flex-direction "column"}}
     (when (sequential? rows)
       [chart-builder rows (when (sequential? columns) columns) (fn? chart)])
     (when (sequential? rows)
       [replay-bar (filter #(some #{:category :date} (:kinds %))
                           (columns-info rows (when (sequential? columns) columns)))])
     [chart-and-table
      ;; Fresh ones each time the code is loaded (keyed by n): AG Grid and
      ;; AG Charts only apply some options when they're first made.
      (when (fn? chart)
        ^{:key n} [ui/error-boundary [run-page chart n]])
      (if (fn? page)
        ^{:key n} [run-page page n]
        [:div {:style {:padding "12px"}}
         [c/callout {:kind :warning}
          "Nothing to show yet: the code needs a " [:code "page"] " function in the "
          [:code "scratchpad.my-data"] " namespace. Press Ctrl+S to load it, or "
          "\"Reset to original\" to start again."]])]]))

(defn- explorer []
  (r/with-let [dragging? (r/atom false)]
    [:div {:on-drag-over (fn [^js e]
                           (.preventDefault e)
                           (reset! dragging? true))
           :on-drag-leave (fn [^js e]
                            (when-not (.contains (.-currentTarget e) (.-relatedTarget e))
                              (reset! dragging? false)))
           :on-drop (fn [^js e]
                      (.preventDefault e)
                      (reset! dragging? false)
                      (open-file! (aget (.. e -dataTransfer -files) 0)))
           :style {:position "relative"}}
     [workspace/view workspace {:help [toolbar] :preview-label "Your table and chart"}
      [preview]]
     (when @dragging?
       [:div {:style {:position "absolute"
                      :inset "8px"
                      :display "flex"
                      :align-items "center"
                      :justify-content "center"
                      :border "3px dashed #54aeff"
                      :border-radius "12px"
                      :background-color "rgba(221, 244, 255, 0.85)"
                      :font-size "22px"
                      :color "#004a87"
                      :pointer-events "none"
                      :z-index 10}}
        "Drop your CSV file to make a table of it"])]))

;; The workspace's code is loaded the first time the page is shown.
(defonce ^:private started? (atom false))

(defn page []
  (r/create-class
   {:display-name "DataExplorer"
    :component-did-mount (fn []
                           (when (compare-and-set! started? false true)
                             (workspace/init! workspace)))
    :reagent-render (fn [] [explorer])}))
