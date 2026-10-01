#!/usr/bin/env bash
# Screenshots the "Elimika load test" Grafana dashboard for a time window, using Playwright (headless Chromium).
#
#   scripts/loadtest/screenshot.sh <from_epoch_ms> <to_epoch_ms> <out_dir>
#
# Writes into <out_dir>:
#   dashboard.png              the whole dashboard (tall viewport, every row expanded)
#   row-<nn>-<slug>.png        one image per dashboard row (Load, App, DB pool, JVM, Container, Postgres)
#   panel-<id>-<slug>.png      one image per panel, rendered through Grafana's /d-solo/ URL
#
# Env: GRAFANA_URL (default http://localhost:3001), DASH_UID (default elimika-loadtest),
#      WAIT_MS (extra settle time per page, default 4000), PLAYWRIGHT_VERSION (default 1.60.0).
# Playwright is installed once under build/loadtest/node (gitignored); Chromium is installed if missing.
set -euo pipefail

if [[ $# -ne 3 ]]; then
    echo "usage: $0 <from_epoch_ms> <to_epoch_ms> <out_dir>" >&2
    exit 2
fi
FROM="$1" TO="$2" OUT="$3"
[[ "$FROM" =~ ^[0-9]+$ && "$TO" =~ ^[0-9]+$ ]] || { echo "from/to must be epoch milliseconds" >&2; exit 2; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
GRAFANA_URL="${GRAFANA_URL:-http://localhost:3001}"
DASH_UID="${DASH_UID:-elimika-loadtest}"
WAIT_MS="${WAIT_MS:-4000}"
PW_VERSION="${PLAYWRIGHT_VERSION:-1.60.0}"
TOOLS="$ROOT/build/loadtest/node"

curl -sf "$GRAFANA_URL/api/health" >/dev/null || { echo "Grafana not reachable at $GRAFANA_URL (run scripts/loadtest/stack-up.sh)" >&2; exit 1; }

if [[ ! -f "$TOOLS/node_modules/playwright/package.json" ]] \
    || ! grep -q "\"version\": \"$PW_VERSION\"" "$TOOLS/node_modules/playwright/package.json"; then
    echo "[screenshot] installing playwright@$PW_VERSION into $TOOLS"
    mkdir -p "$TOOLS"
    npm install --silent --no-audit --no-fund --prefix "$TOOLS" "playwright@$PW_VERSION" >/dev/null
fi
"$TOOLS/node_modules/.bin/playwright" install chromium >/dev/null

mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"

cat >"$TOOLS/screenshot.cjs" <<'JS'
const { chromium } = require('playwright');
const [grafana, uid, from, to, out, waitMs] = process.argv.slice(2);
const wait = Number(waitMs);
const slug = (s) => s.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 60);
const q = `from=${from}&to=${to}&timezone=utc&theme=light`;

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1800, height: 1000 }, deviceScaleFactor: 1 });
  const page = await ctx.newPage();

  const meta = await (await page.request.get(`${grafana}/api/dashboards/uid/${uid}`)).json();
  const panels = meta.dashboard.panels;
  const bottom = Math.max(...panels.map((p) => p.gridPos.y + p.gridPos.h));
  // Grafana's grid is 30px per unit plus an 8px gutter; size the viewport so nothing needs scrolling.
  const height = bottom * 38 + 200;
  await page.setViewportSize({ width: 1800, height });

  await page.goto(`${grafana}/d/${uid}/?${q}&kiosk`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(wait);
  await page.screenshot({ path: `${out}/dashboard.png`, fullPage: true });
  console.log(`${out}/dashboard.png`);

  // One image per row: from a row header down to the next one, clipped from the dashboard page.
  const rows = panels.filter((p) => p.type === 'row');
  const boxes = [];
  for (const r of rows) {
    const el = page.getByText(r.title, { exact: true }).first();
    const box = (await el.count()) ? await el.boundingBox() : null;
    boxes.push(box);
  }
  for (let i = 0; i < rows.length; i++) {
    if (!boxes[i]) { console.error(`row not found on page: ${rows[i].title}`); continue; }
    const top = Math.max(0, boxes[i].y - 8);
    const next = boxes.slice(i + 1).find((b) => b);
    const end = next ? next.y - 8 : height;
    const file = `${out}/row-${String(i + 1).padStart(2, '0')}-${slug(rows[i].title)}.png`;
    await page.screenshot({ path: file, clip: { x: 0, y: top, width: 1800, height: Math.max(50, end - top) }, fullPage: true });
    console.log(file);
  }

  // One image per panel through /d-solo/.
  await page.setViewportSize({ width: 1200, height: 520 });
  for (const p of panels.filter((x) => x.type !== 'row')) {
    await page.goto(`${grafana}/d-solo/${uid}/?${q}&panelId=${p.id}`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(Math.min(wait, 2500));
    const file = `${out}/panel-${String(p.id).padStart(2, '0')}-${slug(p.title)}.png`;
    await page.screenshot({ path: file });
    console.log(file);
  }
  await browser.close();
})().catch((e) => { console.error(e); process.exit(1); });
JS

node "$TOOLS/screenshot.cjs" "$GRAFANA_URL" "$DASH_UID" "$FROM" "$TO" "$OUT" "$WAIT_MS"
