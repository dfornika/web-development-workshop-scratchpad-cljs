(ns scratchpad.web.session-1
  "The Session 1 page for the in-browser version: edit the homework files
  right on the page, with the exercise panels updating alongside."
  (:require [scratchpad.components :as c]
            [scratchpad.views.session-1 :as session-1]
            [scratchpad.web.sci :as sci]
            [scratchpad.web.workspace :as workspace]
            [shadow.resource :as rc]))

(defonce workspace
  (workspace/make
   {:id "session-1"
    ;; The real homework files, embedded at build time - so the browser
    ;; version always starts from exactly the same code as Codespaces.
    :files [{:name "session_1.cljs"
             :path "sessions/session_1.cljs"
             :source (rc/inline "scratchpad/sessions/session_1.cljs")}
            {:name "session_1_bonus.cljs"
             :path "sessions/session_1_bonus.cljs"
             :source (rc/inline "scratchpad/sessions/session_1_bonus.cljs")}]}))

(defn init! []
  (workspace/init! workspace))

(defn- sci-lookup
  "Finds a homework function in the code loaded from the editor."
  [file sym]
  ;; deref'ing render-count redraws the panels whenever a file is loaded
  @sci/render-count
  (sci/resolve-var (symbol (get-in session-1/files [file :ns]) (name sym))))

(def ^:private help
  [c/callout {:style {:font-size "14px"}}
   [:strong "Session 1 homework, in your browser. "]
   "Work through the TODOs in the files on the left (switch between them with "
   "the tabs). " [:strong "Ctrl+S"] " loads the file and updates the exercise "
   "panels on the right. " [:strong "Alt+Enter"] " evaluates the top-level form "
   "at the cursor - try it in the " [:code "(comment ...)"] " blocks - and "
   [:strong "Ctrl+Enter"] " the form at the cursor. "
   "(On a Mac: Cmd instead of Ctrl, Option instead of Alt.) "
   "Your edits are saved in this browser only."])

(defn page []
  [workspace/view workspace {:help help :preview-label "Your results"}
   [session-1/page {:lookup sci-lookup :in-browser? true}]])
