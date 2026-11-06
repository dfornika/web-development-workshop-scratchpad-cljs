(ns scratchpad.ui
  "Building blocks for the site's own pages (nav, exercise panels, error
  handling). You don't need to edit this file - but you're welcome to read it!"
  (:require [reagent.core :as r]
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
  "Shows `code` and the result of calling `f`. Used by the `show-call` macro."
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

(defn nav-bar
  "The site navigation. `views` is a list of maps with :id and :label."
  [views current]
  [:nav {:style {:display "flex"
                 :gap "4px"
                 :padding "0 16px"
                 :background-color "#24292f"}}
   (for [{:keys [id label]} views]
     ^{:key id}
     [:a {:href (str "#/" (name id))
          :style {:padding "10px 12px"
                  :color "white"
                  :text-decoration "none"
                  :font-size "14px"
                  :border-bottom (if (= id current)
                                   "3px solid #54aeff"
                                   "3px solid transparent")}}
      label])])
