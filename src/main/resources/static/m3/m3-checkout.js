(function () {
  'use strict';
  const { api, el, money, notify, getUserId, isDateRange, imageFor } = window.M3;
  const form = document.getElementById('checkout-form');
  const submit = document.getElementById('place-order');
  const errorBox = document.getElementById('checkout-error');
  const host = document.getElementById('checkout-items');
  let items = [];
  let sending = false;
  async function initialize() {
    try {
      const data = await api(`/api/carts/${getUserId()}`);
      items = data.items || [];
      host.replaceChildren();
      if (!items.length) { host.append(el('div', 'notice warning', 'Your cart is empty. Choose a vehicle first.')); return; }
      const dateGroups = new Set(items.map(x => `${x.pickupDate}|${x.returnDate}`));
      const branchGroups = new Set(items.map(x => String(x.branchId)));
      if (dateGroups.size !== 1 || branchGroups.size !== 1 || items.some(x => !isDateRange(x.pickupDate, x.returnDate))) {
        host.append(el('div', 'notice warning', 'All vehicles must use the same valid dates and pickup branch. Edit your cart first.')); return;
      }
      let sum = 0;
      for (const item of items) {
        sum += Number(item.estimatedSubtotal);
        const row = el('div', 'checkout-mini');
        const image = el('img'); image.alt = item.vehicleName; image.src = imageFor(item);
        const details = el('div'); details.append(el('strong', '', item.vehicleName), el('small', '', `${item.pickupDate} → ${item.returnDate} · ${money(item.estimatedSubtotal)}`));
        row.append(image, details); host.append(row);
      }
      document.getElementById('checkout-total').textContent = money(sum);
      document.getElementById('checkout-deposit').textContent = money(Math.round(sum * .3));
      document.getElementById('pickup-location').value = items[0].branchName || '';
      submit.disabled = false;
    } catch (error) { host.replaceChildren(el('div', 'notice error', `Cannot load cart: ${error.message}`)); }
  }
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (sending || submit.disabled) return;
    if (!form.reportValidity()) return;
    errorBox.hidden = true;
    sending = true; submit.disabled = true; submit.textContent = 'Submitting…';
    const body = {
      renterName: document.getElementById('renter-name').value.trim(),
      renterPhone: document.getElementById('renter-phone').value.trim(),
      renterLicenseNumber: document.getElementById('renter-license').value.trim(),
      pickupLocation: document.getElementById('pickup-location').value.trim(),
      paymentMethod: 'AT_PICKUP'
    };
    try {
      const result = await api(`/api/checkout/${getUserId()}`, { method: 'POST', body: JSON.stringify(body) });
      if (!result.bookingId || result.bookingStatus !== 'Pending') throw new Error('Unexpected booking response from backend. Check the server before retrying.');
      // Save only a display snapshot. Real data remains in PostgreSQL; session storage is NOT proof of payment.
      sessionStorage.setItem('morrow-m3-confirmation', JSON.stringify(result));
      window.location.href = `/m3/confirmation.html?bookingId=${encodeURIComponent(result.bookingId)}`;
    } catch (error) { errorBox.textContent = error.message; errorBox.hidden = false; notify('Booking request was not completed.', true); }
    finally { sending = false; submit.disabled = false; submit.textContent = 'Submit booking request ↗'; }
  });
  initialize();
}());
