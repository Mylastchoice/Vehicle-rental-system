(function () {
  'use strict';
  const { api, el, money, notify, isDateRange, imageFor } = window.RentalUI;
  const form = document.getElementById('checkout-form');
  const submit = document.getElementById('place-order');
  const errorBox = document.getElementById('checkout-error');
  const host = document.getElementById('checkout-items');
  let items = [];
  let totalSum = 0;
  let sending = false;

  function updateSummaryAmounts() {
    const isFull = form.elements.paymentOption.value === 'FULL';
    const payable = isFull ? totalSum : Math.round(totalSum * 0.3);
    const remaining = Math.max(0, totalSum - payable);
    document.getElementById('checkout-deposit-label').textContent = isFull
      ? 'Full payment · 100%'
      : 'Deposit · 30%';
    document.getElementById('checkout-deposit').textContent = money(payable);
    document.getElementById('checkout-remaining').textContent = money(remaining);
    document.getElementById('checkout-total').textContent = money(totalSum);
  }

  form.querySelectorAll('fieldset.payment-box').forEach(fieldset => {
    fieldset.querySelectorAll('input[type="radio"]').forEach(radio => {
      radio.addEventListener('change', () => {
        fieldset.querySelectorAll('.payment-option').forEach(label => {
          label.classList.toggle('selected', label.querySelector('input').checked);
        });
        if (radio.name === 'paymentOption') updateSummaryAmounts();
      });
    });
  });

  async function initialize() {
    try {
      const data = await api('/api/carts/me');
      items = data.items || [];
      host.replaceChildren();
      if (!items.length) { host.append(el('div', 'notice warning', 'Your cart is empty. Choose a vehicle first.')); return; }
      const branchGroups = new Set(items.map(x => String(x.branchId)));
      if (branchGroups.size !== 1 || items.some(x => !isDateRange(x.pickupDate, x.returnDate))) {
        host.append(el('div', 'notice warning', 'All vehicles must use valid dates and the same pickup branch. Edit your cart first.')); return;
      }
      totalSum = 0;
      for (const item of items) {
        totalSum += Number(item.estimatedSubtotal);
        const row = el('div', 'checkout-mini');
        const image = el('img'); image.alt = item.vehicleName; image.src = imageFor(item);
        const details = el('div'); details.append(el('strong', '', item.vehicleName), el('small', '', `${item.pickupDate} → ${item.returnDate} · ${money(item.estimatedSubtotal)}`));
        row.append(image, details); host.append(row);
      }
      updateSummaryAmounts();
      document.getElementById('pickup-location').value = items[0].branchName || '';
      const online = form.querySelector('[value="ONLINE"]');
      const options = await api('/api/payment-options').catch(() => ({ online: false }));
      if (options.online) {
        online.disabled = false;
        online.closest('label').classList.remove('disabled');
        document.getElementById('online-payment-hint').textContent = 'Stripe sandbox · use a test card';
      }
      submit.disabled = false;
    } catch (error) { host.replaceChildren(el('div', 'notice error', `Cannot load cart: ${error.message}`)); }
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (sending || submit.disabled) return;
    if (!form.reportValidity()) return;
    const phone = document.getElementById('renter-phone').value.trim();
    if (!/^(?:0|\+84)[35789]\d{8}$/.test(phone)) {
      errorBox.textContent = 'Enter a valid Vietnamese mobile number (e.g. 0901234567).';
      errorBox.hidden = false;
      return;
    }
    errorBox.hidden = true;
    sending = true; submit.disabled = true; submit.textContent = 'Submitting…';
    const body = {
      renterName: document.getElementById('renter-name').value.trim(),
      renterPhone: document.getElementById('renter-phone').value.trim(),
      renterLicenseNumber: document.getElementById('renter-license').value.trim(),
      pickupLocation: document.getElementById('pickup-location').value.trim(),
      paymentMethod: form.elements.payment.value,
      paymentOption: form.elements.paymentOption.value
    };
    try {
      const result = await api('/api/checkout', { method: 'POST', body: JSON.stringify(body) });
      if (!result.bookingId || result.bookingStatus !== 'Pending') throw new Error('Unexpected booking response from backend. Check the server before retrying.');
      if (result.paymentMethod === 'ONLINE') {
        window.location.href = `/bookings/${encodeURIComponent(result.bookingId)}`;
        return;
      }
      sessionStorage.setItem('morrow-booking-confirmation', JSON.stringify(result));
      window.location.href = `/rental/confirmation.html?bookingId=${encodeURIComponent(result.bookingId)}`;
    } catch (error) { errorBox.textContent = error.message; errorBox.hidden = false; notify('Booking request was not completed.', true); }
    finally { sending = false; submit.disabled = false; submit.textContent = 'Submit booking request ↗'; }
  });
  initialize();
}());