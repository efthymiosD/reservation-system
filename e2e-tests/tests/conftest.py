"""Pytest fixtures for the e2e suite.

The suite drives the real OAuth2 authorization-code flow against the
dockerized Keycloak: the login fixture opens the app's login link,
fills Keycloak's own login form and ends up authenticated in the
browser session. No API-level shortcuts - the browser does what a real
user does.
"""
import urllib.request

import pytest
from playwright.sync_api import Page

from tests import constants

REALM_WELL_KNOWN = f"{constants.KEYCLOAK_BASE}/realms/reservation/.well-known/openid-configuration"
APP_HEALTH = f"{constants.BASE_URL}/actuator/health"


@pytest.fixture(scope="session")
def _stack_ready():
    """Fail fast (with a readable message) when the dockerized stack is not up."""
    for url, hint in (
        (APP_HEALTH, "app not running - run: docker compose -f docker-compose.e2e.yml up --build -d"),
        (REALM_WELL_KNOWN, "keycloak realm not up - wait or re-run: docker compose -f docker-compose.e2e.yml up -d"),
    ):
        try:
            with urllib.request.urlopen(url, timeout=5) as resp:
                assert resp.status == 200, f"{url} -> {resp.status}"
        except Exception as exc:  # noqa: BLE001
            pytest.fail(f"e2e stack not reachable: {hint} (error: {exc})")


def login(page: Page, username: str, password: str) -> None:
    """Drive the real Keycloak login form (fresh session via prompt=login)."""
    page.goto(f"{constants.BASE_URL}/oauth2/authorization/keycloak")
    page.wait_for_selector("#username", timeout=30_000)
    page.fill("#username", username)
    page.fill("#password", password)
    page.click("button[type=submit], input[type=submit][name=login]")
    # Back on the app: the OAuth2 callback chain ends on the home page.
    page.wait_for_url(f"{constants.BASE_URL}/*", timeout=30_000)


@pytest.fixture
def customer_page(_stack_ready, page: Page) -> Page:
    """A browser session logged in as the seeded CUSTOMER (alice)."""
    login(page, constants.CUSTOMER_USERNAME, constants.CUSTOMER_PASSWORD)
    return page

