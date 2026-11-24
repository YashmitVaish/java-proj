import { useEffect, useRef, useState } from 'react';
import { EXPENSE_COLOR, INCOME_COLOR } from '../lib/colors.js';
import { budgetState } from '../lib/budgets.js';
import { money, moneyCompact, monthLabel, percentLabel } from '../lib/format.js';
import { Swatch } from './ui.jsx';
import { useTooltip } from '../lib/tooltip.jsx';

const pct = (part, whole) => percentLabel(whole > 0 ? (part / whole) * 100 : 0);

// ---------------------------------------------------------------------------
// Spending strip: one bar the length of the month's income (or spending, if
// that's bigger), filled with each expense category in its own colour.
// ---------------------------------------------------------------------------
export function SpendingStrip({ income, expense, parts }) {
  const tip = useTooltip();
  const whole = Math.max(income, expense);
  const leftover = income - expense;
  if (whole <= 0) return null;

  const segments = parts.filter((p) => p.total > 0);
  const incomeAt = income > 0 && expense > income ? (income / whole) * 100 : null;

  return (
    <figure className="strip-figure">
      <div className="strip" role="img" aria-label={`Spending by category: ${segments.map((s) => `${s.name} ${money(s.total, { whole: true })}`).join(', ')}`}>
        {segments.map((s) => (
          <span
            key={s.name}
            className="strip-seg"
            tabIndex={0}
            style={{ flexGrow: s.total, background: s.color }}
            {...tip.bind(
              <>
                <strong>{s.name}</strong>
                <span>
                  {money(s.total)}, {pct(s.total, expense)} of spending
                </span>
              </>,
            )}
          />
        ))}
        {leftover > 0 && (
          <span
            className="strip-seg strip-left"
            tabIndex={0}
            style={{ flexGrow: leftover }}
            {...tip.bind(
              <>
                <strong>Not spent</strong>
                <span>
                  {money(leftover)}, {pct(leftover, income)} of income
                </span>
              </>,
            )}
          />
        )}
        {incomeAt != null && (
          <span className="strip-income-mark" style={{ left: `${incomeAt}%` }}>
            <span>Income ends here</span>
          </span>
        )}
      </div>
      {tip.layer}
    </figure>
  );
}

/** Ledger-style legend for the strip: swatch, name, amount, share. */
export function CategoryLegend({ parts, total, leftover }) {
  return (
    <ul className="legend-list">
      {parts.map((p) => (
        <li key={p.name}>
          <Swatch color={p.color} />
          <span className="legend-name">{p.name}</span>
          <span className="legend-amount num">{money(p.total, { whole: true })}</span>
          <span className="legend-share num">{pct(p.total, total)}</span>
        </li>
      ))}
      {leftover > 0 && (
        <li className="legend-leftover">
          <span className="swatch swatch-left" aria-hidden="true" />
          <span className="legend-name">Not spent</span>
          <span className="legend-amount num">{money(leftover, { whole: true })}</span>
          <span className="legend-share" />
        </li>
      )}
    </ul>
  );
}

// ---------------------------------------------------------------------------
// Budget meter: fill colour carries how close you are to the limit, and the
// text beside it always says so in words too.
// ---------------------------------------------------------------------------
export function Meter({ percent, label }) {
  const state = budgetState(percent);
  return (
    <div
      className={`meter meter-${state}`}
      role="meter"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.min(100, Math.round(percent))}
      aria-label={label}
    >
      <span style={{ width: `${Math.min(100, Math.max(percent, percent > 0 ? 1.5 : 0))}%` }} />
    </div>
  );
}

// ---------------------------------------------------------------------------
// Horizontal bars: share of spending (or income) per category.
// ---------------------------------------------------------------------------
export function CategoryBars({ rows, colorFor }) {
  const max = Math.max(...rows.map((r) => Number(r.total)), 0);
  const tip = useTooltip();
  return (
    <>
      <ul className="hbars">
        {rows.map((r) => (
          <li key={r.category} {...tip.bind(<><strong>{r.category}</strong><span>{money(r.total)}, {percentLabel(r.percentage)} of the total</span></>)}>
            <span className="hbar-name">
              <Swatch color={colorFor(r.category)} />
              {r.category}
            </span>
            <span className="hbar-track">
              <span className="hbar" style={{ width: `${(Number(r.total) / max) * 100}%`, background: colorFor(r.category) }} />
            </span>
            <span className="hbar-value num">{money(r.total, { whole: true })}</span>
            <span className="hbar-share num">{percentLabel(r.percentage)}</span>
          </li>
        ))}
      </ul>
      {tip.layer}
    </>
  );
}

// ---------------------------------------------------------------------------
// Monthly columns: income and expense side by side for each month of a year.
// ---------------------------------------------------------------------------
function useWidth() {
  const ref = useRef(null);
  const [width, setWidth] = useState(720);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const ro = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);
  return [ref, width];
}

function niceTicks(max) {
  if (max <= 0) return [0, 1000, 2000];
  const rough = max / 4;
  const mag = 10 ** Math.floor(Math.log10(rough));
  const step = [1, 2, 2.5, 5, 10].map((m) => m * mag).find((s) => s >= rough);
  const ticks = [];
  for (let v = 0; v <= max + step * 0.001; v += step) ticks.push(v);
  if (ticks[ticks.length - 1] < max) ticks.push(ticks[ticks.length - 1] + step);
  return ticks;
}

export function MonthlyChart({ data }) {
  const [ref, width] = useWidth();
  const [hover, setHover] = useState(null);
  const height = 260;
  const pad = { top: 12, right: 8, bottom: 28, left: 56 };
  const plotW = Math.max(0, width - pad.left - pad.right);
  const plotH = height - pad.top - pad.bottom;

  const ticks = niceTicks(Math.max(...data.flatMap((d) => [Number(d.income), Number(d.expense)]), 0));
  const top = ticks[ticks.length - 1];
  const y = (v) => pad.top + plotH - (Number(v) / top) * plotH;

  const band = plotW / data.length;
  const barW = Math.max(3, Math.min(18, (band - 10) / 2));
  const narrow = width < 520;

  const hovered = hover != null ? data[hover] : null;

  return (
    <div className="chart" ref={ref}>
      <svg width={width} height={height} role="img" aria-label="Income and expense for each month of the year">
        {ticks.map((t) => (
          <g key={t}>
            <line x1={pad.left} x2={width - pad.right} y1={y(t)} y2={y(t)} className="grid" />
            <text x={pad.left - 8} y={y(t)} className="tick" textAnchor="end" dominantBaseline="middle">
              {t === 0 ? '₹0' : moneyCompact(t)}
            </text>
          </g>
        ))}
        {data.map((d, i) => {
          const cx = pad.left + band * i + band / 2;
          return (
            <g key={d.month}>
              {hover === i && <rect x={cx - band / 2} y={pad.top} width={band} height={plotH} className="hover-band" />}
              <Column x={cx - barW - 1} w={barW} y0={y(0)} y1={y(d.income)} fill={INCOME_COLOR} />
              <Column x={cx + 1} w={barW} y0={y(0)} y1={y(d.expense)} fill={EXPENSE_COLOR} />
              <text x={cx} y={height - 8} className="tick" textAnchor="middle">
                {narrow ? monthLabel(d.month, { short: true, withYear: false }).charAt(0) : monthLabel(d.month, { short: true, withYear: false })}
              </text>
              <rect
                x={cx - band / 2}
                y={pad.top}
                width={band}
                height={plotH + pad.bottom}
                fill="transparent"
                onMouseEnter={() => setHover(i)}
                onMouseLeave={() => setHover(null)}
              />
            </g>
          );
        })}
      </svg>
      {hovered && (
        <div
          className="tooltip tooltip-chart"
          style={{ left: Math.min(Math.max(pad.left + band * hover + band / 2, 90), width - 90), top: 0 }}
        >
          <strong>{monthLabel(hovered.month)}</strong>
          <span className="tip-row">
            <Swatch color={INCOME_COLOR} /> In <b className="num">{money(hovered.income, { whole: true })}</b>
          </span>
          <span className="tip-row">
            <Swatch color={EXPENSE_COLOR} /> Out <b className="num">{money(hovered.expense, { whole: true })}</b>
          </span>
          <span className="tip-row">
            <span className="swatch swatch-blank" aria-hidden="true" /> Net <b className="num">{money(hovered.net, { whole: true })}</b>
          </span>
        </div>
      )}
    </div>
  );
}

// 4px rounded top, square at the baseline
function Column({ x, w, y0, y1, fill }) {
  const h = y0 - y1;
  if (h <= 0) return null;
  const r = Math.min(4, h, w / 2);
  const d = `M${x},${y0} V${y1 + r} Q${x},${y1} ${x + r},${y1} H${x + w - r} Q${x + w},${y1} ${x + w},${y1 + r} V${y0} Z`;
  return <path d={d} fill={fill} />;
}
