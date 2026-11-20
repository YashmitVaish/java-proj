// Category colours, borrowed from Indian banknotes: ₹50 blue, ₹10 brown, ₹100 lavender,
// ₹200 yellow, ₹2000 magenta, ₹20 green, plus an indigo. Checked for colour-blind separation.
// A category keeps its colour everywhere: slots are handed out in the order categories were
// created (by id), never by how much was spent, so colours don't jump around between months.
export const CATEGORY_COLORS = ['#1F8FC4', '#9A5B22', '#7F67C9', '#D9A520', '#C2417F', '#5E8A2E', '#3D4FA8'];
export const NEUTRAL = '#A9ACBD'; // Uncategorized, and anything past the 7th category

export const INCOME_COLOR = '#2A8C6E';
export const EXPENSE_COLOR = '#C2410C';

/** Returns lookup(nameOrId, type) -> hex for the user's categories. */
export function categoryColors(categories = []) {
  const byKey = new Map();
  for (const type of ['EXPENSE', 'INCOME']) {
    const ofType = categories.filter((c) => c.type === type).sort((a, b) => a.id - b.id);
    // income starts further along the palette so Salary doesn't share Food's colour in mixed lists
    const offset = type === 'INCOME' ? 5 : 0;
    let slot = 0;
    for (const c of ofType) {
      const color =
        !c.system && slot < CATEGORY_COLORS.length ? CATEGORY_COLORS[(offset + slot++) % CATEGORY_COLORS.length] : NEUTRAL;
      byKey.set(`${type}:${c.id}`, color);
      byKey.set(`${type}:${c.name}`, color);
    }
  }
  return (nameOrId, type = 'EXPENSE') => byKey.get(`${type}:${nameOrId}`) || NEUTRAL;
}
