# ADR 001: Data Table Flickering and Standardization

## Context
The user reported persistent "flickering" on the Leads pipeline and Outreach tracker pages when applying filters, searching, or paginating. Despite migrating to TanStack Table (React Table v8) for state management, the visual flickering remained consistent across multiple views.

## Root Cause Analysis
The flickering is a UX/UI layer issue caused by three compounding factors during React re-renders:

1. **Fast-Fetch State Strobing:** When a user clicks a filter, the `loading` state is set to `true`. This instantly dropped the entire table's CSS `opacity` to `0.5`. Because the local API resolves almost instantly (often < 50ms), the state flips back to `false` and the opacity jumps back to `1.0`. This rapid `1.0 ➔ 0.5 ➔ 1.0` transition manifests as a jarring visual "flash" or "strobe" effect.
2. **Layout Shift (Height Collapse):** When moving between pages or filtering down a list (e.g., from 50 rows to 2 rows), the DOM removes the rows instantly. This causes the table container to collapse in height, pulling the `Pagination` component and page footer abruptly upward. 
3. **Redundant Re-renders:** Duplicated markup across `LeadsPage` and `OutreachPage` meant each table was managing its own container styles, loading states, and skeleton fallbacks inconsistently.

## Decision
To permanently resolve this and prepare for future modules (like Fulfillment), we will implement a standardized, reusable `<DataTable />` component.

### Architectural Standards for `<DataTable />`:
* **Headless Integration:** It will encapsulate TanStack's `useReactTable` internally.
* **Non-intrusive Loading UI:** Instead of dropping the opacity of the entire table (which causes the strobe effect), loading states will be indicated by a subtle indeterminate progress bar at the top of the table. The table contents will remain fully visible and at `1.0` opacity while background fetches occur.
* **Stable Dimensions:** The table container will enforce a `minHeight` or flex-grow property to ensure that filtering down to 0 or 2 results does not violently collapse the page layout.
* **Universal API:** It will accept `data`, `columns`, `loading`, and a `Pagination` object to be purely presentational.

## Consequences
* **Positive:** Developers no longer need to write complex table HTML/CSS grids for every new page. Flickering is universally solved.
* **Negative:** Pages must conform to the `<DataTable />` API contract, meaning slight refactoring of `LeadsPage` and `OutreachPage` is required to pass `columns` and `data` properly.

