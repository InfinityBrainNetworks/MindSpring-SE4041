# MindSpring — Google Stitch export

Exported from Stitch project `8898709778068597225` on 2026-09-11, in two batches.
Each folder in `screens/` holds `code.html` (full Tailwind/HTML layout) and
`screen.png` (a preview). **Build from `code.html`** — the PNGs only capture the
visible viewport, and a few are blank (see below).

- `DESIGN.md` — design tokens (colours, type, radius, spacing) and component rules
- `logo.png` — 1024×1024 wordmark
- `shader.html` — animated teal/sage WebGL gradient used as a background

## Screens

| Area | Screen folder | Notes |
|---|---|---|
| Onboarding & auth | `onboarding`, `login`, `register` | |
| Home | `home_dashboard` | Mood hero card, habits, streaks, gratitude |
| | `home_dashboard_v2` | Same layout over the animated `shader.html` background |
| | `home_dashboard_perfected` | Adds a top bar (avatar · MindSpring · settings) |
| | `home_dashboard_dark` | Evening reflection, streak tile |
| Habits | `habits_list`, `habit_detail`, `habit_completed` | |
| | `add_habit` | Icon grid + weekday toggles |
| | `add_habit_v2` | "New Habit": hero image, Physical/Mental/Social, Daily/Weekly |
| Mood | `mood_check_in` / `mood_check_in_v2` | v2 differs mainly in background tint |
| | `mood_history` | Week/Month/Year list with swipe-to-delete |
| | `mood_history_v2` | Day strip + timeline with tips |
| Insights | `insights`, `insights_dark`, `insights_perfected` | |
| | `insight_exercise_mood` | Exercise vs mood drill-down, reflection, action |
| Calm | `calm_library` | Meditation of the day + practice categories |
| | `breathing_exercise`, `breathing_exercise_v2`, `breathing_exercise_dark` | |
| | `gratitude_journal` | |
| Profile | `settings_profile` | Edit profile, reminders, dark mode, export, clear data |
| | `profile_settings` | Alternate: account settings list, app theme toggle |

`_v2` marks a screen from the second export batch whose name clashed with the first.

## Blank or partial previews

These PNGs were captured before their fade-in animation finished; the
`code.html` is complete: `home_dashboard_v2`, `home_dashboard_perfected`,
`habit_completed`, `insights_perfected` (mood-trend chart missing).
