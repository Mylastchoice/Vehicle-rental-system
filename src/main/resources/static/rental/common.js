/* MORROW rental UI helpers - browser integration preview, NOT authentication. */
(function () {
  'use strict';
  const root = '/rental/';
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
      response = await fetch(path, { ...options, credentials: 'same-origin', headers: { 'Accept': 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers } });
    } catch (_) {
      throw new Error('Cannot connect to Spring Boot. Start the Java application and check PostgreSQL.');
    }
    let data;
    try { data = await response.json(); } catch (_) { data = {}; }
    if (response.status === 401) throw new Error('Please sign in with your member account before using the rental cart.');
    if (response.status === 403) throw new Error('Your account is not authorized to use the rental cart.');
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
    try { count.textContent = (await api('/api/carts/me')).items.length; }
    catch (_) { count.textContent = '0'; }
  }
  async function refreshAccountLink() {
    const link = document.getElementById('account-link');
    if (!link) return;
    const response = await fetch('/api/me', { headers: { Accept: 'application/json' } }).catch(() => null);
    if (!response?.ok) return;
    const { roles } = await response.json();
    if (roles.includes('ROLE_ADMIN')) {
      link.href = '/admin/bookings';
      link.textContent = 'Manage bookings ↗';
    } else if (roles.includes('ROLE_MEMBER')) {
      link.href = '/profile';
      link.textContent = 'My profile ↗';
      document.getElementById('bookings-link').hidden = false;
    }
  }
  document.addEventListener('DOMContentLoaded', refreshCount);
  document.addEventListener('DOMContentLoaded', refreshAccountLink);
  window.RentalUI = { api, el, money, notify, isDateRange, setupDates, imageFor, advance, refreshCount };
}());
