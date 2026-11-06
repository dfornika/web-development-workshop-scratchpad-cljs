(ns scratchpad.app
  (:require [scratchpad.components :as components]))

(defn app
  "Top-level app component."
  []
  [:div
   [components/header]
   [components/layout-0 [:div]]])