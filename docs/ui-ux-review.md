# Geni UI and usability review

Reviewed on 9 September 2026. Scope: the Android Compose app, its actual demo behavior, navigation, settings, status communication, color and text accessibility. The dark teal identity is retained.

## Findings and changes

| Issue | User impact | Change |
| --- | --- | --- |
| Simulated SOS claimed emergency dispatch, delivered messages and transmitted location | Someone could wrongly believe help was on its way | Explicit SOS demonstration, truthful completion state, no nonworking emergency-call button, static warning instead of strobe |
| Demo pairing, permissions, recording and exports looked real | Users could trust functions that are not connected | Persistent demo indicator; clearer onboarding, camera, voice, storage and export feedback |
| Pre-ride checks always displayed Ready | Failed connections, sensors or missing contact details were hidden | Readiness derives from each input; failures have a warning icon and text; the start button explains and blocks incomplete checks |
| Muted text and navigation labels were faint and small | Harder to read, particularly with low vision | Brighter text; larger navigation labels; native navigation items and selection semantics |
| Cyan represented both brand actions and success | Hard to distinguish an action from a system state | Teal for actions, separate green for readiness, amber for caution, red for urgent actions; labels remain essential |
| White text on bright red buttons lacked contrast | Urgent actions were less legible | Separate dark red filled-button color, retaining bright red for icons/text on dark surfaces |
| Fixed button heights and tightly packed rows | Larger text could clip labels or hide actions | Minimum button heights, wrapping text, scrollable content and a clearer pre-ride/SOS layout |
| Camera back navigation abandoned the ride | Unexpected loss of context | Return to the originating screen; preserve demo ride screen state; confirm before ending a ride |
| Settings reset when leaving and speed unit did not affect the ride | Controls appeared to work but did not | Persist preferences/contact details; connect speed unit to the live demo display; identify hardware options as previews |
| Contact dialog accepted blank or incomplete entries | Invalid details could appear ready | Required name, phone-format validation, phone keyboard, disabled Save until input is usable, discard cancelled edits |
| Hazard overlay did not isolate the background | Screen reader focus and taps could reach underlying controls | Use a modal dialog and support dismissal/back navigation |
| System icons were black on the dark background and top insets were doubled | Status bar became unreadable and wasted space | Explicit light system icons and consumption of the scaffold insets |

## Color decisions

The palette uses common interface conventions, not a claim that colors have identical meanings in every culture or riding situation. Every critical status must also have readable text and/or an icon. The teal brand color is appropriate for primary controls on the current dark surfaces. Green is distinct from teal; amber and red are reserved for caution/urgent meaning rather than ordinary success.

On the elevated surface `#0E1420`, muted text changed from `#64748B` (3.87:1) to `#94A3B8` (7.19:1). White text on the dark red action color `#B42332` is 6.51:1. Automated tests cover opaque primary text/status tokens on the three main dark surfaces, the primary/danger button pairs and the control-outline token. These tests do not certify every image, translucent overlay or screen.

The reference targets are 4.5:1 for normal text, 3:1 for relevant non-text controls, and Android's 48dp minimum touch-target guidance. Shared primary/secondary buttons use a minimum height of 56dp. Visual size alone does not establish glove usability.

Sources: [W3C contrast guidance](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html), [W3C use of color](https://www.w3.org/WAI/WCAG22/Understanding/use-of-color.html), [Android accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).

## Validation

- Debug APK builds successfully.
- Seven focused JVM tests pass: five for readiness/contact validation, two for color contrast.
- Four UI checks passed on both the Pixel 5 Android 16 emulator and Samsung SM-M115F Android 12: disconnected readiness, start action at 200% text size, truthful SOS completion, and SOS cancellation at 200% text size.
- Manual emulator walkthrough verified setup skipping, readable Home/Settings layouts, camera-to-ride back navigation, end-ride confirmation, and the Settings mph choice changing the demo display to mph/miles.
- A fifth UI check passed on the emulator: pinned active-ride controls remain visible and the end action works at 200% text size.

## Remaining limits

This remains a prototype. UI changes do not implement Bluetooth pairing, real cameras/recording, real voice recognition, backup/export, crash detection or emergency dispatch. Hardware preferences are saved as previews. Sample history and scores are illustrative. Contact validation checks formatting, not whether a number can receive a call/message.

The review is not field validation for motorcycle use. Sunlight visibility, gloves, vibration, hearing conditions and safe in-motion interaction require controlled testing with riders and actual hardware. A dark theme alone does not solve daylight readability. Full TalkBack exploration, every screen at every font/display size, all tablet/landscape layouts and localization remain outside the completed checks.
