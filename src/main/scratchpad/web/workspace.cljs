(ns scratchpad.web.workspace
  "An in-browser coding workspace, used by the in-browser Playground and
  Session 1 pages: one or more files in an editor (with tabs to switch
  between them), a REPL output log underneath, and something live on the
  right - side by side, with a draggable divider between them.

  The keys work like Calva's: Alt+Enter evaluates the top-level form,
  Ctrl+Enter the form at the cursor, and Ctrl+S loads the whole file
  (like saving it in Codespaces)."
  (:require [reagent.core :as r]
            [scratchpad.components :as c]
            [scratchpad.state :refer [reload-count]]
            [scratchpad.ui :as ui]
            [scratchpad.web.editor :as editor]
            [scratchpad.web.sci :as sci]))

(def ^:private max-log-entries 30)

(defn make
  "Create a workspace. `id` names it (for saving things in the browser).
  `files` is a vector of maps:
    :name    shown on its tab, and used when downloading, e.g. \"session_1.cljs\"
    :path    where it lives under src/main/scratchpad, e.g. \"sessions/session_1.cljs\"
    :source  its original code"
  [{:keys [id files]}]
  {:id id
   :files files
   :current (r/atom 0)  ; which file is in the editor (an index into files)
   :nss (atom {})       ; file index -> the namespace it was last loaded into
   :log (r/atom ())
   :next-log-id (atom 0)
   :view-ref (atom nil)
   :split (r/atom nil)})

;; ---------------------------------------------------------------------------
;; Saving edits in the browser
;; ---------------------------------------------------------------------------

(defn- storage-key [file]
  (str "scratchpad/" (:path file)))

(defn- saved-text [file]
  (try (.getItem js/localStorage (storage-key file)) (catch :default _ nil)))

(defn- save! [file text]
  (try (.setItem js/localStorage (storage-key file) text) (catch :default _ nil)))

(defn- forget-saved! [file]
  (try (.removeItem js/localStorage (storage-key file)) (catch :default _ nil)))

(defn- file-text
  "The text a file starts with: the saved edits, or else the original."
  [file]
  (or (saved-text file) (:source file)))

;; ---------------------------------------------------------------------------
;; Running code
;; ---------------------------------------------------------------------------

(defn- log! [{:keys [log next-log-id]} code {:keys [ok? value error out]}]
  (let [entry {:id (swap! next-log-id inc)
               :code code :ok? ok? :text (if ok? value error) :out out}]
    (swap! log #(take max-log-entries (conj % entry)))))

(defn- load-file!
  "Evaluate a whole file, then redraw - like saving in Codespaces."
  [{:keys [files nss] :as ws} index text]
  (let [result (sci/eval-code text nil)]
    (when (:ok? result)
      (swap! nss assoc index (:ns result)))
    (log! ws (str ";; loaded " (:name (nth files index)))
          (if (:ok? result) (assoc result :value "ok") result))
    ;; retry any panels that were showing an error, and redraw
    (swap! reload-count inc)
    (sci/render!)))

(defn init!
  "Load every file once, at startup, so everything on the page works
  straight away."
  [{:keys [files] :as ws}]
  (doseq [[index file] (map-indexed vector files)]
    (load-file! ws index (file-text file))))

(defn- eval-form!
  "Evaluate one form, in the namespace of the file it's in (like Calva)."
  [{:keys [current nss] :as ws} code]
  (when (seq code)
    (log! ws code (sci/eval-code code (get @nss @current)))))

(defn- current-text [{:keys [view-ref]}]
  (some-> ^js @view-ref .-state .-doc .toString))

(defn- download! [text filename]
  (let [blob (js/Blob. #js [text] #js {:type "text/plain"})
        url (.createObjectURL js/URL blob)
        a (.createElement js/document "a")]
    (set! (.-href a) url)
    (set! (.-download a) filename)
    (.click a)
    (.revokeObjectURL js/URL url)))

(defn- reset-to-original! [{:keys [files current view-ref log] :as ws}]
  (let [file (nth files @current)]
    (when (js/confirm (str "Throw away your changes to " (:name file)
                           " and go back to the original code?"))
      (forget-saved! file)
      (editor/set-doc! @view-ref (:source file))
      (reset! log ())
      (load-file! ws @current (:source file)))))

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

(defn- output [{:keys [log]}]
  [:div {:style {:font-family "ui-monospace, Menlo, Consolas, monospace"
                 :font-size "13px"
                 :flex "0 0 28%"
                 :min-height "100px"
                 :box-sizing "border-box"
                 :overflow-y "auto"
                 :padding "4px 12px"
                 :border "1px solid #d0d7de"
                 :border-radius "6px"
                 :background-color "#f6f8fa"}}
   (let [entries @log]
     (if (empty? entries)
       [:div {:style {:color "#57606a" :padding "6px 0"}}
        "Results of evaluating code appear here (newest first)."]
       (for [entry entries]
         ^{:key (:id entry)}
         [log-entry entry])))])

;; ---------------------------------------------------------------------------
;; The draggable divider between the code and the right-hand side
;; ---------------------------------------------------------------------------

(def ^:private default-split 50)

(defn- split-key [{:keys [id]}]
  (str "scratchpad/" id "-split"))

(defn- clamp-split
  "Keep the divider between 25% and 75% of the width."
  [percent]
  (max 25 (min 75 percent)))

(defn- load-split [ws]
  (try
    (let [v (js/parseFloat (.getItem js/localStorage (split-key ws)))]
      (if (js/isNaN v) default-split (clamp-split v)))
    (catch :default _ default-split)))

(defn- save-split! [ws percent]
  (try (.setItem js/localStorage (split-key ws) (str percent)) (catch :default _ nil)))

(defn- divider
  "Drag to resize the two sides; double-click to put it back in the middle.
  Uses pointer capture, so the drag keeps working even if the pointer moves
  over the editor or the preview."
  [{:keys [split] :as ws} container-ref]
  (r/with-let [dragging? (r/atom false)]
    [:div {:class "playground-divider"
           :title "Drag to resize. Double-click to reset."
           :style {:flex "0 0 12px"
                   :cursor "col-resize"
                   :display "flex"
                   :justify-content "center"
                   :touch-action "none"}
           :on-pointer-down (fn [^js e]
                              (.preventDefault e)
                              (.setPointerCapture (.-currentTarget e) (.-pointerId e))
                              (reset! dragging? true))
           :on-pointer-move (fn [^js e]
                              (when-let [^js el (and @dragging? @container-ref)]
                                (let [rect (.getBoundingClientRect el)]
                                  (reset! split (clamp-split
                                                 (* 100 (/ (- (.-clientX e) (.-left rect))
                                                           (.-width rect))))))))
           :on-pointer-up (fn [_]
                            (reset! dragging? false)
                            (save-split! ws @split))
           :on-lost-pointer-capture (fn [_] (reset! dragging? false))
           :on-double-click (fn [_]
                              (reset! split default-split)
                              (save-split! ws default-split))}
     [:div {:class "playground-divider-line"
            :style {:width "3px"
                    :height "100%"
                    :border-radius "2px"
                    :background-color (if @dragging? "#54aeff" "#d0d7de")}}]]))

;; ---------------------------------------------------------------------------
;; The workspace itself
;; ---------------------------------------------------------------------------

(defn- small-button [opts label]
  [c/button (merge {:variant :secondary
                    :style {:padding "4px 10px" :font-size "13px"}}
                   opts)
   label])

(defn- file-tabs [{:keys [files current]}]
  (when (> (count files) 1)
    [:div {:style {:display "flex" :gap "2px" :border-bottom "1px solid #d0d7de"}}
     (let [selected @current]
       (for [[index {:keys [name]}] (map-indexed vector files)]
         ^{:key name}
         [:button {:on-click #(reset! current index)
                   :style {:border "none"
                           :background "none"
                           :padding "4px 12px"
                           :margin-bottom "-1px"
                           :cursor "pointer"
                           :font-family "ui-monospace, Menlo, Consolas, monospace"
                           :font-size "13px"
                           :color (if (= index selected) "#004a87" "#57606a")
                           :font-weight (if (= index selected) "bold" "normal")
                           :border-bottom (if (= index selected)
                                            "2px solid #004a87"
                                            "2px solid transparent")}}
          name]))]))

(defn view
  "Show the workspace `ws`. Options:
     :help           shown across the top
     :preview-label  the heading over the right-hand side
     :top-offset-px  extra space taken above the workspace (besides the nav bar)
   `preview` is what to show on the right (it scrolls on its own)."
  [{:keys [files current view-ref split] :as ws} {:keys [help preview-label top-offset-px] :or {top-offset-px 0}} preview]
  (r/with-let [container-ref (atom nil)
               _ (when (nil? @split) (reset! split (load-split ws)))]
    (let [index @current
          file (nth files index)]
      ;; Exactly fills the window below the nav bar, so the page itself never
      ;; scrolls - the editor, the output and the preview each scroll on their own.
      [:div {:style {:height (str "calc(100vh - " (+ ui/nav-height-px top-offset-px) "px)")
                     :box-sizing "border-box"
                     :padding "12px 16px"
                     :display "flex"
                     :flex-direction "column"
                     :gap "10px"
                     :overflow "hidden"}}
       help
       [:div {:ref #(reset! container-ref %)
              :style {:flex "1 1 0"
                      :min-height 0
                      :display "flex"}}
        ;; left: code + output
        [:div {:style {:flex (str "0 0 calc(" @split "% - 6px)")
                       :min-width 0
                       :display "flex"
                       :flex-direction "column"
                       :gap "8px"}}
         [c/row {:gap "6px"}
          [small-button {:variant :primary
                         :on-click #(load-file! ws @current (current-text ws))}
           "Load file (Ctrl+S)"]
          [small-button {:on-click #(download! (current-text ws) (:name file))}
           (str "Download " (:name file))]
          [small-button {:on-click #(reset-to-original! ws)} "Reset to original"]]
         [file-tabs ws]
         ;; a fresh editor for each file (keyed by its index)
         ^{:key index}
         [editor/editor {:initial-doc (file-text file)
                         :view-ref view-ref
                         :on-change #(save! file %)
                         :on-eval #(eval-form! ws %)
                         :on-load #(load-file! ws index %)
                         :style {:flex "1 1 0" :min-height 0}}]
         [output ws]]
        [divider ws container-ref]
        ;; right: whatever this page shows
        [:div {:style {:flex "1 1 0"
                       :min-width 0
                       :display "flex"
                       :flex-direction "column"
                       :gap "8px"}}
         [:div {:style {:font-size "13px" :color "#57606a" :padding "6px 0"}} preview-label]
         [:div {:style {:flex "1 1 0"
                        :min-height 0
                        :overflow "auto"
                        :border "1px solid #d0d7de"
                        :border-radius "6px"
                        :background-color "white"}}
          [ui/error-boundary preview]]]]])))
