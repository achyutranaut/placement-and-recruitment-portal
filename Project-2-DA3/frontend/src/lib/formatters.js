/**
 * Authoritative compensation / package formatter.
 * Guarantees consistent display: ₹XX.XX LPA
 * Never displays raw undefined, null, or naked 'LPA'.
 */
export function formatPackage(amount) {
  if (amount === null || amount === undefined || amount === '' || isNaN(Number(amount))) {
    return '—';
  }
  const num = Number(amount);
  return `₹${num.toFixed(2)} LPA`;
}
