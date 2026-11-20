(function () {
  'use strict';
  const { api, el, money, notify, getUserId, isDateRange, setupDates, imageFor } = window.M3;
  const grid = document.getElementById('vehicle-grid');
  const form = document.getElementById('fleet-form');
  const pickup = document.getElementById('pickup-date');
  const returned = document.getElementById('return-date');
  const selected = document.getElementById('vehicle-select');
  const message = document.getElementById('fleet-message');
  let vehicles = [];
  setupDates(pickup, returned);

  function render(list) {
    grid.replaceChildren();
    if (!list.length) {
      grid.append(el('div', 'skeleton', 'No matching vehicles. Try another filter or ask M1 to add test data.'));
      return;
    }
    for (const v of list) {
      const card = el('article', 'vehicle-card');
      const media = el('div', 'vehicle-media');
      const img = el('img'); img.src = imageFor(v); img.alt = `Vehicle image for ${v.name}`; img.loading = 'lazy';
      media.append(img, el('span', 'pill ' + (v.status === 'Available' ? 'pill-green' : ''), v.status));
      const body = el('div', 'vehicle-body');
      body.append(el('span', 'overline', `CODE / ${v.vehicleCode}`), el('h3', '', v.name), el('p', 'meta', `${v.branchName} · ${v.status}`));
      const bottom = el('div', 'vehicle-bottom');
      const priceDiv = el('div'); priceDiv.append(el('div', 'price', money(Number(v.dailyPrice) * (1 - Number(v.discountRate || 0)))), el('small', '', ' per day'));
      const button = el('button', 'btn btn-dark', 'Check dates ↗');
      button.type = 'button'; button.disabled = v.status !== 'Available';
      button.addEventListener('click', async () => {
        if (!isDateRange(pickup.value, returned.value)) {
          notify('Choose valid rental dates. The return date must be after pickup, and pickup cannot be in the past.', true);
          pickup.focus(); return;
        }
        button.disabled = true; button.textContent = 'Checking…';
        try {
          const query = new URLSearchParams({ pickupDate: pickup.value, returnDate: returned.value });
          const status = await api(`/api/vehicles/${encodeURIComponent(v.vehicleCode)}/availability?${query}`);
          if (!status.available) { notify(`${v.name} is unavailable for these dates.`, true); return; }
          await api(`/api/carts/${getUserId()}/items`, { method: 'POST', body: JSON.stringify({ vehicleCode: v.vehicleCode, pickupDate: pickup.value, returnDate: returned.value }) });
          window.location.href = '/m3/cart.html';
        } catch (error) { notify(error.message, true); }
        finally { button.disabled = false; button.textContent = 'Check dates ↗'; }
      });
      bottom.append(priceDiv, button); body.append(bottom); card.append(media, body); grid.append(card);
    }
  }
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    if (!isDateRange(pickup.value, returned.value)) { notify('Please select valid pickup and return dates.', true); return; }
    const code = selected.value;
    const list = code ? vehicles.filter(v => v.vehicleCode === code) : vehicles;
    render(list);
    document.getElementById('fleet').scrollIntoView({ behavior: 'smooth' });
  });
  (async () => {
    try {
      vehicles = await api('/api/m3/vehicle-options');
      selected.replaceChildren(el('option', '', 'All vehicles'));
      selected.firstChild.value = '';
      for (const v of vehicles) { const opt = el('option', '', `${v.name} (${v.vehicleCode})`); opt.value = v.vehicleCode; selected.append(opt); }
      render(vehicles);
      if (!vehicles.length) { message.textContent = 'Your vehicle table has no cars yet. Ask M1/M5 to add vehicle records and branches. No sample booking records are invented.'; message.hidden = false; }
    } catch (error) {
      grid.replaceChildren(el('div', 'skeleton', 'Vehicle data could not be loaded.'));
      message.textContent = `Backend / DB unavailable: ${error.message}`; message.hidden = false;
    }
  })();
}());
