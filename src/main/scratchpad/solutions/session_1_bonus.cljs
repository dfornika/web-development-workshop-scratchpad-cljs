(ns scratchpad.solutions.session-1-bonus
  "Reference solutions for src/main/scratchpad/sessions/session_1_bonus.cljs.

  Just one way to do it - if yours works, it's right!"
  (:require [reagent.core :as r]
            [scratchpad.components :as c]))


;; 1. Click Counter ----------------------------------------------------------

(defn click-counter
  "A button that counts how many times it's been clicked."
  []
  (r/with-let [clicks (r/atom 0)]
    [c/row {:align "center"}
     [c/button {:on-click (fn [] (swap! clicks inc))}
      "Click Me"]
     [:span "Clicked " @clicks (if (= 1 @clicks) " time" " times")]
     [c/button {:variant :secondary
                :on-click (fn [] (reset! clicks 0))}
      "Reset"]]))


;; 2. Theme Switcher ---------------------------------------------------------

(def themes
  {:light {:background-color "white" :color "#24292f"}
   :dark  {:background-color "#24292f" :color "white"}})

(defn theme-switcher
  "A box with a button that switches it between light and dark mode."
  []
  (r/with-let [theme (r/atom :light)]
    [:div {:style (merge {:padding "16px" :border-radius "8px"}
                         (get themes @theme))}
     [c/paragraph "This box is in " (name @theme) " mode."]
     [c/button {:variant :secondary
                :on-click (fn [] (swap! theme #(if (= % :light) :dark :light)))}
      (if (= @theme :light) "🌙 Dark mode" "☀️ Light mode")]]))


;; 3. Image Gallery ----------------------------------------------------------

(def example-images
  [{:src "images/gel.svg"
    :title "Gel electrophoresis"
    :description "PCR products from four samples, with a ladder."}
   {:src "images/colonies.svg"
    :title "Bacterial colonies"
    :description "Colonies growing on an agar plate."}
   {:src "images/cells.svg"
    :title "Microscopy"
    :description "Rod-shaped bacteria under the microscope."}])

(defn image-gallery
  "Show each image in `images` with its title and description."
  [images]
  [c/grid {:columns 3}
   (for [{:keys [src title description]} images]
     ^{:key src}
     [c/card {:title title}
      [c/image {:src src :alt title}]
      [c/paragraph description]])])


;; 4. Interactive Table ------------------------------------------------------

(def example-samples
  [{:sample-id "S001" :date "2026-10-01" :status "passed"}
   {:sample-id "S002" :date "2026-10-01" :status "failed"}
   {:sample-id "S003" :date "2026-10-02" :status "passed"}
   {:sample-id "S004" :date "2026-10-03" :status "pending"}])

(def status-colors
  {"passed" "#aceebb" "failed" "#ffcecb" "pending" "#fff8c5"})

(def cell-style {:padding "6px 12px" :border-bottom "1px solid #d0d7de" :text-align "left"})

(defn sample-table
  "Show `samples` as a table, one row per sample. Click a column heading to sort."
  [samples]
  (r/with-let [sort-key (r/atom :sample-id)]
    [:table {:style {:border-collapse "collapse"}}
     [:thead
      [:tr
       (for [[k label] [[:sample-id "Sample ID"] [:date "Date"] [:status "Status"]]]
         ^{:key k}
         [:th {:style (merge cell-style {:cursor "pointer"})
               :on-click (fn [] (reset! sort-key k))}
          label (when (= k @sort-key) " ▼")])]]
     [:tbody
      (map (fn [{:keys [sample-id date status]}]
             ^{:key sample-id}
             [:tr
              [:td {:style cell-style} sample-id]
              [:td {:style cell-style} date]
              [:td {:style cell-style}
               [c/tag {:color (get status-colors status)} status]]])
           (sort-by @sort-key samples))]]))
