/* General web-UI bootstrapping: Bootstrap popovers (hovers/focus) used in the
 * navigation bar and table hints. Deferred so the page is fully parsed first. */
document.addEventListener('DOMContentLoaded', () => {
  if (window.bootstrap) {
    document.querySelectorAll('[data-bs-toggle="popover"]').forEach(el => new bootstrap.Popover(el));
  }
});
