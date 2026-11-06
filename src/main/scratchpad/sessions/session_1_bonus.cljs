(ns scratchpad.sessions.session-1-bonus
  "Session 1 bonus homework: general ClojureScript challenges.

  Same as session_1.cljs: write the functions marked TODO, try things out in
  the (comment ...) blocks, and watch the Session 1 page update as you save.

  Remember to load this file into the REPL first (Ctrl+Alt+C Enter)."
  (:require [reagent.core :as r]
            [scratchpad.components :as c]))


;; ===========================================================================
;; 1. Click Counter
;;
;; Replace the alert button with a dynamic counter that updates on click.
;; ===========================================================================

(defn click-counter
  "A button that counts how many times it's been clicked."
  []
  ;; TODO: keep a count in an atom and show it. Look at `gc-calculator` in
  ;; session_1.cljs for how to use r/with-let and r/atom.
  ;; Hint: (swap! my-atom inc) adds one to the number in an atom.
  [c/button {:on-click (fn [] (js/alert "You clicked me!"))}
   "Click Me"])

(comment
  (def clicks (r/atom 0))
  @clicks
  (swap! clicks inc)
  @clicks
  (reset! clicks 0)
  )


;; ===========================================================================
;; 2. Theme Switcher
;;
;; Add a light/dark mode toggle by changing background and font styles
;; using a button.
;; ===========================================================================

(def themes
  {:light {:background-color "white" :color "#24292f"}
   :dark  {:background-color "#24292f" :color "white"}})

(defn theme-switcher
  "A box with a button that switches it between light and dark mode."
  []
  ;; TODO: keep the current theme (:light or :dark) in an atom, use it to
  ;; pick the :style below, and add a button that switches it.
  [:div {:style (merge {:padding "16px" :border-radius "8px"}
                       (get themes :light))}
   [c/paragraph "This box should switch between light and dark."]])

(comment
  (get themes :dark)
  (merge {:padding "16px"} (get themes :dark))

  ;; One way to flip between two values:
  (if (= :light :light) :dark :light)
  )


;; ===========================================================================
;; 3. Image Gallery
;;
;; Display multiple images (e.g. microscope images, DNA gels) with titles
;; and descriptions.
;; ===========================================================================

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
  ;; TODO: Try c/grid, c/card and c/image.
  [c/todo "image-gallery"])

(comment
  (first example-images)
  (map :title example-images)
  (count example-images)
  )


;; ===========================================================================
;; 4. Interactive Table
;;
;; Use `map` to generate a table from a vector of sample metadata like
;; sample ID, date, and status.
;; ===========================================================================

(def example-samples
  [{:sample-id "S001" :date "2026-10-01" :status "passed"}
   {:sample-id "S002" :date "2026-10-01" :status "failed"}
   {:sample-id "S003" :date "2026-10-02" :status "passed"}
   {:sample-id "S004" :date "2026-10-03" :status "pending"}])

(defn sample-table
  "Show `samples` as a table, one row per sample."
  [samples]
  ;; TODO: build a [:table ...]. An HTML table looks like this in hiccup:
  ;;   [:table
  ;;    [:thead [:tr [:th "Sample ID"] [:th "Date"]]]
  ;;    [:tbody
  ;;     [:tr [:td "S001"] [:td "2026-10-01"]]]]
  ;; Use `map` (or `for`) to make one [:tr ...] per sample - and give each
  ;; row a ^{:key ...}.
  ;; Stretch goals: color the status column, or add a button that sorts
  ;; the rows (sort-by might help).
  [c/todo "sample-table"])

(comment
  (map :sample-id example-samples)
  (map (fn [sample] [:tr [:td (:sample-id sample)]]) example-samples)
  (sort-by :status example-samples)
  (filter #(= "passed" (:status %)) example-samples)
  )
