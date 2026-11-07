(ns scratchpad.web.main
  "Entry point for the in-browser version of the site (the :web build).
  Same pages as the regular app, except the Playground and the session
  homework can be edited right on the page."
  (:require ["ag-grid-community" :as ag-grid]
            [reagent.dom.client :as rdomc]
            [scratchpad.app :as app]
            [scratchpad.state :refer [db reload-count]]
            [scratchpad.web.playground :as web-playground]
            [scratchpad.web.homework :as web-homework]))

(defonce root (atom nil))

(def overrides
  {:playground [web-playground/page]
   :session-1-homework [web-homework/page 1]
   :session-2-homework [web-homework/page 2]})

(defn render []
  (rdomc/render @root [app/app overrides]))

(defn ^:dev/after-load re-render []
  (swap! reload-count inc)
  (render))

(defn- sync-view-with-url! []
  (swap! db merge (app/route-from-url)))

(defn ^:export init []
  ;; AG Grid needs its features registered before any grid is drawn
  ;; (scratchpad.main does the same for the Codespaces version)
  (.registerModules ag-grid/ModuleRegistry #js [ag-grid/AllCommunityModule])
  ;; lets pages (like Home) know they're in the in-browser version
  (swap! db assoc :edition :web)
  (sync-view-with-url!)
  (.addEventListener js/window "hashchange" sync-view-with-url!)
  (web-playground/init!)
  (web-homework/init!)
  (when-let [el (.getElementById js/document "app")]
    (reset! root (rdomc/create-root el)))
  (render))
