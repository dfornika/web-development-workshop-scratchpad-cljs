#!/usr/bin/env node
// Packs the whole app into ONE self-contained HTML file: dist/scratchpad.html
//
// Run it with `npm run bundle`, which first compiles the :bundle build.
//
// The result needs no web server and no internet connection: open it
// straight from disk, put it on a shared drive, or attach it to an email.
// It works by inlining everything the page needs:
//   - the compiled JavaScript (target/bundle/js/main.js)
//   - CSS files linked from index.html
//   - images and other files under resources/public that the code refers
//     to by path (e.g. "images/logo.svg"), as data: URIs

import fs from 'node:fs';
import path from 'node:path';

const publicDir = 'resources/public';
const jsFile = 'target/bundle/js/main.js';
const outFile = 'dist/scratchpad.html';

const mimeTypes = {
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.webp': 'image/webp',
  '.ico': 'image/x-icon',
  '.json': 'application/json',
  '.csv': 'text/csv',
  '.tsv': 'text/tab-separated-values',
};

function dataUri(file) {
  const type = mimeTypes[path.extname(file).toLowerCase()] ?? 'application/octet-stream';
  return `data:${type};base64,${fs.readFileSync(file).toString('base64')}`;
}

// Replace quoted references like "images/logo.svg" (or 'images/logo.svg')
// with data: URIs, for every file under resources/public that exists.
// Returns the new text and the list of files that were inlined.
function inlineAssetReferences(text) {
  const inlined = new Set();
  const result = text.replace(
    /(["'])((?:images|data)\/[\w./-]+)\1/g,
    (match, quote, ref) => {
      const file = path.join(publicDir, ref);
      if (!fs.existsSync(file) || !fs.statSync(file).isFile()) return match;
      inlined.add(ref);
      return quote + dataUri(file) + quote;
    });
  return { text: result, inlined };
}

if (!fs.existsSync(jsFile)) {
  console.error(`Can't find ${jsFile}. Run "npm run bundle" rather than this script directly.`);
  process.exit(1);
}

let html = fs.readFileSync(path.join(publicDir, 'index.html'), 'utf8');

// Inline local stylesheets: <link rel="stylesheet" href="css/style.css" ...>
html = html.replace(/<link\s+rel="stylesheet"\s+href="([^"]+)"[^>]*>/g, (match, href) => {
  const file = path.join(publicDir, href);
  if (!fs.existsSync(file)) return match;
  return `<style>\n${fs.readFileSync(file, 'utf8')}\n</style>`;
});

// Inline the favicon
html = html.replace(/<link\s+rel="icon"([^>]*)href="([^"]+)"([^>]*)>/, (match, before, href, after) => {
  const file = path.join(publicDir, href);
  return fs.existsSync(file) ? `<link rel="icon"${before}href="${dataUri(file)}"${after}>` : match;
});

// Inline the compiled JavaScript, plus any assets it refers to.
// "</script" inside the code would end the <script> tag early, so escape it.
const { text: js, inlined } = inlineAssetReferences(fs.readFileSync(jsFile, 'utf8'));
const safeJs = js.replace(/<\/script/gi, '<\\/script');
html = html.replace(
  /<script src="js\/main\.js"[^>]*><\/script>/,
  () => `<script>\n${safeJs}\n</script>`);

fs.mkdirSync(path.dirname(outFile), { recursive: true });
fs.writeFileSync(outFile, html);

const sizeKb = Math.round(fs.statSync(outFile).size / 1024);
console.log(`Wrote ${outFile} (${sizeKb} KB)`);
console.log(`Inlined ${inlined.size} asset(s): ${[...inlined].sort().join(', ') || 'none'}`);
