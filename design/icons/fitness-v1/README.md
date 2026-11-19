# PulsLibre Fitness-Dashboard – SVG Asset Kit v1

**Issue:** #41 · **Design:** Pulse Flow × Fitness Atlas · **Status:** Design assets, no app integration yet.

These SVGs extend the existing PulsLibre icon language from `design/icons/v1/`: 128×128 viewBox, 6-unit rounded outlines, `#b68aff` violet, `#24e2bf` teal accent, translucent `#6334a8` surfaces. SVGs are source assets for design and Android VectorDrawable conversion, **not screenshots or medical visualizations**.

| Asset | Suggested use | Existing alternative |
|---|---|---|
| `fitness.svg` | Fitness section title/navigation if needed | `design/icons/v1/trends.svg` |
| `resting-pulse.svg` | Resting-pulse hero tile | `design/icons/v1/heart.svg` |
| `activity-calendar.svg` | Activity Atlas calendar title | `design/icons/v1/steps.svg` |
| `consistency.svg` | Consistency/goal-days tile | None |
| `coverage.svg` | Data coverage tile | `design/icons/v1/data.svg` |
| `baseline.svg` | Baseline / personal comparison detail | `design/icons/v1/trends.svg` |
| `data-gap.svg` | Missing-data / incomplete-period explanation | `design/icons/v1/info.svg` |

## Implementation notes for Codex

1. Prefer existing `ic_pl_heart`, `ic_pl_steps`, `ic_pl_trends` wherever the semantics match. **Do not replace the current brand logo or the user's CIRQA-specific button.**
2. Android does not render arbitrary SVG directly as a normal VectorDrawable resource. Convert each needed source into an Android vector XML (follow the conventions of `app/src/main/res/drawable/ic_pl_heart.xml`), or use the existing repository converter **after checking whether it supports this folder and the SVG primitives**. Do not assume `tools/svg_icons_to_vector.py` processes `fitness-v1` automatically.
3. Map strokes to theme colors (`@color/pl_icon_line`, etc.) for Dark/Light compatibility. The hard-coded SVG colors are **design-preview defaults**, not the final runtime theme colors. Convert `stroke-dasharray` in `baseline.svg` manually if the converter does not support it.
4. Icon sizes: 20–24 dp inside cards, 24–28 dp for section headings; touch targets >=48 dp. Icons are decorative beside readable labels, not sole carriers of meaning.
5. **Never** render the resting-pulse graph or calendar as a static SVG asset: the chart, step activity calendar, coverage, medians and baseline are data-driven Android Canvas/Views. Missing days must remain visibly missing. The mockup's 72 bpm / 8240 steps / 96% coverage are synthetic illustration only.
6. Follow `docs/science/SCIENTIFIC_DASHBOARD_TILE_BLUEPRINT.md` and the design specification comment in #41; no unsupported resting-pulse inference or health-value judgments.

### Palette for runtime design (reference, not an Android color resource contract)
- Background: existing app navy; card surface: existing theme tokens, do not duplicate.
- Heart rate: soft pink (suggested `#F27AB6`), step activity: `#7956C5` → `#B08AFF`, accent: `#24E2BF`.
- Missing data: neutral gray, **not** zero activity; status color is never a medical assessment.

### Validation checklist
- SVG XML parses and scales cleanly at 20, 24, 32 dp.
- Generated VectorDrawable paths display in both themes.
- Accessibility labels exist on containing Views; large text does not clip.
- Emulator screenshot with synthetic data matches the approved Pulse Flow × Fitness Atlas design.
