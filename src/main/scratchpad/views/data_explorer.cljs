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
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [scratchpad.components :as c]
            [scratchpad.data :as data]
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
     "  (:require [\"ag-grid-react\" :refer [AgGridReact]]\n"
     (when uses-str? "            [clojure.string :as str]\n")
     "            [reagent.core :as r]\n"
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
     ";; ---------------------------------------------------------------------------\n"
     "\n"
     ";; The rows you've ticked, as maps like the ones in `rows`. Tick some,\n"
     ";; then look at @selected at the REPL.\n"
     "(def selected (r/atom []))\n"
     "\n"
     "(defn table\n"
     "  \"The AG Grid table. It's a component of its own that doesn't read\n"
     "  `selected`, so ticking a row doesn't redraw it (which would clear the\n"
     "  ticks).\"\n"
     "  []\n"
     "  [:> AgGridReact\n"
     "   {:rowData rows\n"
     "    :columnDefs columns\n"
     "    ;; for every column: a filter in its menu, and a quick one under its title\n"
     "    :defaultColDef {:filter true :floatingFilter true}\n"
     "    ;; make each column as wide as what's in it\n"
     "    :autoSizeStrategy {:type \"fitCellContents\"}\n"
     "    ;; a checkbox on each row (and one at the top to tick them all),\n"
     "    ;; kept in view on the left\n"
     "    :rowSelection {:mode \"multiRow\"}\n"
     "    :selectionColumnDef {:pinned \"left\"}\n"
     "    ;; AG Grid calls this whenever the ticks change. It gives JavaScript\n"
     "    ;; objects, so js->clj turns them back into maps.\n"
     "    :onSelectionChanged (fn [event]\n"
     "                          (reset! selected\n"
     "                                  (mapv #(js->clj % :keywordize-keys true)\n"
     "                                        (.getSelectedRows (.-api event)))))}])\n"
     "\n"
     "(defn page []\n"
     "  [:div {:style {:height \"100%\" :display \"flex\" :flex-direction \"column\"}}\n"
     "   [:div {:style {:padding \"8px 12px\" :font-size \"14px\" :color \"#57606a\"}}\n"
     "    (count rows) \" rows from \" (data/file-name)\n"
     "    \" - \" (count @selected) \" selected\"]\n"
     "   [:div {:style {:flex \"1 1 0\" :min-height \"300px\"}}\n"
     "    [table]]])\n"
     "\n"
     "(comment\n"
     "  ;; Look at your data (Alt+Enter on each):\n"
     "  (count rows)\n"
     "  (first csv-rows)\n"
     "  (first rows)\n"
     (when category
       (str "  (frequencies (map :" category " rows))\n"))
     (when number-field
       (str "  (apply max (keep :" number-field " rows))\n"))
     "\n"
     "  ;; Tick some rows in the table, then:\n"
     "  @selected\n"
     "  (map :" (second (first columns)) " @selected)\n"
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
  "Calls the generated code's `page`. Taking `_render-count` makes Reagent
  re-run this whenever the preview is redrawn."
  [page _render-count]
  (page))

(defn- preview []
  (let [n @sci/render-count
        page (sci/resolve-var 'scratchpad.my-data/page)]
    (if (fn? page)
      ;; A fresh table each time the code is loaded: AG Grid only applies
      ;; some options (like :autoSizeStrategy) when a grid is first made.
      ^{:key n} [run-page page n]
      [:div {:style {:padding "12px"}}
       [c/callout {:kind :warning}
        "Nothing to show yet: the code needs a " [:code "page"] " function in the "
        [:code "scratchpad.my-data"] " namespace. Press Ctrl+S to load it, or "
        "\"Reset to original\" to start again."]])))

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
     [workspace/view workspace {:help [toolbar] :preview-label "Your table"}
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
