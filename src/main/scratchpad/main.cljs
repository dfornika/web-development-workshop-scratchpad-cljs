(ns scratchpad.main
  (:require [reagent.dom.client :as rdomc]
            [ag-grid-community :as ag-grid]
            [ag-charts-community :as ag-charts]
            [workshop.app :refer [app]]))

(defonce root (atom nil))

(defn render
  "Render the app component inside the 'app' div."
  []
  (rdomc/render @root [app]))


(defn ^:dev/after-load re-render
  "Re-render the site when the source code is updated."
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code.
  ;; This function is called implicitly by its annotation.
  (render))


(defn main
  "Main entrypoint."
  []
  (render))


(defn ^:export init
  "Initialize the application."
  []
  (.registerModules ag-grid/ModuleRegistry #js [ag-grid/AllCommunityModule])
  (.registerModules ag-charts/ModuleRegistry #js [ag-charts/AllCommunityModule])
  (when-let [el (.getElementById js/document "app")]
    (reset! root (rdomc/create-root el)))
  (main))
