# Web Development Workshop Scratchpad

[![Open in Github Codespaces](https://github.com/codespaces/badge.svg)](https://github.com/codespaces/new/dfornika/web-development-workshop-scratchpad-cljs)

A ready-to-go ClojureScript ([shadow-cljs](https://github.com/thheller/shadow-cljs) + [Reagent](https://reagent-project.github.io/)) project for the workshop.

## Getting started

1. Click the **Open in GitHub Codespaces** badge above, then **Create codespace**.
2. Wait for the codespace to finish setting up. The first build downloads all
   the dependencies and takes a few minutes.
3. Start the REPL: open the Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`) and run
   **Calva: Start a Project REPL and Connect (aka Jack-In)**.
   The *Preconfigured REPL Connect* option is selected for you.
4. When the build finishes, the app opens in a new browser tab (port 8000).
   If it doesn't, open the **Ports** tab and click the globe icon next to *Web app*.
5. Edit a file under `src/main/scratchpad/` and save it. The page updates without a reload.

## What's in the app

| Page | What it's for | File to edit |
| --- | --- | --- |
| **Playground** | Your own page to experiment with | `src/main/scratchpad/playground.cljs` |
| **Components** | Ready-made components, with examples to copy | (read-only: `components.cljs`) |
| **REPL Guide** | How REPL-driven development works, habits, troubleshooting, and practice drills | `src/main/scratchpad/repl_practice.cljs` |
| **Session 1** | Session 1 homework, with live results | `src/main/scratchpad/sessions/session_1.cljs` and `session_1_bonus.cljs` |
| **Sessions 2–4** | Coming soon | |

## Live coding cheat sheet

There are two ways to see your changes:

- **Save the file**: the page updates by itself (hot reload). Use this for
  changes to how the page looks.
- **Evaluate code at the REPL**: run one expression and see the result right
  next to your code. Use this to try things out.

| To... | Do this (Command Palette name in brackets) |
| --- | --- |
| Load the current file into the REPL | `Ctrl+Alt+C Enter` (Calva: Load/Evaluate Current File and its Requires/Dependencies) |
| Evaluate the form your cursor is in | `Alt+Enter` (Calva: Evaluate Top Level Form) |
| Evaluate just the innermost form | `Ctrl+Enter` (Calva: Evaluate Current Form) |
| See the HTML a component turns into | evaluate `(c/print-html [my-component])`; it appears in Calva's REPL output window |

On a Mac, use `Ctrl+Option+C Enter` and `Option+Enter`. If a shortcut doesn't
work in the browser, use the Command Palette instead.

**Rich comment blocks.** Each file ends with a `(comment ...)` block. Nothing
inside it runs when the file loads, so it's a safe place to keep experiments.
Put your cursor on a form inside and press `Alt+Enter`.

For a fuller explanation, the habits that help, and hands-on practice drills,
see the **REPL Guide** page in the app.

### Common gotchas

- **"No such namespace" or "Unable to resolve symbol"**: the REPL doesn't
  know about your file yet. Load it first (`Ctrl+Alt+C Enter`). Do this again
  whenever you switch to a different file.
- **"No available JS runtime"**: the REPL runs your code in the browser, so
  the app (port 8000) needs to be open in a tab.
- **The page didn't change after I evaluated a `defn`**: evaluating code
  doesn't redraw the page. Save the file, or evaluate `(scratchpad.main/render)`.
- **Hot reload stopped working** (in Codespaces): open the *shadow-cljs*
  port (9630) from the **Ports** tab once, then reload the app's tab.
- **`[my-component]` vs `(my-component)`**: in hiccup, use square brackets to
  include a component. Round brackets just call the function.
- **"Each child in a list should have a unique key"** in the console: give
  items made with `for` or `map` a key, e.g. `^{:key (:id item)} [:li ...]`.
- **My state reset when I saved**: use `defonce` instead of `def` for
  atoms that should survive a reload.
- **Part of the page shows a red error box**: something in that part threw an
  error. Fix the code and save; the box goes away. The browser console (F12)
  has more details.

## Reference solutions

Example solutions to the homework are on the `solutions` branch, in
`src/main/scratchpad/solutions/`. To copy them into your codespace without
touching your own work, run:

```bash
git fetch origin solutions && git checkout FETCH_HEAD -- src/main/scratchpad/solutions
```

Then open those files, load them into the REPL, and compare them with yours.

## Project layout

| Path | What's in it |
| --- | --- |
| `src/main/scratchpad/playground.cljs` | Your playground page |
| `src/main/scratchpad/sessions/` | Homework for each session |
| `src/main/scratchpad/components.cljs` | The component library |
| `src/main/scratchpad/state.cljs` | App state (`db`, a Reagent atom) |
| `src/main/scratchpad/app.cljs`, `main.cljs` | Navigation and startup |
| `src/main/scratchpad/ui.cljs`, `views/`, `macros.clj` | Plumbing for the built-in pages |
| `resources/public/` | `index.html`, CSS, images and data files served at port 8000 |

## Two versions of the site

- **In Codespaces** (what you get when you jack in): the regular version.
  You edit the code in VS Code and use Calva's REPL.
- **In the browser**: a version where the Playground's code can be edited
  right on the page, with no setup at all. It's published at
  https://dfornika.github.io/web-development-workshop-scratchpad-cljs/ and is
  also available as a single HTML file that works offline. The editor uses
  the same keys as Calva: `Alt+Enter` evaluates the top-level form, `Ctrl+Enter`
  the form at the cursor, and `Ctrl+S` loads the whole file. Edits are saved in
  that browser only, so use **Download .cljs** to keep a copy.

## Handy commands

```bash
npm run dev     # watch & hot-reload the app without Calva
npm run release # optimized build into resources/public/js
npm run dev:web # the in-browser version, at http://localhost:8000/web.html
npm run bundle  # the in-browser version as ONE file: dist/scratchpad.html
```

`npm run bundle` packs the in-browser version - compiled code, CSS and
images - into a single HTML file that works without a web server or internet
connection. Open it straight from disk, put it on a shared drive, or send it
to someone.

Every push to `main` also builds this single-file version and publishes it to
the GitHub Pages link above (see `.github/workflows/publish.yml`). Each run in
the repo's **Actions** tab has the HTML file attached too, if you want to
download it.
