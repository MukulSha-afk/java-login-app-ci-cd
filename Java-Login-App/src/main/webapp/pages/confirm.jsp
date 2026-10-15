<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
<title>Registration confirmed - Java Login App</title>
<%@ include file="fragments/head.jspf" %>
</head>
<body>
<%@ include file="fragments/header.jspf" %>
<main id="main" class="page prose">
  <div class="card card-narrow">
    <h1 class="display display-center">Account created</h1>
    <p class="sub"><c:out value="${message}" default="Your account has been created."/></p>
    <div class="form-actions"><a class="btn btn-primary btn-block" href="login">Sign in</a></div>
  </div>
</main>
<%@ include file="fragments/footer.jspf" %>
</body>
</html>
