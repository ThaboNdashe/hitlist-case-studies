# 04: Fixing React Table Flicker & Jumping Layouts

**Related Synced ADR:** [`ADR-001-DataTable-Flickering-Resolution.md`](../docs/adr/ADR-001-DataTable-Flickering-Resolution.md)

## What Happened
When clicking filters, searching, or flipping pages on our **Leads** and **Outreach** screens, the table flickered badly and the pagination bar at the bottom of the page jumped up and down.

What confused me at first was that we had *already* migrated to **TanStack Table v8 (`@tanstack/react-table`)**. Why was a modern headless table library still flickering?

## What Was Actually Causing It
Because TanStack Table is headless, it only manages state in memory—it doesn't control your DOM or CSS transitions. Looking at how the browser was painting each frame, I found three issues:

1. **The `<50ms` Opacity Strobe:**
   When you clicked a filter, `loading` flipped to `true`, which set `opacity: 0.5` on the entire table. Because our local Spring Boot + Postgres backend answered in **20–40ms**, `loading` immediately flipped back to `false` (`opacity: 1.0`). Dropping the opacity of the whole screen and snapping it back in 30 milliseconds looks just like a camera flash.
2. **Height Collapse (Layout Shift):**
   When filtering a 50-row page down to 2 rows (or 0 rows), the table container shrunk immediately, yanking the `<Pagination />` bar hundreds of pixels up the screen.
3. **Copy-Pasted Table Markup:**
   `LeadsPage` and `OutreachPage` each had their own copy of the CSS grid and loading states, so fixing one didn't fix the other.

## How I Fixed It: One Shared `<DataTable />` Component

I extracted a single reusable `<DataTable />` component (`frontend/src/components/DataTable.jsx`) with three simple rules:

```jsx
<div style={{
  display: 'flex',
  flexDirection: 'column',
  position: 'relative',
  minHeight: 400 // 1. Keeps the footer from jumping when rows shrink
}}>
  {/* 2. Subtle 2px top pulse bar instead of dimming the whole table */}
  <div style={{
    position: 'absolute', top: 0, left: 0, right: 0, height: 2,
    background: loading ? 'var(--accent)' : 'transparent',
    animation: loading ? 'pulseBar 1.5s infinite' : 'none'
  }} />

  {/* 3. Keep existing rows at 1.0 opacity while fetching */}
  {data.length > 0 && (
    <div style={{ pointerEvents: loading ? 'none' : 'auto' }}>
      {/* Table rows render here without flashing */}
    </div>
  )}
</div>
```

1. **Keep Row Opacity at `1.0` During Filter Changes:** Instead of dimming the table on every click, existing rows stay at full opacity with `pointerEvents: 'none'` while the new data loads, and a subtle **2px top accent bar** pulses at the top edge.
2. **Skeletons Only on Initial Cold Load:** `<SkeletonLoader />` only renders when `loading && data.length === 0`.
3. **Locked Minimum Height (`minHeight: 400px`):** The table wrapper holds its height even with 0 or 2 rows, so the pagination controls never jump.

## Result
Filtering and pagination feel instant and steady, and adding a new table for the **Fulfillment** module took ~25 lines of column definitions instead of 250 lines of duplicated grid code.
