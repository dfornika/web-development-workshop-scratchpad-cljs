(ns scratchpad.views.later-sessions
  "Placeholder content for sessions 2-4."
  (:require [scratchpad.components :as c]))

(defn lesson
  "The lesson for session `n` (2, 3 or 4)."
  [n]
  [c/page
   [c/heading {:level 1} (str "Session " n ": Lesson")]
   [c/callout "Coming soon! The slides for this session will appear here."]])

(defn homework
  "The homework for session `n` (2, 3 or 4)."
  [n]
  [c/page
   [c/heading {:level 1} (str "Session " n ": Homework")]
   [c/callout "Coming soon! The homework for this session will appear here."]])
