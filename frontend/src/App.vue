<template>
  <main class="app-shell">
    <!-- ── Top bar ───────────────────────────────────────────── -->
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark" aria-hidden="true"></span>
        <span class="brand-name">Stockify<span class="brand-accent">AI</span></span>
      </div>
    </header>

    <!-- ── Hero ──────────────────────────────────────────────── -->
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">Equity intelligence engine</p>
        <h1>On-demand equity <em>sentiment</em> &amp; price forecasting.</h1>
        <p class="subtitle">
          Analyze the S&P 500 technology and tech-adjacent universe using recent company coverage, Diffbot sentiment labels, daily market prices, and a local next-close prediction model.
        </p>
      </div>
      <div class="hero-side">
        <div class="hero-metric">
          <span>Selected tickers</span>
          <strong>{{ selectedTickers.length }}<i>active</i></strong>
        </div>
      </div>
    </section>

    <!-- ── Run panel ─────────────────────────────────────────── -->
    <section class="panel ingest-panel reveal">
      <div class="section-header">
        <div>
          <p class="eyebrow">Analysis run</p>
          <h2>Run a fresh market scan</h2>
          <p class="muted">Select from the fixed S&P 500 technology universe, refresh article coverage and price history, then update forecasts.</p>
        </div>
        <button class="primary" @click="runLiveIngest" :disabled="loading || selectedTickers.length === 0">
          <span class="btn-dot" :class="{ live: !loading }"></span>
          {{ loading ? 'Running analysis…' : 'Run analysis' }}
        </button>
      </div>

      <div class="controls-grid">
        <label class="field">
          <span>Lookback window</span>
          <select v-model.number="daysBack">
            <option :value="7">Last 7 days</option>
            <option :value="14">Last 14 days</option>
            <option :value="30">Last 30 days</option>
            <option :value="60">Last 60 days</option>
            <option :value="90">Last 90 days</option>
          </select>
        </label>
        <label class="field">
          <span>Articles per ticker</span>
          <input type="number" min="1" max="25" v-model.number="limit" />
        </label>
        <label class="field toggle">
          <input type="checkbox" v-model="reset" />
          <span class="toggle-box"></span>
          <span class="toggle-label">Reset database before run</span>
        </label>
        <label class="field toggle">
          <input type="checkbox" v-model="enrich" />
          <span class="toggle-box"></span>
          <span class="toggle-label">Extract article sentiment</span>
        </label>
      </div>

      <div class="ticker-actions">
        <button class="ghost" @click="selectMegaCap">Default 10</button>
        <button class="ghost" @click="selectCoreSoftware">Software & semis</button>
        <button class="ghost" @click="selectAll">All {{ stockConfig.length }}</button>
        <button class="ghost" @click="selectedTickers=[]">Clear</button>
      </div>

      <div class="ticker-filter">
        <input v-model="tickerQuery" placeholder="Filter the 101-stock universe" />
        <span>{{ displayedStockConfig.length }} shown · {{ selectedTickers.length }} selected</span>
      </div>

      <div class="ticker-grid">
        <label v-for="s in displayedStockConfig" :key="s.ticker" :class="['ticker-pill', selectedTickers.includes(s.ticker) && 'active']">
          <input type="checkbox" :value="s.ticker" v-model="selectedTickers" />
          <span class="ticker-sym">{{ s.ticker }}</span>
          <small>{{ s.company }}</small>
        </label>
      </div>

      <transition name="fade">
        <div v-if="runStatus" class="run-feedback" :class="runStatus.type">
          <div class="run-status">
            <span class="run-status-dot"></span>{{ runStatus.text }}
          </div>
          <ul v-if="runSummary.length" class="run-summary">
            <li v-for="line in runSummary" :key="line">{{ line }}</li>
          </ul>
          <ul v-if="runWarnings.length" class="run-warnings">
            <li v-for="(w, i) in runWarnings" :key="i">{{ w }}</li>
          </ul>
        </div>
      </transition>
      <details v-if="lastResult" class="run-details">
        <summary>Run details</summary>
        <pre>{{ JSON.stringify(lastResult, null, 2) }}</pre>
      </details>
    </section>

    <!-- ── Toolbar ───────────────────────────────────────────── -->
    <section class="toolbar panel reveal">
      <div class="search">
        <span class="search-icon" aria-hidden="true"></span>
        <input v-model="query" placeholder="Search ticker, company, or sector" />
      </div>
      <select v-model="sortBy">
        <option value="prediction">Sort by predicted return</option>
        <option value="sentiment">Sort by sentiment</option>
        <option value="articles">Sort by article count</option>
        <option value="ticker">Sort by ticker</option>
      </select>
    </section>

    <!-- ── Workspace ─────────────────────────────────────────── -->
    <section class="workspace">
      <aside class="panel company-list reveal">
        <div class="list-head">
          <h3>Companies</h3>
          <span>{{ filteredStocks.length }} shown</span>
        </div>
        <button v-for="s in filteredStocks" :key="s.ticker" :class="['stock-row', selected?.ticker === s.ticker && 'selected']" @click="chooseStock(s.ticker)">
          <div class="row-main">
            <strong class="ticker-sym">{{ s.ticker }}</strong>
            <small>{{ s.company }}</small>
          </div>
          <div class="row-meta">
            <span class="num" :class="tone(s.predicted_return)">{{ percent(s.predicted_return) }}</span>
            <small>{{ s.article_count }} articles</small>
          </div>
        </button>
        <div v-if="filteredStocks.length === 0" class="empty">No companies match that search.</div>
      </aside>

      <section class="panel detail reveal" v-if="selected">
        <div class="detail-head">
          <div>
            <p class="eyebrow">{{ selected.sector }}</p>
            <h2><span class="ticker-sym">{{ selected.ticker }}</span> · {{ selected.company }}</h2>
            <p class="muted">
              {{ selected.article_count }} articles analyzed
              <span v-if="selected.latest_date"> · latest sentiment {{ formatDate(selected.latest_date) }}</span>
            </p>
          </div>
          <button class="ghost fav" :class="{ active: favorites.includes(selected.ticker) }" @click="toggleFavorite(selected.ticker)">
            <span class="fav-star" aria-hidden="true"></span>
            {{ favorites.includes(selected.ticker) ? 'Saved' : 'Save favorite' }}
          </button>
        </div>

        <div class="mini-stats">
          <div class="metric-card">
            <span>Latest close</span>
            <strong class="num">{{ money(selected.latest_price) }}</strong>
            <small>{{ formatDate(selected.price_date) }}</small>
          </div>
          <div class="metric-card">
            <span>Predicted next close</span>
            <strong class="num" :class="tone(selected.predicted_return)">{{ money(selected.predicted_price) }}</strong>
            <small>
              <span class="num" :class="tone(selected.predicted_return)">{{ percent(selected.predicted_return) }}</span>
              · {{ modelLabel(selected.prediction_model) }}
            </small>
          </div>
          <div class="metric-card">
            <span>Diffbot sentiment</span>
            <strong class="num" :class="tone(selected.diffbot_sentiment)">{{ fmt(selected.diffbot_sentiment) }}</strong>
            <small>{{ selected.latest_date ? formatDate(selected.latest_date) : 'Aggregate label' }}</small>
          </div>
          <div class="metric-card">
            <span>Favorite</span>
            <strong>{{ favorites.includes(selected.ticker) ? 'Yes' : 'No' }}</strong>
            <small>Saved locally</small>
          </div>
        </div>

        <div class="insight-card">
          <span class="insight-tag">Methodology</span>
          <p>{{ METHODOLOGY }}</p>
        </div>

        <div class="chart-card">
          <div class="chart-head">
            <h3>Sentiment trend</h3>
            <span>Diffbot labels by article date</span>
          </div>
          <apexchart height="320" type="area" :options="sentimentChartOptions" :series="sentimentChartSeries" />
        </div>

        <div class="chart-card">
          <div class="chart-head">
            <h3>Daily close</h3>
            <span>Recent market price history</span>
          </div>
          <apexchart height="320" type="line" :options="priceChartOptions" :series="priceChartSeries" />
        </div>

        <div class="articles-head">
          <h3>Recent analyzed articles</h3>
          <small>Article sentiment is used as a model feature</small>
        </div>
        <article class="article" v-for="a in articles" :key="a.id">
          <div class="article-copy">
            <small class="article-meta">{{ a.source }} · {{ labelName(a.label_source) }} · {{ formatDate(a.published_at) }}</small>
            <a :href="a.url" target="_blank" rel="noreferrer"><h4>{{ a.title }}</h4></a>
            <p>{{ a.summary }}</p>
          </div>
          <div class="score">
            <strong class="num" :class="tone(a.diffbot_sentiment)">{{ fmt(a.diffbot_sentiment) }}</strong>
            <span>Diffbot</span>
          </div>
        </article>
        <div v-if="articles.length === 0" class="empty">No articles for this ticker yet. Run an analysis above.</div>
      </section>
    </section>

    <!-- ── Footer ────────────────────────────────────────────── -->
    <footer class="app-footer">
      <span class="brand-mark small" aria-hidden="true"></span>
      <p>Research prototype. Forecasts are informational and not financial advice.</p>
    </footer>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';

const API = 'http://localhost:8000/api';
const stockConfig = ref([]), stocks = ref([]), summary = ref({}), selected = ref(null), history = ref([]), prices = ref([]), articles = ref([]);
const selectedTickers = ref(['AAPL','MSFT','NVDA','GOOGL','AMZN']);
const tickerQuery = ref('');
const daysBack = ref(90), limit = ref(5), reset = ref(false), enrich = ref(true), loading = ref(false), lastResult = ref(null);
const runStatus = ref(null), runSummary = ref([]), runWarnings = ref([]);
const query = ref(''), sortBy = ref('prediction');
const favorites = ref(JSON.parse(localStorage.getItem('stockify:favorites') || '[]'));

async function request(path, options={}) {
  const res = await fetch(`${API}${path}`, { headers: { 'Content-Type': 'application/json' }, ...options });
  if (!res.ok) {
    let detail = res.statusText;
    try { detail = (await res.json()).detail || detail; } catch {}
    throw new Error(detail);
  }
  return res.json();
}
async function loadConfig(){ stockConfig.value = await request('/config/stocks'); }
async function loadDashboard(){ summary.value = await request('/market/summary'); stocks.value = await request('/stocks'); if(!selected.value && stocks.value.length) await chooseStock(stocks.value[0].ticker); }
async function chooseStock(ticker){ selected.value = await request(`/stocks/${ticker}`); history.value = await request(`/stocks/${ticker}/history`); prices.value = await request(`/stocks/${ticker}/prices`); articles.value = await request(`/stocks/${ticker}/articles?limit=30`); }
function pick(obj, keys){ for(const k of keys){ if(obj && obj[k] !== null && obj[k] !== undefined) return obj[k]; } return null; }
function warningText(e){
  if(typeof e === 'string') return e;
  if(e && typeof e === 'object'){
    const ticker = e.ticker || e.symbol;
    const msg = e.message || e.detail || e.error || e.reason || '';
    if(ticker && msg) return `${ticker}: ${msg}`;
    if(ticker) return `${ticker} returned no new articles from TechCrunch.`;
    if(msg) return msg;
  }
  return String(e);
}
async function runLiveIngest(){
  loading.value = true; runStatus.value = null; runSummary.value = []; runWarnings.value = []; lastResult.value = null;
  try {
    const body = { tickers: selectedTickers.value, limit: limit.value, days_back: daysBack.value, reset: reset.value, enrich: enrich.value, source: 'techcrunch' };
    const result = await request('/ingest/live', { method:'POST', body: JSON.stringify(body) });
    lastResult.value = result;
    await loadDashboard();
    if(selected.value) await chooseStock(selected.value.ticker);

    // Build clean, human summary lines (only show what we actually have)
    const lines = [];
    const articlesN = pick(result, ['articles_saved','articles_indexed','articles']);
    if(articlesN !== null) lines.push(`${articlesN} articles indexed`);
    const pricesN = pick(result, ['prices_refreshed','prices_saved','price_rows','daily_prices']) ?? summary.value.daily_prices;
    if(pricesN !== null && pricesN !== undefined) lines.push(`${pricesN} price rows refreshed`);
    const forecastsN = pick(result, ['forecasts_generated','forecasts','predictions_generated','predictions']) ?? forecastCount.value;
    if(forecastsN !== null && forecastsN !== undefined) lines.push(`${forecastsN} forecasts generated`);
    runSummary.value = lines;

    // Warnings are informational, never alarming
    const errs = Array.isArray(result.errors) ? result.errors : [];
    runWarnings.value = errs.map(warningText);
    runStatus.value = errs.length
      ? { type:'warn', text: errs.length === 1 ? 'Completed with 1 warning' : `Completed with ${errs.length} warnings` }
      : { type:'ok', text:'Completed successfully' };
  } catch(e) {
    // Only a full-run failure surfaces as an error
    runStatus.value = { type:'bad', text:`Run failed — ${e.message}` };
  } finally { loading.value = false; }
}
function selectAll(){ selectedTickers.value = stockConfig.value.map(s=>s.ticker); }
function selectMegaCap(){ selectedTickers.value = ['AAPL','MSFT','NVDA','GOOGL','AMZN','META','TSLA','AMD','NFLX','CRM']; }
function selectCoreSoftware(){ selectedTickers.value = ['MSFT','ADBE','CRM','ORCL','NOW','INTU','PANW','CRWD','FTNT','ZS','NVDA','AVGO','AMD','QCOM','INTC']; }
function toggleFavorite(t){ favorites.value = favorites.value.includes(t) ? favorites.value.filter(x=>x!==t) : [...favorites.value,t]; localStorage.setItem('stockify:favorites', JSON.stringify(favorites.value)); }
function fmt(v){ return v === null || v === undefined ? '—' : `${v >= 0 ? '+' : ''}${Number(v).toFixed(2)}`; }
function percent(v){ return v === null || v === undefined ? '—' : `${v >= 0 ? '+' : ''}${(Number(v)*100).toFixed(2)}%`; }
function money(v){ return v === null || v === undefined ? '—' : `$${Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`; }
function tone(v){ if(v === null || v === undefined) return 'neutral'; return v > 0.005 ? 'pos' : v < -0.005 ? 'neg' : 'neutral'; }
// Single reusable date helper → always renders like "Feb 11, 2026".
// Handles ISO ("2026-02-11"), ISO+time, and RFC strings ("Wed, 11 Feb 2026 00:00:00 GMT").
function formatDate(value){
  if(value === null || value === undefined || value === '') return '—';
  const raw = String(value).trim();
  const iso = raw.match(/^(\d{4}-\d{2}-\d{2})/);          // pin date-only to local midnight (no TZ drift)
  const d = iso ? new Date(`${iso[1]}T00:00:00`) : new Date(raw);
  if(Number.isNaN(d.getTime())) return raw;
  return d.toLocaleDateString('en-US', { month:'short', day:'numeric', year:'numeric' });
}
function labelName(v){
  if(!v) return 'Unlabeled';
  return String(v).toLowerCase().includes('diffbot') ? 'Labeled by Diffbot' : 'Labeled';
}
function modelLabel(v){
  if(!v) return 'Conservative baseline';
  return String(v).includes('baseline') ? 'Baseline forecast' : 'Trained price model';
}
const METHODOLOGY = 'StockifyAI combines recent close price, volume, short-term returns, article count, and Diffbot sentiment. When aligned training history is limited, it falls back to a conservative baseline forecast.';
const forecastCount = computed(()=> stocks.value.filter(s => (s.predicted_price ?? s.predicted_return) !== null && (s.predicted_price ?? s.predicted_return) !== undefined).length);
const displayedStockConfig = computed(()=>{
  const q = tickerQuery.value.trim().toLowerCase();
  if(!q) return stockConfig.value;
  return stockConfig.value.filter(s => [s.ticker, s.company, s.sector].join(' ').toLowerCase().includes(q));
});
const filteredStocks = computed(()=>{
  const q=query.value.toLowerCase();
  let rows = stocks.value.filter(s => [s.ticker,s.company,s.sector].join(' ').toLowerCase().includes(q));
  if(sortBy.value==='prediction') rows.sort((a,b)=>(b.predicted_return ?? -999)-(a.predicted_return ?? -999));
  if(sortBy.value==='sentiment') rows.sort((a,b)=>(b.diffbot_sentiment ?? b.sentiment ?? -999)-(a.diffbot_sentiment ?? a.sentiment ?? -999));
  if(sortBy.value==='articles') rows.sort((a,b)=>b.article_count-a.article_count);
  if(sortBy.value==='ticker') rows.sort((a,b)=>a.ticker.localeCompare(b.ticker));
  return rows;
});

/* ── Chart theming (dark "quant terminal") ─────────────────── */
const CHART_FONT = "'JetBrains Mono', ui-monospace, monospace";
const AXIS_COLOR = '#7c879b';
const GRID_COLOR = 'rgba(148,163,184,0.12)';

// Axis labels show a short "Mon D" form; the full date stays in the tooltip.
const axisDate = (v) => String(v).split(',')[0];

const sentimentChartSeries = computed(()=>[{ name:'Diffbot sentiment', data: history.value.map(h=>Number(h.diffbot_sentiment ?? 0)) }]);
const sentimentChartOptions = computed(()=>({
  chart:{ toolbar:{show:false}, animations:{enabled:true}, background:'transparent', fontFamily:CHART_FONT, parentHeightOffset:0 },
  theme:{ mode:'dark' },
  colors:['#f5b841'],
  dataLabels:{enabled:false},
  stroke:{curve:'smooth', width:2.5},
  fill:{ type:'gradient', gradient:{ shadeIntensity:1, opacityFrom:0.32, opacityTo:0.02, stops:[0,95] } },
  grid:{ borderColor:GRID_COLOR, strokeDashArray:4, padding:{ bottom:12, left:6, right:12 } },
  markers:{ size:0, hover:{ size:5 } },
  xaxis:{
    categories:history.value.map(h=>formatDate(h.date)),
    tickAmount:7,
    labels:{ rotate:-40, rotateAlways:true, hideOverlappingLabels:true, trim:false, formatter:axisDate, style:{colors:AXIS_COLOR, fontSize:'11px'} },
    axisBorder:{show:false}, axisTicks:{color:GRID_COLOR}
  },
  yaxis:{ min:-1, max:1, tickAmount:4, decimalsInFloat:1, labels:{formatter:(v)=>Number(v).toFixed(1), style:{colors:AXIS_COLOR}} },
  tooltip:{ theme:'dark', y:{formatter:(v)=>Number(v).toFixed(2)} }
}));

const priceChartSeries = computed(()=>[{ name:'Daily close', data: prices.value.map(p=>Number(p.close ?? 0)) }]);
const priceChartOptions = computed(()=>({
  chart:{ toolbar:{show:false}, animations:{enabled:true}, background:'transparent', fontFamily:CHART_FONT, parentHeightOffset:0 },
  theme:{ mode:'dark' },
  colors:['#79c0ff'],
  dataLabels:{enabled:false},
  stroke:{curve:'smooth', width:2.5},
  grid:{ borderColor:GRID_COLOR, strokeDashArray:4, padding:{ bottom:12, left:6, right:12 } },
  markers:{ size:0, hover:{ size:5 } },
  xaxis:{
    categories:prices.value.map(p=>formatDate(p.date)), tickAmount:8,
    labels:{ rotate:-40, rotateAlways:true, hideOverlappingLabels:true, trim:false, formatter:axisDate, style:{colors:AXIS_COLOR, fontSize:'11px'} },
    axisBorder:{show:false}, axisTicks:{color:GRID_COLOR}
  },
  yaxis:{ forceNiceScale:true, decimalsInFloat:0, labels:{formatter:(v)=>`$${Math.round(Number(v)).toLocaleString()}`, style:{colors:AXIS_COLOR}} },
  tooltip:{ theme:'dark', y:{formatter:(v)=>money(v)} }
}));
onMounted(async()=>{ await loadConfig(); await loadDashboard(); });
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Fraunces:ital,opsz,wght@0,9..144,400;0,9..144,500;0,9..144,600;1,9..144,500&family=Manrope:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600;700&display=swap');

:global(:root){
  --bg: #06080f;
  --panel: rgba(19,25,40,0.66);
  --panel-2: rgba(15,20,33,0.85);
  --border: rgba(148,163,184,0.13);
  --border-strong: rgba(148,163,184,0.26);
  --text: #eef2f9;
  --text-dim: #9aa6bd;
  --text-faint: #6b7691;
  --accent: #f5b841;
  --accent-2: #ffcf6b;
  --accent-soft: rgba(245,184,65,0.13);
  --pos: #35d399;
  --neg: #fb7185;
  --shadow: 0 28px 64px rgba(0,0,0,0.5);
  --r-lg: 22px;
  --r-md: 16px;
  --r-sm: 12px;
  --sans: 'Manrope', ui-sans-serif, system-ui, -apple-system, sans-serif;
  --serif: 'Fraunces', Georgia, 'Times New Roman', serif;
  --mono: 'JetBrains Mono', ui-monospace, 'SFMono-Regular', monospace;
}

:global(body){
  margin: 0;
  background: var(--bg);
  background-image:
    radial-gradient(900px 540px at 88% -8%, rgba(245,184,65,0.10), transparent 60%),
    radial-gradient(820px 600px at 6% 4%, rgba(56,98,168,0.16), transparent 62%),
    radial-gradient(700px 700px at 100% 100%, rgba(53,211,153,0.06), transparent 60%);
  background-attachment: fixed;
  color: var(--text);
  font-family: var(--sans);
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
}
:global(*) { box-sizing: border-box; }
:global(button), :global(input), :global(select) { font: inherit; }

.app-shell {
  position: relative;
  max-width: 1440px;
  margin: 0 auto;
  padding: 28px clamp(16px, 3vw, 40px) 64px;
}
/* fine grain overlay for depth */
.app-shell::before {
  content: "";
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.035;
  mix-blend-mode: soft-light;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}
.app-shell > * { position: relative; z-index: 1; }

/* shared panel surface */
.panel {
  background: linear-gradient(180deg, var(--panel), var(--panel-2));
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow);
  backdrop-filter: blur(14px);
}

/* entrance animation */
.reveal { animation: rise 0.6s cubic-bezier(0.22, 1, 0.36, 1) both; }
.app-shell > .reveal:nth-of-type(1) { animation-delay: 0.04s; }
.app-shell > .reveal:nth-of-type(2) { animation-delay: 0.10s; }
.app-shell > .reveal:nth-of-type(3) { animation-delay: 0.16s; }
.app-shell .workspace .reveal:nth-of-type(1) { animation-delay: 0.20s; }
.app-shell .workspace .reveal:nth-of-type(2) { animation-delay: 0.26s; }
@keyframes rise { from { opacity: 0; transform: translateY(14px); } to { opacity: 1; transform: none; } }

/* ── Top bar ───────────────────────────────────────────────── */
.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 4px 4px 22px;
  flex-wrap: wrap;
}
.brand { display: flex; align-items: center; gap: 12px; }
.brand-mark {
  width: 26px; height: 26px;
  border-radius: 8px;
  background: linear-gradient(140deg, var(--accent), #c9842b);
  box-shadow: 0 0 22px rgba(245,184,65,0.45), inset 0 1px 0 rgba(255,255,255,0.4);
  transform: rotate(45deg);
}
.brand-name {
  font-family: var(--serif);
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.01em;
}
.brand-accent { color: var(--accent); }
.topbar-chips { display: flex; gap: 10px; flex-wrap: wrap; }
.chip {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 7px 13px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-dim);
  background: rgba(255,255,255,0.03);
  border: 1px solid var(--border);
  border-radius: 999px;
}
.chip-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: var(--pos);
  box-shadow: 0 0 0 0 rgba(53,211,153,0.55);
  animation: pulse 2.2s infinite;
}
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(53,211,153,0.5); }
  70% { box-shadow: 0 0 0 7px rgba(53,211,153,0); }
  100% { box-shadow: 0 0 0 0 rgba(53,211,153,0); }
}

/* ── Hero ──────────────────────────────────────────────────── */
.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 40px;
  padding: 8px 4px 30px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 26px;
  animation: rise 0.6s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.eyebrow {
  margin: 0 0 12px;
  color: var(--accent);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.32em;
  text-transform: uppercase;
}
h1, h2, h3, h4, p { margin-top: 0; }
h1 {
  margin: 0 0 18px;
  font-family: var(--serif);
  font-weight: 500;
  font-size: clamp(36px, 4.4vw, 62px);
  line-height: 1.0;
  letter-spacing: -0.025em;
  color: #fbfcff;
  max-width: 14ch;
}
h1 em {
  font-style: italic;
  color: var(--accent);
  font-weight: 500;
}
.subtitle {
  margin: 0;
  max-width: 56ch;
  color: var(--text-dim);
  font-size: 16px;
  line-height: 1.62;
}
.hero-side { display: grid; gap: 12px; min-width: 230px; }
.hero-metric {
  padding: 16px 18px;
  border-radius: var(--r-md);
  background: rgba(255,255,255,0.02);
  border: 1px solid var(--border);
}
.hero-metric span { display: block; color: var(--text-faint); font-size: 12px; font-weight: 600; letter-spacing: 0.04em; text-transform: uppercase; }
.hero-metric strong {
  display: flex; align-items: baseline; gap: 7px;
  margin-top: 6px;
  font-family: var(--mono);
  font-size: 32px;
  font-weight: 600;
  letter-spacing: -0.02em;
}
.hero-metric i { font-style: normal; font-size: 13px; color: var(--text-faint); font-family: var(--sans); font-weight: 600; }

/* ── Section headers ───────────────────────────────────────── */
.section-header, .detail-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 22px; }
h2 { font-family: var(--serif); font-weight: 500; font-size: 25px; letter-spacing: -0.02em; margin-bottom: 8px; }
h3 { font-size: 17px; font-weight: 700; letter-spacing: -0.01em; margin-bottom: 0; }
h4 { font-size: 16px; line-height: 1.35; font-weight: 700; margin: 6px 0 9px; color: var(--text); }
.muted { margin: 0; color: var(--text-dim); font-size: 14.5px; line-height: 1.55; }

/* numbers everywhere use tabular mono */
.num { font-family: var(--mono); font-variant-numeric: tabular-nums; letter-spacing: -0.01em; }
.ticker-sym { font-family: var(--mono); font-weight: 700; letter-spacing: 0.02em; }

/* ── Buttons ───────────────────────────────────────────────── */
button {
  border: 0;
  border-radius: 999px;
  padding: 12px 20px;
  cursor: pointer;
  font-weight: 700;
  font-size: 14px;
  transition: transform 0.16s ease, background 0.16s ease, border-color 0.16s ease, box-shadow 0.16s ease, color 0.16s ease;
}
button:hover:not(:disabled) { transform: translateY(-1px); }
button:disabled { cursor: not-allowed; opacity: 0.5; }
.primary {
  display: inline-flex; align-items: center; gap: 9px;
  background: linear-gradient(180deg, var(--accent-2), var(--accent));
  color: #1a1206;
  min-width: 168px;
  justify-content: center;
  box-shadow: 0 10px 26px rgba(245,184,65,0.32);
}
.primary:hover:not(:disabled) { box-shadow: 0 14px 32px rgba(245,184,65,0.42); }
.btn-dot { width: 8px; height: 8px; border-radius: 50%; background: #1a1206; opacity: 0.55; }
.btn-dot.live { opacity: 1; animation: pulse-dark 1.8s infinite; }
@keyframes pulse-dark {
  0% { box-shadow: 0 0 0 0 rgba(26,18,6,0.5); }
  70% { box-shadow: 0 0 0 6px rgba(26,18,6,0); }
  100% { box-shadow: 0 0 0 0 rgba(26,18,6,0); }
}
.ghost {
  background: rgba(255,255,255,0.04);
  color: var(--text);
  border: 1px solid var(--border-strong);
  padding: 10px 16px;
}
.ghost:hover:not(:disabled) { background: rgba(255,255,255,0.08); border-color: var(--accent); }
.fav { display: inline-flex; align-items: center; gap: 8px; white-space: nowrap; }
.fav.active { background: var(--accent-soft); border-color: var(--accent); color: var(--accent-2); }
.fav-star { width: 13px; height: 13px; background: currentColor; clip-path: polygon(50% 0,61% 35%,98% 35%,68% 57%,79% 91%,50% 70%,21% 91%,32% 57%,2% 35%,39% 35%); opacity: 0.85; }

/* ── Run panel ─────────────────────────────────────────────── */
.ingest-panel { padding: 26px; margin-bottom: 22px; }
.controls-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-top: 24px; }
.field {
  padding: 13px 15px;
  background: rgba(255,255,255,0.025);
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  color: var(--text-dim);
  font-size: 12px;
  font-weight: 600;
  transition: border-color 0.15s ease;
}
.field:focus-within { border-color: var(--accent); }
.field > span { display: block; margin-bottom: 8px; letter-spacing: 0.04em; text-transform: uppercase; color: var(--text-faint); }
.field select, .field input[type="number"] {
  width: 100%; border: 0; outline: none; background: transparent;
  color: var(--text); font-size: 15px; font-weight: 600;
  font-family: var(--mono);
}
.field select option { background: #121826; color: var(--text); }

/* custom toggle */
.toggle { display: flex; align-items: center; gap: 11px; cursor: pointer; }
.toggle input { position: absolute; opacity: 0; width: 0; height: 0; }
.toggle-box {
  flex: 0 0 auto;
  width: 38px; height: 22px; border-radius: 999px;
  background: rgba(255,255,255,0.08);
  border: 1px solid var(--border-strong);
  position: relative; transition: background 0.18s ease, border-color 0.18s ease;
}
.toggle-box::after {
  content: ""; position: absolute; top: 2px; left: 2px;
  width: 16px; height: 16px; border-radius: 50%;
  background: var(--text-dim); transition: transform 0.18s ease, background 0.18s ease;
}
.toggle input:checked + .toggle-box { background: var(--accent-soft); border-color: var(--accent); }
.toggle input:checked + .toggle-box::after { transform: translateX(16px); background: var(--accent); }
.toggle-label { text-transform: none; letter-spacing: 0; font-size: 13px; color: var(--text); font-weight: 600; }

.ticker-actions { display: flex; gap: 10px; margin: 20px 0 14px; flex-wrap: wrap; }
.ticker-filter { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.ticker-filter input { flex: 1; min-width: 220px; border: 1px solid var(--border); border-radius: 14px; background: rgba(15,23,42,0.7); color: var(--text); padding: 13px 14px; outline: none; font-family: var(--sans); }
.ticker-filter span { color: var(--text-faint); font-size: 12px; font-weight: 700; white-space: nowrap; }
.ticker-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 10px; max-height: 360px; overflow: auto; padding-right: 4px; }
.ticker-pill {
  display: block; padding: 12px 14px; border-radius: var(--r-md);
  border: 1px solid var(--border);
  background: rgba(255,255,255,0.02);
  cursor: pointer; transition: border-color 0.15s ease, background 0.15s ease, transform 0.15s ease;
}
.ticker-pill:hover { transform: translateY(-1px); border-color: var(--border-strong); }
.ticker-pill.active { border-color: var(--accent); background: var(--accent-soft); }
.ticker-pill input { display: none; }
.ticker-pill .ticker-sym { display: block; font-size: 15px; color: var(--text); }
.ticker-pill.active .ticker-sym { color: var(--accent-2); }
.ticker-pill small { display: block; margin-top: 2px; color: var(--text-faint); font-size: 11.5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.run-feedback {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: var(--r-md);
  border: 1px solid transparent;
  background: rgba(255,255,255,0.025);
}
.run-feedback.ok { background: rgba(53,211,153,0.08); border-color: rgba(53,211,153,0.28); }
.run-feedback.warn { background: rgba(245,184,65,0.08); border-color: rgba(245,184,65,0.28); }
.run-feedback.bad { background: rgba(251,113,133,0.09); border-color: rgba(251,113,133,0.32); }
.run-status { display: flex; align-items: center; gap: 9px; font-weight: 700; font-size: 14.5px; }
.run-feedback.ok .run-status { color: #6ee7b7; }
.run-feedback.warn .run-status { color: var(--accent-2); }
.run-feedback.bad .run-status { color: #fda4af; }
.run-status-dot { width: 8px; height: 8px; border-radius: 50%; background: currentColor; flex: 0 0 auto; }
.run-summary {
  display: flex; flex-wrap: wrap; gap: 8px;
  list-style: none; margin: 12px 0 0; padding: 0;
}
.run-summary li {
  font-family: var(--mono);
  font-size: 12.5px; font-weight: 500;
  color: var(--text-dim);
  padding: 6px 12px;
  background: rgba(255,255,255,0.04);
  border: 1px solid var(--border);
  border-radius: 999px;
}
.run-warnings { list-style: none; margin: 12px 0 0; padding: 0; }
.run-warnings li {
  position: relative;
  padding: 4px 0 4px 18px;
  color: var(--text-dim);
  font-size: 13.5px;
  line-height: 1.5;
}
.run-warnings li::before {
  content: ""; position: absolute; left: 2px; top: 11px;
  width: 5px; height: 5px; border-radius: 50%;
  background: var(--accent); opacity: 0.7;
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.run-details { margin-top: 14px; color: var(--text-dim); }
.run-details summary { cursor: pointer; font-weight: 600; font-size: 13px; color: var(--text-faint); }
.run-details summary:hover { color: var(--text-dim); }
.run-details pre { white-space: pre-wrap; overflow: auto; max-height: 260px; margin-top: 10px; padding: 16px; border-radius: var(--r-md); background: #04060c; color: #c8d3e6; font-family: var(--mono); font-size: 12.5px; border: 1px solid var(--border); }

/* ── Stats ─────────────────────────────────────────────────── */
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 22px; }
.stat { padding: 20px; }
.stat span { display: block; color: var(--text-faint); font-size: 12px; font-weight: 600; letter-spacing: 0.04em; text-transform: uppercase; }
.stat strong { display: block; margin-top: 8px; font-size: 30px; font-weight: 600; color: #fbfcff; letter-spacing: -0.02em; }

/* ── Toolbar ───────────────────────────────────────────────── */
.toolbar { display: grid; grid-template-columns: 1fr 250px; gap: 12px; padding: 14px; margin-bottom: 22px; }
.search { position: relative; }
.search-icon {
  position: absolute; left: 16px; top: 50%; transform: translateY(-50%);
  width: 14px; height: 14px; border-radius: 50%;
  border: 2px solid var(--text-faint);
}
.search-icon::after { content: ""; position: absolute; right: -4px; bottom: -4px; width: 7px; height: 2px; background: var(--text-faint); transform: rotate(45deg); border-radius: 2px; }
.toolbar input, .toolbar select {
  width: 100%; padding: 14px 16px; outline: none;
  color: var(--text);
  background: rgba(255,255,255,0.03);
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  font-size: 14px; font-weight: 500;
  transition: border-color 0.15s ease;
}
.toolbar input { padding-left: 42px; }
.toolbar input:focus, .toolbar select:focus { border-color: var(--accent); }
.toolbar input::placeholder { color: var(--text-faint); }
.toolbar select option { background: #121826; }

/* ── Workspace ─────────────────────────────────────────────── */
.workspace { display: grid; grid-template-columns: 350px 1fr; gap: 20px; align-items: start; }
.company-list { padding: 16px; position: sticky; top: 20px; max-height: calc(100vh - 40px); overflow: auto; }
.list-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; padding: 0 6px; }
.list-head span { color: var(--text-faint); font-size: 12px; font-weight: 600; }
.stock-row {
  display: flex; justify-content: space-between; align-items: center; width: 100%;
  border-radius: var(--r-md); padding: 13px 14px; margin-bottom: 2px;
  background: transparent; text-align: left; position: relative;
  border: 1px solid transparent;
}
.stock-row:hover { background: rgba(255,255,255,0.035); }
.stock-row.selected { background: rgba(255,255,255,0.05); border-color: var(--border-strong); }
.stock-row.selected::before {
  content: ""; position: absolute; left: 0; top: 22%; bottom: 22%; width: 3px;
  border-radius: 3px; background: var(--accent);
}
.row-main strong { display: block; font-size: 15px; color: var(--text); }
.row-main small { color: var(--text-faint); font-size: 12px; }
.row-meta { text-align: right; }
.row-meta span { display: block; font-weight: 600; font-size: 14px; }
.row-meta small { color: var(--text-faint); font-size: 11.5px; }

/* ── Detail ────────────────────────────────────────────────── */
.detail { padding: 26px; overflow: hidden; }
.detail-head h2 { display: flex; flex-wrap: wrap; gap: 8px; align-items: baseline; }
.mini-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin: 24px 0; }
.metric-card {
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  background: rgba(255,255,255,0.025);
}
.metric-card span { display: block; color: var(--text-faint); font-size: 11.5px; font-weight: 600; letter-spacing: 0.04em; text-transform: uppercase; }
.metric-card strong { display: block; margin-top: 7px; font-size: 25px; font-weight: 600; letter-spacing: -0.02em; }
.metric-card small { display: block; margin-top: 5px; color: var(--text-faint); font-size: 12.5px; font-weight: 600; }

.insight-card {
  padding: 18px 20px; border-radius: var(--r-md);
  background: var(--accent-soft);
  border: 1px solid rgba(245,184,65,0.22);
  margin-bottom: 6px;
}
.insight-tag { display: inline-block; margin-bottom: 8px; color: var(--accent-2); font-size: 11px; font-weight: 700; letter-spacing: 0.16em; text-transform: uppercase; }
.insight-card p { margin: 0; color: var(--text-dim); line-height: 1.6; font-size: 14.5px; }

.chart-card { margin-top: 18px; padding: 20px; border: 1px solid var(--border); border-radius: var(--r-lg); background: rgba(255,255,255,0.02); }
.chart-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.chart-head span { color: var(--text-faint); font-size: 12.5px; }

.articles-head { display: flex; justify-content: space-between; align-items: center; margin: 30px 0 14px; }
.articles-head small { color: var(--text-faint); font-size: 12.5px; }
.article {
  display: grid; grid-template-columns: 1fr 120px; gap: 20px;
  padding: 18px; border: 1px solid var(--border);
  border-radius: var(--r-md); margin-bottom: 12px;
  background: rgba(255,255,255,0.02);
  transition: border-color 0.15s ease, transform 0.15s ease;
}
.article:hover { border-color: var(--border-strong); transform: translateY(-1px); }
.article a { text-decoration: none; }
.article a:hover h4 { color: var(--accent-2); }
.article-meta { display: block; margin-bottom: 4px; color: var(--text-faint); font-size: 12px; font-weight: 600; }
.article h4 { transition: color 0.15s ease; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.article-copy p { margin: 0; color: var(--text-dim); line-height: 1.6; font-size: 14px; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.score { text-align: right; border-left: 1px solid var(--border); padding-left: 18px; }
.score strong { display: block; font-size: 30px; font-weight: 600; letter-spacing: -0.03em; }
.score span { color: var(--text-faint); font-size: 12px; font-weight: 600; }

.empty { padding: 22px; color: var(--text-faint); border: 1px dashed var(--border-strong); border-radius: var(--r-md); font-size: 14px; text-align: center; }

.pos { color: var(--pos) !important; }
.neg { color: var(--neg) !important; }
.neutral { color: var(--text-dim) !important; }

/* scrollbars */
.company-list::-webkit-scrollbar, .run-details pre::-webkit-scrollbar { width: 8px; height: 8px; }
.company-list::-webkit-scrollbar-thumb, .run-details pre::-webkit-scrollbar-thumb { background: var(--border-strong); border-radius: 8px; }

/* ── Footer ────────────────────────────────────────────────── */
.app-footer {
  display: flex; align-items: center; gap: 10px;
  margin-top: 40px; padding-top: 22px;
  border-top: 1px solid var(--border);
}
.app-footer p { margin: 0; color: var(--text-faint); font-size: 12.5px; letter-spacing: 0.01em; }
.brand-mark.small { width: 14px; height: 14px; border-radius: 5px; box-shadow: 0 0 12px rgba(245,184,65,0.3); }

/* ── Responsive ────────────────────────────────────────────── */
@media (max-width: 1100px) {
  .hero, .section-header, .detail-head { flex-direction: column; align-items: stretch; }
  .hero-side { grid-template-columns: 1fr 1fr; }
  .controls-grid, .mini-stats, .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .workspace { grid-template-columns: 1fr; }
  .company-list { position: static; max-height: none; }
}
@media (max-width: 720px) {
  .controls-grid, .mini-stats, .stats-grid, .toolbar, .hero-side { grid-template-columns: 1fr; }
  .article { grid-template-columns: 1fr; }
  .score { text-align: left; border-left: 0; padding-left: 0; border-top: 1px solid var(--border); padding-top: 12px; }
  .primary { width: 100%; }
}
</style>