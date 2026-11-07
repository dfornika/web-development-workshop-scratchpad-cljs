#!/usr/bin/env node
// Packs the whole app into ONE self-contained HTML file: dist/scratchpad.html
//
// Run it with `npm run bundle`, which first compiles the :bundle build -
// the in-browser version of the site, where the Playground can be edited
// right on the page.
//
// The result needs no web server and no internet connection: open it
// straight from disk, put it on a shared drive, or attach it to an email.
// It works by inlining everything the page needs:
//   - the compiled JavaScript (target/bundle/js/main.js)
//   - CSS files and favicons linked from index.html
//   - every image under resources/public/images, plus a small script that
//     swaps in the embedded copy whenever an <img> refers to one by path
//     (e.g. "images/logo-dna.svg"). Doing this at display time, rather
//     than rewriting paths in the code, means code shown to people - like
//     the Playground's source in the editor - keeps its normal, readable
//     paths, and images used in code typed into the page work too.

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

// All files under `dir` (recursively), as paths relative to resources/public
// with forward slashes, e.g. "images/logo-dna.svg".
function listFiles(dir) {
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
    const full = path.join(dir, entry.name);
    return entry.isDirectory() ? listFiles(full) : [full];
  });
}

const imageFiles = listFiles(path.join(publicDir, 'images'))
  .filter(f => mimeTypes[path.extname(f).toLowerCase()]?.startsWith('image/'));
const embeddedImages = Object.fromEntries(
  imageFiles.map(f => [path.relative(publicDir, f).split(path.sep).join('/'), dataUri(f)]));

// Runs before the app starts. Whenever an <img> appears (or its src changes)
// with a src like "images/logo-dna.svg", swap in the embedded copy.
const imageSwapScript = `<script>
(function () {
  var images = ${JSON.stringify(embeddedImages)};
  function fix(img) {
    var src = img.getAttribute('src') || '';
    if (src.indexOf('./') === 0) src = src.slice(2);
    else if (src.indexOf('/') === 0) src = src.slice(1);
    if (images[src]) img.setAttribute('src', images[src]);
  }
  function fixAll(node) {
    if (node.nodeType !== 1) return;
    if (node.tagName === 'IMG') fix(node);
    node.querySelectorAll('img').forEach(fix);
  }
  new MutationObserver(function (mutations) {
    mutations.forEach(function (m) {
      if (m.type === 'attributes') fix(m.target);
      else m.addedNodes.forEach(fixAll);
    });
  }).observe(document.documentElement,
             { subtree: true, childList: true, attributes: true, attributeFilter: ['src'] });
})();
</script>`;

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

// Inline the favicon(s)
html = html.replace(/<link\s+rel="icon"([^>]*)href="([^"]+)"([^>]*)>/g, (match, before, href, after) => {
  const file = path.join(publicDir, href);
  return fs.existsSync(file) ? `<link rel="icon"${before}href="${dataUri(file)}"${after}>` : match;
});

// Inline the compiled JavaScript, after the image-swapping script.
// "</script" inside the code would end the <script> tag early, so escape it.
const js = fs.readFileSync(jsFile, 'utf8');
const safeJs = js.replace(/<\/script/gi, '<\\/script');
html = html.replace(
  /<script src="js\/main\.js"[^>]*><\/script>/,
  () => `${imageSwapScript}\n<script>\n${safeJs}\n</script>`);

fs.mkdirSync(path.dirname(outFile), { recursive: true });
fs.writeFileSync(outFile, html);

const sizeKb = Math.round(fs.statSync(outFile).size / 1024);
console.log(`Wrote ${outFile} (${sizeKb} KB)`);
console.log(`Embedded ${imageFiles.length} image(s): ${Object.keys(embeddedImages).sort().join(', ') || 'none'}`);
