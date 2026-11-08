(ns scratchpad.web.sci
  "Runs ClojureScript typed into the page, using SCI (the Small Clojure
  Interpreter). Used by the in-browser Playground and by the editable
  examples on the Components page. Code evaluated here gets the *real*
  Reagent, component library and app state, so it behaves just like the
  compiled code in src/main."
  (:require ["ag-charts-community" :as ag-charts-community]
            ["ag-charts-react" :as ag-charts-react]
            ["ag-grid-community" :as ag-grid-community]
            ["ag-grid-react" :as ag-grid-react]
            [cljs.pprint :as pp]
            [clojure.string :as str]
            [reagent.core :as r]
            [sci.configs.reagent.reagent :as sci-reagent]
            [sci.core :as sci]
            [scratchpad.components]
            [scratchpad.data :as data]
            [scratchpad.web.sci-async :as sci-async]
            [scratchpad.state :as state]
            [scratchpad.views.taps :as taps]))

;; Bumping this redraws the preview. Code in the editor can do that by
;; calling (scratchpad.main/render), same as in Codespaces.
(defonce render-count (r/atom 0))

(defn render! []
  (swap! render-count inc)
  nil)

(def ^:private components-ns (sci/create-ns 'scratchpad.components nil))
(def ^:private state-ns (sci/create-ns 'scratchpad.state nil))
(def ^:private main-ns (sci/create-ns 'scratchpad.main nil))
(def ^:private data-ns (sci/create-ns 'scratchpad.data nil))

;; cljs.pprint's functions print with the compiled code's print, which
;; doesn't go to the editors' output (or to SCI's with-out-str). So they
;; make a string here, and the SCI versions below print it.
(defn- pprint-str* [x]
  (with-out-str (pp/pprint x)))

(defn- print-table-str*
  ([rows] (with-out-str (pp/print-table rows)))
  ([ks rows] (with-out-str (pp/print-table ks rows))))

(def ^:private pprint-source
  "(in-ns 'cljs.pprint)
   (defn pprint
     \"Pretty-prints x: like prn, but long values are spread over several
     lines.\"
     [x] (print (pprint-str* x)))
   (defn print-table
     \"Prints a sequence of maps as a table. ks (optional) picks the columns.\"
     ([rows] (print (print-table-str* rows)))
     ([ks rows] (print (print-table-str* ks rows))))")

(defonce ctx
  (sci/init
   {:namespaces (merge (:namespaces sci-reagent/config)
                       ;; core.async (with a stand-in go and <!) and cljs-http
                       sci-async/namespaces
                       {;; newer clojure.core functions that SCI doesn't have yet
                        'clojure.core {'parse-long parse-long
                                       'parse-double parse-double
                                       'parse-boolean parse-boolean
                                       'parse-uuid parse-uuid
                                       ;; tapped values show up in the tap panel
                                       'tap> tap>
                                       'add-tap add-tap
                                       'remove-tap remove-tap}
                        ;; filled in below, with functions that print the way SCI does
                        'cljs.pprint {'pprint-str* pprint-str*
                                      'print-table-str* print-table-str*}
                        'scratchpad.components (sci/copy-ns scratchpad.components components-ns)
                        'scratchpad.state {'db (sci/copy-var state/db state-ns)}
                        'scratchpad.data {'csv-rows (sci/copy-var data/csv-rows data-ns)
                                          'file-name (sci/copy-var data/file-name data-ns)}
                        'scratchpad.main {'render (sci/copy-var render! main-ns)}})
    ;; allow js/alert, js/console, js/document, ...
    ;; (:require [clojure.pprint :as pp]) works too, like in compiled code
    :ns-aliases {'clojure.pprint 'cljs.pprint}
    :classes {'js js/globalThis :allow :all}
    ;; npm libraries code typed into the page can (:require [\"...\"]), just
    ;; like compiled code can
    :js-libs {"ag-charts-community" ag-charts-community
              "ag-charts-react" ag-charts-react
              "ag-grid-community" ag-grid-community
              "ag-grid-react" ag-grid-react}}))

(sci/eval-string* ctx pprint-source)

;; Printing that isn't captured - like a println inside a go block, which
;; runs after the code that started it has finished - goes to the browser
;; console (F12), instead of failing.
(sci/alter-var-root sci/print-fn
                    (constantly (fn [s]
                                  (let [s (str/replace s #"\n$" "")]
                                    (when-not (str/blank? s) (js/console.log s))))))

;; Tells the tap panel which namespace a value was tapped from: the one
;; SCI is running code in. (Outside of running code - like in a button's
;; on-click - that's just the default, user, which says nothing useful.)
(set! taps/current-sci-ns
      (fn []
        (let [n (str (sci/ns-name @sci/ns))]
          (when-not (= n "user") n))))

;; Lets c/hiccup name the components typed into the page, like
;; gene-viewer: it looks for the function among the vars in SCI's
;; namespaces. (Compiled functions here are minified, so they don't know
;; their own names.) It runs through eval-form, since SCI only knows its
;; namespaces while it's evaluating something.
(def ^:private find-name-form
  '(fn [f]
     (some (fn [ns]
             (some (fn [[sym v]]
                     (when (try (identical? @v f) (catch :default _ false))
                       sym))
                   (ns-publics ns)))
           (all-ns))))

(set! scratchpad.components/*find-name*
      (fn [f]
        (try
          (sci/eval-form ctx (list find-name-form (list 'quote f)))
          (catch :default _ nil))))

(defn- format-value [v]
  (binding [*print-length* 50
            *print-level* 6]
    (pr-str v)))

(defn- error-message [e]
  (let [{:keys [line column]} (ex-data e)]
    (str (ex-message e)
         (when line (str " (line " line (when column (str ", column " column)) ")")))))

(defn eval-code
  "Evaluate `code` (a string of one or more forms) in namespace `ns` (a SCI
  namespace object, or nil for the default). Anything printed is captured.

  Returns a map with :ok? and either :value (as a string) or :error, plus
  :out (printed output) and :ns (the namespace evaluation ended in)."
  [code ns]
  (let [out (atom [])]
    (try
      (let [{:keys [val ns]} (sci/binding [sci/print-fn (fn [s] (swap! out conj s))
                                           sci/print-newline true]
                               (sci/eval-string+ ctx code (when ns {:ns ns})))]
        {:ok? true
         :value (format-value val)
         :out (apply str @out)
         :ns ns})
      (catch :default e
        {:ok? false
         :error (error-message e)
         :out (apply str @out)}))))

(defn create-ns!
  "Evaluate an (ns ...) form and return the SCI namespace it creates, to
  evaluate other code in later, e.g.
  (create-ns! \"(ns my.examples (:require [scratchpad.components :as c]))\")"
  [ns-form]
  (:ns (sci/eval-string+ ctx ns-form)))

(defn eval-value
  "Evaluate `code` in namespace `ns` and return the actual value (not a
  printed version of it): {:ok? true :value v}, or {:ok? false :error msg}.
  Either way, :out is anything it printed."
  [code ns]
  (let [out (atom [])]
    (try
      (let [v (sci/binding [sci/print-fn (fn [s] (swap! out conj s))
                            sci/print-newline true]
                (:val (sci/eval-string+ ctx code {:ns ns})))]
        {:ok? true :value v :out (apply str @out)})
      (catch :default e
        {:ok? false :error (error-message e) :out (apply str @out)}))))

(defn resolve-var
  "Look up a var's current value, e.g. (resolve-var 'scratchpad.playground/page).
  Returns nil if it doesn't exist (yet)."
  [sym]
  (try
    (sci/eval-form ctx sym)
    (catch :default _ nil)))
