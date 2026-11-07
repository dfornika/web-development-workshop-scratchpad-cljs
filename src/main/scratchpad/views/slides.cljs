(ns scratchpad.views.slides
  "A simple slide viewer: one slide at a time, with Previous / Next buttons,
  the arrow keys, and a fullscreen button.

  A slide is a map: {:title \"...\" :body [...hiccup...]}. Text on a slide
  is sized relative to the slide's width (with container query units, cqw),
  so it scales up nicely in fullscreen."
  (:require [reagent.core :as r]
            [scratchpad.components :as c]
            [scratchpad.web.editor :as editor]))

;; Which slide each deck is on, by deck id. defonce, so it survives hot
;; reloads and switching pages.
(defonce ^:private positions (r/atom {}))

(defn code
  "Syntax-colored code for a slide. `language` is :clojure (default) or :html."
  ([text] (code {} text))
  ([{:keys [language] :or {language :clojure}} text]
   ^{:key text}
   [editor/editor {:initial-doc text
                   :read-only? true
                   :line-numbers? false
                   :language language
                   :font-size "1.45cqw"
                   :style {:margin "0.6cqw 0"}}]))

(defn two-columns
  "Lays out `children` side by side on a slide."
  [& children]
  (into [:div {:style {:display "grid"
                       :grid-template-columns (str "repeat(" (count children) ", minmax(0, 1fr))")
                       :gap "2.5cqw"}}]
        children))

(defn- editing?
  "true if the keyboard is being used to type somewhere (so the arrow keys
  shouldn't change slides)."
  [^js target]
  (or (#{"INPUT" "TEXTAREA" "SELECT"} (.-tagName target))
      (.-isContentEditable target)))

(defn- nav-button [opts label]
  [c/button (merge {:variant :secondary :style {:padding "4px 12px" :font-size "14px"}} opts)
   label])

(defn viewer
  "Shows the slides in `slides`. `id` identifies the deck, to remember which
  slide it's on."
  [id slides]
  (r/with-let [slide-box (atom nil)
               go! (fn [n] (swap! positions assoc id (max 0 (min (dec (count slides)) n))))
               on-key (fn [^js e]
                        (when-not (editing? (.-target e))
                          (let [i (get @positions id 0)]
                            (case (.-key e)
                              ("ArrowRight" "PageDown") (do (.preventDefault e) (go! (inc i)))
                              ("ArrowLeft" "PageUp") (do (.preventDefault e) (go! (dec i)))
                              "Home" (go! 0)
                              "End" (go! (dec (count slides)))
                              nil))))
               _ (.addEventListener js/window "keydown" on-key)]
    (let [i (min (get @positions id 0) (dec (count slides)))
          {:keys [title body]} (nth slides i)]
      [:div {:style {:max-width "1100px" :margin "0 auto"}}
       ;; the slide
       [:div {:ref #(reset! slide-box %)
              :data-component "slide"
              :style {:container-type "inline-size"
                      :aspect-ratio "16 / 9"
                      :box-sizing "border-box"
                      :overflow "auto"
                      :padding "3.5cqw 4.5cqw"
                      :background-color "white"
                      :border "1px solid #d0d7de"
                      :border-radius "10px"
                      :box-shadow "0 2px 8px rgba(0,0,0,0.06)"}}
        (when title
          [:h2 {:style {:margin "0 0 2cqw 0"
                        :font-size "3.4cqw"
                        :color "#004a87"
                        :border-bottom "0.25cqw solid #004a87"
                        :padding-bottom "0.8cqw"}}
           title])
        (into [:div {:style {:font-size "2cqw" :line-height "1.5"}}] body)]
       ;; the controls
       [c/row {:align "center" :style {:justify-content "space-between" :margin-top "10px"}}
        [nav-button {:on-click #(go! (dec i)) :disabled? (zero? i)} "← Previous"]
        [:span {:style {:color "#57606a" :font-size "14px"}}
         (str "Slide " (inc i) " of " (count slides)) " · use ← → to move"]
        [c/row {:gap "6px"}
         [nav-button {:on-click #(some-> ^js @slide-box .requestFullscreen)} "Fullscreen"]
         [nav-button {:variant :primary
                      :on-click #(go! (inc i))
                      :disabled? (= i (dec (count slides)))}
          "Next →"]]]])
    (finally
      (.removeEventListener js/window "keydown" on-key))))
