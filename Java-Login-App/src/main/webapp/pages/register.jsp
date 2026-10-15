<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
<title>Create account - Java Login App</title>
<%@ include file="fragments/head.jspf" %>
</head>
<body>
<%@ include file="fragments/header.jspf" %>
<main id="main" class="page">
  <div class="hero-grid">
    <section class="card card-narrow" aria-labelledby="reg-title">
      <h1 id="reg-title">Create your account</h1>
      <p class="sub">Join in under a minute. Passwords are BCrypt-hashed.</p>
      <c:if test="${not empty message}">
        <div class="alert alert-success" role="status"><c:out value="${message}"/></div>
      </c:if>
      <c:if test="${not empty errors.form}">
        <div class="alert alert-error" role="alert"><c:out value="${errors.form}"/></div>
      </c:if>
      <form method="post" action="register" data-loading>
        <!-- Spring Security CSRF token -->
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <div class="field-row">
          <div class="field"><label for="firstName">First name *</label><input id="firstName" type="text" name="firstName" autocomplete="given-name" placeholder="Aarav" required /><div class="field-error"><c:if test="${not empty errors.firstName}"><c:out value="${errors.firstName}"/></c:if></div></div>
          <div class="field"><label for="lastName">Last name</label><input id="lastName" type="text" name="lastName" autocomplete="family-name" placeholder="Sharma" maxlength="100" /><div class="field-error"><c:if test="${not empty errors.lastName}"><c:out value="${errors.lastName}"/></c:if></div></div>
        </div>
        <div class="field"><label for="email">Email</label><input id="email" type="email" name="email" autocomplete="email" placeholder="you@example.com" maxlength="254" /><div class="field-error"><c:if test="${not empty errors.email}"><c:out value="${errors.email}"/></c:if></div><div class="hint">Used for account notices only.</div></div>
        <div class="field"><label for="userName">Username *</label><input id="userName" type="text" name="userName" autocomplete="username" placeholder="Choose a username" minlength="3" maxlength="50" required /><div class="field-error"><c:if test="${not empty errors.userName}"><c:out value="${errors.userName}"/></c:if></div></div>
        <div class="field"><label for="password">Password *</label><div class="password-wrap"><input id="password" type="password" name="password" autocomplete="new-password" placeholder="Min 8 characters" minlength="8" maxlength="100" required /><button type="button" class="icon-btn" data-toggle-password data-target="password" aria-pressed="false">Show</button></div><div class="meter" aria-hidden="true"><span id="pw-meter-fill"></span></div><div class="meter-label" id="pw-label">Use 8+ characters with mixed case, a number, and a symbol.</div><div class="field-error"><c:if test="${not empty errors.password}"><c:out value="${errors.password}"/></c:if></div></div>
        <div class="field"><label for="confirmPassword">Confirm password *</label><div class="password-wrap"><input id="confirmPassword" type="password" name="confirmPassword" autocomplete="new-password" placeholder="Repeat your password" required /><button type="button" class="icon-btn" data-toggle-password data-target="confirmPassword" aria-pressed="false">Show</button></div><div class="field-error" id="confirm-error" aria-live="polite"><c:if test="${not empty errors.confirmPassword}"><c:out value="${errors.confirmPassword}"/></c:if></div></div>
        <div class="form-actions"><button class="btn btn-primary btn-block" type="submit"><span class="spinner" aria-hidden="true"></span><span class="btn-label">Create account</span></button></div>
      </form>
      <p class="alt-link">Already have an account? <a href="login">Sign in</a></p>
    </section>
    <aside class="brand-panel" aria-label="What you get">
      <div>
        <h2>Production-grade basics.</h2>
        <p>Parameterized SQL, per-request error handling, session auth, responsive accessible forms.</p>
        <div class="stat-row">
          <div class="stat"><b>No SQLi</b>prepared statements</div>
          <div class="stat"><b>PRG</b>no re-post on Back</div>
          <div class="stat"><b>A11y</b>focus + labels</div>
        </div>
      </div>
    </aside>
  </div>
</main>
<%@ include file="fragments/footer.jspf" %>
<script src="js/app.js"></script>
<script>
(function(){
  var p = document.getElementById('password');
  var m = document.getElementById('pw-meter-fill');
  var l = document.getElementById('pw-label');
  if(!p || !m) return;
  p.addEventListener('input', function(){
    var v = p.value, s = 0;
    if(v.length >= 8) s++;
    if(/[a-z]/.test(v) && /[A-Z]/.test(v)) s++;
    if(/\d/.test(v)) s++;
    if(/[^A-Za-z0-9]/.test(v)) s++;
    m.style.width = (s * 25) + '%';
    var n = ['Too weak','Weak','Okay','Good','Strong'];
    if(l) l.textContent = v ? n[s] : 'Use 8+ characters with mixed case, a number, and a symbol.';
  });
  var c = document.getElementById('confirmPassword');
  var e = document.getElementById('confirm-error');
  var k = function(){
    var b = c.value && p.value !== c.value;
    c.setAttribute('aria-invalid', b ? 'true' : 'false');
    if(e) e.textContent = b ? 'Passwords do not match.' : '';
  };
  p.addEventListener('input', k);
  c.addEventListener('input', k);
})();
</script>
</body>
</html>
