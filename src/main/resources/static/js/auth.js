// Bật/tắt hiển thị mật khẩu khi bấm biểu tượng con mắt
function togglePassword(inputId) {
  const field = document.getElementById(inputId);
  if (!field) return;
  field.type = field.type === 'password' ? 'text' : 'password';
}

// Hiển thị hoặc ẩn lỗi trên từng ô nhập liệu
function setFieldError(inputId, errorId, hasError, customMessage) {
  const input = document.getElementById(inputId);
  const errorEl = document.getElementById(errorId);
  if (!input || !errorEl) return;

  if (hasError) {
    input.classList.add('input-error');
    if (customMessage) errorEl.textContent = customMessage;
    errorEl.classList.add('show');
  } else {
    input.classList.remove('input-error');
    errorEl.classList.remove('show');
  }
}

document.addEventListener('DOMContentLoaded', function () {
  // 1. Kiểm tra form Đăng ký (UC03 - register.html)
  const registerForm = document.getElementById('registerForm');
  if (registerForm) {
    registerForm.addEventListener('submit', function (e) {
      let valid = true;

      const fullName = document.getElementById('fullName').value.trim();
      const phone = document.getElementById('phone').value.trim();
      const email = document.getElementById('email').value.trim();
      const password = document.getElementById('password').value;
      const confirmPassword = document.getElementById('confirmPassword').value;
      const captchaEl = document.getElementById('captchaInput');

      setFieldError('fullName', 'fullNameError', fullName === '');
      if (fullName === '') valid = false;

      const phoneRegex = /^(\+84|0)[0-9]{9,10}$/;
      const cleanPhone = phone.replace(/\s+/g, '');
      setFieldError('phone', 'phoneError', !phoneRegex.test(cleanPhone));
      if (!phoneRegex.test(cleanPhone)) valid = false;

      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      setFieldError('email', 'emailError', !emailRegex.test(email));
      if (!emailRegex.test(email)) valid = false;

      setFieldError('password', 'passwordError', password.length < 6);
      if (password.length < 6) valid = false;

      const mismatch = password !== confirmPassword || confirmPassword === '';
      setFieldError('confirmPassword', 'confirmError', mismatch);
      if (mismatch) valid = false;

      if (captchaEl && captchaEl.value.trim() === '') {
        captchaEl.classList.add('input-error');
        valid = false;
      } else if (captchaEl) {
        captchaEl.classList.remove('input-error');
      }

      if (!valid) e.preventDefault();
    });
  }

  // 2. Kiểm tra form Quên mật khẩu (UC05a - forgot-password.html)
  const forgotForm = document.getElementById('forgotForm');
  if (forgotForm) {
    forgotForm.addEventListener('submit', function (e) {
      const email = document.getElementById('email').value.trim();
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(email)) {
        e.preventDefault();
        setFieldError('email', 'emailError', true);
      } else {
        setFieldError('email', 'emailError', false);
      }
    });
  }

  // 3. Kiểm tra form Đặt lại mật khẩu mới (UC05b - reset-password.html)
  const resetForm = document.getElementById('resetForm');
  if (resetForm) {
    resetForm.addEventListener('submit', function (e) {
      let valid = true;
      const newPwd = document.getElementById('newPassword').value;
      const confirmInput = document.getElementById('confirmPassword') || document.getElementById('confirmNewPassword');
      const confirmPwd = confirmInput ? confirmInput.value : '';
      const confirmInputId = confirmInput ? confirmInput.id : 'confirmPassword';

      setFieldError('newPassword', 'newPwdError', newPwd.length < 6);
      if (newPwd.length < 6) valid = false;

      const mismatch = newPwd !== confirmPwd || confirmPwd === '';
      setFieldError(confirmInputId, 'confirmNewPwdError', mismatch);
      if (mismatch) valid = false;

      if (!valid) e.preventDefault();
    });
  }

  // 4. Kiểm tra form Cập nhật hồ sơ (UC06 - profile.html)
  const profileForm = document.getElementById('profileForm');
  if (profileForm) {
    profileForm.addEventListener('submit', function (e) {
      let valid = true;
      const fullName = document.getElementById('fullName').value.trim();
      const phone = document.getElementById('phone').value.trim();

      setFieldError('fullName', 'profileNameError', fullName === '');
      if (fullName === '') valid = false;

      const phoneRegex = /^(\+84|0)[0-9]{9,10}$/;
      const cleanPhone = phone.replace(/\s+/g, '');
      setFieldError('phone', 'profilePhoneError', !phoneRegex.test(cleanPhone));
      if (!phoneRegex.test(cleanPhone)) valid = false;

      if (!valid) e.preventDefault();
    });
  }
});