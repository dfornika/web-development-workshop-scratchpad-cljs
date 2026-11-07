(ns scratchpad.web.homework
  "The Homework pages for the in-browser version: edit a session's homework
  files right on the page, with the exercise panels updating alongside."
  (:require [scratchpad.components :as c]
            [scratchpad.views.session :as session]
            [scratchpad.views.session-1 :as session-1]
            [scratchpad.views.session-2 :as session-2]
            [scratchpad.web.sci :as sci]
            [scratchpad.web.workspace :as workspace]
            [shadow.resource :as rc]))

;; Each session's homework: its workspace (the real files, embedded at build
;; time - so the browser version always starts from exactly the same code as
;; Codespaces), and the page that shows the results.
(defonce sessions
  {1 {:workspace (workspace/make
                  {:id "session-1"
                   :files [{:name "session_1.cljs"
                            :path "sessions/session_1.cljs"
                            :source (rc/inline "scratchpad/sessions/session_1.cljs")}
                           {:name "session_1_bonus.cljs"
                            :path "sessions/session_1_bonus.cljs"
                            :source (rc/inline "scratchpad/sessions/session_1_bonus.cljs")}]})
      :files session-1/files
      :page session-1/page}
   2 {:workspace (workspace/make
                  {:id "session-2"
                   :files [{:name "session_2.cljs"
                            :path "sessions/session_2.cljs"
                            :source (rc/inline "scratchpad/sessions/session_2.cljs")}
                           {:name "session_2_bonus.cljs"
                            :path "sessions/session_2_bonus.cljs"
                            :source (rc/inline "scratchpad/sessions/session_2_bonus.cljs")}]})
      :files session-2/files
      :page session-2/page}})

(defn init!
  "Load every session's homework files, so all the pages work straight away."
  []
  (doseq [[_ {:keys [workspace]}] (sort sessions)]
    (workspace/init! workspace)))

(defn- sci-lookup
  "Finds homework functions in the code loaded from the editor."
  [files]
  (fn [file sym]
    ;; deref'ing render-count redraws the panels whenever a file is loaded
    @sci/render-count
    (sci/resolve-var (symbol (get-in files [file :ns]) (name sym)))))

(defn- help [n]
  [c/callout {:style {:font-size "14px"}}
   [:strong (str "Session " n " homework, in your browser. ")]
   "Work through the TODOs in the files on the left (switch between them with "
   "the tabs). " [:strong "Ctrl+S"] " loads the file and updates the exercise "
   "panels on the right. " [:strong "Alt+Enter"] " evaluates the top-level form "
   "at the cursor - try it in the " [:code "(comment ...)"] " blocks - and "
   [:strong "Ctrl+Enter"] " the form at the cursor. "
   "(On a Mac: Cmd instead of Ctrl, Option instead of Alt.) "
   "Your edits are saved in this browser only."])

(defn page
  "The in-browser Homework page for session `n`."
  [n]
  (let [{:keys [workspace files page]} (get sessions n)]
    [workspace/view workspace {:help [help n]
                               :preview-label "Your results"
                               ;; it sits under the Lesson | Homework tabs
                               :top-offset-px session/subnav-height-px}
     [page {:lookup (sci-lookup files) :in-browser? true}]]))
