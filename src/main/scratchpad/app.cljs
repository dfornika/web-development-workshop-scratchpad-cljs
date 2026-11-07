(ns scratchpad.app
  "The top level of the site: the nav bar plus whichever page is selected.
  The page you'll mostly edit is src/main/scratchpad/playground.cljs."
  (:require [scratchpad.playground :as playground]
            [scratchpad.state :refer [db]]
            [scratchpad.ui :as ui]
            [scratchpad.views.gallery :as gallery]
            [scratchpad.views.home :as home]
            [scratchpad.views.repl-guide :as repl-guide]
            [scratchpad.views.session-1 :as session-1]
            [scratchpad.views.later-sessions :as later-sessions]))

(def views
  [{:id :home :label "Home"}
   {:id :session-1 :label "Session 1"}
   {:id :session-2 :label "Session 2"}
   {:id :session-3 :label "Session 3"}
   {:id :session-4 :label "Session 4"}
   {:id :playground :label "Playground"}
   {:id :components :label "Components"}
   {:id :repl-guide :label "REPL Guide"}])

(defn view-from-url
  "Which page to show, based on the #/... part of the URL."
  []
  (let [id (keyword (subs (.. js/window -location -hash) 2))]
    (if (some #(= id (:id %)) views)
      id
      :home)))

(defn app
  "Top-level app component. `overrides` (optional) maps a page id to a
  different component to show for that page - the in-browser build uses
  it to swap in an editable Playground."
  ([] (app {}))
  ([overrides]
   (let [current (or (:view @db) :home)]
     [:div
      [ui/nav-bar views current]
      ;; keyed by page, so an error on one page doesn't follow you to the next
      ^{:key current}
      [ui/error-boundary
       (or
        (get overrides current)
        (case current
          :home [home/page]
          :session-1 [session-1/page]
          :session-2 [later-sessions/page 2]
          :session-3 [later-sessions/page 3]
          :session-4 [later-sessions/page 4]
          :playground [playground/page]
          :components [gallery/page]
          :repl-guide [repl-guide/page]))]])))
