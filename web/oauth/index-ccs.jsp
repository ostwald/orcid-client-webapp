<%@ include file="/TagLibIncludes.jsp" %>
<%-- <%@page import="org.dlsciences.ccs.google.GoogleAuthHelper" %> --%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<c:import url="/head.jsp"/>
<title>Google Info</title>
<script type="text/javascript">

var calendarListJson = '${calendarListJson}' ? '${calendarListJson}'.evalJSON() : null;
var userInfoJson = '${userInfoJson}' ? '${userInfoJson}'.evalJSON() : null;

function pageInit () {
	log ("pageInit");
	
	if (userInfoJson) {
		displayUserInfo (userInfoJson);
	}
	
}

function displayUserInfo (json) {
	var container = $('user-info');
	container.update();
	var img = new Element ('img', {src:json.picture, width:"100px"}).setStyle({float:'left',paddingRight:'10px'});
	container.insert (img);
	container.insert (new Element ('div').insert (json.name));
	container.insert (new Element ('div').insert (json.email));
	container.insert (new Element ('div').insert (json.id));
/* 	for (key in json) {
		container.insert (new Element('div').update(key + ": " + json[key]));
	} */
}

function getCalendarList() {
	var p = new Hash();
	p.set ('code', parms.code);
	p.set ('state', parms.state);
	p.set ('info', 'calendarList');
	
	var url = contextPath + "/oauth.do?" + p.toQueryString();
	window.location = url;
}

Event.observe (window, "load", pageInit);

</script>
<style>

body {
	margin:1em;
	}
	
#oath-params {
	float:right;
	font-size:9pt;
	
}

h3 {
	margin-top:20px;
}

</style>
</head>
<body>


<div id="oath-params">
			<div>state: ${param.state}</div>
			<div>code: ${param.code}</div>
</div>
<h1>Google Info</h1>
<c:choose>
	<c:when test="${empty userInfoJson}">
		<a href="${googleAuthHelper.loginUrl}">log in with google</a>
	</c:when>
	<c:otherwise>
	
		<h3>User Info</h3>
		<c:choose>
			<c:when test="${not empty userInfoJson}">
				<div id="user-info"></div>
			</c:when>
			<c:otherwise><input type="button" value="Get user info" onclick="getCalendarList()"/></c:otherwise>
		</c:choose>
		
		<br clear="all"/>
		
		<input type="button" value="Refresh" onclick="window.location='oauth.do';" />

	</c:otherwise>
</c:choose>

</body>
</html>
