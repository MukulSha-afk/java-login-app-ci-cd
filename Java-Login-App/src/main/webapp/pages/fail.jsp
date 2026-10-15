<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
<title>Something went wrong - Java Login App</title>
<%@ include file="fragments/head.jspf" %>
</head>
<body>
<%@ include file="fragments/header.jspf" %>
<main id="main" class="page prose">
  <div class="card card-narrow">
    <h1 class="display display-center">Request failed</h1>
    <div class="alert alert-error" role="alert"><c:out value="${errorMessage}" default="Invalid username or password. Please try again."/></div>
    <div class="form-actions"><a class="btn btn-primary btn-block" href="login">Back to sign in</a><a class="btn btn-ghost btn-block" href="home">Home</a></div>
  </div>
</main>
<%@ include file="fragments/footer.jspf" %>
</body>
</html>
