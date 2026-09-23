# Repository instructions for agents

## Android UI structure

- Keep each distinct screen or UI component in its own focused Kotlin file. A screen file should compose and connect components, not define unrelated UI components inline.
- Put reusable UI components in `app/src/main/java/ai/wakehook/app/ui/components/`. Keep feature-specific components alongside their feature, such as alarm-editor components in `ui/edit/`.
- Move cross-screen formatting and other shared UI helpers into `app/src/main/java/ai/wakehook/app/ui/utils/`; keep pure business rules in `domain/` and avoid copying the same helper into multiple screens.
- Prefer small composables with clear inputs, state, and event callbacks. Keep UI state at the lowest level that owns it. Share a generic component only when it has a real caller beyond its original screen.
- Keep domain rules and persistence out of composables. Model behavior in domain, data, or view-model code and give UI explicit state and callbacks.
- Keep local playback preferences device-only. Do not add sound, vibration, or snooze settings to the Drive alarm protocol unless the user explicitly asks for a protocol change.

## Design approval

- For a substantial visual redesign, first make a reviewable mockup in `docs/design/` and get the user's approval before changing native app UI.
- When the user has approved a specific mockup for native implementation, that approval covers the requested change. Do not ask for approval again while implementing it.

## Verification

- For Android behavior changes, run the relevant unit tests and build. Visually check significant screen changes when an Android emulator is available.
- Keep accessibility labels, RTL layout, font scaling, and touch targets in mind when changing Compose components.
