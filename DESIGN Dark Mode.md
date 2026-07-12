---
name: Nocturnal Study
colors:
  surface: '#151311'
  surface-dim: '#151311'
  surface-bright: '#3b3936'
  surface-container-lowest: '#100e0c'
  surface-container-low: '#1d1b19'
  surface-container: '#211f1d'
  surface-container-high: '#2c2927'
  surface-container-highest: '#373432'
  on-surface: '#e7e1de'
  on-surface-variant: '#ddc0c0'
  inverse-surface: '#e7e1de'
  inverse-on-surface: '#32302e'
  outline: '#a48a8b'
  outline-variant: '#564242'
  surface-tint: '#ffb2b6'
  primary: '#ffb2b6'
  on-primary: '#65061c'
  primary-container: '#8b2635'
  on-primary-container: '#ffa3a9'
  inverse-primary: '#a43946'
  secondary: '#cfc5b9'
  on-secondary: '#352f27'
  secondary-container: '#4f483f'
  on-secondary-container: '#c1b7ab'
  tertiary: '#d1c4bd'
  on-tertiary: '#372f2a'
  tertiary-container: '#534a45'
  on-tertiary-container: '#c7bab3'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdadb'
  primary-fixed-dim: '#ffb2b6'
  on-primary-fixed: '#40000d'
  on-primary-fixed-variant: '#842131'
  secondary-fixed: '#ece1d5'
  secondary-fixed-dim: '#cfc5b9'
  on-secondary-fixed: '#201b13'
  on-secondary-fixed-variant: '#4c463d'
  tertiary-fixed: '#eee0d8'
  tertiary-fixed-dim: '#d1c4bd'
  on-tertiary-fixed: '#211a16'
  on-tertiary-fixed-variant: '#4e4540'
  background: '#151311'
  on-background: '#e7e1de'
  surface-variant: '#373432'
typography:
  headline-xl:
    fontFamily: Playfair Display
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-xl-mobile:
    fontFamily: Playfair Display
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Playfair Display
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-md:
    fontFamily: Playfair Display
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Source Serif 4
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Source Serif 4
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.03em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  container-max: 1200px
  gutter: 24px
  margin-mobile: 20px
  margin-desktop: 64px
---

## Brand & Style
The brand personality evokes the quiet, focused atmosphere of a private library at midnight. It targets an audience that values intellectual depth, slow-form content, and premium craftsmanship. The emotional response is one of calm, curiosity, and sophisticated focus.

The design style is **Minimalist-Editorial** with a **Tactile** edge. It leans into high-quality typography and generous negative space to create a sense of breathability within a dark environment. The transition from a daylight aesthetic to this system should feel like a shift in illumination—where surfaces remain matte and soft, but shadows deepen and highlights are reserved for intentional focal points.

## Colors
The palette is rooted in a deep charcoal-brown neutral (#1A1816) that serves as the "ink-black" substrate, providing a softer visual experience than true black. 

The primary accent is a muted Mahogany (#8B2635), used sparingly for calls to action and navigational cues to maintain the "candlelit" metaphor. The secondary color is a warm Parchment (#E5DACE), used primarily for text and high-contrast iconography to ensure readability against the dark ground. Tertiary tones are used for container backgrounds and subtle borders, creating a layered effect of wood-grained depths.

## Typography
Typography is the cornerstone of the literary experience. **Playfair Display** is used for all headlines to provide a high-contrast, editorial feel that mimics traditional book titling. 

**Source Serif 4** handles body text, chosen for its exceptional legibility in dark mode and its "bookish" character. For functional elements like navigation, tags, and small labels, **Inter** provides a clean, modern counterpoint that ensures the UI doesn't feel overly archaic. All serif text should utilize slightly increased line height to prevent the "glow" effect common in white-on-dark text displays.

## Layout & Spacing
The layout follows a **Fixed Grid** philosophy on desktop to mimic the centered, focused column of a printed book page. On mobile, the system shifts to a fluid grid with generous side margins (20px) to maintain the sense of a physical object.

Spacing is governed by an 8px rhythm. Use "Macro-spacing" (64px+) between major sections to emphasize the minimalist aesthetic. Containers should have internal padding that scales with the screen size, ensuring that text never feels crowded against its boundaries.

## Elevation & Depth
Depth is conveyed through **Tonal Layers** rather than heavy shadows. The base background is the deepest tone, with surface containers using the tertiary mahogany-charcoal mix to appear slightly closer to the viewer.

When shadows are necessary, use a "Deep Amber" tint (#2A1A10) with a high blur radius and very low opacity (15-20%). This mimics the way light diffuses in a dimly lit room. Interactive elements may use a subtle 1px inner-border (stroke) of a lighter wood-tone to define edges without breaking the matte aesthetic.

## Shapes
The shape language is defined by the "Rounded Eight" principle (8px corners). This provides a soft, organic feel that bridges the gap between the sharpness of serif type and the comfort of the "Nocturnal Study" theme. 

Larger containers (Cards, Modals) utilize `rounded-lg` (16px) or `rounded-xl` (24px) to feel like substantial, tactile objects—like the leather corners of a blotter or the rounded spine of a classic volume.

## Components
- **Buttons:** Primary buttons use a solid Mahogany fill with Parchment text. Secondary buttons use a Parchment outline with a subtle 5% fill on hover.
- **Input Fields:** Use a dark-charcoal background with a 1px border in the tertiary tone. Focus states transition the border color to Mahogany.
- **Cards:** Use a slightly lighter neutral than the background. Forgo heavy shadows in favor of a subtle 1px border that is only 10% lighter than the card background itself.
- **Lists:** Items are separated by thin, low-opacity rules (10% Parchment).
- **Chips/Tags:** Small, pill-shaped elements using the tertiary color for the background and a medium-weight Inter typeface for the label.
- **Reading Progress:** A thin Mahogany bar at the top of the viewport to indicate depth within a long-form article.