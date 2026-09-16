/*
 * /admin/map field-layout editor: drag fields on the SVG canvas (move via the
 * body, resize via the corner handle), snapping to a fixed grid. Pointer events
 * + the SVG CTM keep dragging consistent at any rendered size, including touch
 * screens. The number inputs in the side panel are kept in sync both ways, so a
 * plain form submit always persists the current geometry.
 */
(() => {
  'use strict';

  const GRID = 20;
  const MIN_SIZE = GRID * 2;
  const svg = document.getElementById('map-editor-svg');
  if (!svg) return;

  let canvasWidth = 1200;
  let canvasHeight = 400;
  const viewBox = svg.getAttribute('viewBox') || '';
  const parts = viewBox.split(/\s+/).map(Number);
  if (parts.length === 4) {
    canvasWidth = parts[2];
    canvasHeight = parts[3];
  }

  const SVG_NS = 'http://www.w3.org/2000/svg';
  const TOLERANCE = GRID * 0.4;

  const fields = Array.from(svg.querySelectorAll('.map-field'));
  if (fields.length === 0) return;

  function clamp(value, min, max) {
    return Math.min(Math.max(value, min), max);
  }

  function snap(value) {
    return Math.round(value / GRID) * GRID;
  }

  // Map a client (viewport) coordinate to SVG user units using the current CTM,
  // which accounts for any CSS scaling, zoom or scrolling (mobile-ready).
  function toUser(clientX, clientY) {
    const point = svg.createSVGPoint();
    point.x = clientX;
    point.y = clientY;
    const ctm = svg.getScreenCTM();
    return ctm ? point.matrixTransform(ctm.inverse()) : point;
  }

  function redraw(field) {
    const x = parseInt(field.dataset.x, 10) || 0;
    const y = parseInt(field.dataset.y, 10) || 0;
    const w = parseInt(field.dataset.w, 10) || 0;
    const h = parseInt(field.dataset.h, 10) || 0;
    const id = field.dataset.id;

    field.querySelector('.map-field__body').setAttribute('x', x);
    field.querySelector('.map-field__body').setAttribute('y', y);
    field.querySelector('.map-field__body').setAttribute('width', w);
    field.querySelector('.map-field__body').setAttribute('height', h);

    const label = field.querySelector('.map-field__label');
    label.setAttribute('x', x + w / 2);
    label.setAttribute('y', y + h / 2);

    field.querySelector('.map-field__handle').setAttribute('x', x + w - GRID);
    field.querySelector('.map-field__handle').setAttribute('y', y + h - GRID);

    for (const name of ['x', 'y', 'width', 'height']) {
      const input = document.getElementById(`${name}-${id}`);
      if (input) {
        input.value = name === 'x' ? x : name === 'y' ? y : name === 'width' ? w : h;
      }
    }
  }

  function startDrag(event, field, mode) {
    if (event.button !== 0 && event.button !== undefined) return;
    event.preventDefault();
    const origin = toUser(event.clientX, event.clientY);
    const start = {
      x: parseInt(field.dataset.x, 10) || 0,
      y: parseInt(field.dataset.y, 10) || 0,
      w: parseInt(field.dataset.w, 10) || 0,
      h: parseInt(field.dataset.h, 10) || 0
    };
    field.classList.add('map-field--dragging');
    svg.setPointerCapture(event.pointerId);

    function toEnd(event) {
      const cur = toUser(event.clientX, event.clientY);
      const dx = cur.x - origin.x;
      const dy = cur.y - origin.y;

      if (mode === 'resize') {
        const w = clamp(snap(start.w + dx), MIN_SIZE, canvasWidth - start.x);
        const h = clamp(snap(start.h + dy), MIN_SIZE, canvasHeight - start.y);
        field.dataset.w = w;
        field.dataset.h = h;
      } else {
        const x = clamp(snap(start.x + dx), 0, canvasWidth - start.w);
        const y = clamp(snap(start.y + dy), 0, canvasHeight - start.h);
        field.dataset.x = x;
        field.dataset.y = y;
      }
      redraw(field);
    }

    function onMove(event) {
      event.preventDefault();
      toEnd(event);
    }

    function cleanup() {
      field.classList.remove('map-field--dragging');
      svg.removeEventListener('pointermove', onMove);
      svg.removeEventListener('pointerup', cleanup);
      svg.removeEventListener('pointercancel', cleanup);
    }

    svg.addEventListener('pointermove', onMove);
    svg.addEventListener('pointerup', cleanup);
    svg.addEventListener('pointercancel', cleanup);
  }

  fields.forEach(field => {
    const body = field.querySelector('.map-field__body');
    const handle = field.querySelector('.map-field__handle');

    body.addEventListener('pointerdown', event => startDrag(event, field, 'move'));
    handle.addEventListener('pointerdown', event => startDrag(event, field, 'resize'));

    // Typed positions keep the SVG in sync (accessibility / touch fallback).
    field.dataset.x = body.getAttribute('x') || 0;
    field.dataset.y = body.getAttribute('y') || 0;
    field.dataset.w = body.getAttribute('width') || 0;
    field.dataset.h = body.getAttribute('height') || 0;

    for (const name of ['x', 'y', 'width', 'height']) {
      const input = document.getElementById(`${name}-${field.dataset.id}`);
      if (!input) continue;
      input.addEventListener('change', () => {
        const value = parseInt(input.value, 10);
        if (Number.isNaN(value)) return;
        const current = {
          x: parseInt(field.dataset.x, 10) || 0,
          y: parseInt(field.dataset.y, 10) || 0,
          w: parseInt(field.dataset.w, 10) || 0,
          h: parseInt(field.dataset.h, 10) || 0
        };
        const min = name === 'width' || name === 'height' ? MIN_SIZE : 0;
        const max = name === 'width' ? canvasWidth - current.x
            : name === 'height' ? canvasHeight - current.y
            : name === 'x' ? canvasWidth - current.w : canvasHeight - current.h;
        field.dataset[name.startsWith('w') ? 'w' : name.startsWith('h') ? 'h' : name] = clamp(value, min, max);
        redraw(field);
      });
    }
  });
})();