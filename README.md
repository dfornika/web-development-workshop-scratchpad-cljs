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

## Project layout

| Path | What's in it |
| --- | --- |
| `src/main/scratchpad/main.cljs` | Entry point: mounts the app and re-renders on reload |
| `src/main/scratchpad/app.cljs` | Top-level `app` component |
| `src/main/scratchpad/components.cljs` | Reusable components |
| `src/main/scratchpad/state.cljs` | App state (`db`, a Reagent atom) |
| `src/main/scratchpad/loaders.cljs` | Helpers for loading JSON into `db` |
| `src/main/scratchpad/sessions/` | Scratch files for each workshop session |
| `src/test/` | Tests (namespaces ending in `-test`) |
| `resources/public/` | `index.html`, CSS, images and data files served at port 8000 |

## Handy commands

```bash
npm run dev     # watch & hot-reload the app without Calva
npm test        # compile and run the tests once
npm run release # optimized build into resources/public/js
```
