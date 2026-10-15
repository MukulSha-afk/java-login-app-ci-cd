// Minimal progressive enhancement: password toggle, inline validation, loading state.
(function () {
  function toggle(btn) {
    var input = document.getElementById(btn.getAttribute('data-target'));
    if (!input) return;
    var show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    btn.textContent = show ? 'Hide' : 'Show';
    btn.setAttribute('aria-pressed', String(show));
  }
  document.querySelectorAll('[data-toggle-password]').forEach(function (btn) {
    btn.addEventListener('click', function () { toggle(btn); });
  });

  document.querySelectorAll('form[data-loading]').forEach(function (form) {
    form.addEventListener('submit', function () {
      var btn = form.querySelector('button[type=submit],input[type=submit]');
      if (!form.checkValidity()) return;
      if (btn) {
        btn.classList.add('loading');
        if (btn.disabled !== undefined) btn.disabled = true;
        var label = btn.querySelector('.btn-label');
        if (label) label.textContent = 'Please wait…';
      }
    });
  });

  var pwd = document.getElementById('password');
  var meter = document.getElementById('pw-meter');
  var meterLabel = document.getElementById('pw-label');
  if (pwd && meter) {
    pwd.addEventListener('input', function () {
      var v = pwd.value, score = 0;
      if (v.length >= 8) score++;
      if (/[a-z]/.test(v) && /[A-Z]/.test(v)) score++;
      if (/\d/.test(v)) score++;
      if (/[^A-Za-z0-9]/.test(v)) score++;
      meter.firstElementChild.style.width = (score * 25) + '%';
      var names = ['Too weak', 'Weak', 'Okay', 'Good', 'Strong'];
      if (meterLabel) meterLabel.textContent = v ? names[score] : 'Use 8+ characters with mixed case, a number, and a symbol.';
    });
  }

  var confirm = document.getElementById('confirmPassword');
  if (pwd && confirm) {
    var err = document.getElementById('confirm-error');
    var check = function () {
      var bad = confirm.value && pwd.value !== confirm.value;
      confirm.setAttribute('aria-invalid', bad ? 'true' : 'false');
      if (err) err.textContent = bad ? 'Passwords do not match.' : '';
    };
    pwd.addEventListener('input', check);
    confirm.addEventListener('input', check);
  }
})();
