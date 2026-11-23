(function () {
  'use strict';
  const { api, el, money, notify, isDateRange, imageFor, refreshCount, advance } = window.RentalUI;
  const itemsHost = document.getElementById('cart-items');
  const empty = document.getElementById('cart-empty');
  const warning = document.getElementById('cart-warning');
  const next = document.getElementById('cart-checkout');
  async function load() {
    next.disabled = true;
    try {
      const cart = await api('/api/carts/me');
      const items = cart.items || [];
      itemsHost.replaceChildren(); empty.hidden = items.length > 0;
      let total = 0;
      const branches = new Set();
      for (const item of items) {
        total += Number(item.estimatedSubtotal);
        branches.add(String(item.branchId));
        const card = el('article', 'cart-card');
        const img = el('img', 'cart-thumb'); img.alt = `Image for ${item.vehicleName}`; img.src = imageFor(item);
        const info = el('div');
        info.append(el('span', 'cart-code', item.vehicleCode), el('h3', 'cart-title', item.vehicleName), el('p', 'cart-branch', item.branchName), el('div', 'cart-price', money(item.estimatedSubtotal)));
        const edit = el('form', 'edit-form');
        const pickLabel = el('label', '', 'Pickup'); const pick = el('input'); pick.type = 'date'; pick.value = item.pickupDate; pick.min = advance(0); pickLabel.append(pick);
        const returnLabel = el('label', '', 'Return'); const ret = el('input'); ret.type = 'date'; ret.value = item.returnDate; ret.min = item.pickupDate; returnLabel.append(ret);
        pick.addEventListener('change', () => { ret.min = pick.value; });
        const save = el('button', 'mini-btn', 'Save dates'); save.type = 'submit';
        const remove = el('button', 'mini-btn danger', 'Remove'); remove.type = 'button';
        edit.append(pickLabel, returnLabel, save, remove);
        edit.addEventListener('submit', async (event) => {
          event.preventDefault();
          if (!isDateRange(pick.value, ret.value)) { notify('Please select a valid rental period.', true); return; }
          save.disabled = true;
          try { await api(`/api/carts/me/items/${item.cartItemId}`, { method: 'PUT', body: JSON.stringify({ pickupDate: pick.value, returnDate: ret.value }) }); notify('Rental dates updated.'); await load(); }
          catch (error) { notify(error.message, true); } finally { save.disabled = false; }
        });
        remove.addEventListener('click', async () => {
          if (!window.confirm(`Remove ${item.vehicleName} from your cart?`)) return;
          remove.disabled = true;
          try { await api(`/api/carts/me/items/${item.cartItemId}`, { method: 'DELETE' }); notify('Vehicle removed.'); await load(); }
          catch (error) { notify(error.message, true); } finally { remove.disabled = false; }
        });
        info.append(edit); card.append(img, info); itemsHost.append(card);
      }
      const deposit = Math.round(total * 0.3);
      document.getElementById('cart-subtotal').textContent = money(total);
      document.getElementById('cart-total').textContent = money(total);
      document.getElementById('cart-deposit').textContent = money(deposit);
      document.getElementById('cart-remaining').textContent = money(total - deposit);
      warning.hidden = true;
      if (items.length && branches.size > 1) {
        warning.textContent = 'Checkout requires every vehicle to belong to the same pickup branch. Remove vehicles from different branches to proceed.';
        warning.hidden = false;
      } else if (items.some(x => !isDateRange(x.pickupDate, x.returnDate))) {
        warning.textContent = 'Your cart contains expired or invalid rental dates. Please update them before checkout.';
        warning.hidden = false;
      } else next.disabled = items.length === 0;
      await refreshCount();
    } catch (error) { itemsHost.replaceChildren(el('div', 'notice error', `Could not load cart: ${error.message}`)); empty.hidden = true; }
  }
  next.addEventListener('click', () => { window.location.href = '/rental/checkout.html'; });
  load();
}());