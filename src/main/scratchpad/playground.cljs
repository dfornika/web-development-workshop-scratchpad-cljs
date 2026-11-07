(ns scratchpad.playground
  "Your playground: a page to make your own.

  Change anything you like and save the file - the page in your browser
  updates straight away, without a reload.

  Looking for ideas? The Components page shows ready-made pieces you can
  copy in here, like [c/card ...] or [c/button ...]."
  (:require [scratchpad.components :as c]
            [scratchpad.state :refer [db]]))

(defn header
  "The header at the top of the page."
  []
  [:header {:style {:display "flex"
                    :justify-content "space-between"
                    :align-items "center"
                    :padding "8px 16px"
                    :height "64px"}}
   [:h1 {:style {:font-family "Arial"
                 :color "#004a87"
                 :margin "0px"}}
    "Header"]
   [:img {:src "images/logo.svg" :height "48px"}]])

(defn welcome-banner
  "A component made from scratch: plain hiccup tags ([:div ...], [:h2 ...])
  styled with a :style map - no component library needed. Everything in
  the component library is built exactly like this.

  Try changing the colors, the border, or the rounded corners (border-radius)."
  [title text]
  [:div {:style {:background-color "#e6f0fa"
                 :border "2px solid #004a87"
                 :border-radius "12px"
                 :padding "20px 24px"
                 :margin-bottom "24px"}}
   [:h2 {:style {:margin "0 0 8px 0" :color "#004a87"}}
    title]
   [:p {:style {:margin "0" :line-height "1.5"}}
    text]])

(defn page
  "The whole page. Add things inside the [:div ...] to see them appear."
  []
  [:div
   [header]

   ;; A message you can set from the REPL - see the (comment ...) below.
   (when-let [message (:message @db)]
     [c/callout {:kind :success} message])

   ;; From here on, it's components from the library (c/...), plus the
   ;; welcome-banner defined above. See the Components page for more.
   [c/page
    [welcome-banner
     "Welcome to your playground!"
     "Everything on this page comes from src/main/scratchpad/playground.cljs. Change something, save, and watch it update."]

    ;; Two columns, side by side
    [c/grid {:columns 2}
     [c/card {:title "About our lab"}
      [c/paragraph
       "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod "
       "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim "
       "veniam, quis nostrud exercitation ullamco laboris."]
      [c/paragraph
       "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum "
       "dolore eu fugiat nulla pariatur."]]
     [c/card {:title "Latest gel"
              :style {:background-color "#fdf6e3"}}
      [c/image {:src "images/gel.svg"
                :caption "PCR products from four samples"}]]]

    [c/section {:title "This week in numbers"}
     [c/row
      [c/stat {:label "Samples" :value 96}]
      [c/stat {:label "Passed QC" :value 91}]
      [c/stat {:label "Mean coverage" :value 87.4 :unit "x"}]]]

    [c/section {:title "Ideas to try"}
     [c/bullet-list
      "Change the text in the welcome banner"
      "Give the \"About our lab\" card a background color, like the gel card"
      "Change [c/grid {:columns 2} ...] to 3 columns, and add another card"
      "Add a [c/button ...] - copy one from the Components page"]]

    [c/footer "Made with ClojureScript and Reagent"]]])


;; A "rich comment" block. Nothing in here runs when the file loads - it's a
;; place to keep code you want to try at the REPL, one form at a time.
;;
;; First, load this file into the REPL:
;;   Command Palette (Ctrl+Shift+P) -> "Calva: Load/Evaluate Current File and its Requires/Dependencies"
;;   (shortcut: Ctrl+Alt+C Enter)
;; Then put your cursor inside a form below and press Alt+Enter to evaluate it.
;; The result appears inline, right next to the code.
(comment
  ;; Plain values
  (+ 1 2 3)
  (str "Hello, " "workshop" "!")
  {:gene-name "KPC-3" :start 21331 :end 22598}

  ;; Talk to the browser
  (js/alert "Hello from the REPL!")
  (js/console.log "Look in the browser console (F12)")
  (set! (.-title js/document) "My page")

  ;; Change the app's state - watch the page update without saving anything
  (swap! db assoc :message "Hello from the REPL! 👋")
  (swap! db dissoc :message)
  @db

  ;; Hiccup is just data: evaluate a component and look at what it returns
  (header)

  ;; ...and see the HTML it turns into. print-html shows it in Calva's REPL
  ;; output window; html returns it as a string.
  (c/print-html [header])
  (c/print-html [c/card {:title "KPC-3"} [c/paragraph "A carbapenemase gene."]])
  (c/html [:p "Hello"])
  ;; Notice :on-click doesn't appear in the HTML: React attaches event
  ;; handlers with JavaScript instead.
  (c/print-html [c/button {:on-click (fn [] (js/alert "Hi"))} "Click"])

  ;; Components need saving to show up on the page. If you've re-defined one
  ;; at the REPL instead, this redraws the page:
  (scratchpad.main/render)
  )
