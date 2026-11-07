(ns scratchpad.web.playground
  "The Playground page for the in-browser version: edit
  src/main/scratchpad/playground.cljs right on the page, with a live
  preview and a REPL."
  (:require [reagent.core :as r]
            [scratchpad.components :as c]
            [scratchpad.ui :as ui]
            [scratchpad.web.editor :as editor]
            [scratchpad.web.sci :as sci]
            [shadow.resource :as rc]))

;; The real playground.cljs, embedded at build time - so the browser
;; version always starts from exactly the same code as Codespaces.
(def original-source (rc/inline "scratchpad/playground.cljs"))

(def ^:private storage-key "scratchpad/playground.cljs")
(def ^:private max-log-entries 30)

(defn- load-saved []
  (try (.getItem js/localStorage storage-key) (catch :default _ nil)))

(defn- save! [text]
  (try (.setItem js/localStorage storage-key text) (catch :default _ nil)))

(defn- forget-saved! []
  (try (.removeItem js/localStorage storage-key) (catch :default _ nil)))

;; The namespace the file was last loaded into. Single forms are evaluated
;; there, like in Calva.
(defonce current-ns (atom nil))
(defonce output-log (r/atom ()))
(defonce view-ref (atom nil))

(defonce ^:private next-log-id (atom 0))

(defn- log! [entry]
  (let [entry (assoc entry :id (swap! next-log-id inc))]
    (swap! output-log #(take max-log-entries (conj % entry)))))

(defn- log-result! [code {:keys [ok? value error out]}]
  (log! {:code code :ok? ok? :text (if ok? value error) :out out}))

(defn load-file!
  "Evaluate the whole file and redraw the preview - like saving in Codespaces."
  [text]
  (let [result (sci/eval-code text nil)]
    (when (:ok? result)
      (reset! current-ns (:ns result)))
    (log-result! ";; loaded the file" (if (:ok? result) (assoc result :value "ok") result))
    (sci/render!)))

(defn- eval-form! [code]
  (when (seq code)
    (log-result! code (sci/eval-code code @current-ns))))

(defn- download! [text]
  (let [blob (js/Blob. #js [text] #js {:type "text/plain"})
        url (.createObjectURL js/URL blob)
        a (.createElement js/document "a")]
    (set! (.-href a) url)
    (set! (.-download a) "playground.cljs")
    (.click a)
    (.revokeObjectURL js/URL url)))

(defn- current-text []
  (some-> ^js @view-ref .-state .-doc .toString))

(defn- reset-to-original! []
  (when (js/confirm "Throw away your changes and go back to the original code?")
    (forget-saved!)
    (editor/set-doc! @view-ref original-source)
    (reset! output-log ())
    (load-file! original-source)))

;; ---------------------------------------------------------------------------
;; The preview
;; ---------------------------------------------------------------------------

(defn- run-page
  "Calls the playground's `page` function. Taking `_render-count` as an
  argument makes Reagent re-run this whenever the preview is redrawn."
  [page _render-count]
  (page))

(defn- preview []
  (let [n @sci/render-count
        page (sci/resolve-var 'scratchpad.playground/page)]
    [:div {:style {:border "1px solid #d0d7de"
                   :border-radius "6px"
                   :background-color "white"
                   :min-height "200px"}}
     (if (fn? page)
       [ui/error-boundary [run-page page n]]
       [c/callout {:kind :warning}
        "Nothing to show yet: the file needs to define a "
        [:code "page"] " function in the " [:code "scratchpad.playground"]
        " namespace. Load the file with Ctrl+S."])]))

;; ---------------------------------------------------------------------------
;; The output log
;; ---------------------------------------------------------------------------

(defn- log-entry [{:keys [code ok? text out]}]
  [:div {:style {:padding "6px 0" :border-bottom "1px solid #eaeef2"}}
   [:div {:style {:color "#57606a" :white-space "pre-wrap"}} code]
   (when (seq out)
     [:div {:style {:white-space "pre-wrap"}} out])
   [:div {:style {:color (if ok? "#1a7f37" "#cf222e") :white-space "pre-wrap"}}
    (if ok? (str "=> " text) (str "Error: " text))]])

(defn- output []
  [:div {:style {:font-family "ui-monospace, Menlo, Consolas, monospace"
                 :font-size "13px"
                 :height "220px"
                 :overflow-y "auto"
                 :padding "4px 12px"
                 :border "1px solid #d0d7de"
                 :border-radius "6px"
                 :background-color "#f6f8fa"}}
   (let [entries @output-log]
     (if (empty? entries)
       [:div {:style {:color "#57606a" :padding "6px 0"}}
        "Results of evaluating code appear here (newest first)."]
       (for [entry entries]
         ^{:key (:id entry)}
         [log-entry entry])))])

;; ---------------------------------------------------------------------------
;; The page
;; ---------------------------------------------------------------------------

(defn- small-button [opts label]
  [c/button (merge {:variant :secondary
                    :style {:padding "4px 10px" :font-size "13px"}}
                   opts)
   label])

(defn- help []
  [c/callout {:style {:margin-bottom "12px" :font-size "14px"}}
   [:strong "This is the in-browser Playground. "]
   "Edit the code on the left. "
   [:strong "Ctrl+S"] " loads the whole file and redraws the preview (like saving in Codespaces). "
   [:strong "Alt+Enter"] " evaluates the top-level form at the cursor, and "
   [:strong "Ctrl+Enter"] " the form at the cursor; results appear under the editor. "
   "(On a Mac: Cmd instead of Ctrl, Option instead of Alt.) "
   "Your edits are saved in this browser only."])

(defn init!
  "Load the saved (or original) code once, at startup."
  []
  (load-file! (or (load-saved) original-source)))

(defn page []
  (r/with-let [initial (or (load-saved) original-source)]
    [:div {:style {:padding "16px"}}
     [help]
     [:div {:style {:display "grid"
                    :grid-template-columns "minmax(0, 1fr) minmax(0, 1fr)"
                    :gap "16px"}}
      [c/column {:gap "8px"}
       [c/row {:gap "6px"}
        [small-button {:variant :primary :on-click #(load-file! (current-text))} "Load file (Ctrl+S)"]
        [small-button {:on-click #(download! (current-text))} "Download .cljs"]
        [small-button {:on-click reset-to-original!} "Reset to original"]]
       [editor/editor {:initial-doc initial
                       :view-ref view-ref
                       :on-change save!
                       :on-eval eval-form!
                       :on-load load-file!
                       :style {:height "60vh"}}]
       [output]]
      [c/column {:gap "8px"}
       [:div {:style {:font-size "13px" :color "#57606a" :padding "6px 0"}} "Preview"]
       [preview]]]]))
