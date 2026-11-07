(ns scratchpad.views.lessons.session-1
  "Session 1 lesson slides: Introduction to ClojureScript."
  (:require [scratchpad.components :as c]
            [scratchpad.views.slides :as slides :refer [code two-columns]]))

(def ^:private codespaces-url
  "https://github.com/codespaces/new/dfornika/web-development-workshop-scratchpad-cljs")

(def ^:private web-url
  "https://dfornika.github.io/web-development-workshop-scratchpad-cljs/")

(defn- small [& children]
  (into [:div {:style {:font-size "1.6cqw" :color "#57606a"}}] children))

(defn- example-box [title & children]
  [:div {:style {:border "1px solid #d0d7de"
                 :border-radius "8px"
                 :padding "1.4cqw 1.8cqw"
                 :background-color "#f6f8fa"}}
   [:div {:style {:font-weight "bold" :margin-bottom "0.6cqw"}} title]
   (into [:div {:style {:font-size "1.7cqw"}}] children)])

(def slides
  [;; ---------------------------------------------------------------- intro
   {:body
    [[:div {:style {:display "flex"
                    :flex-direction "column"
                    :justify-content "center"
                    :align-items "center"
                    :text-align "center"
                    :min-height "40cqw"}}
      [:img {:src "images/logo-dna.svg" :alt "" :style {:height "12cqw"}}]
      [:h1 {:style {:font-size "5cqw" :color "#004a87" :margin "2cqw 0 1cqw 0"}}
       "Session 1: Introduction to ClojureScript"]
      [:p {:style {:font-size "2.4cqw" :color "#57606a" :margin 0}}
       "Web Development Workshop"]]]}

   {:title "Motivation"
    :body
    [[:p {:style {:font-size "2.6cqw" :margin "0 0 2cqw 0"}}
      "How can web technologies make lab data easier to see, share and connect?"]
     [two-columns
      [example-box "Routine sequencing QC"
       "Consistent QC metrics for every sequencing run, whatever the pathogen - "
       "with run-level checks that stop systematically bad runs from going into "
       "automated analysis."]
      [example-box "Pathogen-specific QC"
       "Metrics and checks tailored to one pathogen, with charts that "
       "summarize the data at a glance."]]
     [:div {:style {:height "2cqw"}}]
     [two-columns
      [example-box "Service monitoring"
       "A status page for the automated analysis services running on a server, "
       "including log messages that help when debugging problems."]
      [example-box "Data format converters"
       "A tool that takes an export from one system and turns it into the format "
       "another system needs - no copying and pasting between spreadsheets."]]]}

   {:title "What is ClojureScript?"
    :body
    [[:p "ClojureScript is Clojure that compiles to JavaScript - so it runs in any modern web browser."]
     [two-columns
      [:div
       [:strong "Why it's nice to work with"]
       [c/bullet-list
        [:span [:strong "Immutable data"] " - maps, vectors and sets never change once created, so state is easier to reason about and debug."]
        [:span [:strong "Functional"] " - mostly pure functions: cleaner, more testable code, with fewer side effects."]
        [:span [:strong "JavaScript interop"] " - use any JavaScript library, and call browser APIs directly."]]]
      [:div
       [:strong "What it's good for"]
       [c/bullet-list
        [:span [:strong "Web apps"] " - dynamic, interactive sites like dashboards."]
        [:span [:strong "Interactive UIs"] " - built from components, with libraries like Reagent."]
        [:span [:strong "Live data"] " - visualizations that react to user input or changing data."]]]]]}

   ;; --------------------------------------------------------------- syntax
   {:title "Syntax: data literals"
    :body
    [[:p "Code is written as " [:strong "S-expressions"] ": lists in parentheses. "
      "Data is written directly, as " [:em "literals"] ":"]
     [two-columns
      [code ";; Numbers
0  1  3.14

;; Strings
\"hello\"

;; Keywords
:gene-name

;; Booleans, and nothing
true  false  nil"]
      [code ";; Vectors
[1 2 \"a\" \"hello\" 3.14]

;; Maps
{:hello \"world\"
 :goodbye \"earth\"}

;; Sets
#{\"blaKPC\" \"mecA\"}"]]]}

   {:title "Syntax: names and function calls"
    :body
    [[:p "Give a value a name with " [:code "def"] ":"]
     [code "(def x 5)

(def gene {:gene-name \"KPC-3\"
           :start 21331
           :end 22598})"]
     [:p "Calling a function: the function goes " [:em "inside"] " the parentheses, first."]
     [two-columns
      [example-box "Many other languages" [code "add(2, 3)"]]
      [example-box "ClojureScript" [code "(add 2 3)"]]]]}

   {:title "Functions"
    :body
    [[:div {:style {:display "grid"
                    :grid-template-columns "repeat(3, minmax(0, 1fr))"
                    :gap "2cqw"}}
      [example-box "Anonymous"
       [code "(fn [n]
  (+ n 1))"]
       [small "For a quick, throwaway function - often passed to "
        [:code "map"] ", " [:code "filter"] " or " [:code "reduce"] "."]]
      [example-box "Named, with def + fn"
       [code "(def add-one
  (fn [n]
    (+ n 1)))"]
       [small "Shows that a function is just a value with a name."]]
      [example-box "Named, with defn"
       [code "(defn add-one
  \"Adds one to a number\"
  [n]
  (+ n 1))"]
       [small [:code "defn"] " = " [:code "def"] " + " [:code "fn"] ", plus a description. "
        "Use this for most functions."]]]
     [c/callout {:style {:margin-top "2cqw"}}
      "Tip: prefer " [:code "defn"] " for reusable functions, and anonymous functions for short, inline logic."]]}

   {:title "Anatomy of a function"
    :body
    [[two-columns
      [code "(defn add-one
  \"Adds one to a number n\"
  [n]
  (+ n 1))"]
      [c/bullet-list
       [:span [:code "add-one"] " - the function's name"]
       [:span [:code "\"Adds one...\""] " - a docstring: an optional description"]
       [:span [:code "[n]"] " - its arguments: the inputs"]
       [:span [:code "(+ n 1)"] " - its body: what it does"]]]
     [:p {:style {:margin-top "2cqw"}} "A bigger example:"]
     [code "(defn mean-coverage
  \"The mean coverage of some samples.\"
  [samples]
  (let [coverages (map :coverage samples)]
    (/ (reduce + coverages) (count coverages))))"]]}

   ;; ------------------------------------------------------- core functions
   {:title "Core functions: map, filter, reduce"
    :body
    [[:div {:style {:display "grid"
                    :grid-template-columns "repeat(3, minmax(0, 1fr))"
                    :gap "2cqw"}}
      [example-box "map"
       [small "Applies a function to each item, giving a sequence of results."]
       [code "(map inc [1 2 3])
;; => (2 3 4)"]]
      [example-box "filter"
       [small "Keeps the items a function says yes to."]
       [code "(filter even? [1 2 3 4])
;; => (2 4)"]]
      [example-box "reduce"
       [small "Combines all the items into a single result."]
       [code "(reduce + [1 2 3 4])
;; => 10"]]]
     [:p {:style {:margin-top "2cqw"}} "With lab data:"]
     [code "(def samples [{:id \"S001\" :coverage 87.4}
              {:id \"S002\" :coverage 12.1}
              {:id \"S003\" :coverage 64.0}])

(map :id samples)                       ;; => (\"S001\" \"S002\" \"S003\")
(filter #(> (:coverage %) 50) samples)  ;; => the two good samples"]]}

   {:title "Core functions: conj, into, some"
    :body
    [[:div {:style {:display "grid"
                    :grid-template-columns "repeat(3, minmax(0, 1fr))"
                    :gap "2cqw"}}
      [example-box "conj"
       [small "Adds to a collection - where depends on the kind of collection."]
       [code "(conj [1 2 3] 4)
;; => [1 2 3 4]

(conj '(1 2 3) 4)
;; => (4 1 2 3)"]]
      [example-box "into"
       [small "Pours one collection into another, keeping the target's type."]
       [code "(into [] '(1 2 3))
;; => [1 2 3]

(into #{} [1 1 2])
;; => #{1 2}"]]
      [example-box "some"
       [small "The first truthy result of a function, or nil."]
       [code "(some #{2 4} [1 2 3])
;; => 2

(some even? [1 3 5])
;; => nil"]]]]}

   {:title "Core functions: take, drop, sort, sort-by"
    :body
    [[two-columns
      [example-box "take and drop"
       [small "The first few items - or everything after them."]
       [code "(take 2 [1 2 3 4])
;; => (1 2)

(drop 2 [1 2 3 4])
;; => (3 4)"]]
      [example-box "sort and sort-by"
       [small "In natural order - or ordered by what a function returns."]
       [code "(sort [3 1 4 2])
;; => (1 2 3 4)

(sort-by count [\"apple\" \"fig\" \"banana\"])
;; => (\"fig\" \"apple\" \"banana\")"]]]]}

   {:title "A first program"
    :body
    [[code "(ns my-app.core)            ; which namespace (file) this is

(defn greet [name]          ; define a function
  (str \"Hello, \" name \"!\"))  ; str joins values into one string

(js/console.log (greet \"World\"))  ; js/ reaches JavaScript's built-ins"]
     [c/bullet-list
      [:span [:code "ns"] " names the namespace - usually one per file."]
      [:span [:code "str"] " joins things into a single string."]
      [:span [:code "js/"] " gives you JavaScript's global objects, like "
       [:code "js/console"] ", " [:code "js/document"] " and " [:code "js/alert"] "."]]
     [c/callout {:kind :success}
      "Try it: paste " [:code "(greet \"World\")"]
      "-style experiments into a " [:code "(comment ...)"] " block and evaluate them with Alt+Enter."]]}

   ;; -------------------------------------------------------- HTML and CSS
   {:title "From ClojureScript to a web page"
    :body
    [[:p "Every app starts from a small HTML page. This project's is "
      [:code "resources/public/index.html"] ":"]
     [code {:language :html}
      "<!DOCTYPE html>
<html>
  <head>
    <meta charset=\"UTF-8\">
    <title>Workshop Scratchpad</title>
    <link rel=\"stylesheet\" href=\"css/style.css\">
  </head>
  <body>
    <div id=\"app\"></div>                 <!-- the app is drawn in here -->
    <script src=\"js/main.js\"></script>   <!-- the compiled ClojureScript -->
  </body>
</html>"]
     [small [:code "<head>"] " holds information about the page and links the CSS (the styles). "
      [:code "<div id=\"app\">"] " is where ClojureScript puts everything you see."]]}

   {:title "Hiccup: HTML as data"
    :body
    [[:p "With Reagent, pages are written as " [:strong "hiccup"]
      ": ordinary vectors that look like HTML."]
     [two-columns
      [code "(defn app []
  [:div
   [:h1 \"Hello from ClojureScript\"]
   [:p {:style {:color \"green\"}}
    \"This paragraph is hiccup.\"]])"]
      [code {:language :html}
       "<div>
  <h1>Hello from ClojureScript</h1>
  <p style=\"color:green\">
    This paragraph is hiccup.
  </p>
</div>"]]
     [c/bullet-list
      [:span [:code "[:div ...]"] " is a " [:code "<div>"] " - the first item is the tag, then an optional map of attributes, then what goes inside."]
      [:span "A component is just a function that returns hiccup. "
       [:code "src/main/scratchpad/main.cljs"] " draws the " [:code "app"]
       " component into " [:code "<div id=\"app\">"] "."]
      [:span "See it for yourself: every example on the " [:strong "Components"]
       " page has an HTML tab."]]]}

   {:title "Talking to the browser directly"
    :body
    [[:p "You can also change the page through JavaScript interop:"]
     [code "(defn update-content []
  (let [el (.getElementById js/document \"app\")]
    (set! (.-innerHTML el)
          \"<h1>Hello, World!</h1><p>Set by interop.</p>\")))"]
     [c/bullet-list
      [:span [:code "(.getElementById js/document \"app\")"]
       " calls a method: " [:code "document.getElementById(\"app\")"] " in JavaScript."]
      [:span [:code "(.-innerHTML el)"] " reads a property, and " [:code "set!"] " changes it."]]
     [c/callout "Most of the time Reagent does this for you - you describe the page "
      "with hiccup, and it keeps the browser up to date."]]}

   ;; --------------------------------------------------------- hands-on
   {:title "Hands-on: your first web page"
    :body
    [[:p "Now let's put it together. In this part you'll:"]
     [c/bullet-list {:numbered? true}
      "Open the workshop in GitHub Codespaces"
      "Start a REPL and connect to it"
      "Customize a web page's layout with ClojureScript"
      "Add and style components like headers, text, logos and buttons"]
     [c/callout {:kind :success}
      "No Codespaces? Most of this works in the "
      [c/link {:href web-url} "in-browser version"]
      " too - edit the Playground right on the page."]]}

   {:title "Setting up"
    :body
    [[c/bullet-list {:numbered? true}
      [:span "Sign in to GitHub, and open the "
       [c/link {:href codespaces-url} "workshop in Codespaces"]
       " (or click " [:strong "Open in GitHub Codespaces"] " in the README)."]
      [:span "Click " [:strong "Create codespace"] ", and wait while it sets up - "
       "a few minutes the first time, while it downloads everything it needs."]
      [:span "When VS Code appears in your browser, you're ready."]]
     [c/callout
      [:strong "Keeping your work: "]
      "a codespace you create from the workshop repo keeps your edits. To save them "
      "to your own GitHub account too, fork the repo first and create the codespace "
      "from your fork."]]}

   {:title "What's a REPL?"
    :body
    [[:p [:strong "REPL"] " stands for " [:strong "Read-Eval-Print Loop"] ":"]
     [c/bullet-list {:numbered? true}
      [:span [:strong "Read"] " the code you give it"]
      [:span [:strong "Evaluate"] " it - run it"]
      [:span [:strong "Print"] " the result"]
      [:span [:strong "Loop"] " back, ready for more"]]
     [:p "Here, the REPL connects VS Code (with the Calva extension) to your app "
      "running in the browser. You can run code inside the running app and see the "
      "result straight away - no restarting."]
     [c/callout "The " [:strong "REPL Guide"] " page explains how the pieces fit "
      "together, and has drills to practise with."]]}

   {:title "Starting the REPL"
    :body
    [[c/bullet-list {:numbered? true}
      [:span "Open the Command Palette: " [:strong "Ctrl+Shift+P"] " (Cmd+Shift+P on a Mac)."]
      [:span "Type " [:code "jack"] " and choose "
       [:strong "Calva: Start a Project REPL and Connect (aka Jack-In)"] "."]
      [:span "The " [:strong "Preconfigured REPL Connect"]
       " option is chosen for you - just wait for it to start."]
      [:span "When it's ready, the app opens in a new browser tab (port 8000). If it "
       "doesn't, open the " [:strong "Ports"] " tab and click the globe next to "
       [:em "Web app"] "."]]
     [c/callout {:kind :warning}
      "If saving a file doesn't update the page, open port 9630 from the Ports tab "
      "once, then reload the app's tab."]]}

   {:title "Your playground"
    :body
    [[:p "Open " [:code "src/main/scratchpad/playground.cljs"]
      " - that's the " [:strong "Playground"] " page in the app."]
     [two-columns
      [:div
       [:strong "Save the file"]
       [c/bullet-list
        "The page updates by itself, straight away"
        "Best for changing how the page looks"]]
      [:div
       [:strong "Evaluate at the REPL"]
       [c/bullet-list
        "Put your cursor on a form and press Alt+Enter"
        "The result appears right next to your code"
        "Best for trying things out and testing functions"]]]
     [c/callout {:kind :success}
      "The " [:code "(comment ...)"] " block at the bottom of the file is full of "
      "things to try. Load the file first with " [:strong "Ctrl+Alt+C Enter"] "."]]}

   ;; ------------------------------------------------------- try these edits
   {:title "Try it: change the header"
    :body
    [[:p "The header is a " [:code "c/header"] " from the component library. Change its options:"]
     [two-columns
      [example-box "Before"
       [code "[c/header {:title \"Lab Website\"
           :title-color \"#004a87\"
           :logo-src \"images/logo-dna.svg\"
           :height-px 48}]"]]
      [example-box "Try"
       [code "[c/header {:title \"My First Web App\"
           :title-color \"green\"
           :logo-src \"images/logo-dna.svg\"
           :height-px 48
           :style {:font-family \"Georgia\"}}]"]]]
     [small "Save, and watch the page change. Try other colors and fonts too."]]}

   {:title "Try it: swap the logo"
    :body
    [[:p "There are five logos in " [:code "resources/public/images"] ":"]
     [:div {:style {:display "flex" :gap "3cqw" :align-items "center" :margin "1.5cqw 0"}}
      (for [logo ["dna" "flask" "microscope" "molecule" "bacterium"]]
        ^{:key logo}
        [:div {:style {:text-align "center"}}
         [:img {:src (str "images/logo-" logo ".svg") :alt logo :style {:height "7cqw"}}]
         [:div {:style {:font-size "1.4cqw" :font-family "monospace"}} (str "logo-" logo ".svg")]])]
     [code "[c/header {:title \"Lab Website\"
           :logo-src \"images/logo-microscope.svg\"
           :height-px 80}]"]
     [small "Change " [:code ":logo-src"] " to use a different one, and "
      [:code ":height-px"] " to change its size."]]}

   {:title "Try it: a background color"
    :body
    [[:p "Components take a " [:code ":style"] " map, merged into their own styles:"]
     [code "[c/header {:title \"Lab Website\"
           :logo-src \"images/logo-dna.svg\"
           :style {:background-color \"#f0f8ff\"
                   :height \"120px\"}}]"]
     [small "CSS property names become keywords: " [:code "background-color"] " → "
      [:code ":background-color"] ". Try it on the cards further down the page too."]]}

   {:title "Try it: add some text"
    :body
    [[:p "Add a paragraph underneath the header:"]
     [code "[:div
 [c/header {:title \"Lab Website\" ...}]
 [:p {:style {:font-size \"18px\" :padding \"10px\"}}
  \"Welcome to our learning app!\"]
 ...]"]
     [small "That's plain hiccup. The component library has a "
      [:code "c/paragraph"] " too - browse the Components page for more."]]}

   {:title "Try it: add a button"
    :body
    [[:p "Add a button below your paragraph:"]
     [code "[c/button {:on-click (fn [] (js/alert \"You clicked me!\"))}
 \"Click Me\"]"]
     [c/bullet-list
      [:span [:code ":on-click"] " takes a function, called when the button is clicked."]
      [:span [:code "(fn [] ...)"] " is an anonymous function, like the ones from earlier."]]
     [small "Bonus homework: make the button count its clicks instead."]]}

   ;; ------------------------------------------------------------- wrap-up
   {:title "Summary"
    :body
    [[:div {:style {:display "grid"
                    :grid-template-columns "repeat(3, minmax(0, 1fr))"
                    :gap "2cqw"}}
      [example-box "ClojureScript basics"
       [c/bullet-list
        "Data: vectors, maps, keywords"
        "Functions with defn and fn"
        "Core functions: map, filter, reduce, conj, take, sort..."]]
      [example-box "HTML and CSS"
       [c/bullet-list
        "Pages as hiccup"
        "Styles as :style maps"
        "Components: functions that return hiccup"]]
      [example-box "Live coding"
       [c/bullet-list
        "A ready-made Codespace"
        "The REPL, via Calva"
        "Edit, save, see it change"]]]
     [:p {:style {:margin-top "2.5cqw" :text-align "center" :font-size "2.4cqw"}}
      "You can now build and change simple web pages, live, with ClojureScript."]]}

   {:title "Homework, and where to learn more"
    :body
    [[two-columns
      [:div
       [:strong "Homework"]
       [:p "The " [:strong "Homework"] " tab at the top of this page has four "
        "genomics-themed mini projects, plus four bonus challenges - each with a "
        "live panel showing what your code does."]
       [c/link {:href "#/session-1/homework"} "Go to the homework →"]]
      [:div
       [:strong "Resources"]
       [c/bullet-list
        [c/link {:href "https://clojurescript.org/"} "ClojureScript"]
        [c/link {:href "https://reagent-project.github.io/"} "Reagent"]
        [c/link {:href "https://calva.io/get-started-with-clojure/"} "Getting started with Calva"]
        [c/link {:href "https://www.braveclojure.com/core-functions-in-depth/"} "Core functions in depth (Brave Clojure)"]
        [c/link {:href "https://clojure.org/api/cheatsheet"} "Clojure cheatsheet"]
        [c/link {:href "https://docs.github.com/en/codespaces"} "GitHub Codespaces docs"]]]]]}])

(defn page []
  [c/page {:max-width "1200px"}
   [slides/viewer "session-1" slides]])
