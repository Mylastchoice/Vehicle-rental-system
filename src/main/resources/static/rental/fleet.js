(function () {
  'use strict';
  const { api, el, money, notify, isDateRange, setupDates, imageFor } = window.RentalUI;
  const grid = document.getElementById('vehicle-grid');
  const form = document.getElementById('fleet-form');
  const pickup = document.getElementById('pickup-date');
  const returned = document.getElementById('return-date');
  const selected = document.getElementById('vehicle-select');
  const category = document.getElementById('category-select');
  const branch = document.getElementById('branch-select');
  const keyword = document.getElementById('vehicle-keyword');
  const message = document.getElementById('fleet-message');
  let vehicles = [];
  setupDates(pickup, returned);

  function render(list, checked = false) {
    grid.replaceChildren();
    if (!list.length) { grid.append(el('div', 'skeleton', 'No matching vehicles are available for these filters and dates.')); return; }
    for (const v of list) {
      const card = el('article', 'vehicle-card');
      const media = el('div', 'vehicle-media');
      const img = el('img'); img.src = imageFor(v); img.alt = `Vehicle image for ${v.name}`; img.loading = 'lazy';
      const available = !checked || v.availableForDates === true;
      media.append(img, el('span', 'pill ' + (v.status === 'Available' && available ? 'pill-green' : ''), checked ? (available ? 'Available for dates' : 'Not available') : v.status));
      const body = el('div', 'vehicle-body');
      body.append(el('span', 'overline', `CODE / ${v.vehicleCode}`), el('h3', '', v.name),
        el('p', 'meta', `${v.categoryName || ''} · ${v.branchName} · ${v.status}`));
      const bottom = el('div', 'vehicle-bottom');
      const priceDiv = el('div'); priceDiv.append(el('div', 'price', money(Number(v.dailyPrice) * (1 - Number(v.discountRate || 0)))), el('small', '', ' per day'));
      const actions = el('div', 'vehicle-actions');
      const details = el('a', 'text-link', 'View details ↗');
      details.href = `/rental/vehicle.html?code=${encodeURIComponent(v.vehicleCode)}`;
      const button = el('button', 'btn btn-dark', 'Check dates ↗');
      button.type = 'button'; button.disabled = v.status !== 'Available' || (checked && !available);
      button.addEventListener('click', async () => {
        if (!isDateRange(pickup.value, returned.value)) { notify('Choose valid pickup and return dates.', true); pickup.focus(); return; }
        button.disabled = true; button.textContent = 'Checking…';
        try {
          const query = new URLSearchParams({ pickupDate: pickup.value, returnDate: returned.value });
          const status = await api(`/api/vehicles/${encodeURIComponent(v.vehicleCode)}/availability?${query}`);
          if (!status.available) { notify(`${v.name} is unavailable for these dates.`, true); return; }
          await api('/api/carts/me/items', { method: 'POST', body: JSON.stringify({ vehicleCode: v.vehicleCode, pickupDate: pickup.value, returnDate: returned.value }) });
          window.location.href = '/rental/cart.html';
        } catch (error) { notify(error.message, true); }
        finally { button.disabled = false; button.textContent = 'Check dates ↗'; }
      });
      actions.append(details, button);
      bottom.append(priceDiv, actions); body.append(bottom); card.append(media, body); grid.append(card);
    }
  }

  function candidates() {
    const term = keyword.value.trim().toLowerCase();
    return vehicles.filter(v => (!selected.value || v.vehicleCode === selected.value)
      && (!category.value || String(v.categoryId) === category.value)
      && (!branch.value || String(v.branchId) === branch.value)
      && (!term || String(v.name).toLowerCase().includes(term) || String(v.vehicleCode).toLowerCase().includes(term)));
  }

  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (!isDateRange(pickup.value, returned.value)) { notify('Please select valid pickup and return dates.', true); return; }
    const button = form.querySelector('button[type=submit]');
    const matches = candidates();
    button.disabled = true; button.textContent = 'Checking live availability…';
    message.hidden = false; message.textContent = 'Checking live availability in PostgreSQL…';
    try {
      // Bounded parallelism avoids flooding Spring Boot with dozens of requests at once.
      const checked = [];
      for (let i = 0; i < matches.length; i += 5) {
        const chunk = matches.slice(i, i + 5);
        const results = await Promise.all(chunk.map(async v => {
          if (v.status !== 'Available') return { ...v, availableForDates: false };
          const query = new URLSearchParams({ pickupDate: pickup.value, returnDate: returned.value });
          const response = await api(`/api/vehicles/${encodeURIComponent(v.vehicleCode)}/availability?${query}`);
          return { ...v, availableForDates: response.available === true };
        }));
        checked.push(...results);
      }
      const available = checked.filter(v => v.availableForDates);
      render(checked, true);
      message.textContent = `${available.length} of ${checked.length} matching cars are available for ${pickup.value} → ${returned.value}.`;
      document.getElementById('fleet').scrollIntoView({ behavior: 'smooth' });
    } catch (error) {
      message.textContent = `Cannot verify availability: ${error.message}`;
      grid.replaceChildren(el('div', 'skeleton', 'Please retry the search when the server is available.'));
    } finally { button.disabled = false; button.textContent = 'Find your car ↗'; }
  });

  (async () => {
    try {
      vehicles = await api('/api/vehicle-options');
      selected.replaceChildren(el('option', '', 'All vehicles')); selected.firstChild.value = '';
      const categories = new Map(), branches = new Map();
      for (const v of vehicles) {
        const opt = el('option', '', `${v.name} (${v.vehicleCode})`); opt.value = v.vehicleCode; selected.append(opt);
        categories.set(String(v.categoryId), v.categoryName);
        branches.set(String(v.branchId), v.branchName);
      }
      category.replaceChildren(el('option', '', 'All types')); category.firstChild.value = '';
      for (const [id, name] of categories) { const opt = el('option', '', name); opt.value = id; category.append(opt); }
      branch.replaceChildren(el('option', '', 'All branches')); branch.firstChild.value = '';
      for (const [id, name] of branches) { const opt = el('option', '', name); opt.value = id; branch.append(opt); }
      render(vehicles);
      if (!vehicles.length) { message.textContent = 'The database currently has no vehicles. Apply the shared dev seed to the demo database.'; message.hidden = false; }
    } catch (error) {
      grid.replaceChildren(el('div', 'skeleton', 'Vehicle data could not be loaded.'));
      message.textContent = `Backend / DB unavailable: ${error.message}`; message.hidden = false;
    }
  })();
}());
