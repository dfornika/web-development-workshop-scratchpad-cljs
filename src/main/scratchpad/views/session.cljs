(ns scratchpad.views.session
  "A session page: a second row of tabs (Lesson | Homework) under the nav
  bar, and whichever of the two is selected. The address says which:
  #/session-1/lesson or #/session-1/homework."
  (:require [scratchpad.state :refer [db]]
            [scratchpad.ui :as ui]))

(def subnav-height-px
  "Height of the Lesson | Homework tabs."
  36)

(defn current-tab
  "Which tab is selected, from the address: :lesson (the default) or :homework."
  []
  (if (= "homework" (:subview @db)) :homework :lesson))

(defn- tabs [n selected]
  [:div {:style {:position "sticky"
                 :top (str ui/nav-height-px "px")
                 :z-index 900
                 :height (str subnav-height-px "px")
                 :box-sizing "border-box"
                 :display "flex"
                 :align-items "stretch"
                 :gap "4px"
                 :padding "0 16px"
                 :background-color "#eaeef2"
                 :border-bottom "1px solid #d0d7de"}}
   [:span {:style {:display "flex" :align-items "center" :margin-right "8px"
                   :font-weight "bold" :font-size "14px" :color "#24292f"}}
    (str "Session " n)]
   (for [[id label] [[:lesson "Lesson"] [:homework "Homework"]]]
     ^{:key id}
     [:a {:href (str "#/session-" n "/" (name id))
          :style {:display "flex"
                  :align-items "center"
                  :padding "0 12px"
                  :font-size "14px"
                  :text-decoration "none"
                  :color (if (= id selected) "#004a87" "#57606a")
                  :font-weight (if (= id selected) "bold" "normal")
                  :border-bottom (if (= id selected)
                                   "3px solid #004a87"
                                   "3px solid transparent")}}
      label])])

(defn page
  "The page for session `n`. `lesson` and `homework` are what to show on
  each tab."
  [n {:keys [lesson homework]}]
  (let [selected (current-tab)]
    [:div
     [tabs n selected]
     (if (= selected :homework) homework lesson)]))
