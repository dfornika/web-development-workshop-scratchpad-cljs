(ns scratchpad.playground
  "Your playground: a page to make your own.

  Change anything you like and save the file - the page in your browser
  updates straight away, without a reload.

  Looking for ideas? The Components page shows ready-made pieces you can
  copy in here, like [c/card ...] or [c/button ...]."
  (:require [cljs.pprint :as pp]
            [scratchpad.components :as c]
            [scratchpad.state :refer [db]]))

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
   ;; The header, from the component library. Try changing the options,
   ;; or add a :style map, e.g. :style {:background-color "#f0f8ff"}.
   ;; Other logos to try (all in resources/public/images): logo-flask.svg,
   ;; logo-microscope.svg, logo-molecule.svg, logo-bacterium.svg
   [c/header {:title "Lab Website"
              :title-color "#004a87"
              :logo-src "images/logo-dna.svg"
              :height-px 48}]

   ;; A message you can set from the REPL - see the (comment ...) below.
   (when-let [message (:message @db)]
     [c/callout {:kind :success} message])

   ;; The rest is components from the library (c/...), plus the
   ;; welcome-banner defined above. See the Components page for more.
   [c/page
    [welcome-banner
     "Welcome to your playground!"
     "Everything on this page comes from src/main/scratchpad/playground.cljs. Change something, save, and watch it update."]

    ;; Two columns, side by side
    [c/grid {:columns 2}
     [c/card {:title "About our lab"}
      [c/paragraph
       "We're a public health genomics lab. We sequence bacterial and viral "
       "samples to help track outbreaks, monitor antimicrobial resistance, "
       "and support our partners in the field."]
      [c/paragraph
       "This site shares our latest results, the tools we use, and how to "
       "get in touch with the team."]]
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

    [c/footer
     "Made with "
     [c/link {:href "https://clojurescript.org/"} "ClojureScript"]
     " and "
     [c/link {:href "https://reagent-project.github.io/"} "Reagent"]]]])


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
  (welcome-banner "Hi" "There")
  (c/header {:title "Lab Website"})
  ;; Components inside the result show up as #object[...]. c/hiccup shows
  ;; their names, and c/expand turns them into plain HTML elements:
  (c/hiccup (welcome-banner "Hi" "There"))
  (c/expand [c/header {:title "Lab Website"}])

  ;; ...and see the HTML it turns into. print-html shows it in Calva's REPL
  ;; output window; html returns it as a string.
  (c/print-html [c/header {:title "Lab Website"}])
  (c/print-html [c/card {:title "KPC-3"} [c/paragraph "A carbapenemase gene."]])
  (c/html [:p "Hello"])
  ;; Notice :on-click doesn't appear in the HTML: React attaches event
  ;; handlers with JavaScript instead.
  (c/print-html [c/button {:on-click (fn [] (js/alert "Hi"))} "Click"])

  ;; Pretty-printing: pp/pprint spreads long data over several lines, and
  ;; pp/print-table shows a list of maps as a table. Both print (and
  ;; return nil); c/pprint-str and c/table-str give you the text instead.
  (def samples [{:id "S001" :reads 1200 :qc "pass"}
                {:id "S002" :reads 3400 :qc "fail"}])
  (pp/pprint {:run "RUN_001" :samples samples})
  (pp/print-table samples)
  (c/table-str [:id :qc] samples)

  ;; tap> sends a value to the tap panel (the "tap>" button at the bottom
  ;; right of the page), where you can unfold nested data
  (tap> {:run "RUN_001" :samples samples})
  ;; c/tap-> taps a value and passes it on, for the middle of a pipeline
  (->> samples
       (filter #(= "pass" (:qc %)))
       c/tap->
       (map :reads))

  ;; Components need saving to show up on the page. If you've re-defined one
  ;; at the REPL instead, this redraws the page:
  (scratchpad.main/render)
  )
