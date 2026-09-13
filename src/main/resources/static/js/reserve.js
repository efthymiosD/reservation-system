/*
 * Reservation page interaction (T6): selecting an available field on the SVG
 * venue map. Only client-side presentation — every rule (availability, pricing,
 * conflicts) stays on the server; the map state is refreshed by the server after
 * every slot change or failed submission.
 */
document.addEventListener('DOMContentLoaded', () => {
  // Bootstrap tooltips (held-slot / conflict hints on the disabled Reserve
  // button). Guarded: a missing Bootstrap bundle must never kill the rest.
  if (window.bootstrap) {
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => new bootstrap.Tooltip(el));
  }

  const slotForm = document.getElementById('slot-form');
  const updateButton = document.getElementById('update-availability-button');
  const fields = document.querySelectorAll('.venue-map .resource');
  const reserveButton = document.getElementById('reserve-button');
  const hiddenResourceId = document.getElementById('hidden-resource-id');
  const selectedFieldLabel = document.getElementById('selected-field-label');

  // Slot changes update the preview automatically; the button remains only as
  // the no-JavaScript fallback.
  if (slotForm) {
    slotForm.querySelectorAll('select, input').forEach(control => {
      control.addEventListener('change', () => slotForm.submit());
    });
    if (updateButton) {
      updateButton.classList.add('d-none');
    }
  }

  const STATE_LABELS = {
    'resource--available': '✓ free',
    'resource--reserved': '✕ reserved'
  };

  let selected = null;

  function stateClassOf(field) {
    return ['resource--available', 'resource--reserved']
        .find(cls => field.classList.contains(cls));
  }

  function restoreStateText(field) {
    const stateText = field.querySelector('.resource__state');
    if (stateText) {
      stateText.textContent = STATE_LABELS[stateClassOf(field)];
    }
  }

  function deselectCurrent() {
    if (!selected) return;
    selected.classList.remove('resource--selected');
    restoreStateText(selected);
    selected = null;
  }

  function select(field) {
    if (!field.classList.contains('resource--available')) return;  // reserved is not selectable
    deselectCurrent();
    selected = field;
    field.classList.add('resource--selected');
    const stateText = field.querySelector('.resource__state');
    if (stateText) {
      stateText.textContent = '✓ selected';
    }
    if (hiddenResourceId) {
      hiddenResourceId.value = field.dataset.resourceId;
    }
    if (selectedFieldLabel) {
      selectedFieldLabel.textContent = field.dataset.label || '';
    }
    if (reserveButton) {
      reserveButton.disabled = false;
    }
  }

  fields.forEach(field => {
    field.addEventListener('click', () => select(field));
    field.addEventListener('keydown', event => {
      if ((event.key === 'Enter' || event.key === ' ')
          && field.classList.contains('resource--available')) {
        event.preventDefault();
        select(field);
      }
    });
  });
});
