(ns scratchpad.app
  "The top level of the site: the nav bar plus whichever page is selected.
  The page you'll mostly edit is src/main/scratchpad/playground.cljs."
  (:require [scratchpad.playground :as playground]
            [scratchpad.state :refer [db]]
            [scratchpad.ui :as ui]
            [scratchpad.views.gallery :as gallery]
            [scratchpad.views.repl-guide :as repl-guide]
            [scratchpad.views.session-1 :as session-1]
            [scratchpad.views.later-sessions :as later-sessions]))

(def views
  [{:id :playground :label "Playground"}
   {:id :components :label "Components"}
   {:id :repl-guide :label "REPL Guide"}
   {:id :session-1  :label "Session 1"}
   {:id :sessions-2-4 :label "Sessions 2–4"}])

(defn view-from-url
  "Which page to show, based on the #/... part of the URL."
  []
  (let [id (keyword (subs (.. js/window -location -hash) 2))]
    (if (some #(= id (:id %)) views)
      id
      :playground)))

(defn app
  "Top-level app component."
  []
  (let [current (or (:view @db) :playground)]
    [:div
     [ui/nav-bar views current]
     ;; keyed by page, so an error on one page doesn't follow you to the next
     ^{:key current}
     [ui/error-boundary
      (case current
        :playground [playground/page]
        :components [gallery/page]
        :repl-guide [repl-guide/page]
        :session-1 [session-1/page]
        :sessions-2-4 [later-sessions/page])]]))
