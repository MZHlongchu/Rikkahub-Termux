# Mode Injections

A mode injection is a reusable prompt block.

Use it when you want a named mode such as:
- study mode
- translation mode
- code review mode
- writing polish mode

## Quick Start

1. Create one injection.
2. Give it a name that describes the mode.
3. Choose whether it appears before or after the system prompt.
4. Put the actual instruction into the content.
5. Use priority to control order if multiple injections can be active together.

## Position

- **Before system prompt**: stronger framing for high-level behavior.
- **After system prompt**: safer default for most extra instructions.
