(ns scratchpad.views.repl-guide
  "The REPL Guide page: how REPL-driven development works, habits that
  make it reliable, and what to do when it isn't. The hands-on drills
  live in src/main/scratchpad/repl_practice.cljs."
  (:require [scratchpad.components :as c]
            [scratchpad.repl-practice :as practice]
            [scratchpad.ui :as ui]))

(def ^:private practice-file "src/main/scratchpad/repl_practice.cljs")

;; ---------------------------------------------------------------------------
;; How the pieces fit together
;; ---------------------------------------------------------------------------

(defn- box [title & lines]
  [c/card {:style {:flex "1 1 180px" :text-align "center"}}
   [:div {:style {:font-weight "bold" :margin-bottom "6px"}} title]
   (into [:div {:style {:font-size "13px" :color "#57606a" :line-height "1.4"}}]
         (interpose [:br] lines))])

(defn- arrow [label]
  [:div {:style {:display "flex"
                 :flex-direction "column"
                 :align-items "center"
                 :justify-content "center"
                 :font-size "12px"
                 :color "#57606a"
                 :min-width "90px"}}
   [:div {:style {:font-size "22px" :color "#004a87"}} "⇄"]
   label])

(defn- diagram []
  [c/row {:gap "8px" :align "stretch"}
   [box "VS Code + Calva"
    "where you write code"
    "and press Alt+Enter"]
   [arrow "nREPL"]
   [box "shadow-cljs"
    "compiles ClojureScript"
    "into JavaScript"
    "(runs in the codespace)"]
   [arrow "websocket"]
   [box "Browser tab (port 8000)"
    "where your code"
    "actually runs"]])

(defn- how-it-fits []
  [:<>
   [diagram]
   [c/paragraph {:style {:margin-top "16px"}}
    "When you evaluate a form, Calva sends it to shadow-cljs, which compiles it "
    "to JavaScript and sends it to your browser tab to run. The result travels "
    "all the way back and appears next to your code. A few things follow from this:"]
   [c/bullet-list
    [:span [:strong "The browser tab is the REPL. "]
     "If the app isn't open in a tab, there's nowhere for your code to run "
     "(\"No available JS runtime\"). Keep exactly one tab of the app open - "
     "with several, it's hard to tell which one your code ran in."]
    [:span [:strong "The REPL only knows what it's been told. "]
     "Each file is a namespace, and you need to load it (Ctrl+Alt+C Enter) "
     "before you can evaluate the forms inside it."]
    [:span [:strong "Reloading the tab starts it from scratch. "]
     "Anything you defined at the REPL but didn't save is gone; anything you "
     "deleted from a file is finally gone too."]]])

;; ---------------------------------------------------------------------------
;; Two ways to see changes
;; ---------------------------------------------------------------------------

(def ^:private cell {:padding "8px 12px"
                     :border-bottom "1px solid #d0d7de"
                     :text-align "left"
                     :vertical-align "top"
                     :line-height "1.4"})

(defn- table [headers rows]
  [:div {:style {:overflow-x "auto"}}
   [:table {:style {:border-collapse "collapse" :width "100%" :background-color "white"}}
    [:thead
     (into [:tr] (for [h headers] ^{:key h} [:th {:style (merge cell {:background-color "#f6f8fa"})} h]))]
    [:tbody
     (for [[i row] (map-indexed vector rows)]
       ;; with-meta, because ^{:key ...} on an (into ...) wouldn't reach the vector
       (with-meta (into [:tr] (for [value row] [:td {:style cell} value]))
         {:key i}))]]])

(defn- two-loops []
  [:<>
   [table ["" "Save the file" "Evaluate a form"]
    [[[:strong "What happens"]
      "shadow-cljs reloads the whole file and redraws the page"
      "Just that one form runs, in the browser; the result appears inline"]
     [[:strong "Best for"]
      "Changing how the page looks: components, layout, styles"
      "Exploring data, trying out a function, poking at app state"]
     [[:strong "Watch out"]
      "Plain def's are re-run, so their values reset (use defonce for state)"
      "The page is not redrawn - save, or evaluate (scratchpad.main/render)"]]]
   [c/paragraph {:style {:margin-top "12px"}}
    "Experienced REPL users switch between these constantly: explore and "
    "build up a function at the REPL, then save to see it on the page."]])

;; ---------------------------------------------------------------------------
;; Habits
;; ---------------------------------------------------------------------------

(defn- habit-id [title]
  (ui/anchor-id "habit" title))

;; Each habit is [title & body]. Listed once here, used by both the page
;; and its table of contents.
(def ^:private habit-list
  [["1. Load the file first"
    "Every time you open or switch to a file, load it (Ctrl+Alt+C Enter). "
    "It's the fix for most \"No such namespace\" and \"undeclared Var\" errors."]
   ["2. Keep experiments in rich comment blocks"
    "Code inside " [:code "(comment ...)"] " never runs on its own, so it's a "
    "safe place to keep REPL experiments right next to the code they're about. "
    "Leave useful ones in - they're living documentation."]
   ["3. Evaluate small, from the inside out"
    "Don't write a big expression and hope. Evaluate the innermost part "
    "(Ctrl+Enter), check the result, then move out a layer. When something "
    "breaks, you'll know exactly which layer did it. (Drill 1)"]
   ["4. Look at your data before writing code for it"
    "Evaluate the data first and see its real shape. Most bugs are a wrong "
    "guess about the data (a string where you expected a number, a vector "
    "of maps where you expected a map...)."]
   ["5. Capture values from inside functions"
    "Put a temporary " [:code "(def x x)"] " inside a function to grab the "
    "values it was called with, then explore them at the REPL. (Drill 5)"]
   ["6. Use tap> for big data"
    [:code "(tap> data)"] " sends a value to the shadow-cljs Inspect page "
    "(port 9630), where you can click through nested data. (Drill 7)"]
   ["7. Keep state in defonce atoms"
    "State in a " [:code "defonce"] " atom survives saves, and you can read "
    "and change it from the REPL to try out different situations. (Drill 2)"]
   ["8. Reload the browser tab now and then"
    "The running page remembers everything you've ever evaluated, including "
    "functions you've since renamed or deleted. A reload makes sure your code "
    "works from a clean start. (Drill 4)"]])

(defn- habit [title & body]
  [ui/anchor (habit-id title)
   [:div {:style {:margin-bottom "16px"}}
    [:h3 {:style {:margin "0 0 4px 0" :font-size "17px"}} title]
    (into [:div {:style {:line-height "1.5"}}] body)]])

(defn- habits []
  (into [:<>]
        (for [[title & body] habit-list]
          ^{:key title} (into [habit title] body))))

;; ---------------------------------------------------------------------------
;; Troubleshooting
;; ---------------------------------------------------------------------------

(defn- troubleshooting []
  [:<>
   [table ["Symptom" "What's going on" "What to do"]
    [["\"No available JS runtime\""
      "No browser tab is connected to run your code"
      "Open the app (port 8000), or reload its tab"]
     ["\"No such namespace\" or \"Use of undeclared Var\""
      "The REPL hasn't loaded this file yet"
      "Load the file: Ctrl+Alt+C Enter"]
     ["I evaluated a defn but the page didn't change"
      "Evaluating doesn't redraw the page"
      [:span "Save the file, or evaluate " [:code "(scratchpad.main/render)"]]]
     ["The page stopped updating when I save"
      "Either the code doesn't compile, or the browser lost its connection"
      [:span "Look for a warning box in the browser, or errors in the terminal "
       "running shadow-cljs. Then reload the tab. In Codespaces, open port "
       "9630 from the Ports tab once, then reload."]]
     ["A function I deleted still works"
      "A \"ghost\" definition, still in the browser's memory"
      "Reload the browser tab"]
     ["My state reset when I saved"
      "It's in a def, which re-runs on every save"
      "Use defonce for state that should survive saves"]
     ["Evaluation never finishes, or the tab freezes"
      "Probably an infinite sequence like (range), or an endless loop"
      "Reload the tab. Use (take 10 ...) to look at long sequences"]
     ["Strange errors mentioning Java classes"
      "The code went to the Clojure (JVM) REPL, not the ClojureScript one"
      "Check the REPL indicator in Calva's status bar says cljs. .cljs files use the ClojureScript REPL automatically."]
     ["I changed deps.edn, package.json or shadow-cljs.edn and nothing happened"
      "Those are only read when the REPL starts"
      "Restart: close the REPL and jack in again"]]]
   [c/callout {:kind :info :style {:margin-top "16px"}}
    [:strong "When in doubt, climb the reset ladder. "]
    "Try each step until things work again: "
    "(1) reload the browser tab, "
    "(2) load your file again with Ctrl+Alt+C Enter, "
    "(3) restart the REPL (jack in again), "
    "(4) in Codespaces, rebuild the container. "
    "Most problems are fixed by step 1 or 2."]])

;; ---------------------------------------------------------------------------
;; Practice
;; ---------------------------------------------------------------------------

(defn- practice-panel []
  [c/card {:title "Practice panel"}
   [c/paragraph {:style {:margin-top 0 :color "#57606a"}}
    "Live output from " [:code practice-file] ". Keep this visible while you do the drills."]
   [c/column {:gap "8px"}
    [c/label-value "greeting" (practice/greeting)]
    [c/label-value "clicks" (:clicks @practice/practice-state)]
    [c/label-value "summarize" (practice/summarize practice/example-samples)]
    [:div
     [c/button {:on-click practice/say-hello!} "Say hello"]]]])

(defn- practice []
  [:<>
   [c/paragraph
    "Reading about the REPL only gets you so far - it's a skill, like touch "
    "typing. Open " [:code practice-file] ", load it, and work through the "
    "eight drills. Each one takes a few minutes and has a visible effect here:"]
   [ui/error-boundary [practice-panel]]
   [c/paragraph {:style {:margin-top "16px"}}
    "Two more things you might explore later, if you're curious:"]
   [c/bullet-list
    [:span [:strong "Structural editing (entirely optional). "]
     "Some people like editors that treat code as nested forms rather than "
     "text, with commands like \"Slurp\" and \"Barf\" that move brackets for "
     "you. Plenty of experienced Clojure programmers never use them, and "
     "just type their brackets. We've turned Calva's structural editing "
     "shortcuts off in this workshop so they don't surprise you; if you "
     "want to try them, see "
     [c/link {:href "https://calva.io/paredit/"} "Calva's paredit guide"] "."]
    [:span [:strong "Calva's own tutorial. "]
     "Search for \"Getting Started\" in the Command Palette to open an "
     "interactive REPL tutorial that runs right inside VS Code."]]])

(def ^:private sections
  [{:title "How the pieces fit together" :content how-it-fits}
   {:title "Two ways to see your changes" :content two-loops}
   {:title "Habits that make it work" :content habits :children habit-list}
   {:title "When things go wrong" :content troubleshooting}
   {:title "Practice makes fluent" :content practice}])

(defn- section-id [title]
  (ui/anchor-id "repl-guide" title))

(def ^:private toc-entries
  (for [{:keys [title children]} sections
        entry (cons {:id (section-id title) :label title :level 1}
                    (for [[habit-title] children]
                      {:id (habit-id habit-title) :label habit-title :level 2}))]
    entry))

(defn page []
  [c/page {:max-width "1200px"}
   [ui/with-table-of-contents {:top-id "repl-guide-top" :entries toc-entries}
    [c/heading {:level 1} "REPL Guide"]
    [c/callout
     "The REPL lets you talk to your running app while you build it. It's "
     "wonderful when it works - but it takes some understanding and some "
     "practice before it feels natural, and everyone gets stuck sometimes. "
     "This page explains how it fits together, the habits that help, and what "
     "to do when it misbehaves."]
    (for [{:keys [title content]} sections]
      ^{:key title}
      [ui/anchor (section-id title)
       [c/section {:title title}
        [content]]])]])
