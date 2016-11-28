<%@ include file="/TagLibIncludes.jsp" %>
<%-- <%@page import="org.dlsciences.ccs.google.GoogleAuthHelper" %> --%>
<!DOCTYPE HTML>

<c:set var="userInfoJson" value="${tmpGoogleUserInfo}" />
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<c:import url="/head.jsp"/>
<title>Confirm Google Info</title>
<script type="text/javascript">

	// Assign global vars for googleUserInfo
	var userInfoJson = null;
	<c:if test="${empty tmpGoogleUserInfo}" >alert ("tmpGoogleUserInfo is empty");</c:if>
	<c:if test="${not empty tmpGoogleUserInfo}" >
		try {
		
 			window.USER_GOOGLE_INFO = {
				email: '${tmpGoogleUserInfo.email}',
				id: '${tmpGoogleUserInfo.id}',
				accessToken: '${tmpGoogleUserInfo.accessToken}',
				userInfoJson: ${tmpGoogleUserInfo.profile}
			}
			
		} catch (error) {
			log ("could not get userGoogleId: " + error);
		}
		log ("- userGoogleId: " + USER_GOOGLE_INFO.id);
		log ("- accessToken: " + USER_GOOGLE_INFO.accessToken);
		log ("- userInfoJson: " + JSON.stringify(USER_GOOGLE_INFO.userInfoJson, undefined, 2));
		log ("  ............ ");
	</c:if>


function pageInit () {
	log ("pageInit");
	
	displayUserInfo (USER_GOOGLE_INFO.userInfoJson);
}

function displayUserInfo (json) {

	if (json.picture)
		$('googlePicture').src = json.picture;
	else
		$('googlePicture').up ("td").hide();
	$('googleName').update (json.name);
	$('googleEmail').update (json.email);
}

Event.observe (window, "load", pageInit);

</script>
<style>

body {
	margin:1em;
	}
	

h3 {
	margin-top:20px;
}

#profile {
	border:thin orange solid;
	padding:15px;
	border-radius:5px;
	margin:20px auto 20px auto;
	box-shadow: 2px 2px 2px #888888;
}

img#googlePicture {
	padding-right:10px;
	width:50px;
}

#googleName {
	font-weight:bold;
}

#layout {
	width:600px;
	margin:0px auto 0px auto;
	text-align:center;
}

</style>
</head>
<body>

<h1>Confirm Google Info</h1>
	<div id="layout">
	
	<c:choose>
		<c:when test="${googleIdInUse == 'true'}">
			<h3 style="color:red;">The Google identity is already in use. Click on <i>Try again</i> choose a different Google account</h3>
		</c:when>
		<c:otherwise>
			<h3>Is this the Google identity you wish to use in the CCS?</h3>
		</c:otherwise>
	</c:choose>
	
	
<%-- 	<div>If not, you must <a href="http://google.com" target="_blank" 
		title="Open Google and find user in upper right corner">log this user out</a> and Try Again</div> --%>
	
	
	<table id="profile">
		<tr valign="middle">
			<td>
				<img id="googlePicture" 
					style="float: left; padding-right: 10px;">
			</td>
			<td>
				<div id="googleName">Jonathan Ostwald</div>
				<div id="googleEmail">jonathan.ostwald@gmail.com</div>
			</td>
		</tr>
	</table>
	
	<div align="center">
			<input id="use-id-button" ${googleIdInUse == 'true' ? 'disabled' : ''} class="smallButton" type="button" value="Use this identity">
			<input id="try-again-button" class="smallButton" type="button" value="Try again">
	</div>
</div>

</body>
</html>
<script type="text/javascript">
(function () {
	var context = "${context}";
	var params = {
		context: '${context}',
		command: 'confirm',
	}
	
	var url = "oauth.do?" + $H(params).toQueryString();
	$('use-id-button').observe ('click', function (event) {
			log ("USE click")
			window.location=url+"&value=true";
	});
	$('try-again-button').observe ('click', function (event) {
			log ("TRY AGAIN click");
			// window.location=url+"&value=false";
			// window.location.reload(true); // this doesn't work - has to go back to outhLoginUrl
			window.location="oauth.do?" + $H({context:context}).toQueryString();
	});
}());
</script>
