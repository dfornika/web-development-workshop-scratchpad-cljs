(ns scratchpad.components
  (:require [scratchpad.state :refer [db]]))

(defn header
  "Header component, with configurable title, title-color and height"
  [& {:keys [title title-color height-px logo-src]
      :or {title "Header"
           title-color "#004a87"
           height-px 48
           logo-src "images/logo.svg"}}]
  [:header {:style {:height (str height-px "px")}}
   [:div
    [:h1 {:style {:color title-color}} 
     title]]
   [:div {:style {:display "grid" :align-self "center" :justify-self "end"}}
    [:img {:src logo-src :height (str height-px "px")}]]])


(defn layout-0
  ""
  [children]
  [:div children])

(defn config-view
  []
  [:div
   [:textarea {:style {:font-family  "monospace"}}
    (str (:config @db))]])