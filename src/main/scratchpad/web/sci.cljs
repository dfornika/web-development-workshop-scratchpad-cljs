(ns scratchpad.web.sci
  "Runs ClojureScript typed into the page, using SCI (the Small Clojure
  Interpreter). Code evaluated here gets the *real* Reagent, component
  library and app state, so it behaves just like the compiled code in
  src/main."
  (:require [reagent.core :as r]
            [sci.configs.reagent.reagent :as sci-reagent]
            [sci.core :as sci]
            [scratchpad.components]
            [scratchpad.state :as state]))

;; Bumping this redraws the preview. Code in the editor can do that by
;; calling (scratchpad.main/render), same as in Codespaces.
(defonce render-count (r/atom 0))

(defn render! []
  (swap! render-count inc)
  nil)

(def ^:private components-ns (sci/create-ns 'scratchpad.components nil))
(def ^:private state-ns (sci/create-ns 'scratchpad.state nil))
(def ^:private main-ns (sci/create-ns 'scratchpad.main nil))

(defonce ctx
  (sci/init
   {:namespaces (merge (:namespaces sci-reagent/config)
                       {'scratchpad.components (sci/copy-ns scratchpad.components components-ns)
                        'scratchpad.state {'db (sci/copy-var state/db state-ns)}
                        'scratchpad.main {'render (sci/copy-var render! main-ns)}})
    ;; allow js/alert, js/console, js/document, ...
    :classes {'js js/globalThis :allow :all}}))

(defn- format-value [v]
  (binding [*print-length* 50
            *print-level* 6]
    (pr-str v)))

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
        (let [{:keys [line column]} (ex-data e)]
          {:ok? false
           :error (str (ex-message e)
                       (when line (str " (line " line (when column (str ", column " column)) ")")))
           :out (apply str @out)})))))

(defn resolve-var
  "Look up a var's current value, e.g. (resolve-var 'scratchpad.playground/page).
  Returns nil if it doesn't exist (yet)."
  [sym]
  (try
    (sci/eval-form ctx sym)
    (catch :default _ nil)))
