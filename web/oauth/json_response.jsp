<%@ include file="/TagLibIncludes.jsp" %>
<%-- <%@page import="org.dlsciences.ccs.google.GoogleAuthHelper" %> --%>


<c:choose>
	<c:when test="${not empty error}">
		<c:set var="jsonResponse">
		<error>
			<c:out value="${error}"/>
		</error>
		</c:set>
		${f:xml2json(jsonResponse)}
	</c:when>
	<c:otherwise>
		${apiData}
	</c:otherwise>
</c:choose>

