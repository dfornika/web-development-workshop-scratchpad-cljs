(ns scratchpad.ui
  "Building blocks for the site's own pages (nav, exercise panels, error
  handling). You don't need to edit this file - but you're welcome to read it!"
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [scratchpad.components :as c]
            [scratchpad.state :refer [reload-count]]))

(defn- error-box [error]
  [c/callout {:kind :error}
   [:strong "Something went wrong while drawing this part of the page:"]
   [:pre {:style {:white-space "pre-wrap" :margin "8px 0"}}
    (str (or (ex-message error) error))]
   [:small "Fix the code and save the file to try again. "
    "The browser console (F12) has more details."]])

(defn error-boundary
  "Renders its children, but if they throw an error while rendering, shows
  the error instead of breaking the whole page. Retries after the next hot
  reload, so fixing the code and saving clears the error."
  [& _children]
  (r/create-class
   {:display-name "ErrorBoundary"
    ;; React calls this when a child throws, and immediately re-renders
    ;; with the state it returns - so the error has to be recorded here.
    :get-derived-state-from-error (fn [error]
                                    #js {:error error
                                         :reload @reload-count})
    :reagent-render
    (fn [& children]
      (let [state (.-state (r/current-component))]
        (if (and state
                 (.-error state)
                 ;; deref'ing reload-count here also re-renders us after
                 ;; every hot reload, which is when we retry the children
                 (= (.-reload state) @reload-count))
          [error-box (.-error state)]
          (into [:<>] children))))}))

(defn call-result
  "Shows `code` and the result of calling `f`. `f` is called while drawing,
  so if it throws, the surrounding error boundary catches it."
  [code f]
  (let [result (f)]
    [c/code-block
     code "\n"
     [:span {:style {:color "#57606a"}} ";; => " (pr-str result)]]))

(defn exercise
  "A panel for one exercise: title, the task, where to edit it, and then
  the live result of the participant's code."
  [{:keys [title task edit]} & children]
  [c/card {:style {:margin-bottom "16px"}}
   [:h3 {:style {:margin "0 0 4px 0"}} title]
   [:p {:style {:margin "0 0 8px 0" :line-height "1.5"}} task]
   [:p {:style {:margin "0 0 12px 0" :font-size "13px" :color "#57606a"}}
    "✏️ Edit " [:code edit]]
   (into [error-boundary] children)])

(def nav-height-px
  "Height of the nav bar. Things that stick to the top of the page, or get
  scrolled into view, leave this much room so the nav doesn't cover them."
  40)

(defn nav-bar
  "The site navigation. `views` is a list of maps with :id and :label.
  It stays at the top of the window as you scroll (position: sticky)."
  [views current]
  [:nav {:style {:position "sticky"
                 :top 0
                 ;; above everything else, including code editors
                 :z-index 1000
                 :height (str nav-height-px "px")
                 :box-sizing "border-box"
                 :display "flex"
                 :gap "4px"
                 :padding "0 16px"
                 :background-color "#24292f"}}
   (for [{:keys [id label]} views]
     ^{:key id}
     [:a {:href (str "#/" (name id))
          :style {:display "flex"
                  :align-items "center"
                  :padding "0 12px"
                  :color "white"
                  :text-decoration "none"
                  :font-size "14px"
                  :border-bottom (if (= id current)
                                   "3px solid #54aeff"
                                   "3px solid transparent")}}
      label])])

;; ---------------------------------------------------------------------------
;; A floating table of contents
;; ---------------------------------------------------------------------------

(defn anchor-id
  "A DOM id made from some text, e.g. (anchor-id \"repl\" \"Two ways\")
  gives \"repl-two-ways\"."
  [prefix text]
  (str prefix "-" (str/replace (str/lower-case text) #"[^a-z0-9]+" "-")))

(defn- scroll-to!
  "Smoothly scroll the element with `id` into view. We can't use normal
  #anchor links here, because the part of the URL after # is already used
  to pick the page (e.g. #/components)."
  [id]
  (some-> (.getElementById js/document id)
          (.scrollIntoView #js {:behavior "smooth" :block "start"})))

(defn anchor
  "Wraps `children` in a div with DOM id `id`, so the table of contents
  can scroll to it. Leaves room for the nav bar above it."
  [id & children]
  (into [:div {:id id
               :style {:scroll-margin-top (str (+ nav-height-px 16) "px")}}]
        children))

(defn- toc-link [id text style]
  [:a {:href "#"
       :on-click (fn [event]
                   (.preventDefault event)
                   (scroll-to! id))
       :style (merge {:display "block"
                      :padding "2px 0"
                      :color "#0969da"
                      :text-decoration "none"}
                     style)}
   text])

(defn- table-of-contents [top-id entries]
  [:nav {:style {:position "sticky"
                 :top (str (+ nav-height-px 16) "px")
                 :align-self "start"
                 :max-height (str "calc(100vh - " (+ nav-height-px 32) "px)")
                 :overflow-y "auto"
                 :font-size "14px"
                 :padding-right "8px"}}
   [toc-link top-id "↑ Top" {:color "#57606a" :margin-bottom "8px"}]
   (for [{:keys [id label level]} entries]
     ;; with-meta, because ^{:key ...} on an (if ...) wouldn't reach the vector
     (with-meta
       (if (= level 2)
         [toc-link id label {:padding-left "8px"}]
         [toc-link id label {:font-weight "bold"
                             :color "#24292f"
                             :margin-top "10px"}])
       {:key id}))])

(defn with-table-of-contents
  "A page layout with a table of contents that floats down the left side.

  `top-id` is the DOM id of the top of the page. `entries` is a list of
  {:id ... :label ... :level 1-or-2}, where each :id belongs to an
  element on the page (see `anchor`)."
  [{:keys [top-id entries]} & children]
  [:div {:style {:display "grid"
                 :grid-template-columns "170px minmax(0, 1fr)"
                 :gap "32px"}}
   [table-of-contents top-id entries]
   (into [:div {:id top-id
                :style {:scroll-margin-top (str (+ nav-height-px 16) "px")}}]
         children)])
