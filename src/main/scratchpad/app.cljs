(ns scratchpad.app
  "The top level of the site: the nav bar plus whichever page is selected.
  The page you'll mostly edit is src/main/scratchpad/playground.cljs."
  (:require [scratchpad.playground :as playground]
            [scratchpad.state :refer [db]]
            [scratchpad.ui :as ui]
            [scratchpad.views.gallery :as gallery]
            [scratchpad.views.home :as home]
            [scratchpad.views.js-interop :as js-interop]
            [scratchpad.views.repl-guide :as repl-guide]
            [scratchpad.views.lessons.session-1 :as lesson-1]
            [scratchpad.views.lessons.session-2 :as lesson-2]
            [scratchpad.views.session :as session]
            [scratchpad.views.session-1 :as session-1]
            [scratchpad.views.session-2 :as session-2]
            [scratchpad.views.later-sessions :as later-sessions]))

(def views
  [{:id :home :label "Home"}
   {:id :session-1 :label "Session 1"}
   {:id :session-2 :label "Session 2"}
   {:id :session-3 :label "Session 3"}
   {:id :session-4 :label "Session 4"}
   {:id :playground :label "Playground"}
   {:id :components :label "Components"}
   {:id :repl-guide :label "REPL Guide"}
   {:id :js-interop :label "JS Interop"}])

(defn route-from-url
  "Which page (and which tab on it) to show, from the #/... part of the URL,
  e.g. \"#/session-1/homework\" -> {:view :session-1 :subview \"homework\"}."
  []
  (let [[page sub] (.split (subs (.. js/window -location -hash) 2) "/")
        id (keyword page)]
    {:view (if (some #(= id (:id %)) views) id :home)
     :subview sub}))

(defn app
  "Top-level app component. `overrides` (optional) maps a page id to a
  different component to show for that page - the in-browser build uses
  it to swap in its editable Playground and session homework (as
  :session-1-homework, :session-2-homework)."
  ([] (app {}))
  ([overrides]
   (let [{:keys [view subview]} @db
         current (or view :home)]
     [:div
      [ui/nav-bar views current]
      ;; keyed by page (and tab), so an error on one doesn't follow you to the next
      ^{:key (str current "/" subview)}
      [ui/error-boundary
       (or
        (get overrides current)
        (case current
          :home [home/page]
          :session-1 [session/page 1 {:lesson [lesson-1/page]
                                      :homework (get overrides :session-1-homework
                                                     [session-1/page])}]
          :session-2 [session/page 2 {:lesson [lesson-2/page]
                                      :homework (get overrides :session-2-homework
                                                     [session-2/page])}]
          :session-3 [session/page 3 {:lesson [later-sessions/lesson 3]
                                      :homework [later-sessions/homework 3]}]
          :session-4 [session/page 4 {:lesson [later-sessions/lesson 4]
                                      :homework [later-sessions/homework 4]}]
          :playground [playground/page]
          :components [gallery/page]
          :repl-guide [repl-guide/page]
          :js-interop [js-interop/page]))]])))
