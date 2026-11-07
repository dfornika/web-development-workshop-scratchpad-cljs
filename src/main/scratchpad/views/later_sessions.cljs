(ns scratchpad.views.later-sessions
  "Placeholder pages for sessions 2-4."
  (:require [scratchpad.components :as c]))

(defn page
  "The page for session `n` (2, 3 or 4)."
  [n]
  [c/page
   [c/heading {:level 1} (str "Session " n)]
   [c/callout "Coming soon! Material for this session will appear here."]])
