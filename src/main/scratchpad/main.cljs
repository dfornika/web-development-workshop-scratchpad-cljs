(ns scratchpad.main
  (:require [reagent.dom.client :as rdomc]
            [ag-grid-community :as ag-grid]
            [ag-charts-community :as ag-charts]
            [scratchpad.app :as app]
            [scratchpad.state :refer [db reload-count]]))

(defonce root (atom nil))

(defn render
  "Render the app component inside the 'app' div.
  Handy at the REPL if you've re-defined a component without saving:
  (scratchpad.main/render)"
  []
  (rdomc/render @root [app/app]))


(defn ^:dev/after-load re-render
  "Re-render the site when the source code is updated."
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code.
  ;; This function is called implicitly by its annotation.
  (swap! reload-count inc)
  (render))


(defn- sync-view-with-url!
  "Store the page selected in the URL (e.g. #/session-1) in the db."
  []
  (swap! db assoc :view (app/view-from-url)))


(defn main
  "Main entrypoint."
  []
  (render))


(defn ^:export init
  "Initialize the application."
  []
  (.registerModules ag-grid/ModuleRegistry #js [ag-grid/AllCommunityModule])
  (.registerModules ag-charts/ModuleRegistry #js [ag-charts/AllCommunityModule])
  (sync-view-with-url!)
  (.addEventListener js/window "hashchange" sync-view-with-url!)
  (when-let [el (.getElementById js/document "app")]
    (reset! root (rdomc/create-root el)))
  (main))
