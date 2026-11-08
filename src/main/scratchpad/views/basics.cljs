(ns scratchpad.views.basics
  "The ClojureScript Basics page: a beginner-friendly tour of the language
  and its everyday idioms.

  Every example can be edited and run on the page. They're run by SCI, in
  a namespace that has the page's example data (`sample` and `samples`)."
  (:require [reagent.core :as r]
            [scratchpad.components :as c]
            [scratchpad.ui :as ui]
            [scratchpad.views.reference :as reference :refer [mono snippet table]]
            [scratchpad.web.sci :as sci]))

;; The data the examples use. It's shown on the page, and run to set up the
;; namespace the examples run in - so what you see is exactly what they use.
(def ^:private samples-source
  (str "(def sample\n"
       "  {:id \"S001\" :lineage \"JN.1\" :status \"PASS\" :reads 1200})\n\n"
       "(def samples\n"
       "  [{:id \"S001\" :lineage \"JN.1\" :status \"PASS\" :reads 1200}\n"
       "   {:id \"S002\" :lineage \"KP.2\" :status \"FAIL\" :reads 300}\n"
       "   {:id \"S003\" :lineage \"JN.1\" :status \"PASS\" :reads 950}\n"
       "   {:id \"S004\" :lineage \"XBB.1.5\" :status \"WARN\" :reads 610}])"))

;; Where the examples run: made the first time one is drawn.
(defonce ^:private examples-ns
  (delay
    (let [ns (sci/create-ns! (str "(ns scratchpad.basics-examples\n"
                                  "  (:require [clojure.string :as str]\n"
                                  "            [reagent.core :as r]))"))]
      (sci/eval-code samples-source ns)
      ns)))

(defn- example
  "An example you can edit and run."
  [code]
  [reference/example examples-ns code])

;; ---------------------------------------------------------------------------
;; Sections
;; ---------------------------------------------------------------------------

(defn- reading-code []
  [:<>
   [c/paragraph
    "ClojureScript code is made of " [:em "forms"] " in round brackets. The "
    "first thing inside the brackets is the function (or operation), and the "
    "rest are its arguments. So where JavaScript writes " [:code "max(3, 8, 5)"]
    ", ClojureScript writes:"]
   [example "(max 3 8 5)"]
   [c/paragraph
    "Even arithmetic works this way, which means " [:code "+"] " can take "
    "any number of arguments. Forms nest, and the innermost ones run first:"]
   [example "(+ 1 2 3)"]
   [example "(* 2 (+ 1 2))"]
   [example "(str \"GC content: \" 52 \"%\")"]
   [c/paragraph
    "Every form gives back a value - there are no \"statements\". Commas are "
    "optional (they count as spaces), and there are three ways to leave notes "
    "or switch code off:"]
   [snippet (str "; a comment, to the end of the line\n"
                 "#_(this form is skipped)\n"
                 "(comment\n"
                 "  ;; nothing in here runs when the file loads - but you can\n"
                 "  ;; evaluate each form with Alt+Enter. Great for experiments.\n"
                 "  (max 3 8 5))")]])

(defn- data []
  [:<>
   [c/paragraph "The simple values:"]
   [table ["Kind" "Looks like" "Notes"]
    [["Number" [mono "42  3.14  -1"] "Whole numbers and decimals are both just numbers"]
     ["String" [mono "\"GATTACA\""] "Always double quotes"]
     ["Keyword" [mono ":status"] "A name, mostly used as map keys"]
     ["Boolean" [mono "true  false"] ""]
     ["Nothing" [mono "nil"] [:span "Like JavaScript's " [:code "null"]]]]]
   [c/paragraph {:style {:margin-top "16px"}} "And the collections:"]
   [table ["Kind" "Looks like" "Use it for"]
    [["Vector" [mono "[\"S001\" \"S002\" \"S003\"]"] "A list of things, in order"]
     ["Map" [mono "{:id \"S001\" :status \"PASS\"}"] "Named values: keys and values"]
     ["Set" [mono "#{\"mecA\" \"blaKPC\"}"] "A group of unique things - good for \"is it in here?\""]
     ["List" [mono "'(1 2 3)"] "Code is made of lists; you'll rarely make your own"]]]
   [c/paragraph {:style {:margin-top "16px"}}
    "Collections can hold anything, including other collections. Keywords "
    "and sets can also be used as functions, to look something up:"]
   [example "(count [\"S001\" \"S002\" \"S003\"])"]
   [example "(:status {:id \"S001\" :status \"PASS\"})"]
   [example "(contains? #{\"mecA\" \"blaKPC\"} \"mecA\")"]
   [example "(#{\"mecA\" \"blaKPC\"} \"dnaA\")"]
   [c/callout {:kind :info}
    [:strong "True and false. "]
    "Only " [:code "false"] " and " [:code "nil"] " count as false. Everything "
    "else - including " [:code "0"] " and the empty string " [:code "\"\""]
    " - counts as true. So " [:code "(if 0 \"yes\" \"no\")"] " gives "
    [:code (pr-str (if 0 "yes" "no"))] "."]
   [c/paragraph {:style {:margin-top "16px"}}
    "The examples on the rest of this page use this data - one sample, and "
    "a vector of samples:"]
   [snippet samples-source]])

(defn- naming []
  [:<>
   [c/paragraph
    [:code "def"] " gives a value a name, for the whole file. " [:code "defn"]
    " defines a function: its name, an optional description, its arguments "
    "in square brackets, and its body. The last form in the body is what it "
    "gives back - there's no " [:code "return"] "."]
   [snippet (str "(def resistance-genes #{\"mecA\" \"blaKPC\" \"vanA\"})\n\n"
                 "(defn resistance-gene?\n"
                 "  \"true if `gene` is a known resistance gene.\"\n"
                 "  [gene]\n"
                 "  (contains? resistance-genes gene))")]
   [c/paragraph
    [:code "let"] " gives names to values for a moment, inside a function. "
    "Each name can use the ones before it, so it's a good way to break a "
    "calculation into steps:"]
   [example (str "(let [bases \"ATGCGC\"\n"
                 "      gc (count (filter #{\"G\" \"C\"} bases))]\n"
                 "  (/ gc (count bases)))")]
   [c/paragraph
    "Names can use dashes, and end in " [:code "?"] " or " [:code "!"] ". By "
    "convention, " [:code "?"] " means \"answers yes or no\" and " [:code "!"]
    " means \"changes something\"."]])

(defn- functions []
  [:<>
   [c/paragraph
    [:code "fn"] " makes a function without a name, for when you need one "
    "just once - often to hand to another function, like " [:code "map"] ":"]
   [example "(map (fn [n] (* n 2)) [1 2 3])"]
   [c/paragraph
    "There's an even shorter way to write small ones: " [:code "#( )"]
    ", where " [:code "%"] " is the argument (or " [:code "%1"] ", "
    [:code "%2"] "... if there are several):"]
   [example "(map #(* % 2) [1 2 3])"]
   [example "(filter #(> (:reads %) 900) samples)"]
   [c/paragraph
    "Functions are values like any other: you can store them, pass them "
    "around, and give them as arguments. Event handlers in Reagent are "
    "exactly this: " [:code "{:on-click #(swap! counter inc)}"] "."]])

(defn- immutable []
  [:<>
   [c/paragraph
    "ClojureScript's data never changes. Functions like " [:code "assoc"]
    " don't change a map - they give you back a " [:em "new"] " map, and "
    "the original stays as it was:"]
   [example (str "(let [updated (assoc sample :status \"FAIL\")]\n"
                 "  [(:status sample) (:status updated)])")]
   [c/paragraph
    "This sounds limiting, but it means nothing can change your data behind "
    "your back, and it's cheap: the new map shares everything it can with "
    "the old one. When you do need something that changes over time - like "
    "the state of a page - you use an atom (see "
    [:strong "State: atoms"] " below)."]])

(defn- maps []
  [:<>
   [c/paragraph "Looking things up:"]
   [example "(:lineage sample)"]
   [example "(get sample :notes \"none\")    ; with a default, if it's missing"]
   [example "(get-in samples [1 :status])    ; a path, into nested data"]
   [c/paragraph "Making changed copies:"]
   [example "(assoc sample :status \"WARN\" :checked? true)"]
   [example "(update sample :reads + 100)    ; apply a function to one value"]
   [example "(dissoc sample :reads)"]
   [example "(merge sample {:status \"FAIL\" :notes \"low coverage\"})"]
   [example "(assoc-in samples [1 :status] \"PASS\")   ; change inside nested data"]
   [c/paragraph "Taking them apart:"]
   [example "(select-keys sample [:id :status])"]
   [example "(keys sample)"]])

(defn- collections []
  [:<>
   [c/paragraph
    "Rather than writing loops, you build a result step by step with "
    "functions that take a collection and give back a new one:"]
   [example "(map :id samples)"]
   [example "(filter #(= \"PASS\" (:status %)) samples)"]
   [example "(remove #(= \"PASS\" (:status %)) samples)"]
   [example "(reduce + (map :reads samples))    ; combine into one value"]
   [example "(sort-by :reads samples)"]
   [c/paragraph "A few more you'll reach for often:"]
   [example "(first samples)"]
   [example "(take 2 (map :id samples))"]
   [example "(frequencies (map :status samples))"]
   [example "(group-by :lineage (map #(select-keys % [:id :lineage]) samples))"]
   [example "(some #(= \"FAIL\" (:status %)) samples)    ; is there any?"]
   [example "(every? #(> (:reads %) 100) samples)    ; are they all?"]
   [c/callout {:kind :info}
    [:strong "Round brackets in the results? "]
    [:code "map"] ", " [:code "filter"] " and friends give back a " [:em "sequence"]
    ", which prints with round brackets. It works just like a vector for "
    "reading. If you need an actual vector, use " [:code "mapv"] " / "
    [:code "filterv"] ", or " [:code "(vec ...)"] " or " [:code "(into [] ...)"] "."]])

(defn- decisions []
  [:<>
   [c/paragraph
    [:code "if"] " takes a test, a \"then\" and an \"else\" - and like "
    "everything else, gives back a value:"]
   [example "(if (> (:reads sample) 500) \"enough reads\" \"too few reads\")"]
   [c/paragraph
    [:code "when"] " is for when there's no \"else\" (it gives " [:code "nil"]
    " instead), and can run several forms. In Reagent it's the usual way "
    "to show something only sometimes: " [:code "(when error? [c/callout ...])"] "."]
   [c/paragraph
    [:code "cond"] " checks one test after another, and " [:code "case"]
    " matches a value against some fixed choices:"]
   [example (str "(let [reads 610]\n"
                 "  (cond\n"
                 "    (< reads 500) \"low\"\n"
                 "    (< reads 1000) \"medium\"\n"
                 "    :else \"high\"))")]
   [example (str "(case (:status sample)\n"
                 "  \"PASS\" \"green\"\n"
                 "  \"WARN\" \"orange\"\n"
                 "  \"red\")    ; the last one is for anything else")]
   [c/paragraph
    [:code "or"] " gives back the first value that isn't false or nil - "
    "handy for defaults:"]
   [example "(or (:notes sample) \"No notes\")"]])

(defn- threading []
  [:<>
   [c/paragraph
    "Code that does several steps in a row can get deeply nested, and you "
    "have to read it from the inside out:"]
   [snippet "(sort (map :id (filter #(= \"PASS\" (:status %)) samples)))"]
   [c/paragraph
    "The " [:em "threading macros"] " let you write the same thing as a "
    "list of steps, top to bottom. " [:code "->>"] " passes each result to "
    "the next step as its " [:strong "last"] " argument:"]
   [example (str "(->> samples\n"
                 "     (filter #(= \"PASS\" (:status %)))\n"
                 "     (map :id)\n"
                 "     (sort))")]
   [c/paragraph
    [:code "->"] " passes it as the " [:strong "first"] " argument instead:"]
   [example (str "(-> sample\n"
                 "    (assoc :status \"WARN\")\n"
                 "    (update :reads + 100)\n"
                 "    (dissoc :lineage))")]
   [c/callout {:kind :info}
    [:strong "Which one? "]
    "Collection functions (" [:code "map"] ", " [:code "filter"] ", "
    [:code "reduce"] "...) take the collection last: use " [:code "->>"] ". "
    "Map functions (" [:code "assoc"] ", " [:code "update"] ", "
    [:code "get"] "...) take the map first: use " [:code "->"] "."]
   [c/paragraph {:style {:margin-top "12px"}}
    [:code "some->"] " is like " [:code "->"] ", but stops (giving "
    [:code "nil"] ") as soon as a step gives " [:code "nil"] " - useful when "
    "something might be missing:"]
   [example "(some-> (:notes sample) str/upper-case)"]])

(defn- destructuring []
  [:<>
   [c/paragraph
    [:em "Destructuring"] " pulls values out of a map or vector and names "
    "them, all in one go. For maps, " [:code "{:keys [...]}"] " names the "
    "keys you want:"]
   [example (str "(let [{:keys [id status]} sample]\n"
                 "  (str id \" is \" status))")]
   [c/paragraph
    "It works anywhere you name things - most usefully, in a function's "
    "arguments. This is how Reagent components usually take their options:"]
   [snippet (str "(defn status-badge [{:keys [status reads]}]\n"
                 "  [:span {:style {:color (if (= status \"PASS\") \"green\" \"red\")}}\n"
                 "   status \" (\" reads \" reads)\"])\n\n"
                 "[status-badge sample]")]
   [c/paragraph
    [:code ":or"] " gives defaults for missing keys, and " [:code ":as"]
    " keeps a name for the whole map:"]
   [example (str "(let [{:keys [id notes] :or {notes \"none\"} :as s} sample]\n"
                 "  [id notes (count s)])")]
   [c/paragraph
    "For vectors (and strings, and other sequences), square brackets pick "
    "things out by position, and " [:code "&"] " collects the rest:"]
   [example (str "(let [[first-sample second-sample & others] samples]\n"
                 "  [(:id first-sample) (:id second-sample) (count others)])")]
   [example "(let [[a b c] \"ATG\"] [c b a])"]])

(defn- loops []
  [:<>
   [c/paragraph
    [:code "for"] " builds a sequence by going through a collection (it's "
    "not a loop like JavaScript's). It can filter with " [:code ":when"]
    " and name things with " [:code ":let"] ":"]
   [example (str "(for [{:keys [id reads]} samples\n"
                 "      :when (> reads 500)]\n"
                 "  (str id \": \" reads))")]
   [c/paragraph
    "This is the usual way to draw a list of things in Reagent - each one "
    "with a " [:code ":key"] " so React can keep track of them:"]
   [snippet (str "[:ul\n"
                 " (for [{:keys [id status]} samples]\n"
                 "   ^{:key id} [:li id \": \" status])]")]
   [c/paragraph
    "To count, use " [:code "range"] ". And " [:code "map-indexed"]
    " gives you each item's position along with it:"]
   [example "(range 5)"]
   [example "(map-indexed (fn [i s] [i (:id s)]) samples)"]
   [c/paragraph
    [:code "doseq"] " looks like " [:code "for"] ", but is for doing "
    "something with each item - like printing it - rather than building a "
    "result. It always gives back " [:code "nil"] "."]
   [snippet (str "(doseq [s samples]\n"
                 "  (println (:id s) (:status s)))")]])

(defn- strings []
  [:<>
   [c/paragraph
    [:code "str"] " joins values into a string. Most other string functions "
    "live in " [:code "clojure.string"] ", which you require like this: "
    [:code "(:require [clojure.string :as str])"] "."]
   [example "(str \"Sample \" (:id sample) \" has \" (:reads sample) \" reads\")"]
   [example "(str/upper-case \"gattaca\")"]
   [example "(str/join \", \" (map :id samples))"]
   [example "(str/split \"S001,S002,S003\" #\",\")"]
   [example "(str/includes? \"GATTACA\" \"TTA\")"]
   [example "(str/trim \"  S001  \")"]
   [c/paragraph
    "A string is also a sequence of characters, so the collection "
    "functions work on it too:"]
   [example "(frequencies \"GATTACA\")"]
   [example "(count (filter #{\"G\" \"C\"} \"GATTACA\"))"]
   [example "(apply str (reverse \"GATTACA\"))"]])

(defonce ^:private counter (r/atom 0))

(defn- counter-demo []
  [c/row {:gap "12px" :align "center"}
   [c/button {:on-click #(swap! counter inc)} "Add one"]
   [c/button {:variant :secondary :on-click #(reset! counter 0)} "Reset"]
   [:span "Clicked " [:strong @counter] " times"]])

(defn- state []
  [:<>
   [c/paragraph
    "An " [:em "atom"] " is a box holding a value that can change. "
    [:code "@"] " reads what's in it; " [:code "swap!"] " changes it by "
    "applying a function; " [:code "reset!"] " replaces it outright:"]
   [example (str "(let [clicks (atom 0)]\n"
                 "  (swap! clicks inc)\n"
                 "  (swap! clicks + 10)\n"
                 "  @clicks)")]
   [c/paragraph
    "Reagent's " [:code "r/atom"] " works the same way, and also redraws "
    "every component that read it when it changes. Use " [:code "defonce"]
    " for it, so saving the file doesn't reset it:"]
   [snippet (str "(defonce counter (r/atom 0))\n\n"
                 "(defn counter-demo []\n"
                 "  [c/row {:gap \"12px\" :align \"center\"}\n"
                 "   [c/button {:on-click #(swap! counter inc)} \"Add one\"]\n"
                 "   [c/button {:variant :secondary :on-click #(reset! counter 0)} \"Reset\"]\n"
                 "   [:span \"Clicked \" [:strong @counter] \" times\"]])")]
   [c/card {:title "Try it"}
    [ui/error-boundary [counter-demo]]]
   [c/paragraph {:style {:margin-top "12px"}}
    "A common pattern is to keep all of a page's state in one map, in one "
    "atom, and change it with " [:code "swap!"] " plus the map functions "
    "from above:"]
   [snippet (str "(defonce app-db (r/atom {:samples [] :selected-id nil}))\n\n"
                 "(swap! app-db assoc :selected-id \"S002\")\n"
                 "(swap! app-db update :samples conj {:id \"S005\" :status \"PASS\"})\n"
                 "(:selected-id @app-db)")]])

(defn- namespaces []
  [:<>
   [c/paragraph
    "Each file is a " [:em "namespace"] ", named at the top with " [:code "ns"]
    ". The name matches the file's path, with dashes where the file name has "
    "underscores: " [:code "src/main/scratchpad/sessions/session_1.cljs"]
    " is " [:code "scratchpad.sessions.session-1"] ". To use another "
    "namespace, " [:code ":require"] " it:"]
   [snippet (str "(ns scratchpad.my-page\n"
                 "  (:require [clojure.string :as str]           ; use as str/join, str/split...\n"
                 "            [reagent.core :as r]               ; r/atom, r/with-let...\n"
                 "            [scratchpad.components :as c]))    ; c/button, c/card...")]
   [c/paragraph
    "Remember to load a file into the REPL (Ctrl+Alt+C Enter) before "
    "evaluating forms in it - see the " [c/link {:href "#/repl-guide"} "REPL Guide"] "."]])

(defn- mistakes []
  [:<>
   [table ["You wrote" "What happens" "Instead"]
    [[[mono "(1 2 3)"]
      [:span "An error like " [:em "1 is not a function"] ": round brackets mean \"call this\""]
      [:span "Use a vector: " [:code "[1 2 3]"]]]
     [[mono "(defn double x (* x 2))"]
      "An error: the arguments need square brackets"
      [mono "(defn double [x] (* x 2))"]]
     [[:span [mono "(assoc sample :status \"FAIL\")"] ", expecting " [:code "sample"] " to change"]
      "sample is unchanged - assoc gave back a new map, which wasn't kept"
      [:span "Use the result (" [:code "let"] ", " [:code "->"] "), or keep "
       "changing state in an atom with " [:code "swap!"]]]
     [[mono "(:status {\"status\" \"PASS\"})"]
      [:span [:code "nil"] ": the key is the string " [:code "\"status\""]
       ", not the keyword " [:code ":status"]]
      [:span [:code "(get m \"status\")"] ", or use keywords as keys"]]
     [[mono "(= \"42\" 42)"]
      [:span [:code "false"] " - text from an input is always a string"]
      [:span [:code "(parse-long \"42\")"] " or " [:code "(parse-double \"97.5\")"]]]
     [[mono "(map println samples)"]
      "Nothing printed (at least, not when you expected)"
      [:span [:code "map"] " only runs when its result is used. For doing "
       "things, use " [:code "doseq"] " or " [:code "run!"]]]
     [[mono "(map f {:a 1 :b 2})"]
      [:span [:code "f"] " gets " [:code "[key value]"] " pairs, not just the values"]
      [:span "Destructure the pair: " [:code "(map (fn [[k v]] ...) m)"] ", or use "
       [:code "vals"]]]
     [[mono "(if test (do-this) (and-this) (otherwise))"]
      [:span "An error: " [:code "if"] " takes exactly a test, a then and an else"]
      [:span [:code "when"] " for several steps, or wrap them in " [:code "(do ...)"]]]
     ["Brackets that don't match"
      "Errors that point somewhere confusing"
      "The editor highlights each bracket's partner; Calva's Format Document (Shift+Alt+F) re-indents, which shows where things went wrong"]]]
   [c/paragraph {:style {:margin-top "12px"}}
    "Good places to learn more: the "
    [c/link {:href "https://cljs.info/cheatsheet/"} "ClojureScript cheatsheet"]
    " (every core function, grouped by what it does), "
    [c/link {:href "https://clojuredocs.org/"} "ClojureDocs"]
    " (examples for each function - it's for Clojure, but nearly all of it "
    "applies), "
    [c/link {:href "https://clojurescript.org/"} "clojurescript.org"]
    ", and the free online book "
    [c/link {:href "https://www.braveclojure.com/clojure-for-the-brave-and-true/"}
     "Clojure for the Brave and True"] "."]])

;; ---------------------------------------------------------------------------
;; The page
;; ---------------------------------------------------------------------------

(defn page []
  [reference/page
   {:id "basics"
    :title "ClojureScript Basics"
    :intro ["A beginner-friendly tour of the language and the patterns you'll "
            "use every day. " [:strong "Every example is live:"] " change it and press "
            [:strong "Ctrl+Enter"] " (or " [:strong "▶ Run"] ") to see what it does now - "
            "break it, fix it, try your own ideas. " [:strong "Reset"]
            " puts it back. They work just like evaluating code in the REPL."]
    :sections [{:title "Reading the code" :content reading-code}
               {:title "Values and collections" :content data}
               {:title "Naming things: def, defn, let" :content naming}
               {:title "Functions without names" :content functions}
               {:title "Data that doesn't change" :content immutable}
               {:title "Working with maps" :content maps}
               {:title "Working with collections" :content collections}
               {:title "Making decisions" :content decisions}
               {:title "Threading: -> and ->>" :content threading}
               {:title "Destructuring" :content destructuring}
               {:title "for, range and doseq" :content loops}
               {:title "Strings" :content strings}
               {:title "State: atoms" :content state}
               {:title "Namespaces" :content namespaces}
               {:title "Common mistakes" :content mistakes}]}])
