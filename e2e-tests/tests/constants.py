"""Static constants for the e2e suite.

The app runs with a fixed clock (docker-compose.e2e.yml:
APP_CLOCK_FIXED_INSTANT=2026-10-12T10:00:00Z), so every date the tests
book is deterministic. 14 days ahead of the clock date is comfortably
inside the P1M advance window.
"""
import os

BASE_URL = os.environ.get("E2E_BASE_URL", "http://localhost:8080")

# The app's fixed "today" is 2026-10-12 (UTC clock; venue is Europe/Warsaw).
# Each journey books clockDate + 14d + deviceOffset, so every run is
# byte-for-byte identical AND device tests never collide on the same slot
# (cancellation deadline 120 min makes same-slot cleanup in-test impossible).
BOOKING_DATE = "2026-10-26"

# Per-device booking date offsets from BOOKING_DATE (deterministic spread
# across the advance window; each device gets its own exclusive day).
DEVICE_DATE_OFFSETS = {
    "iPhone SE": 0,
    "Pixel 7": 1,
    "iPhone 14": 2,
    "iPad (gen 7)": 3,
    "Desktop 1280": 4,
}

# Per-browser offsets STRIDE on top of the device offsets, so the same device
# running under two browsers never collides (cc CI runs chromium AND webkit).
# Max effective offset: 4 (device) + 7 (webkit) = 11 days; firefox +14 keeps
# every date within the P1M advance window (max date 2026-11-09 < 2026-11-12).
BROWSER_DATE_OFFSETS = {
    "chromium": 0,
    "webkit": 7,
    "firefox": 14,
}

def booking_date(device: str | None = None, browser: str = "chromium") -> str:
    """Deterministic slot date; device- AND browser-specific when given."""
    from datetime import date, timedelta
    base = date.fromisoformat(BOOKING_DATE)
    device_offset = DEVICE_DATE_OFFSETS.get(device, 0) if device else 0
    browser_offset = BROWSER_DATE_OFFSETS.get(browser, 0)
    return (base + timedelta(days=device_offset + browser_offset)).isoformat()

# Slot pick (inside the seeded 14:00-23:00 opening hours, on the 30-min grid)
START_HOUR = "18:00"
DURATION_OPTION_HOUR = "1 h"

# Seeded resources (V2__seed_data.sql): Field 1 = ...101
FIELD_1_LABEL = "Field 1"
FIELD_1_UUID = "a0000000-0000-0000-0000-000000000101"

# Seeded pricing: 80.00 PLN/h -> 1 h = 80.00 PLN
PRICE_1H = "80.00"

# Keycloak test users (keycloak/realm-export.json)
CUSTOMER_USERNAME = "alice"
CUSTOMER_PASSWORD = "alice"

# Keycloak container base (browser-facing, pinned via KC_HOSTNAME_URL)
KEYCLOAK_BASE = "http://localhost:8081"

# Device context options for the responsive safety net. Explicit viewport
# descriptors instead of the runtime Playwright registry: deterministic and
# valid module imports without touching playwright's event loop.
# iPhone SE  is our smallest supported viewport (375x667).
DEVICE_DESCRIPTORS = {
    "iPhone SE": {
        "viewport": {"width": 375, "height": 667},
        "device_scale_factor": 2,
        "is_mobile": True,
        "has_touch": True,
    },
    "Pixel 7": {
        "viewport": {"width": 412, "height": 915},
        "device_scale_factor": 2.625,
        "is_mobile": True,
        "has_touch": True,
    },
    "iPhone 14": {
        "viewport": {"width": 390, "height": 844},
        "device_scale_factor": 3,
        "is_mobile": True,
        "has_touch": True,
    },
    "iPad (gen 7)": {
        "viewport": {"width": 810, "height": 1080},
        "device_scale_factor": 2,
        "is_mobile": True,
        "has_touch": True,
    },
    "Desktop 1280": {
        "viewport": {"width": 1280, "height": 800},
        "device_scale_factor": 1,
        "is_mobile": False,
        "has_touch": False,
    },
}
