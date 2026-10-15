<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
<title>Sign out - Java Login App</title>
<%@ include file="fragments/head.jspf" %>
</head>
<body>
<%@ include file="fragments/header.jspf" %>
<main id="main" class="page prose">
  <div class="card card-narrow" style="text-align:center">
    <h1>Sign out?</h1>
    <p class="sub">Your session will end and you will return to the sign-in page.</p>
    <%-- Actual sign-out is a CSRF-protected POST handled by Spring Security. --%>
    <form method="post" action="<c:url value='/logout'/>">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
      <div class="form-actions">
        <button class="btn btn-primary btn-block" type="submit">Sign out</button>
        <a class="btn btn-ghost btn-block" href="<c:url value='/dashboard'/>">Cancel</a>
      </div>
    </form>
  </div>
</main>
<%@ include file="fragments/footer.jspf" %>
</body>
</html>