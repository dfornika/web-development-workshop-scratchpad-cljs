(ns scratchpad.views.js-interop
  "The JS Interop page: how to use JavaScript - the browser's built-in
  objects and npm libraries - from ClojureScript.

  The results shown under the examples (;; => ...) aren't typed in by hand:
  each example's code really runs when the page is drawn."
  (:require [reagent.core :as r]
            [scratchpad.components :as c]
            [scratchpad.ui :as ui]
            [scratchpad.web.editor :as editor]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- snippet
  "Syntax-colored code."
  [text]
  ^{:key text}
  [editor/editor {:initial-doc text
                  :read-only? true
                  :line-numbers? false
                  :font-size "13px"
                  :style {:margin "8px 0"
                          :border "1px solid #d0d7de"
                          :border-radius "6px"
                          :overflow "hidden"}}])

(defn- example
  "Syntax-colored `code`, followed by the result of calling `f` - which
  should be the same code, so the result shown is the real one."
  [code f]
  [snippet (str code "\n;; => " (pr-str (f)))])

(def ^:private cell {:padding "6px 12px"
                     :border-bottom "1px solid #d0d7de"
                     :text-align "left"
                     :vertical-align "top"})

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

(defn- mono [text]
  [:code {:style {:white-space "nowrap"}} text])

;; ---------------------------------------------------------------------------
;; Sections
;; ---------------------------------------------------------------------------

(defn- cheat-sheet []
  [:<>
   [c/paragraph
    "ClojureScript compiles to JavaScript, so everything JavaScript can do - "
    "the browser's built-in objects, and any library from npm - is available "
    "to you. \"Interop\" is the name for the bits of syntax that reach out to it. "
    "Here's most of it on one table:"]
   [table ["In JavaScript" "In ClojureScript" "What it does"]
    [[[mono "s.toUpperCase()"] [mono "(.toUpperCase s)"] "Call a method"]
     [[mono "s.indexOf(\"TTA\")"] [mono "(.indexOf s \"TTA\")"] "Call a method, with arguments"]
     [[mono "s.length"] [mono "(.-length s)"] "Read a property"]
     [[mono "window.location.hash"] [mono "(.. js/window -location -hash)"] "Read a chain of properties"]
     [[mono "document.title = \"Hi\""] [mono "(set! (.-title js/document) \"Hi\")"] "Change a property"]
     [[mono "Math.round(2.6)"] [mono "(js/Math.round 2.6)"] "Use a global (anything built into the browser)"]
     [[mono "new Date(2026, 0, 15)"] [mono "(js/Date. 2026 0 15)"] "Create an object with new"]
     [[mono "{sampleId: \"S001\"}"] [mono "#js {:sampleId \"S001\"}"] "A JavaScript object"]
     [[mono "[1, 2, 3]"] [mono "#js [1 2 3]"] "A JavaScript array"]]]
   [c/paragraph {:style {:margin-top "12px"}}
    "The rule of thumb: in JavaScript the object comes first and the method "
    "after the dot. In ClojureScript, as always, the thing you're doing comes "
    "first - " [:code "(.method object arguments...)"] "."]])

(defn- methods-and-properties []
  [:<>
   [c/paragraph
    "A symbol starting with a dot calls a method. The object goes first, then "
    "any arguments:"]
   [example "(.toUpperCase \"gattaca\")" #(.toUpperCase "gattaca")]
   [example "(.indexOf \"GATTACA\" \"TTA\")" #(.indexOf "GATTACA" "TTA")]
   [example "(.toFixed 3.14159 2)" #(.toFixed 3.14159 2)]
   [example "(.padStart \"7\" 3 \"0\")" #(.padStart "7" 3 "0")]
   [c/paragraph
    "A dot and a dash, " [:code ".-"] ", reads a property instead. (A method "
    "is something you call; a property is just a value.)"]
   [example "(.-length \"GATTACA\")" #(.-length "GATTACA")]
   [c/paragraph
    [:code ".."] " follows a chain of properties and methods, one after another. "
    "These two lines do the same thing:"]
   [example "(.-hash (.-location js/window))" #(.-hash (.-location js/window))]
   [example "(.. js/window -location -hash)" #(.. js/window -location -hash)]
   [c/paragraph
    [:code "set!"] " changes a property. Try this one in the REPL - look at "
    "the browser tab's title afterwards:"]
   [snippet "(set! (.-title js/document) \"Hello from the REPL!\")"]])

(defn- title-demo []
  (r/with-let [original (.-title js/document)]
    [c/row {:gap "8px"}
     [c/button {:on-click #(set! (.-title js/document) "Hello from ClojureScript!")}
      "Change the tab's title"]
     [c/button {:variant :secondary
                :on-click #(set! (.-title js/document) original)}
      "Put it back"]]
    (finally
      (set! (.-title js/document) original))))

(defn- globals []
  [:<>
   [c/paragraph
    "Everything built into the browser lives under " [:code "js/"] ": "
    [:code "js/document"] " (the page), " [:code "js/window"] " (the browser "
    "tab), " [:code "js/console"] ", " [:code "js/Math"] ", " [:code "js/JSON"]
    ", " [:code "js/Date"] ", and so on. You can call them like functions:"]
   [example "(js/Math.round 2.6)" #(js/Math.round 2.6)]
   [example "(js/Math.max 3 8 5)" #(js/Math.max 3 8 5)]
   [example "(js/parseInt \"42\")" #(js/parseInt "42")]
   [example "(js/parseFloat \"97.5%\")" #(js/parseFloat "97.5%")]
   [snippet (str "(js/console.log \"hello\")     ; shows up in the browser console (F12)\n"
                 "(js/alert \"Hello!\")          ; a pop-up message\n"
                 "(js/setTimeout #(js/alert \"Time's up!\") 2000)   ; after 2 seconds")]
   [c/paragraph
    "Put a dot " [:em "after"] " a name to create a new object (like "
    [:code "new"] " in JavaScript):"]
   [example "(.getFullYear (js/Date. 2026 0 15))" #(.getFullYear (js/Date. 2026 0 15))]
   [c/paragraph "And here's " [:code "set!"] " from the last section, on a button:"]
   [title-demo]
   [snippet (str "[c/button {:on-click #(set! (.-title js/document) \"Hello from ClojureScript!\")}\n"
                 " \"Change the tab's title\"]")]])

(defn- objects-and-data []
  [:<>
   [c/paragraph
    "JavaScript objects and arrays are " [:em "not"] " the same as "
    "ClojureScript maps and vectors. A JavaScript object can be changed in "
    "place, and the ClojureScript functions you know - " [:code ":key"] ", "
    [:code "get"] ", " [:code "assoc"] ", " [:code "="] " - don't work with it:"]
   [example "(:sampleId #js {:sampleId \"S001\"})" #(:sampleId #js {:sampleId "S001"})]
   [example "(.-sampleId #js {:sampleId \"S001\"})"
            #(let [^js sample #js {:sampleId "S001"}] (.-sampleId sample))]
   [example "(= #js [1 2 3] #js [1 2 3])" #(= #js [1 2 3] #js [1 2 3])]
   [example "(= [1 2 3] [1 2 3])" #(= [1 2 3] [1 2 3])]
   [c/paragraph
    "So the usual approach is: keep your own data as ClojureScript data, and "
    "convert at the edges, when you hand data to JavaScript or get some back."]
   [example "(clj->js {:sampleId \"S001\" :lineages [\"JN.1\" \"KP.2\"]})"
            #(clj->js {:sampleId "S001" :lineages ["JN.1" "KP.2"]})]
   [example "(js->clj #js {:sampleId \"S001\"} :keywordize-keys true)"
            #(js->clj #js {:sampleId "S001"} :keywordize-keys true)]
   [c/paragraph
    "JSON - the text format most web services send and receive - goes "
    "through JavaScript objects:"]
   [example "(js/JSON.stringify (clj->js {:sampleId \"S001\" :qcStatus \"PASS\"}))"
            #(js/JSON.stringify (clj->js {:sampleId "S001" :qcStatus "PASS"}))]
   [example "(js->clj (js/JSON.parse \"{\\\"qcStatus\\\": \\\"PASS\\\"}\") :keywordize-keys true)"
            #(js->clj (js/JSON.parse "{\"qcStatus\": \"PASS\"}") :keywordize-keys true)]
   [c/callout {:kind :info}
    [:strong "#js is shallow. "]
    [:code "#js {:a {:b 1}}"] " makes a JavaScript object on the outside, but "
    "the " [:code "{:b 1}"] " inside is still a ClojureScript map. Use "
    [:code "clj->js"] " when there's nesting - it converts all the way down."]])

(defn- events-demo []
  (r/with-let [text (r/atom "gattaca")]
    [c/column {:gap "8px"}
     [:input {:value @text
              :on-change (fn [event] (reset! text (.. event -target -value)))
              :style {:padding "6px 8px" :font-size "14px" :max-width "300px"}}]
     [c/label-value "(.toUpperCase text)" (.toUpperCase @text)]
     [c/label-value "(.-length text)" (.-length @text)]]))

(defn- events []
  [:<>
   [c/paragraph
    "Reagent passes your " [:code ":on-click"] ", " [:code ":on-change"]
    " (and so on) functions a JavaScript event object. The most common "
    "thing to do with it is read what someone typed into an " [:code "[:input]"] ":"]
   [snippet (str "(defn events-demo []\n"
                 "  (r/with-let [text (r/atom \"gattaca\")]\n"
                 "    [c/column {:gap \"8px\"}\n"
                 "     [:input {:value @text\n"
                 "              :on-change (fn [event] (reset! text (.. event -target -value)))}]\n"
                 "     [c/label-value \"(.toUpperCase text)\" (.toUpperCase @text)]\n"
                 "     [c/label-value \"(.-length text)\" (.-length @text)]]))")]
   [c/card {:title "Try it"}
    [ui/error-boundary [events-demo]]]
   [c/paragraph {:style {:margin-top "12px"}}
    [:code "(.. event -target -value)"] " is " [:code "event.target.value"]
    " in JavaScript: the event's target is the input box, and its value is "
    "the text in it. (The workshop's " [:code "c/text-input"] " component does "
    "this step for you, and hands your function the text.) Two other things you'll see:"]
   [snippet (str "(.preventDefault event)   ; stop the browser's usual reaction (e.g. following a link)\n"
                 "(.-key event)             ; which key was pressed, in an :on-key-down")]])

(defn- npm-libraries []
  [:<>
   [c/paragraph
    "Libraries from npm are listed in " [:code "package.json"] " and "
    "required with a " [:em "string"] " instead of a symbol - that's how you "
    "can tell a JavaScript library from a ClojureScript namespace:"]
   [snippet (str "(ns my-page\n"
                 "  (:require [\"ag-grid-react\" :refer [AgGridReact]]   ; a JavaScript library\n"
                 "            [reagent.core :as r]))                  ; a ClojureScript namespace")]
   [c/paragraph
    "The library's documentation will be written for JavaScript, so you'll "
    "translate its examples with the cheat sheet above. For React components "
    "like " [:code "AgGridReact"] ", Reagent has its own piece of syntax, "
    [:code ":>"] ", and turns a map of props into a JavaScript object for you:"]
   [snippet (str ";; JavaScript (JSX):  <AgGridReact rowData={rows} columnDefs={columns} />\n"
                 "[:> AgGridReact {:rowData rows :columnDefs columns}]")]
   [c/paragraph
    "Use the library's own names for props - " [:code ":rowData"] ", not "
    [:code ":row-data"] ". And when a library calls one of your functions, "
    "what it passes in is usually a JavaScript object, so you read it with "
    [:code ".-"] ":"]
   [snippet (str "{:field \"qcStatus\"\n"
                 " :cellStyle (fn [params]\n"
                 "              (if (= (.-value params) \"FAIL\")\n"
                 "                #js {:color \"red\"}\n"
                 "                nil))}")]
   [c/paragraph
    "Session 2's lesson goes through AG Grid in detail."]])

(defn- gotchas []
  [:<>
   [table ["Watch out for" "What happens" "What to do"]
    [[[:span "Dashes in names: " [:code "(.-sample-id obj)"]]
      [:span "ClojureScript turns dashes into underscores, so this reads "
       [:code "obj.sample_id"] " - usually " [:code "nil"] "."]
      [:span "Use camelCase for JavaScript data: " [:code "{:sampleId ...}"]]]
     [[:span "Using " [:code ":key"] " or " [:code "get"] " on a JavaScript object"]
      [:span "Always " [:code "nil"] ", with no error"]
      [:span "Use " [:code ".-"] ", or convert it with " [:code "js->clj"] " first"]]
     [[:span [:code "(str js-object)"]]
      [:span [:code "\"[object Object]\""] " - not very helpful"]
      [:span "Look at it with " [:code "js/console.log"] ", " [:code "tap>"] ", or "
       [:code "js->clj"]]]
     [[:span [:code "null"] " and " [:code "undefined"]]
      "JavaScript has two kinds of \"nothing\""
      [:span "Both count as " [:code "nil"] ": " [:code "(nil? js/undefined)"] " is true"]]
     [[:span "A misspelled method: " [:code "(.toUppercase s)"]]
      [:span "An error: " [:em "s.toUppercase is not a function"]]
      "Check the spelling and capitals in the JavaScript docs (MDN is a good reference)"]
     [[:span "Interop works in the REPL, but not in a release build ("
       [:code "npm run bundle"] ")"]
      "The optimizer renamed a property it didn't know was JavaScript's"
      [:span "Add a " [:code "^js"] " hint: " [:code "(fn [^js params] (.-value params))"]
       ". shadow-cljs warns you where."]]]]
   [c/paragraph {:style {:margin-top "12px"}}
    "The " [:code "js/"] " names are documented on "
    [c/link {:href "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference"}
     "MDN's JavaScript reference"]
    " and "
    [c/link {:href "https://developer.mozilla.org/en-US/docs/Web/API"} "Web API reference"]
    ". The official guide to interop is on "
    [c/link {:href "https://clojurescript.org/reference/javascript-api"} "clojurescript.org"]
    ", and "
    [c/link {:href "https://shadow-cljs.github.io/docs/UsersGuide.html#js-deps"}
     "the shadow-cljs User's Guide"]
    " covers using npm libraries."]])

;; ---------------------------------------------------------------------------
;; The page
;; ---------------------------------------------------------------------------

(def ^:private sections
  [{:title "Cheat sheet" :content cheat-sheet}
   {:title "Methods and properties" :content methods-and-properties}
   {:title "The browser's built-ins: js/" :content globals}
   {:title "JavaScript objects vs ClojureScript data" :content objects-and-data}
   {:title "Events" :content events}
   {:title "npm libraries" :content npm-libraries}
   {:title "Gotchas" :content gotchas}])

(defn- section-id [title]
  (ui/anchor-id "js-interop" title))

(def ^:private toc-entries
  (for [{:keys [title]} sections]
    {:id (section-id title) :label title :level 1}))

(defn page []
  [c/page {:max-width "1200px"}
   [ui/with-table-of-contents {:top-id "js-interop-top" :entries toc-entries}
    [c/heading {:level 1} "JS Interop"]
    [c/callout
     "How to use JavaScript from ClojureScript: the browser's built-in objects, "
     "and libraries from npm. The results shown after " [:code ";; =>"]
     " are real - each example runs when the page is drawn. Try them in the "
     "REPL too, and change them to see what happens."]
    (for [{:keys [title content]} sections]
      ^{:key title}
      [ui/anchor (section-id title)
       [c/section {:title title}
        [content]]])]])
