(ns scratchpad.web.playground
  "The Playground page for the in-browser version: edit
  src/main/scratchpad/playground.cljs right on the page, with a live
  preview and a REPL."
  (:require [scratchpad.components :as c]
            [scratchpad.web.sci :as sci]
            [scratchpad.web.workspace :as workspace]
            [shadow.resource :as rc]))

(defonce workspace
  (workspace/make
   {:id "playground"
    ;; The real playground.cljs, embedded at build time - so the browser
    ;; version always starts from exactly the same code as Codespaces.
    :files [{:name "playground.cljs"
             :path "playground.cljs"
             :source (rc/inline "scratchpad/playground.cljs")}]}))

(defn init! []
  (workspace/init! workspace))

(defn- run-page
  "Calls the playground's `page` function. Taking `_render-count` as an
  argument makes Reagent re-run this whenever the preview is redrawn."
  [page _render-count]
  (page))

(defn- preview []
  (let [n @sci/render-count
        page (sci/resolve-var 'scratchpad.playground/page)]
    (if (fn? page)
      [run-page page n]
      [c/callout {:kind :warning}
       "Nothing to show yet: the file needs to define a "
       [:code "page"] " function in the " [:code "scratchpad.playground"]
       " namespace. Load the file with Ctrl+S."])))

(def ^:private help
  [c/callout {:style {:font-size "14px"}}
   [:strong "This is the in-browser Playground. "]
   "Edit the code on the left. "
   [:strong "Ctrl+S"] " loads the whole file and redraws the preview (like saving in Codespaces). "
   [:strong "Alt+Enter"] " evaluates the top-level form at the cursor, and "
   [:strong "Ctrl+Enter"] " the form at the cursor; results appear under the editor. "
   "(On a Mac: Cmd instead of Ctrl, Option instead of Alt.) "
   "Your edits are saved in this browser only."])

(defn page []
  [workspace/view workspace {:help help :preview-label "Preview"}
   [preview]])
