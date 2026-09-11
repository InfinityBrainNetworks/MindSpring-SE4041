---
name: MindSpring
colors:
  surface: '#f2fcf8'
  surface-dim: '#d2dcd9'
  surface-bright: '#f2fcf8'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#ecf6f2'
  surface-container: '#e6f0ec'
  surface-container-high: '#e0eae7'
  surface-container-highest: '#dbe5e1'
  on-surface: '#141d1b'
  on-surface-variant: '#404947'
  inverse-surface: '#293230'
  inverse-on-surface: '#e9f3ef'
  outline: '#707977'
  outline-variant: '#bfc8c6'
  surface-tint: '#2d6862'
  primary: '#003935'
  on-primary: '#ffffff'
  primary-container: '#10514c'
  on-primary-container: '#88c2bb'
  inverse-primary: '#97d1ca'
  secondary: '#835400'
  on-secondary: '#ffffff'
  secondary-container: '#feb64e'
  on-secondary-container: '#714800'
  tertiary: '#2f3330'
  on-tertiary: '#ffffff'
  tertiary-container: '#464946'
  on-tertiary-container: '#b5b8b3'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#b2eee7'
  primary-fixed-dim: '#97d1ca'
  on-primary-fixed: '#00201d'
  on-primary-fixed-variant: '#0d4f4a'
  secondary-fixed: '#ffddb5'
  secondary-fixed-dim: '#ffb956'
  on-secondary-fixed: '#2a1800'
  on-secondary-fixed-variant: '#643f00'
  tertiary-fixed: '#e1e3de'
  tertiary-fixed-dim: '#c5c7c3'
  on-tertiary-fixed: '#191c1a'
  on-tertiary-fixed-variant: '#444844'
  background: '#f2fcf8'
  on-background: '#141d1b'
  surface-variant: '#dbe5e1'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.02em
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 22px
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  screen_padding: 24px
  card_padding: 20px
  gutter: 16px
  stack_sm: 8px
  stack_md: 16px
  stack_lg: 32px
---

## Brand & Style
The design system is built on a "Grounded Vitality" narrative, specifically tailored for the academic environment. It focuses on reducing cognitive load through generous whitespace and a sophisticated Material 3 implementation. The aesthetic balances the stability of deep botanical tones with the warmth of sunlight-inspired accents, creating an interface that feels both scholarly and supportive.

The style is **Modern Corporate with Tactile Softness**, utilizing high-radius containers and diffuse shadows to mimic physical paper and smooth stones. This creates a safe, non-judgmental digital space that encourages daily reflection and habit formation.

## Colors
The palette follows a strict 60-30-10 distribution to maintain visual hierarchy and emotional regulation.

- **Primary (Deep Teal):** Used for structural elements like top app bars, bottom navigation backgrounds, and active selection states. It represents depth and grounding.
- **Accent (Warm Amber):** Reserved exclusively for high-priority calls to action, progress streaks, and mood highlights. It provides a warm, optimistic contrast to the teal.
- **Neutral/Background:** A pale sage off-white serves as the canvas, reducing eye strain compared to pure white.
- **Dark Mode:** When active, the background shifts to a deep obsidian-green, and surfaces transition to a slightly elevated charcoal-teal to maintain depth without losing the brand's organic feel.

## Typography
This design system utilizes **Plus Jakarta Sans** for all tiers to maintain a modern, friendly, and legible character. 

- **Display:** Used for welcome headers and high-level summary numbers (e.g., total meditation minutes).
- **Title:** Used for card headers and section titles.
- **Body:** The workhorse for habit descriptions and journaling entries. The 15px size ensures high readability for student users who may be experiencing fatigue.
- **Label/Caption:** Used for metadata, timestamps, and supporting text within charts or navigation icons.

## Layout & Spacing
The layout adheres to an **8dp square grid** system. To foster a sense of openness and "breathing room," the system employs a generous **24dp global screen padding**.

- **Grid Model:** A 4-column fluid grid for mobile, expanding to 8 or 12 for larger viewports.
- **Card Spacing:** Elements within cards should maintain a minimum of 20dp padding to match the corner radius, creating a consistent internal visual rhythm.
- **Vertical Rhythm:** Use 32dp (stack_lg) to separate major sections, and 16dp (stack_md) for related content groups.

## Elevation & Depth
Depth is conveyed through **Soft Ambient Shadows** rather than traditional Material 2 sharp shadows. Surfaces should feel like they are floating slightly above the sage background.

- **Level 0 (Background):** #F4F6F1, no shadow.
- **Level 1 (Content Cards):** #FFFFFF, 20px radius, Shadow: 0px 4px 20px rgba(27, 36, 34, 0.06).
- **Level 2 (Floating Action Buttons/Modals):** Shadow: 0px 8px 30px rgba(27, 36, 34, 0.12).
- **Navigation:** The bottom bar is flat (Level 0 elevation) but uses color (Primary Teal) to distinguish itself from the content area.

## Shapes
The shape language is defined by **High-Radius Geometry**. 

- **Cards:** Use a 20px (`rounded-xl` equivalent in this system) corner radius to evoke a soft, organic feel.
- **Buttons:** All buttons are fully "Pill-shaped" (circular caps) to differentiate them from the rectangular card structures and imply a friendly, tappable nature.
- **Input Fields:** Use an 8px radius for a slightly more structured look compared to buttons, ensuring they feel like "containers" for information.

## Components

### Bottom Navigation
- **Background:** Primary Teal (#10514C).
- **Active State:** Icon and label transition to Warm Amber (#E8A33D).
- **Inactive State:** On-Teal (#EAF2EF) at 70% opacity.

### Buttons
- **Primary CTA:** Pill-shaped, Warm Amber background with Deep Charcoal text. Used for "Start Session" or "Complete Habit."
- **Secondary/Action:** Pill-shaped, Primary Teal background with On-Teal text. Used for "Add Note" or "View Details."
- **Ghost:** Primary Teal text, no background. Used for "Cancel" or "Skip."

### Cards
- **Style:** White background, 20px radius, soft diffuse shadow.
- **Interaction:** On press, the card should scale slightly (98%) and increase shadow density.

### Input Fields
- **Style:** Outlined, 8px radius.
- **Inactive Border:** Secondary Text (#5C6B67) at 40% opacity.
- **Focused Border:** Primary Teal (#10514C) at 2px thickness.
- **Label:** Floating label style as per Material 3 specs.

### Mood Selector
- **UI:** A horizontal row of 5 circular icons.
- **Selection:** When a mood is selected, it expands slightly and gains a Warm Amber ring highlight, with the background of the circle changing to the specific Mood Scale color.

### Progress Bars
- **Track:** Secondary Text (#5C6B67) at 10% opacity.
- **Indicator:** Warm Amber for habit streaks; Primary Teal for general completion.