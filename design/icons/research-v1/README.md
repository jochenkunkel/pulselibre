# PulsLibre – Research Dashboard SVG Kit v1

Design-only SVG assets for the science tickets, complementing `design/icons/v1/` and `design/icons/fitness-v1/`.

| Issue | Design direction | SVG assets |
|---|---|---|
| [#43](https://github.com/jochenkunkel/pulselibre/issues/43) | Night Flow × Recovery Bridge | `issue-43/sleep-moon.svg`, `night-timeline.svg`, `next-day.svg` |
| [#44](https://github.com/jochenkunkel/pulselibre/issues/44) | Day-to-Night Relay | `issue-44/activity-to-night.svg`, `paired-days.svg` |
| [#45](https://github.com/jochenkunkel/pulselibre/issues/45) | Insight Constellation × Evidence Lens | `issue-45/correlation.svg`, `evidence.svg` |
| [#46](https://github.com/jochenkunkel/pulselibre/issues/46) | Matched Effort | `issue-46/matched-exertion.svg` |
| [#47](https://github.com/jochenkunkel/pulselibre/issues/47) | 90-Day Horizon × Trend Stories | `issue-47/long-term-trend.svg`, `trend-break.svg` |

## Contract
- 128×128 SVG sources, 6-unit rounded outline, PulsLibre violet `#b68aff` and teal `#24e2bf`. Theme-aware Android XML VectorDrawables must be generated/verified before use.
- Assets are decorative/title icons **not** charts, health measurements, diagnostic claims or source-of-truth fixtures. Render all curves, calendars, correlations and gaps from validated local data.
- The original `design/icons/v1/` icons should be reused where possible. Do not overwrite the brand logo, user-customized CIRQA button or existing icon resources.
- Use accessible labels on parent UI controls, >=48 dp touch targets, Dark/Light checks, TalkBack and large-font testing.
- Scientific data-source and confidence gates are described in each issue; missing unverified sleep, HRV, resting pulse and activity context must remain missing. Do not synthesize health results.
- **Status:** SVG source files committed; Android conversion, compile and emulator/S25 screenshots **not yet performed**. No app runtime files changed.
