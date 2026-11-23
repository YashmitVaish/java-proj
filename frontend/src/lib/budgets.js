/** 'ok' under 80% of the limit, 'near' from 80%, 'over' past 100%. */
export function budgetState(percent) {
  if (percent > 100) return 'over';
  if (percent >= 80) return 'near';
  return 'ok';
}

/** Overall budget first, then categories A to Z. */
export function sortBudgets(list) {
  return [...list].sort((a, b) =>
    a.category === 'Overall' ? -1 : b.category === 'Overall' ? 1 : a.category.localeCompare(b.category),
  );
}
