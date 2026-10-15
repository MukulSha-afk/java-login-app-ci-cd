<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
<title>Sign in - Java Login App</title>
<%@ include file="fragments/head.jspf" %>
</head>
<body>
<%@ include file="fragments/header.jspf" %>
<main id="main" class="page">
  <div class="hero-grid">
    <section class="card card-narrow" aria-labelledby="login-title">
      <h1 id="login-title">Welcome back</h1>
      <p class="sub">Sign in to continue to your workspace.</p>
      <c:if test="${not empty message}">
        <div class="alert alert-success" role="status"><c:out value="${message}"/></div>
      </c:if>
      <c:if test="${not empty errorMessage}">
        <div class="alert alert-error" role="alert"><c:out value="${errorMessage}"/></div>
      </c:if>
      <c:if test="${not empty genericLoginError}">
        <div class="alert alert-error" role="alert"><c:out value="${genericLoginError}"/></div>
      </c:if>
      <form method="post" action="login" data-loading>
        <!-- Spring Security CSRF token -->
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <div class="field">
          <label for="username">Username or email</label>
          <input id="username" type="text" name="username" autocomplete="username" required placeholder="Enter your username or email" />
          <div class="field-error" aria-live="polite"></div>
        </div>
        <div class="field">
          <label for="password">Password</label>
          <div class="password-wrap">
            <input id="password" type="password" name="password" autocomplete="current-password" required placeholder="Enter your password" />
            <button type="button" class="icon-btn" data-toggle-password data-target="password" aria-pressed="false">Show</button>
          </div>
          <div class="field-error" aria-live="polite"></div>
        </div>
        <div class="form-actions">
          <button class="btn btn-primary btn-block" type="submit"><span class="spinner" aria-hidden="true"></span><span class="btn-label">Sign in</span></button>
        </div>
      </form>
      <p class="alt-link">New here? <a href="register">Create an account</a></p>
    </section>
    <aside class="brand-panel" aria-label="Why Java Login App">
      <div>
        <h2>Secure auth, done right.</h2>
        <p>BCrypt-hashed passwords, parameterized queries, session-guarded dashboard, clean JSP and MySQL stack.</p>
        <div class="stat-row">
          <div class="stat"><b>BCrypt</b>password hashing</div>
          <div class="stat"><b>MySQL 8</b>Employee store</div>
          <div class="stat"><b>Docker</b>one-command run</div>
        </div>
      </div>
    </aside>
  </div>
</main>
<%@ include file="fragments/footer.jspf" %>
<script src="js/app.js"></script>
</body>
</html>
