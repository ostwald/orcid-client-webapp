<%-- Source the necessary tag libraries --%>
<%@ page language="java" isErrorPage="true" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://www.dlese.org/dpc/dds/tags/dleseELFunctions" %>

<%-- The following is necessary to make BASIC authorization/401 work properly in Tomcat: --%>
<% 
	response.addHeader("WWW-Authenticate", "BASIC realm=\"DLESE\"");
	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
%>

<head>
<title>Unauthorized (401)</title>

<%-- Output Java exception details, if available --%>
<% if (exception != null) { %>
	<!-- Server debugging information:  
	<% exception.printStackTrace(new java.io.PrintWriter(out)); %> -->
<% } else { %>
	<!--  No stack trace available. -->
<% } %>

<%-- Determine my current domain URL --%>
<c:set var="domain" value="${f:serverUrl(pageContext.request)}"/>


</head>
<body>


<h1>Unauthorized</h1>



</body></html>


