"""Cross-device/cross-browser safety net (the only implemented spec).

Replays the booking journey under seeded device descriptors and asserts
the page stays usable at every size:

- no horizontal page scroll (mobile-first contract)
- the reserve flow is completable at 375px (iPhone SE)
- venue-map tiles reachable through the swipe-to-explore scroller
- runs on whatever browsers pytest-playwright selects (CI: chromium +
  webkit, see .github/workflows/ci.yml)

The other specs (booking.spec.py, availability.spec.py) are documented
in SPECS-BACKLOG.md for the first QA automation engineer to implement.
"""
import re

import pytest
from playwright.sync_api import Page, expect

from tests import constants
from tests.conftest import login

CONFIRMATION_URL_PATTERN = re.compile(r"/reservations/[0-9a-f-]+/confirmation")


def _no_horizontal_scroll(page) -> None:
    """document must not scroll horizontally at this viewport size."""
    overflow = page.evaluate(
        "() => ({ sw: document.scrollingElement.scrollWidth,"
        "        iw: window.innerWidth })"
    )
    assert overflow["sw"] <= overflow["iw"], (
        f"horizontal page overflow: scrollWidth={overflow['sw']} "
        f"> viewport {overflow['iw']}"
    )


def book_first_field(page: Page, device: str | None = None) -> None:
    """Happy path: reserve Field 1 for the deterministic slot and confirm."""
    date = constants.booking_date(device)
    page.goto(f"{constants.BASE_URL}/reserve")
    page.wait_for_selector("#slot-form")

    page.fill("#date", date)
    page.select_option("#start", constants.START_HOUR)
    page.select_option("#durationMinutes", label=constants.DURATION_OPTION_HOUR)
    # reserve.js auto-submits the slot form on change; the reload renders the map
    page.wait_for_selector(".venue-map .resource", timeout=15_000)

    tile = page.locator(
        f".venue-map .resource[data-label='{constants.FIELD_1_LABEL}']"
    )
    expect(tile).to_be_visible()
    tile.click()

    expect(page.locator("#hidden-resource-id")).to_have_value(constants.FIELD_1_UUID)

    reserve_button = page.locator("#reserve-button")
    expect(reserve_button).to_be_enabled()
    reserve_button.click()

    # success -> confirmation page for the fresh reservation
    page.wait_for_url(CONFIRMATION_URL_PATTERN, timeout=30_000)
    expect(page.get_by_text(constants.PRICE_1H).first).to_be_visible()
    expect(page.get_by_text(constants.FIELD_1_LABEL).first).to_be_visible()

    page.goto(f"{constants.BASE_URL}/my-reservations")
    expect(page.get_by_text(constants.FIELD_1_LABEL).first).to_be_visible()


@pytest.mark.parametrize("device", list(constants.DEVICE_DESCRIPTORS))
def test_booking_journey_fits_and_works_on_devices(browser_name, browser, device, _stack_ready):
    """Replay the journey on seeded mobile/tablet devices + assert no overflow."""
    context = browser.new_context(**constants.DEVICE_DESCRIPTORS[device], locale="en-US")
    page = context.new_page()
    try:
        login(page, constants.CUSTOMER_USERNAME, constants.CUSTOMER_PASSWORD)
        book_first_field(page, device)
        _no_horizontal_scroll(page)
    finally:
        context.close()


def test_navbar_collapses_on_phone(browser, _stack_ready):
    """Below Bootstrap's md breakpoint the navbar switches to the hamburger."""
    context = browser.new_context(**constants.DEVICE_DESCRIPTORS["iPhone SE"], locale="en-US")
    page = context.new_page()
    try:
        page.goto(f"{constants.BASE_URL}/")
        expect(page.locator(".navbar-toggler")).to_be_visible()
        page.locator(".navbar-toggler").click()
        expect(page.locator("#mainNav").get_by_role("link", name="Make a reservation")).to_be_visible()
    finally:
        context.close()


def test_navbar_expands_on_desktop(browser, _stack_ready):
    context = browser.new_context(
        **constants.DEVICE_DESCRIPTORS["Desktop 1280"], locale="en-US")
    page = context.new_page()
    try:
        page.goto(f"{constants.BASE_URL}/")
        # Bootstrap HIDES the toggler at >=768px (element stays in the DOM)
        expect(page.locator(".navbar-toggler")).not_to_be_visible()
    finally:
        context.close()
