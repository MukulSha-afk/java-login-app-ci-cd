<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
<title>Dashboard - Java Login App</title>
<meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
<meta http-equiv="Pragma" content="no-cache">
<%@ include file="fragments/head.jspf" %>
</head>
<body>
<a class="skip-link" href="#main">Skip to content</a>
<header class="site-header">
  <a class="brand" href="home"><span class="brand-mark">JA</span><span class="brand-text">Java Login App</span></a>
  <nav class="site-nav" aria-label="Account">
    <span class="user-chip"><span class="avatar" aria-hidden="true"><c:out value="${fn:toUpperCase(fn:substring(user.userName,0,1))}"/></span><c:out value="${user.userName}"/></span>
  </nav>
</header>
<main id="main" class="page">
  <div class="dash-head">
    <div><span class="eyebrow">Dashboard</span><h1 class="display">Welcome, <c:out value="${user.userName}"/>.</h1><p class="lede">You are signed in. This page renders only your session identity.</p></div>
    <div class="cta-row">
      <a class="btn btn-ghost" href="<c:url value='/home'/>">Home</a>
      <%-- Logout is POST-only and CSRF-protected; GET /logout only shows the confirmation page. --%>
      <form method="post" action="<c:url value='/logout'/>" class="logout-form">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <button class="btn btn-primary" type="submit">Log out</button>
      </form>
    </div>
  </div>
  <div class="alert alert-success" role="status">Signed in successfully as <b><c:out value="${user.userName}"/></b>.</div>
  <div class="panel-grid">
    <section class="panel" aria-labelledby="t-account"><h2 id="t-account">Account</h2><p>Identity from the current session. No profile fields are stored beyond registration.</p><dl class="kv"><dt>Username</dt><dd><c:out value="${user.userName}"/></dd><dt>Source</dt><dd>HttpSession</dd><dt>Status</dt><dd>Active</dd></dl></section>
    <section class="panel" aria-labelledby="t-sec"><h2 id="t-sec">Security</h2><p>What protects this login.</p><div class="empty">BCrypt password hashing, parameterized queries, PRG redirect on login, session-guarded dashboard, no-cache on Back.</div></section>
    <section class="panel" aria-labelledby="t-next"><h2 id="t-next">Next steps</h2><p>Continue exploring the flow.</p><div class="cta-row"><a class="btn btn-ghost" href="register">New account</a><a class="btn btn-ghost" href="login">Switch user</a></div></section>
  </div>
</main>
<%@ include file="fragments/footer.jspf" %>
</body>
</html>
