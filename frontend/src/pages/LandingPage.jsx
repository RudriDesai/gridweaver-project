import "./LandingPage.css";

const WAVE_POINTS =
  "0,90.0 6,100.5 12,110.0 18,117.5 24,122.3 30,124.0 36,122.3 42,117.5 48,110.0 54,100.5 60,90.0 66,79.5 72,70.0 78,62.5 84,57.7 90,56.0 96,57.7 102,62.5 108,70.0 114,79.5 120,90.0 126,100.5 132,110.0 138,117.5 144,122.3 150,124.0 156,122.3 162,117.5 168,110.0 174,100.5 180,90.0 186,79.5 192,70.0 198,62.5 204,57.7 210,56.0 216,57.7 222,62.5 228,70.0 234,79.5 240,90.0 246,100.5 252,110.0 258,117.5 264,122.3 270,124.0 276,122.3 282,117.5 288,110.0 294,100.5 300,90.0 306,79.5 312,70.0 318,62.5 324,57.7 330,56.0 336,57.7 342,62.5 348,70.0 354,79.5 360,90.0 366,100.5 372,110.0 378,117.5 384,122.3 390,124.0 396,122.3 402,117.5 408,110.0 414,100.5 420,90.0 426,79.5 432,70.0 438,62.5 444,57.7 450,56.0 456,57.7 462,62.5 468,70.0 474,79.5 480,90.0 486,100.5 492,110.0 498,117.5 504,122.3 510,124.0 516,122.3 522,117.5 528,110.0 534,100.5 540,90.0 546,79.5 552,70.0 558,62.5 564,57.7 570,56.0 576,57.7 582,62.5 588,70.0 594,79.5 600,90.0 606,100.5 612,110.0 618,117.5 624,122.3 630,124.0 636,122.3 642,117.5 648,110.0 654,100.5 660,90.0 666,79.5 672,70.0 678,62.5 684,57.7 690,56.0 696,57.7 702,62.5 708,70.0 714,79.5 720,90.0 726,100.5 732,110.0 738,117.5 744,122.3 750,124.0 756,122.3 762,117.5 768,110.0 774,100.5 780,90.0 786,79.5 792,70.0 798,62.5 804,57.7 810,56.0 816,57.7 822,62.5 828,70.0 834,79.5 840,90.0 846,100.5 852,110.0 858,117.5 864,122.3 870,124.0 876,122.3 882,117.5 888,110.0 894,100.5 900,90.0 906,79.5 912,70.0 918,62.5 924,57.7 930,56.0 936,57.7 942,62.5 948,70.0 954,79.5 960,90.0 966,100.5 972,110.0 978,117.5 984,122.3 990,124.0 996,122.3 1002,117.5 1008,110.0 1014,100.5 1020,90.0 1026,79.5 1032,70.0 1038,62.5 1044,57.7 1050,56.0 1056,57.7 1062,62.5 1068,70.0 1074,79.5 1080,90.0 1086,100.5 1092,110.0 1098,117.5 1104,122.3 1110,124.0 1116,122.3 1122,117.5 1128,110.0 1134,100.5 1140,90.0 1146,79.5 1152,70.0 1158,62.5 1164,57.7 1170,56.0 1176,57.7 1182,62.5 1188,70.0 1194,79.5 1200,90.0 1206,100.5 1212,110.0 1218,117.5 1224,122.3 1230,124.0 1236,122.3 1242,117.5 1248,110.0 1254,100.5 1260,90.0 1266,79.5 1272,70.0 1278,62.5 1284,57.7 1290,56.0 1296,57.7 1302,62.5 1308,70.0 1314,79.5 1320,90.0 1326,100.5 1332,110.0 1338,117.5 1344,122.3 1350,124.0 1356,122.3 1362,117.5 1368,110.0 1374,100.5 1380,90.0 1386,79.5 1392,70.0 1398,62.5 1404,57.7 1410,56.0 1416,57.7 1422,62.5 1428,70.0 1434,79.5 1440,90.0 1446,100.5 1452,110.0 1458,117.5 1464,122.3 1470,124.0 1476,122.3 1482,117.5 1488,110.0 1494,100.5 1500,90.0 1506,79.5 1512,70.0 1518,62.5 1524,57.7 1530,56.0 1536,57.7 1542,62.5 1548,70.0 1554,79.5 1560,90.0 1566,100.5 1572,110.0 1578,117.5 1584,122.3 1590,124.0 1596,122.3 1602,117.5 1608,110.0 1614,100.5 1620,90.0 1626,79.5 1632,70.0 1638,62.5 1644,57.7 1650,56.0 1656,57.7 1662,62.5 1668,70.0 1674,79.5 1680,90.0 1686,100.5 1692,110.0 1698,117.5 1704,122.3 1710,124.0 1716,122.3 1722,117.5 1728,110.0 1734,100.5 1740,90.0 1746,79.5 1752,70.0 1758,62.5 1764,57.7 1770,56.0 1776,57.7 1782,62.5 1788,70.0 1794,79.5 1800,90.0 1806,100.5 1812,110.0 1818,117.5 1824,122.3 1830,124.0 1836,122.3 1842,117.5 1848,110.0 1854,100.5 1860,90.0 1866,79.5 1872,70.0 1878,62.5 1884,57.7 1890,56.0 1896,57.7 1902,62.5 1908,70.0 1914,79.5 1920,90.0 1926,100.5 1932,110.0 1938,117.5 1944,122.3 1950,124.0 1956,122.3 1962,117.5 1968,110.0 1974,100.5 1980,90.0 1986,79.5 1992,70.0 1998,62.5 2004,57.7 2010,56.0 2016,57.7 2022,62.5 2028,70.0 2034,79.5 2040,90.0 2046,100.5 2052,110.0 2058,117.5 2064,122.3 2070,124.0 2076,122.3 2082,117.5 2088,110.0 2094,100.5 2100,90.0 2106,79.5 2112,70.0 2118,62.5 2124,57.7 2130,56.0 2136,57.7 2142,62.5 2148,70.0 2154,79.5 2160,90.0 2166,100.5 2172,110.0 2178,117.5 2184,122.3 2190,124.0 2196,122.3 2202,117.5 2208,110.0 2214,100.5 2220,90.0 2226,79.5 2232,70.0 2238,62.5 2244,57.7 2250,56.0 2256,57.7 2262,62.5 2268,70.0 2274,79.5 2280,90.0 2286,100.5 2292,110.0 2298,117.5 2304,122.3 2310,124.0 2316,122.3 2322,117.5 2328,110.0 2334,100.5 2340,90.0 2346,79.5 2352,70.0 2358,62.5 2364,57.7 2370,56.0 2376,57.7 2382,62.5 2388,70.0 2394,79.5 2400,90.0";

const STORM_POINTS =
  "0,45 14,20 30,32 46,16 62,30 78,12 96,60 112,15 128,58 144,10 160,52 176,18 192,64 208,20 220,45";

const MODULES = [
  {
    code: "VT-01",
    name: "Virtual Thread Ingestion",
    stack: "Java 21 · Project Loom",
    copy:
      "Every IoT ping gets its own virtual thread instead of competing for a spot in a fixed OS thread pool. Ten thousand pings or a hundred thousand — the ingestion layer doesn't queue, it just runs them.",
  },
  {
    code: "SM-02",
    name: "State Machine Engine",
    stack: "Spring State Machine",
    copy:
      "Each battery node lives in one of four states — Charging, Discharging, Idle, Fault — and the transitions are rules, not guesses. Grid load crosses a threshold, the machine moves the node, no human in the loop.",
  },
  {
    code: "EB-03",
    name: "Event Broker",
    stack: "Apache Kafka",
    copy:
      "Telemetry spikes hit Kafka first, not the state machine directly. When 50,000 nodes report in the same second, the broker absorbs the burst so the processing layer never falls behind.",
  },
  {
    code: "GD-04",
    name: "GIS Dashboard",
    stack: "React · Leaflet",
    copy:
      "A live map of the grid — node states, power-flow vectors, and zone heatmaps update as events arrive, so an operator sees the rebalancing happen, not just a log of what already happened.",
  },
];

const FLOW = [
  { label: "IoT Nodes", detail: "Solar panels, home batteries" },
  { label: "Virtual Thread Ingestion", detail: "One thread per connection" },
  { label: "Kafka Broker", detail: "Buffers the telemetry spike" },
  { label: "State Machine", detail: "Decides charge / discharge" },
  { label: "GIS Dashboard", detail: "Shows the rebalance live" },
];

export default function LandingPage({ onOpenDashboard }) {
  return (
    <div className="lp">
      <a className="lp-skip" href="#lp-main">
        Skip to content
      </a>

      <header className="lp-nav">
        <div className="lp-nav-inner">
          <span className="lp-logo">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" aria-hidden="true">
              <path d="M13 2 4 14h6l-1 8 9-12h-6l1-8Z" fill="currentColor" />
            </svg>
            GridWeaver
          </span>
          <nav className="lp-nav-links" aria-label="Primary">
            <a href="#modules">Modules</a>
            <a href="#architecture">Architecture</a>
            <a href="#storm">The scenario</a>
          </nav>
          <button type="button" className="lp-btn lp-btn-primary lp-nav-cta" onClick={onOpenDashboard}>
            Open dashboard
          </button>
        </div>
      </header>

      <main id="lp-main">
        {/* ================= HERO ================= */}
        <section className="lp-hero">
          <div className="lp-hero-wave" aria-hidden="true">
            <svg
              className="lp-wave-svg lp-wave-scroll"
              viewBox="0 0 1200 180"
              preserveAspectRatio="none"
            >
              <polyline points={WAVE_POINTS} fill="none" stroke="url(#waveGrad)" strokeWidth="2.5" />
              <defs>
                <linearGradient id="waveGrad" x1="0" x2="1" y1="0" y2="0">
                  <stop offset="0%" stopColor="#38bdf8" stopOpacity="0" />
                  <stop offset="15%" stopColor="#38bdf8" stopOpacity="0.9" />
                  <stop offset="85%" stopColor="#38bdf8" stopOpacity="0.9" />
                  <stop offset="100%" stopColor="#38bdf8" stopOpacity="0" />
                </linearGradient>
              </defs>
            </svg>
            <svg className="lp-storm-svg" viewBox="0 0 220 90" preserveAspectRatio="none">
              <polyline points={STORM_POINTS} fill="none" stroke="#ffb020" strokeWidth="3" strokeLinejoin="round" />
            </svg>
          </div>

          <div className="lp-hero-content">
            <p className="lp-eyebrow">Grid-ops platform · storm-tested</p>
            <h1 className="lp-h1">
              The grid finds its new balance before your operator finishes reading the alert.
            </h1>
            <p className="lp-hero-sub">
              GridWeaver ingests tens of thousands of IoT nodes on Java 21 virtual threads, lets a
              state machine decide who charges and who discharges, and redraws the map in under a
              second — while the storm is still overhead.
            </p>
            <div className="lp-hero-cta">
              <button type="button" className="lp-btn lp-btn-primary" onClick={onOpenDashboard}>
                Open live dashboard
              </button>
              <a className="lp-btn lp-btn-ghost" href="#architecture">
                See the architecture
              </a>
            </div>
          </div>
        </section>

        {/* ================= TELEMETRY STRIP ================= */}
        <section className="lp-telemetry" aria-label="System specifications">
          <div className="lp-telemetry-inner">
            <div className="lp-tstat">
              <span className="lp-tstat-value">50,000+</span>
              <span className="lp-tstat-label">Concurrent node connections</span>
            </div>
            <div className="lp-tstat">
              <span className="lp-tstat-value">&lt;1s</span>
              <span className="lp-tstat-label">Map reflects a full reroute</span>
            </div>
            <div className="lp-tstat">
              <span className="lp-tstat-value">1:1</span>
              <span className="lp-tstat-label">Virtual thread per IoT ping</span>
            </div>
            <div className="lp-tstat">
              <span className="lp-tstat-value">4</span>
              <span className="lp-tstat-label">Tracked battery states</span>
            </div>
          </div>
        </section>

        {/* ================= SCENARIO ================= */}
        <section className="lp-section lp-scenario" id="storm">
          <div className="lp-section-inner lp-scenario-inner">
            <div>
              <p className="lp-kicker">The scenario this was built for</p>
              <h2 className="lp-h2">A storm doesn't send a warning. It sends 50,000 events at once.</h2>
              <p className="lp-body">
                A city operator is watching the board. A storm front rolls in and 50,000 solar
                nodes drop output in the same few seconds. A traditional thread-per-connection
                backend would exhaust memory trying to hold that many sockets open. GridWeaver's
                ingestion layer doesn't blink — every ping gets a lightweight virtual thread, the
                state machine fires <code>DISCHARGE</code> on the battery nodes that need to pick
                up the slack, and the dashboard shows the power rerouting while the storm is still
                on the map.
              </p>
            </div>
            <div className="lp-scenario-card" role="img" aria-label="Battery state legend: Charging, Discharging, Idle, Fault">
              <p className="lp-scenario-card-title">Node states the map tracks</p>
              <ul className="lp-state-list">
                <li>
                  <span className="lp-dot lp-dot-charging" /> Charging — soaking up surplus
                </li>
                <li>
                  <span className="lp-dot lp-dot-discharging" /> Discharging — covering the gap
                </li>
                <li>
                  <span className="lp-dot lp-dot-idle" /> Idle — load within normal range
                </li>
                <li>
                  <span className="lp-dot lp-dot-fault" /> Fault — flagged for the operator
                </li>
              </ul>
            </div>
          </div>
        </section>

        {/* ================= MODULES ================= */}
        <section className="lp-section" id="modules">
          <div className="lp-section-inner">
            <p className="lp-kicker">Four modules, one pipeline</p>
            <h2 className="lp-h2">Every part of the pipeline earns its place under load.</h2>
            <div className="lp-modules-grid">
              {MODULES.map((m) => (
                <article className="lp-module-card" key={m.code}>
                  <div className="lp-module-head">
                    <span className="lp-module-code">{m.code}</span>
                    <span className="lp-module-stack">{m.stack}</span>
                  </div>
                  <h3 className="lp-module-name">{m.name}</h3>
                  <p className="lp-module-copy">{m.copy}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* ================= ARCHITECTURE FLOW ================= */}
        <section className="lp-section lp-arch" id="architecture">
          <div className="lp-section-inner">
            <p className="lp-kicker">Architecture</p>
            <h2 className="lp-h2">From a solar panel's ping to a pixel on the map.</h2>
            <div className="lp-flow" role="list">
              {FLOW.map((step, i) => (
                <div className="lp-flow-step" role="listitem" key={step.label}>
                  <div className="lp-flow-node">
                    <span className="lp-flow-index">{String(i + 1).padStart(2, "0")}</span>
                    <span className="lp-flow-label">{step.label}</span>
                    <span className="lp-flow-detail">{step.detail}</span>
                  </div>
                  {i < FLOW.length - 1 && <span className="lp-flow-arrow" aria-hidden="true" />}
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ================= CTA ================= */}
        <section className="lp-cta">
          <div className="lp-section-inner lp-cta-inner">
            <h2 className="lp-h2">Watch 50,000 nodes lose power — and watch the grid answer.</h2>
            <p className="lp-body">
              The live dashboard runs the actual ingestion, state machine, and map. Nothing on it
              is a mockup.
            </p>
            <button type="button" className="lp-btn lp-btn-primary lp-btn-lg" onClick={onOpenDashboard}>
              Open live dashboard
            </button>
          </div>
        </section>
      </main>

      <footer className="lp-footer">
        <div className="lp-section-inner lp-footer-inner">
          <span className="lp-logo lp-footer-logo">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" aria-hidden="true">
              <path d="M13 2 4 14h6l-1 8 9-12h-6l1-8Z" fill="currentColor" />
            </svg>
            GridWeaver
          </span>
          <ul className="lp-stack-chips">
            <li>Java 21</li>
            <li>Spring State Machine</li>
            <li>Apache Kafka</li>
            <li>React</li>
            <li>Leaflet</li>
          </ul>
          <p className="lp-footer-note">A systems demo for decentralized microgrid resilience.</p>
        </div>
      </footer>
    </div>
  );
}
