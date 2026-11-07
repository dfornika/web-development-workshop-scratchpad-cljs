(ns scratchpad.views.home
  "The Home page: what the workshop is, and how to use this site."
  (:require [scratchpad.components :as c]
            [scratchpad.state :refer [db]]))

(def ^:private codespaces-url
  "https://github.com/codespaces/new/dfornika/web-development-workshop-scratchpad-cljs")

(def ^:private web-url
  "https://dfornika.github.io/web-development-workshop-scratchpad-cljs/")

(defn- hero []
  [:div {:style {:background "linear-gradient(135deg, #004a87 0%, #0969da 100%)"
                 :color "white"
                 :border-radius "12px"
                 :padding "32px"
                 :margin-bottom "24px"}}
   [c/row {:align "center" :style {:justify-content "space-between"}}
    [:div {:style {:flex "1 1 400px"}}
     [:h1 {:style {:margin "0 0 8px 0" :font-size "34px"}}
      "Web Development Workshop"]
     [:p {:style {:margin "0" :font-size "18px" :line-height "1.5" :opacity 0.95}}
      "Building small, useful web apps for sharing lab data - "
      "with ClojureScript and Reagent."]]
    [:img {:src "images/logo-dna.svg" :alt "" :style {:height "96px"}}]]])

(defn- which-version []
  (if (= :web (:edition @db))
    [c/callout {:kind :info}
     [:strong "You're using the in-browser version. "]
     "Nothing to install: the Playground's code can be edited right on the page. "
     "For the full setup - your own editor and a REPL - "
     [c/link {:href codespaces-url} "open the workshop in GitHub Codespaces"] "."]
    [c/callout {:kind :success}
     [:strong "You're running the workshop in your own environment. "]
     "Edit files under " [:code "src/main/scratchpad/"] ", save, and this page "
     "updates by itself. There's also an "
     [c/link {:href web-url} "in-browser version"]
     " to share with people who haven't set anything up."]))

(defn- session-card [n title description ready?]
  [c/card {:title (str "Session " n ": " title)
           :style {:opacity (if ready? 1 0.7)}}
   [c/paragraph {:style {:margin-top 0}} description]
   (if ready?
     [c/link {:href (str "#/session-" n)} "Go to the homework →"]
     [c/tag "Coming soon"])])

(defn- page-card [href title description]
  [c/card {:title [c/link {:href href} title]}
   [c/paragraph {:style {:margin 0}} description]])

(defn page []
  [c/page {:max-width "1100px"}
   [hero]
   [which-version]

   [c/section {:title "About the workshop"}
    [c/paragraph
     "Labs collect a lot of data - sequencing QC, sample tracking, pipeline "
     "results - that's often hard for the people who need it to see. Small web "
     "apps are a great way to share it: a dashboard of QC metrics, a status page "
     "for automated analyses, a tool that turns one spreadsheet format into another."]
    [c/paragraph
     "Over four sessions we'll learn to build apps like these with ClojureScript "
     "(a Clojure that runs in the browser) and Reagent (a way to build web pages "
     "out of small, reusable components). Along the way, we'll get comfortable "
     "with REPL-driven development: building a program while it runs."]]

   [c/section {:title "The sessions"}
    [c/grid {:columns 2}
     [session-card 1 "Introduction to ClojureScript"
      "Data, functions, and turning hiccup into a web page. Homework: genomics-themed mini projects."
      true]
     [session-card 2 "To be announced" "Details coming soon." false]
     [session-card 3 "To be announced" "Details coming soon." false]
     [session-card 4 "To be announced" "Details coming soon." false]]]

   [c/section {:title "How to use this site"}
    [c/grid {:columns 2}
     [page-card "#/session-1" "Session pages"
      "Homework for each session. Every exercise has a live panel that shows what your code does as you work on it."]
     [page-card "#/playground" "Playground"
      "A page that's all yours. Change it however you like - layout, colors, text, images - and see the result straight away."]
     [page-card "#/components" "Components"
      "Ready-made building blocks for your pages, with examples you can edit and copy, the source code behind each one, and the HTML it turns into."]
     [page-card "#/repl-guide" "REPL Guide"
      "How live coding with the REPL works, habits that make it reliable, what to do when it misbehaves, and drills to practise."]]]

   [c/section {:title "Getting started"}
    [c/grid {:columns 2}
     [c/card {:title "In GitHub Codespaces"}
      [c/bullet-list {:numbered? true}
       [:span "Open the " [c/link {:href codespaces-url} "workshop in Codespaces"]
        " and wait for it to set up (a few minutes the first time)."]
       [:span "Start the REPL: Command Palette (Ctrl+Shift+P) → "
        [:strong "Calva: Start a Project REPL and Connect (aka Jack-In)"] "."]
       "When the app opens in a browser tab, edit a file and save it."]]
     [c/card {:title "In your browser"}
      [c/bullet-list {:numbered? true}
       [:span "Open the " [c/link {:href web-url} "in-browser version"]
        " - nothing to install."]
       [:span "Go to the " [c/link {:href "#/playground"} "Playground"]
        " and edit the code on the left."]
       "Press Ctrl+S to see your changes. Your edits are saved in your browser."]]]]

   [c/footer
    "Made with "
    [c/link {:href "https://clojurescript.org/"} "ClojureScript"]
    " and "
    [c/link {:href "https://reagent-project.github.io/"} "Reagent"]]])
