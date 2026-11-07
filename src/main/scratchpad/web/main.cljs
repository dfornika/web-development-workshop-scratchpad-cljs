(ns scratchpad.web.main
  "Entry point for the in-browser version of the site (the :web build).
  Same pages as the regular app, except the Playground and the Session 1
  homework can be edited right on the page."
  (:require [reagent.dom.client :as rdomc]
            [scratchpad.app :as app]
            [scratchpad.state :refer [db reload-count]]
            [scratchpad.web.playground :as web-playground]
            [scratchpad.web.session-1 :as web-session-1]))

(defonce root (atom nil))

(def overrides
  {:playground [web-playground/page]
   :session-1 [web-session-1/page]})

(defn render []
  (rdomc/render @root [app/app overrides]))

(defn ^:dev/after-load re-render []
  (swap! reload-count inc)
  (render))

(defn- sync-view-with-url! []
  (swap! db assoc :view (app/view-from-url)))

(defn ^:export init []
  ;; lets pages (like Home) know they're in the in-browser version
  (swap! db assoc :edition :web)
  (sync-view-with-url!)
  (.addEventListener js/window "hashchange" sync-view-with-url!)
  (web-playground/init!)
  (web-session-1/init!)
  (when-let [el (.getElementById js/document "app")]
    (reset! root (rdomc/create-root el)))
  (render))
