FKP_2 v1.25 — System-wide Mouse

The top-row ➤ mouse button opens a touch control panel for a system-wide
screen cursor.

Features:
- Cursor overlay on the actual Android screen, including browser/web pages.
- Touchpad movement from the keyboard mouse panel.
- Separate left-click and right-click controls are exposed.
- Four directional controls with repeat.
- Cursor contrast adapts from a screenshot sample under the cursor:
  dark background -> white cursor; light background -> black cursor.
- Cursor has a contrasting outline.
- Main keyboard layout is unchanged.
- Accessibility must be enabled for the FKP_2 mouse service in Android settings.

Important Android limitation:
AccessibilityService gesture injection provides a screen tap, but Android's
public accessibility gesture API does not provide a true right-button mouse
event. The right-click control is therefore exposed in the UI but currently
uses the same screen-tap mechanism rather than falsely claiming native
right-button injection.
