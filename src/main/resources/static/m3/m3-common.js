/* MORROW M3 UI helpers - browser integration preview, NOT authentication. */
(function () {
  'use strict';
  const root = '/m3/';
  const storageKey = 'morrow-m3-test-user';
  const userInput = document.getElementById('test-user');
  let flashTimeout;
  const date = (d) => [d.getFullYear(), String(d.getMonth() + 1).padStart(2, '0'), String(d.getDate()).padStart(2, '0')].join('-');
  const advance = (days) => { const d = new Date(); d.setDate(d.getDate() + days); return date(d); };
  const money = (number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(number) || 0);
  const el = (tag, className, content) => {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (content !== undefined) node.textContent = content;
    return node;
  };
  async function api(path, options = {}) {
    let response;
    try {
      response = await fetch(path, { ...options, headers: { 'Accept': 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers } });
    } catch (_) {
      throw new Error('Cannot connect to Spring Boot. Start the Java application and check PostgreSQL.');
    }
    let data;
    try { data = await response.json(); } catch (_) { data = {}; }
    if (!response.ok) throw new Error(data.message || data.detail || `Server rejected request (HTTP ${response.status}).`);
    return data;
  }
  function notify(message, error = false) {
    const toast = document.getElementById('toast');
    clearTimeout(flashTimeout);
    toast.textContent = message;
    toast.classList.toggle('error', error);
    toast.hidden = false;
    flashTimeout = setTimeout(() => { toast.hidden = true; }, 6000);
  }
  const isDateRange = (from, to) => from && to && from >= date(new Date()) && to > from;
  function getUserId() {
    const n = Number(userInput?.value);
    if (!Number.isSafeInteger(n) || n < 1) throw new Error('Enter an existing positive test user ID at the top of the page.');
    return n;
  }
  function setupDates(start, end) {
    if (!start || !end) return;
    const min = date(new Date());
    start.min = min;
    end.min = advance(1);
    if (!start.value) start.value = advance(1);
    if (!end.value) end.value = advance(4);
    start.addEventListener('change', () => {
      const d = new Date(start.value + 'T12:00:00');
      if (!Number.isNaN(d.getTime())) { d.setDate(d.getDate() + 1); end.min = date(d); }
      if (end.value <= start.value) end.value = end.min;
    });
  }
  function imageFor(vehicle) {
    const name = String(vehicle.name || vehicle.vehicleName || '').toLowerCase();
    if (name.includes('vf 3') || name.includes('vf3')) return root + 'assets/vinfast-vf3.jpg';
    if (name.includes('vf 5') || name.includes('vf5')) return root + 'assets/vinfast-vf5.jpg';
    if (name.includes('veloz')) return root + 'assets/vf-veloz.jpg';
    const raw = vehicle.imageUrl;
    if (typeof raw === 'string' && (/^https:\/\//.test(raw) || /^\/(?!\/)/.test(raw))) return raw;
    return root + 'assets/road-trip.jpg'; // illustrative fallback, not guaranteed vehicle photo
  }
  async function refreshCount() {
    const count = document.getElementById('cart-count');
    if (!count) return;
    try { count.textContent = (await api(`/api/carts/${getUserId()}`)).items.length; }
    catch (_) { count.textContent = '0'; }
  }
  if (userInput) {
    let saved = null;
    try { saved = localStorage.getItem(storageKey); } catch (_) { /* storage disabled */ }
    userInput.value = saved || '1';
    userInput.addEventListener('change', () => {
      try { localStorage.setItem(storageKey, userInput.value); } catch (_) { /* storage disabled */ }
      window.location.reload();
    });
  }
  document.addEventListener('DOMContentLoaded', refreshCount);
  window.M3 = { api, el, money, notify, getUserId, isDateRange, setupDates, imageFor, advance, refreshCount };
}());
