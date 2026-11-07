(ns scratchpad.repl-practice
  "REPL practice drills. Work through the (comment ...) blocks below, one
  form at a time. The \"Practice panel\" on the REPL Guide page shows the
  effects live, so keep it open in your browser while you go.

  Before you start, load this file into the REPL:
    Command Palette (Ctrl+Shift+P) ->
    \"Calva: Load/Evaluate Current File and its Requires/Dependencies\"
    (shortcut: Ctrl+Alt+C Enter)

  Then put your cursor on a form inside a (comment ...) block and press
  Alt+Enter to evaluate it."
  (:require [clojure.string :as str]
            [reagent.core :as r]))

;; State shown in the practice panel. `defonce` means saving this file
;; doesn't reset it - try changing it from the REPL in drill 2.
(defonce practice-state
  (r/atom {:clicks 0
           :message "Hello!"}))

(def example-samples
  [{:sample-id "S001" :coverage 87.4 :status "passed"}
   {:sample-id "S002" :coverage 12.1 :status "failed"}
   {:sample-id "S003" :coverage 64.0 :status "passed"}])

(defn greeting
  "The greeting shown in the practice panel."
  []
  (str "👋 " (:message @practice-state)))

(defn summarize
  "A short summary of some samples, shown in the practice panel."
  [samples]
  ;; Drill 5: uncomment the next line, save, then evaluate `samples` at the REPL
  ;; (def samples samples)
  (let [passed (filter #(= "passed" (:status %)) samples)]
    (str (count passed) " of " (count samples) " samples passed")))

(defn say-hello!
  "Called when you click the button in the practice panel."
  []
  (swap! practice-state update :clicks inc)
  (println "Hello from a button click! (look - I'm in the browser console)"))


;; ---------------------------------------------------------------------------
;; Drill 1: Evaluate inside out
;;
;; Put your cursor on the innermost form and press Ctrl+Enter (evaluate
;; current form). Then move outwards, one layer at a time, and evaluate
;; again. Watch the result change at each step. This is how you build up
;; (and debug) bigger expressions.
;; ---------------------------------------------------------------------------
(comment
  (str/join ", "
            (map str/upper-case
                 (filter #(> (count %) 3)
                         ["kpc" "mecA" "vanA" "ndm" "oxa48"]))))


;; ---------------------------------------------------------------------------
;; Drill 2: Change the running app from the REPL
;;
;; Evaluate these one at a time and watch the practice panel. Nothing
;; needs saving: the page reacts because practice-state is a Reagent atom.
;; ---------------------------------------------------------------------------
(comment
  @practice-state
  (swap! practice-state assoc :message "I changed this from the REPL")
  (swap! practice-state update :clicks + 10)
  (reset! practice-state {:clicks 0 :message "Hello!"})
  )


;; ---------------------------------------------------------------------------
;; Drill 3: Evaluating a function doesn't redraw the page
;;
;; 1. Change the "👋 " in `greeting` above to "🧬 ", then put your cursor
;;    on the defn and press Alt+Enter. The function is updated...
;; 2. ...but the panel doesn't change. Evaluating code doesn't redraw.
;; 3. Evaluate this to redraw by hand:
;; ---------------------------------------------------------------------------
(comment
  (greeting) ; the function itself has changed - see?
  (scratchpad.main/render)
  ;; 4. Or just save the file: hot reload redraws for you.
  )


;; ---------------------------------------------------------------------------
;; Drill 4: Ghost definitions
;;
;; The running page remembers every definition you've evaluated - even
;; after you delete it from the file.
;; ---------------------------------------------------------------------------
(comment
  ;; 1. Evaluate this defn (Alt+Enter), then call it:
  (defn temporary-helper [] "I exist!")
  (temporary-helper)
  ;; 2. Now delete the defn above, and save the file.
  ;; 3. Evaluate this again. It still works! The old definition lives on
  ;;    in the browser.
  (temporary-helper)
  ;; 4. Reload the browser tab (F5), load this file again (Ctrl+Alt+C Enter),
  ;;    and try once more. Now it's gone.
  ;; Moral: if something only works until you reload, a ghost is
  ;; probably involved. Reload now and then to check your code still
  ;; works from a clean start.
  )


;; ---------------------------------------------------------------------------
;; Drill 5: Capture values from inside a function ("inline def")
;;
;; When a function misbehaves, you want to see the exact values it was
;; called with. A temporary `def` inside the function grabs them.
;; ---------------------------------------------------------------------------
(comment
  ;; 1. Uncomment the (def samples samples) line in `summarize`, and save.
  ;;    The practice panel calls summarize, so `samples` now holds
  ;;    whatever the panel passed in:
  samples
  (count samples)
  (map :coverage samples)
  ;; 2. Now you can try fixes against the real data, right here:
  (filter #(> (:coverage %) 50) samples)
  ;; 3. Remember to delete the def again when you're done!
  )


;; ---------------------------------------------------------------------------
;; Drill 6: Where does output go?
;; ---------------------------------------------------------------------------
(comment
  ;; Printed during a REPL evaluation -> shows up in Calva's output:
  (println "Hello from the REPL!")
  ;; Printed by code the *browser* runs (a click, a render) -> browser
  ;; console. Click "Say hello" in the practice panel, then open the
  ;; browser console (F12) to find its message.
  ;; js/console.log always goes to the browser console:
  (js/console.log "Hello, console!")
  )


;; ---------------------------------------------------------------------------
;; Drill 7: Explore big data with tap> and the inspector
;;
;; Big nested data is hard to read inline. `tap>` sends a value to the
;; shadow-cljs Inspect page, where you can click through it.
;; ---------------------------------------------------------------------------
(comment
  (tap> example-samples)
  (tap> @practice-state)
  ;; Open the shadow-cljs UI (port 9630 - in Codespaces, use the Ports
  ;; tab) and click "Inspect" at the top to see them.
  )


;; ---------------------------------------------------------------------------
;; Drill 8: Careful with infinite sequences
;; ---------------------------------------------------------------------------
(comment
  (range 10)
  (take 5 (range))  ; fine: only takes 5
  (take 5 (cycle ["A" "T" "G" "C"]))
  ;; Evaluating (range) on its own would try to print an infinite list and
  ;; freeze the browser tab. If that ever happens, reload the tab.
  )
