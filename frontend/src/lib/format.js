// Money, date and month helpers. All amounts are INR; dates are YYYY-MM-DD, months YYYY-MM.

const inrWhole = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const inrExact = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2 });
const inrCompact = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', notation: 'compact', maximumFractionDigits: 1 });

/** ₹1,23,456.00 (exact) or ₹1,23,456 (whole). Uses Indian digit grouping. */
export function money(value, { whole = false } = {}) {
  const n = Number(value) || 0;
  return (whole ? inrWhole : inrExact).format(n);
}

/** ₹1.2L, ₹45K: for chart axes. */
export function moneyCompact(value) {
  return inrCompact.format(Number(value) || 0);
}

const pad = (n) => String(n).padStart(2, '0');

/** Today's date as YYYY-MM-DD in the user's own time zone (toISOString would use UTC). */
export function today() {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function currentMonth() {
  return today().slice(0, 7);
}

export function shiftMonth(ym, by) {
  const [y, m] = ym.split('-').map(Number);
  const d = new Date(y, m - 1 + by, 1);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
}

/** First and last day of a month, for the from/to query params. */
export function monthRange(ym) {
  const [y, m] = ym.split('-').map(Number);
  const last = new Date(y, m, 0).getDate();
  return { from: `${ym}-01`, to: `${ym}-${pad(last)}` };
}

export function yearRange(year) {
  return { from: `${year}-01-01`, to: `${year}-12-31` };
}

function parseLocal(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number);
  return new Date(y, m - 1, d || 1);
}

/** "November 2026", or "Nov" with { short: true } */
export function monthLabel(ym, { short = false, withYear = true } = {}) {
  const opts = short ? { month: 'short' } : { month: 'long' };
  if (withYear) opts.year = 'numeric';
  return parseLocal(ym).toLocaleDateString('en-IN', opts);
}

/** "15 Nov", plus the year when it isn't the current one. */
export function dateLabel(dateStr) {
  const d = parseLocal(dateStr);
  const sameYear = d.getFullYear() === new Date().getFullYear();
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', ...(sameYear ? {} : { year: 'numeric' }) });
}

export function weekdayLabel(dateStr) {
  return parseLocal(dateStr).toLocaleDateString('en-IN', { weekday: 'short' });
}

/** Share as a whole percent; tiny non-zero shares read "<1%" rather than a misleading "0%". */
export function percentLabel(value) {
  const n = Number(value) || 0;
  if (n > 0 && n < 1) return '<1%';
  return `${Math.round(n)}%`;
}
