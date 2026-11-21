(function () {
  'use strict';
  const { money } = window.RentalUI;
  const good = document.getElementById('confirmation-pending');
  const empty = document.getElementById('confirmation-empty');
  let data;
  try { data = JSON.parse(sessionStorage.getItem('morrow-booking-confirmation') || 'null'); }
  catch (_) { data = null; }
  const requestedId = new URLSearchParams(window.location.search).get('bookingId');
  if (data && data.bookingId && String(data.bookingId) === requestedId && data.bookingStatus === 'Pending') {
    document.getElementById('result-code').textContent = `#${data.bookingId}`;
    document.getElementById('result-total').textContent = money(data.totalAmount);
    document.getElementById('result-deposit').textContent = money(data.depositAmount);
    good.hidden = false;
  } else empty.hidden = false;
}());
