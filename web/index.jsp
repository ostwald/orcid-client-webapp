
<%@ include file="/TagLibIncludes.jsp" %>
<%@ taglib prefix="html" uri="/WEB-INF/tlds/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/tlds/struts-logic.tld" %>
<%@ taglib prefix="bean-el" uri="/WEB-INF/tlds/struts-bean-el.tld"%>
<%@ taglib prefix="html-el" uri="/WEB-INF/tlds/struts-html-el.tld" %>
<%@ taglib prefix="logic-el" uri="/WEB-INF/tlds/struts-logic-el.tld" %>
<c:set var="contextUrl"><%@ include file="/ContextUrl.jsp" %></c:set>
<!DOCTYPE html>

<html>
<head>
    <meta http-equiv="content-type" content="text/html; charset=utf-8"/>
    <title>ORCID Client</title>

    <c:import url="/head.jsp"/>

    <script>

    log ("hello world");

    function msg (s, clear) {
        var $msg_el = $('#msg');
        if (clear)
            $msg_el.html('');
        $msg_el.append ($t('div').html(s))
    }

    </script>


</head>

<body>

<div style="float:right;">
    <div id="msg">hello</div>
</div>

<h1>Welcome to the ORCID client</h1>

	<button type="button" id="orcid-login">Login with ORCID</button>

</body>

<script>

$(function () {

    $('button#orcid-login').click (function (event) {
        // build auth request ...
        // send auth request and expect a token ...
        msg ("ouch");
        var oauthUrl = "${contextUrl}/oauth.do"
        window.location = oauthUrl
    })
})

</script>


</html>


