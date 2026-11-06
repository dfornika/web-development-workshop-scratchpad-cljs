(ns scratchpad.app
  (:require [scratchpad.components :as components]
            [scratchpad.state :refer [db]]))

(defn app
  "Top-level app component."
  []
  [:div
   [components/header]
   [components/layout-0 [:div]]])