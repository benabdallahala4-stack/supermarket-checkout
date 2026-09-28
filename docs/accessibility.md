# Accessibility and responsive review

Reviewed 27 September 2026 against WCAG 2.1 A/AA checks, keyboard interaction and responsive content behavior. This is a scoped engineering review, not a claim of complete conformance.

## Findings and changes

| Finding | Impact | Resolution |
| --- | --- | --- |
| Refresh/retry removed the focused control, leaving focus on the document body | Keyboard users lost their place | Focus moves to the persistent Products heading before loading |
| Remove, decrement-to-zero and clear removed or disabled the active cart control | Keyboard continuation became unpredictable | Focus moves to the cart heading; ordinary decrement keeps its control focused |
| Checkout disables its active submit control | Focus may be lost while awaiting a response | Focus moves to the persistent checkout status before submission; receipt/error updates do not move it again |
| A long valid monetary value expanded a 320px viewport to 442px | Horizontal scrolling obscured content | Product actions and amounts can shrink/wrap; narrow product rows stack |
| 200% text enlargement overflowed the title at 320px | Text did not reflow | Title wraps; cart headings/quantity controls also support wrapping |

Headings/status targets use tabindex=-1 and do not add extra Tab stops. Their focus indicator remains visible. Buttons retain accessible product-specific names. Receipt amounts use description lists; product, cart and receipt items use semantic lists. No modal or custom keyboard widget is introduced.

## Evidence

- Seven executed focus test cases cover refresh, retry, removal, decrement-to-zero, clear, ordinary decrement and checkout: four standalone `it` cases plus the three inputs of one parameterized `it.each` case.
- Real browser Enter/Space activation and Tab continuation checked. After refresh, Tab reaches Refresh products; after removal, focus remains at Your cart. Checkout updates retain focus on the status.
- Browser widths 320, 390, 768 and 1366 tested with a 228-character product name and a 40-digit exact-decimal price. No horizontal overflow after fixes.
- 200% CSS text enlargement at 320px tested separately.
- Enabled controls checked at desktop and narrow widths meet a 44-by-44 CSS-pixel usability target. This target is stronger than the WCAG 2.1 AA requirement; WCAG 2.1 target-size criterion 2.5.5 is AAA.
- axe-core 4.13.0 with wcag2a, wcag2aa and wcag21aa tags found no violations in catalog, empty receipt, populated receipt, long-content and catalog-error states. The populated cart required manual contrast review of the minus symbol; axe classified its glyph as non-text. Other checked states had no incomplete results.

## Contrast

Ratios calculated from the rendered palette using sRGB relative luminance:

| Foreground/background | Ratio |
| --- | --- |
| Main text #172b26 / white | 14.89:1 |
| Muted text #51615b / white | 6.54:1 |
| Muted text #51615b / surface #f5f7f5 | 6.07:1 |
| White button text, including minus glyph / green #21613e | 7.38:1 |
| Green #21613e / offer #edf6ef | 6.69:1 |
| Green links #21613e / surface #f5f7f5 | 6.86:1 |

Disabled controls are visually distinct and unavailable; their contrast is exempt from the applicable contrast criteria. Enabled focus outlines and controls were inspected in browser screenshots.

## Repeat the review

Run `./scripts/verify.sh all`. Start the application using README instructions. With only the keyboard, add a product, change quantity, remove it, refresh, calculate, and retry a simulated network failure. Confirm visible focus remains at the documented target and Tab continues naturally. Check 320px and desktop widths, long names/amounts, text enlargement, empty catalog, pending and error states. Run [axe-core](https://github.com/dequelabs/axe-core) or its browser tooling with the same rule tags, then review incomplete results manually.

Manual accessibility testing with specialized software was outside this exercise's scope. Automated checks do not establish complete WCAG conformance.
