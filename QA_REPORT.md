# QA Report — Saeedi Essence Order List

**Test date:** 18 October 2026 (Asia/Karachi)  
**Target:** `index-1.html`  
**Repository revision tested:** `c8bfd08` (`arena/2aff49b3-order`)  
**Overall result at the tested revision:** 10 findings — **6 Major, 4 Minor, 0 Critical**.

**Resolved in the Android packaging follow-up:** Q-01 and Q-06.

## Test approach and limitations

- Reviewed the app, README, inline JavaScript, and print/export code.
- Exercised the UI and persistence flows with DOM automation under Node.js 22 and jsdom 26: add/edit/remove, category/company grouping, quantity steppers, theme, text/copy, print invocation, PDF/image action paths, clearing, and reload from saved state.
- Seeded and rendered 501 items (501 category groups); all 501 rows rendered in the DOM. A mocked canvas recorded the requested image dimensions and draw operations. Clipboard, native share, print, download, PDF, and canvas rasterization were mocked where needed.
- **A full browser was not available:** the bundled headless Chromium could not start because the environment lacks `libnspr4.so`. Therefore, real-device layout checks (phone/tablet/desktop), browser console inspection, print-preview/page-break inspection, actual PNG/PDF pixels, and delivery into messaging/email apps could not be verified. Browser-dependent image-limit findings below are based on the generated canvas dimensions and source behavior; they are not claimed as a native-browser reproduction.
- The external jsPDF/font CDN was not loaded in jsdom. The missing-PDF-library fallback was exercised; the successful PDF path was exercised with a stub, not a real PDF renderer.

## Findings

### Q-01 — Prototype-named categories or companies crash list rendering (**Major**)

**Code:** `index-1.html:279–285`.

**Steps to reproduce**
1. Start with an empty order.
2. Add an item with a normal item name and category `__proto__` (the same issue occurs with names such as `constructor` or `toString`). Alternatively, use one of these as a company and group by company.
3. Reload the page.

**Expected:** The special-character name is treated as ordinary text and the item remains visible.  
**Actual:** `groups()` uses a plain `{}` as a map. The inherited property is mistaken for a group, causing `TypeError: Cannot read properties of undefined (reading 'push')`. The item is saved to localStorage before rendering fails, so the same error recurs on reload and the saved order is no longer usable in the normal UI. This was reproduced in the DOM test.

**Suggested fix:** Use `Map` or `Object.create(null)` for user-controlled group keys (and validate loaded state before rendering).

**Status:** Fixed in the Android packaging follow-up with a prototype-safe group map; special-name regression checks pass.

### Q-02 — Invalid weight and quantity values are accepted or silently changed (**Major**)

**Code:** `index-1.html:304–317`.

**Steps to reproduce**
1. Add an item with quantity `0`, then another with quantity `1.5`.
2. Try quantity `-2`; try weight `0` and `-5`.
3. Leave quantity blank or enter a non-number value where the browser allows it, then add the item.

**Expected:** Reject invalid values with a clear message and preserve the user's entered data until corrected.  
**Actual:** Zero quantity and zero weight are accepted; fractional quantity is accepted; negative quantity silently becomes `1`; negative weight silently becomes blank. Blank/non-numeric quantity also defaults to `1`. No validation feedback identifies that the app changed the value. This can create a materially different order than the user entered.

**Suggested fix:** Validate explicitly before save. If the business rules require whole positive order counts and positive weights when supplied, enforce those rules and show an inline error instead of substituting defaults or dropping values.

### Q-03 — Picture export is one unbounded canvas and cannot support long lists reliably (**Major**)

**Code:** `index-1.html:113`, `490–519`, `641–647`.

**Steps to reproduce**
1. Add or load 501 items spread across distinct categories.
2. Choose **Picture**.

**Expected:** Every item is included in one or more legible, shareable images, or the app offers a clear alternative.  
**Actual:** The picture path calls `planPages(null)` and draws the entire order into one canvas. The 501-item test requested a **1080 × 87,502 px** canvas. That exceeds common browser canvas-dimension limits; depending on the browser it can fail, produce an empty/partial image, or consume excessive memory. The picture dialog also limits the image to `56vh`, so even a successful very-tall image cannot remain legible when fitted into the dialog. The native canvas result could not be verified in this environment.

**Suggested fix:** Paginate/chunk picture output, or impose a safe item limit and direct users to the multipage PDF. Keep each page at a readable size and handle canvas failures with visible feedback.

### Q-04 — Picture and PDF truncate long item, company, and category names (**Major**)

**Code:** `index-1.html:474–478`, `560–563`, `588–596`.

**Steps to reproduce**
1. Add a long item name, company name, or category name (up to the app's field limits).
2. Compare the on-screen/text order with **Picture** and **PDF** output.

**Expected:** Exact order details appear in the export, wrapping or continuing onto another line/page if needed.  
**Actual:** The canvas renderer calls `fit()` with fixed column widths and replaces overflow with an ellipsis. The item, company, and category are therefore shortened in both Picture and PDF (PDF uses the same drawing function); for example, long company names are clipped to a short prefix. Plain-text output retains the full string, so the exports do not contain the same data as the screen/text order.

**Suggested fix:** Wrap text, allocate flexible column widths, or continue long fields on another line. Do not silently remove identifying text from an order document.

### Q-05 — No first-class category management or category filter (**Major — acceptance-criteria gap**)

**Code:** `index-1.html:145–160`, `300–305`, `334–352`.

**Steps to reproduce**
1. Try to add a category without adding an item, rename a category for all its items, or delete a category that contains items.
2. Look for a category-only filter.

**Expected:** Add/edit/rename/delete category actions and a way to filter by category.  
**Actual:** Categories are only free-text values derived from existing items. The chips/datalist help reuse a value, and an individual item can be edited, but there is no category entity or central rename/delete action. `By category` groups the entire list; it is not a filter. Deleting a category with items cannot be performed as a category operation.

**Suggested fix:** Add a category data model and management actions, define what happens to assigned items on delete, and provide an actual category filter. Normal per-item category edits did update the visible list and suggestions in the exercised flow.

### Q-06 — GitHub Pages instructions do not match the tracked entry-point filename (**Major — deployment**)

**Code/docs:** `README.md:3, 35, 40`; tracked app is `index-1.html`.

**Steps to reproduce**
1. Follow the README's GitHub Pages instructions to publish the repository root.
2. Inspect the tracked root files and visit the published root URL.

**Expected:** The app is available at the site root as described.  
**Actual at the tested revision:** The tracked app was `index-1.html`; there was no `index.html`. The README referred to `index.html` and instructed users to upload/update that filename. A root Pages URL would not find the documented default entry point.

**Status:** Resolved in the Android packaging follow-up by making `index.html` the canonical web entry point.

**Suggested fix:** Publish the app as `index.html`, or update the deployment instructions/configuration so the root URL serves `index-1.html`.

### Q-07 — Very large finite quantities display as `Infinity` (**Minor**)

**Code:** `index-1.html:256`.

**Steps to reproduce**
1. Enter a large but finite quantity such as `1e307` and add the item.

**Expected:** Display the value accurately or reject it with a maximum-value message.  
**Actual:** The value is accepted and saved, but `fmt()` multiplies it by 100 before rounding; the multiplication overflows and the row/group/order totals display `Infinity`. This was reproduced in the DOM test.

**Suggested fix:** Apply a sensible maximum and use overflow-safe formatting that does not multiply beyond the numeric range.

### Q-08 — Item numbering restarts at 1 in every group (**Minor**)

**Code:** `index-1.html:382–384`, `446–448`, `508`.

**Steps to reproduce**
1. Add one item in each of two categories and view the list or text export.

**Expected:** The order is numbered continuously, consistent with the README's “1, 2, 3 and so on.”  
**Actual:** Both items are numbered `1`. The numbering counter is reset inside each group; the same occurs in text and canvas exports.

**Suggested fix:** Maintain one order-wide counter across groups (or update the documented behavior if numbering is intentionally group-local).

### Q-09 — Copy fallback can report success when copying failed (**Minor**)

**Code:** `index-1.html:705–712`.

**Steps to reproduce**
1. Open **Text** for an order.
2. Simulate `navigator.clipboard.writeText()` being unavailable/rejected and `document.execCommand('copy')` returning `false`.
3. Press **Copy text**.

**Expected:** Show a failure/manual-copy instruction when neither copy method succeeds.  
**Actual:** The fallback ignores the boolean return from `execCommand('copy')` and displays **Copied** even when the command returned `false`. This failure path was reproduced with a mocked clipboard.

**Suggested fix:** Check the return value and only show **Copied** on success; otherwise leave the text selected and show the manual-copy message.

### Q-10 — A quantity of one is labeled “1 pcs” (**Minor**)

**Code:** `index-1.html:259–260`.

**Steps to reproduce**
1. Add an item with quantity `1` and unit `pcs`.
2. Check the row and group/order totals or text output.

**Expected:** A singular label such as “1 pc” or “1 piece.”  
**Actual:** `plural()` returns `pcs` unchanged for one, producing “1 pcs.”

**Suggested fix:** Add a singular mapping for `pcs` (or display “piece/pieces”).

## What worked well

- The initial empty state and required item-name feedback worked; export/share actions correctly asked for items before proceeding on an empty order.
- Add, edit, per-item delete, edit-dialog Remove/Cancel/Save, quantity steppers, group toggles, theme toggle, and two-step **Clear all** worked in the exercised DOM flows. Rapid double-clicking **Add to order** did not create a duplicate because the name field is cleared after the first add.
- Shop/order-company fields and items were saved in localStorage and restored in a simulated reload. Blank optional fields defaulted to category `General` and quantity `1` as coded.
- Duplicate items remained as separate rows and contributed to the group/order totals; this is a reasonable and predictable behavior.
- Emoji and HTML-like category text such as `Tea 🍵 & <spice>` rendered as literal text rather than markup.
- Text export retained full strings. The app gave a clear fallback message when the PDF library was missing. No script errors were observed in the normal interaction paths; the prototype-key crash is the reproduced exception.

## Summary and priorities

| Severity at tested revision | Count |
|---|---:|
| Critical | 0 |
| Major | 6 |
| Minor | 4 |
| **Total** | **10** |

### Top 5 remaining fixes to make first

1. **Q-02:** Prevent silent changes to entered weights and quantities; validate before saving.
2. **Q-03:** Make long-list picture output paginated and readable, with safe canvas bounds.
3. **Q-04:** Preserve full item/company/category text in picture and PDF exports.
4. **Q-05:** Add category CRUD and a true category filter.
5. **Q-07:** Avoid overflow when formatting very large finite quantities.

Q-01 (prototype-safe grouping) and Q-06 (the `index.html` entry point) were resolved in the Android packaging follow-up.
