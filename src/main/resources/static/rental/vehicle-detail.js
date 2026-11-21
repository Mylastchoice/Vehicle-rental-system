(function () {
  'use strict';
  const { api, money, notify, setupDates, isDateRange, imageFor } = window.RentalUI;
  const params = new URLSearchParams(location.search);
  const code = params.get('code');
  const form = document.getElementById('detail-form');
  const pickup = document.getElementById('detail-pickup');
  const returned = document.getElementById('detail-return');
  let vehicle;
  setupDates(pickup, returned);
  form.addEventListener('submit', async e => {
    e.preventDefault();
    if (!vehicle || !isDateRange(pickup.value, returned.value)) {
      notify('Choose valid pickup and return dates.', true); return;
    }
    try {
      const query = new URLSearchParams({ pickupDate: pickup.value, returnDate: returned.value });
      const availability = await api(`/api/vehicles/${encodeURIComponent(code)}/availability?${query}`);
      if (!availability.available) { notify('This car is unavailable for your dates.', true); return; }
      await api('/api/carts/me/items', { method: 'POST',
        body: JSON.stringify({ vehicleCode: code, pickupDate: pickup.value, returnDate: returned.value }) });
      location.href = '/rental/cart.html';
    } catch (error) { notify(error.message, true); }
  });
  (async () => {
    try {
      const vehicles = await api('/api/vehicle-options');
      vehicle = vehicles.find(v => v.vehicleCode === code);
      if (!vehicle) throw new Error('Vehicle not found.');
      document.getElementById('detail-title').textContent = vehicle.name;
      document.getElementById('detail-description').textContent = vehicle.detailedDescription || vehicle.shortDescription || 'No additional description provided.';
      document.getElementById('detail-category').textContent = `Type: ${vehicle.categoryName || '—'}`;
      document.getElementById('detail-branch').textContent = `Pickup branch: ${vehicle.branchName || '—'}`;
      document.getElementById('detail-plate').textContent = `Plate: ${vehicle.licensePlate || '—'}`;
      document.getElementById('detail-price').textContent = money(Number(vehicle.dailyPrice) * (1 - Number(vehicle.discountRate || 0))) + ' / day';
      const img = document.getElementById('detail-image'); img.src = imageFor(vehicle); img.alt = vehicle.name;
      if (vehicle.status !== 'Available') {
        document.querySelector('#detail-form button').disabled = true;
        document.getElementById('detail-warning').textContent = `This vehicle is currently ${vehicle.status}.`;
        document.getElementById('detail-warning').hidden = false;
      }
    } catch (error) { notify(error.message, true); document.getElementById('detail-title').textContent = 'Vehicle not found'; }
  })();
}());
