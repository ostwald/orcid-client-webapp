<%@ include file="/TagLibIncludes.jsp" %>
<META http-equiv="Content-Type" content="text/html; charset=UTF-8">
<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<c:set var="contextUrl"><%@ include file="/ContextUrl.jsp" %></c:set>

<script>


	var GOOGLE_INTEGRATION_ENABLED = false;

	if (GOOGLE_INTEGRATION_ENABLED) {
		window.USER_GOOGLE_INFO = {};
		window.googleAuthError = '${not empty googleAuthError ? googleAuthError : ""}';
		<c:if test="${not empty googleUserInfo}" >
			try {
				userInfoJson = ${googleUserInfo.profile};
				USER_GOOGLE_INFO = {
					email: '${googleUserInfo.email}',
					id: '${googleUserInfo.id}',
					accessToken: '${googleUserInfo.accessToken}',
					userInfoJson: ${googleUserInfo.profile}
				}

			} catch (error) {
				alert ("could not get userGoogleId: " + error);
			}

			log ("- userGoogleId: " + USER_GOOGLE_INFO.id);
			log ("- accessToken: " + USER_GOOGLE_INFO.accessToken);
			log ("  ............ ");
		</c:if>
	}

</script>

<link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">
<link rel="stylesheet" href="${contextUrl}/styles.css">


<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
<script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.11.1/jquery-ui.min.js"></script>
<script src="${contextUrl}/javascript/utils.js"></script>