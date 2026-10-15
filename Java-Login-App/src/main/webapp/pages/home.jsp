<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
<title>Java Login App - Secure JSP authentication</title>
<%@ include file="fragments/head.jspf" %>
</head>
<body>
<%@ include file="fragments/header.jspf" %>
<main id="main" class="page">
  <div class="hero-grid">
    <div>
      <span class="eyebrow">Spring Boot 2.7 - JSP - MySQL - Docker</span>
      <h1 class="display">Authentication with a premium finish.</h1>
      <p class="lede">A portfolio-ready login and registration flow: BCrypt hashing, parameterized SQL, session-guarded dashboard, responsive accessible UI.</p>
      <ul class="feature-list">
        <li><span class="tick">ok</span><span><b>No fake analytics.</b> No tracking scripts, no dead buttons, no nonfunctional social login.</span></li>
        <li><span class="tick">ok</span><span><b>Secure by default.</b> No string-concatenated SQL, no plaintext passwords.</span></li>
        <li><span class="tick">ok</span><span><b>Correct navigation.</b> Post/Redirect/Get so Back never re-submits login.</span></li>
        <li><span class="tick">ok</span><span><b>Runs anywhere.</b> One WAR, one MySQL, one docker compose up.</span></li>
      </ul>
      <div class="cta-row"><a class="btn btn-primary" href="login">Sign in</a><a class="btn btn-ghost" href="register">Create account</a></div>
    </div>
    <aside class="brand-panel" aria-label="Highlights">
      <div><h2>Clean stack, honest UI.</h2><p>Every control on this site does something real. No fake social login, no dead buttons.</p><div class="stat-row"><div class="stat"><b>Java 11</b>Spring Boot 2.7</div><div class="stat"><b>JSP + JSTL</b>server rendered</div><div class="stat"><b>MySQL 8</b>Employee table</div></div></div>
    </aside>
  </div>
</main>
<%@ include file="fragments/footer.jspf" %>
</body>
</html>
