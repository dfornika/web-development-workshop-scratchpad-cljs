// Writes resources/public/data/example-qc-samples.csv: made-up QC results
// for SARS-CoV-2 samples, sequenced in weekly runs. Run it with
//
//     node scripts/make-example-data.mjs
//
// It always writes the same file (the random numbers come from a fixed
// seed). Things built in, for the Data Explorer's charts and replay:
//
//   - Lineages rise and fall over time: JN.1 fades, KP.2 and KP.3.1.1 have
//     their turn, then XEC and LP.8.1 take over.
//   - Reporting delay: each run sequences samples collected 1-5 weeks
//     earlier (some referral-lab samples even later). So while runs are
//     still arriving, the most recent weeks look quiet - they fill in as
//     later runs come in.
//   - QC follows the numbers: low depth, low Q30 or contamination fail
//     (and get no lineage: "Unassigned"); borderline ones get a WARN.

import { writeFileSync } from 'node:fs';

// mulberry32: a small random number generator with a seed
let seed = 20260304;
const random = () => {
  seed = (seed + 0x6d2b79f5) | 0;
  let t = seed;
  t = Math.imul(t ^ (t >>> 15), t | 1);
  t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};
const between = (lo, hi) => lo + random() * (hi - lo);
const normal = (mean, sd) => {
  const u = 1 - random(), v = random();
  return mean + sd * Math.sqrt(-2 * Math.log(u)) * Math.cos(2 * Math.PI * v);
};
const pick = weights => {
  const total = Object.values(weights).reduce((a, b) => a + b, 0);
  let r = random() * total;
  for (const [k, w] of Object.entries(weights)) { if ((r -= w) < 0) return k; }
  return Object.keys(weights).at(-1);
};

const DAY = 24 * 60 * 60 * 1000;
const day = s => new Date(s + 'T00:00:00Z').getTime();
const iso = t => new Date(t).toISOString().slice(0, 10);
// the Monday of t's week
const weekStart = t => t - ((new Date(t).getUTCDay() + 6) % 7) * DAY;

const firstRun = day('2026-03-04');   // a Wednesday
const runs = 16;

// How common each lineage is, `w` weeks after the first run's week
const lineageWeights = w => {
  const wave = (peak, width) => Math.exp(-((w - peak) ** 2) / (2 * width ** 2));
  return {
    'JN.1': 6 * wave(-6, 4),
    'KP.2': 5 * wave(2, 3),
    'KP.3.1.1': 5 * wave(7, 3),
    'XEC': 5 * wave(12, 3.5),
    'LP.8.1': 5 * wave(17, 3.5),
  };
};

// Days from collection to sequencing: mostly 1-3 weeks, sometimes longer
const delayDays = () => {
  if (random() < 0.08) return Math.round(between(30, 45));   // referral labs
  return Math.round(Math.max(4, normal(13, 5)));
};

const notesFor = (status, delay, contaminated) => {
  if (contaminated) return 'possible contamination, flagged by lab';
  if (delay > 28) return 'referral lab, late arrival';
  if (status === 'FAIL') return pick({ '': 3, 'low DNA, re-extracted': 2, 'label smudged, ID checked by "hand"': 1 });
  if (status === 'WARN') return pick({ '': 4, 're-run requested': 2, 'Ct value high (32)': 1 });
  return pick({ '': 30, 'Ct value high (32)': 1 });
};

const csvField = s => (/[",\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s);

const header = ['sample_id', 'run_id', 'collection_date', 'epi_week', 'sequenced_at', 'lineage',
                'total_reads', 'percent_q30', 'mean_depth', 'contaminated', 'qc_status', 'notes'];
const lines = [header.join(',')];
let n = 0;

for (let r = 0; r < runs; r++) {
  const runDay = firstRun + r * 7 * DAY;
  const runId = 'RUN_' + String(r + 1).padStart(3, '0');
  const size = Math.round(between(16, 24));
  const samples = [];
  for (let i = 0; i < size; i++) {
    const delay = delayDays();
    const collected = runDay - delay * DAY;
    const weeksIn = (collected - weekStart(firstRun)) / (7 * DAY);
    // (capped, so one huge sample doesn't squash every chart of it)
    const reads = Math.round(Math.min(4.5e6, Math.exp(normal(Math.log(1.1e6), 0.7))));
    const q30 = Math.min(98.5, normal(91.5, 3.5));
    const depth = reads * between(0.0028, 0.0042);
    const contaminated = random() < 0.03;
    const status = contaminated || depth < 1000 || q30 < 85 ? 'FAIL'
                 : depth < 2000 || q30 < 88 ? 'WARN'
                 : 'PASS';
    const lineage = status === 'FAIL' ? 'Unassigned' : pick(lineageWeights(weeksIn));
    // sequenced between 8am and 6pm on the run day
    const at = new Date(runDay + between(8, 18) * 60 * 60 * 1000).toISOString().slice(0, 19);
    samples.push({ collected, at, lineage, reads, q30, depth, contaminated, status, delay });
  }
  // sample IDs in the order they were logged: by collection date
  samples.sort((a, b) => a.collected - b.collected);
  for (const s of samples) {
    n++;
    lines.push([
      'S' + String(n).padStart(3, '0'), runId, iso(s.collected), iso(weekStart(s.collected)), s.at,
      s.lineage, String(s.reads), s.q30.toFixed(1), s.depth.toFixed(1), String(s.contaminated),
      s.status, notesFor(s.status, s.delay, s.contaminated),
    ].map(csvField).join(','));
  }
}

const out = new URL('../resources/public/data/example-qc-samples.csv', import.meta.url);
writeFileSync(out, lines.join('\n') + '\n');
console.log(`Wrote ${n} samples from ${runs} runs to ${out.pathname}`);
