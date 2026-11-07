(ns scratchpad.web.editor
  "A ClojureScript code editor (CodeMirror 6 + Nextjournal's clojure-mode),
  with REPL-style keybindings that mirror Calva's.

  It's deliberately a *plain* editor: you type exactly what you type.
  clojure-mode's structural editing (paredit) is left out on purpose - no
  auto-inserted closing brackets, no protected brackets, no slurp/barf
  keys - since it takes a while to get used to. We only use its syntax
  highlighting, plus a highlight on matching brackets."
  (:require ["@codemirror/commands" :refer [defaultKeymap history historyKeymap indentWithTab]]
            ["@codemirror/language" :refer [bracketMatching defaultHighlightStyle syntaxHighlighting]]
            ["@codemirror/state" :refer [EditorState Prec]]
            ["@codemirror/view" :refer [EditorView drawSelection highlightActiveLine keymap lineNumbers]]
            ["@nextjournal/clojure-mode" :as clojure-mode]
            ["@nextjournal/clojure-mode/extensions/eval-region" :as eval-region]
            [reagent.core :as r]))

(def ^:private theme
  (.theme EditorView
          #js {"&" #js {:height "100%"
                        :font-size "14px"
                        :background-color "white"}
               ".cm-scroller" #js {:font-family "ui-monospace, Menlo, Consolas, monospace"
                                   :overflow "auto"}
               "&.cm-focused" #js {:outline "none"}}))

(defn- repl-keymap
  "Keybindings, mirroring Calva:
     Alt+Enter     evaluate the top-level form around the cursor
     Ctrl+Enter    evaluate the form at the cursor (Cmd+Enter on a Mac)
     Ctrl+S        load the whole file (Cmd+S on a Mac) - like saving"
  [callbacks]
  (let [run (fn [k text]
              (when-let [f (get @callbacks k)]
                (f text))
              true)]
    (.highest Prec
              (.of keymap
                   #js [#js {:key "Alt-Enter"
                             :run (fn [^js view]
                                    (run :on-eval (eval-region/top_level_string (.-state view))))}
                        #js {:key "Mod-Enter"
                             :run (fn [^js view]
                                    (run :on-eval (eval-region/cursor_node_string (.-state view))))}
                        #js {:key "Mod-s"
                             :preventDefault true
                             :run (fn [^js view]
                                    (run :on-load (.. view -state -doc toString)))}]))))

(defn- make-view [parent doc callbacks {:keys [line-numbers? max-height]
                                         :or {line-numbers? true}}]
  (EditorView.
   #js {:parent parent
        :state (.create EditorState
                        #js {:doc doc
                             :extensions
                             #js [(repl-keymap callbacks)
                                  theme
                                  (if max-height
                                    (.theme EditorView #js {"&" #js {:max-height max-height}})
                                    #js [])
                                  ;; wrap long lines rather than scrolling sideways
                                  (.-lineWrapping EditorView)
                                  (if line-numbers? (lineNumbers) #js [])
                                  (highlightActiveLine)
                                  (drawSelection)
                                  (history)
                                  (syntaxHighlighting defaultHighlightStyle)
                                  ;; Clojure syntax only. (clojure-mode/default_extensions
                                  ;; and clojure-mode/complete_keymap would add paredit.)
                                  (clojure-mode/syntax clojure-mode/parser)
                                  (bracketMatching)
                                  (.of keymap #js [indentWithTab])
                                  (.of keymap historyKeymap)
                                  (.of keymap defaultKeymap)
                                  (.. EditorView -updateListener
                                      (of (fn [^js update]
                                            (when (.-docChanged update)
                                              (when-let [f (:on-change @callbacks)]
                                                (f (.. update -state -doc toString)))))))]})}))

(defn set-doc!
  "Replace everything in the editor `view` with `text`."
  [^js view text]
  (.dispatch view #js {:changes #js {:from 0
                                     :to (.. view -state -doc -length)
                                     :insert text}}))

(defn editor
  "A code editor. Options:
     :initial-doc  the starting text
     :on-change    called with the full text whenever it changes
     :on-eval      called with the text of a form to evaluate
     :on-load      called with the full text to load it (Ctrl+S)
     :view-ref     an atom that will hold the CodeMirror EditorView
     :line-numbers? show line numbers (default true)
     :max-height   e.g. \"300px\": grow with the text up to this height, then scroll
     :style        styles for the surrounding box"
  [_opts]
  (let [view (atom nil)
        callbacks (atom nil)]
    (r/create-class
     {:display-name "CodeEditor"
      :component-will-unmount (fn [] (some-> ^js @view .destroy))
      :reagent-render
      (fn [{:keys [initial-doc view-ref style] :as opts}]
        ;; keep the latest callbacks, so the editor always calls current ones
        (reset! callbacks opts)
        [:div {:style (merge {:border "1px solid #d0d7de"
                              :border-radius "6px"
                              :overflow "hidden"}
                             style)
               :ref (fn [el]
                      (when (and el (nil? @view))
                        (reset! view (make-view el initial-doc callbacks opts))
                        (when view-ref (reset! view-ref @view))))}])})))
