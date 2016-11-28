/*
 * Copyright 2002-2009 
 *
 * Digital Learning Sciences (DLS)
 * University Corporation for Atmospheric Research (UCAR) 
 * P.O. Box 3000
 * Boulder, CO 80307-3000
 * 
 * AND 
 * 
 * Institute of Cognitive Science 
 * University of Colorado at Boulder
 * Campus Box 594
 * Boulder, CO 80309-0594
 *
 * This file is part of the Curriculum Customization Service (CCS) software
 */

package org.iis.orcid.action;

import org.iis.orcid.GoogleAuthHelper;
import org.iis.orcid.GoogleUserInfo;
import org.iis.orcid.GoogleAPIRequestor;
import org.iis.orcid.UserBean;

import javax.servlet.*;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.SAXReader;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;
import java.net.URL;
import java.net.URLEncoder;
import java.beans.XMLEncoder;
import java.beans.XMLDecoder;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import org.json.JSONObject;

import org.dlese.dpc.webapps.tools.GeneralServletTools;
import org.dlese.dpc.util.TimedURLConnection;

import com.google.api.client.auth.oauth2.Credential;

/**
 *  Action that handles surveys. <p>
 *
 *
 *
 * @author    Jonathan Ostwald
 */
public final class OAuthAction extends Action {
	private static boolean debug = true;

    private static final String CLIENT_ID = "APP-ME9Y2F05R0S03YKU";
    private static final String CLIENT_SECRET = "7453431a-3cf9-4e2d-802a-71a40e781fa0";
    private static final String BASE_API_URL = "https//api.sandbox.orcid.org/v1.2";
    private static final String TOKEN_URL = "https://sandbox.orcid.org/oauth/token";
    private static final String AUTHORIZE_URL = "https://sandbox.orcid.org/oauth/authorize";

    private static final String FAKE_USER_ID = "0";


    /**  Constructor for the OAuthAction object */
	public OAuthAction() { }


	// --------------------------------------------------------- Public Methods

	/**
	 *  Process the specified HTTP request, and create the corresponding HTTP response (or forward to another web
	 *  component that will create it). Return an <code>ActionForward</code> instance describing where and how
	 *  control should be forwarded, or <code>null</code> if the response has already been completed.
	 *
	 * @param  mapping        The ActionMapping used to select this instance
	 * @param  response       The HTTP response we are creating
	 * @param  form           The ActionForm for the given page
	 * @param  req            The HTTP request.
	 * @return                The ActionForward instance describing where and how control should be forwarded
	 * @exception  Exception  If error.
	 */
	public ActionForward execute(
	                             ActionMapping mapping,
	                             ActionForm form,
	                             HttpServletRequest req,
	                             HttpServletResponse response)
		 throws Exception {

		/*
		 *  Design note:
		 *  Only one instance of this class gets created for the app and shared by
		 *  all threads. To be thread-safe, use only local variables, not instance
		 *  variables (the JVM will handle these properly using the stack). Pass
		 *  all variables via method signatures rather than instance vars.
		 */
		System.out.println("\n\n=======================================================\n" + getDateStamp());

		// Extract attributes we will need
		Locale locale = getLocale(req);
		ActionErrors errors = new ActionErrors();
		HttpSession session = req.getSession(true);

		UserBean currentUser = (UserBean) session.getAttribute("currentUser");
		// prtln ("- currentUser: " + currentUser.getUsername() + " (" + currentUser.getUserId() + ")");
		
/* 		String requestedUrl = req.getRequestURI();
		String queryString = req.getQueryString();
		if(queryString != null && !queryString.equalsIgnoreCase("null") && queryString.length() > 0 )
			requestedUrl += "?" + queryString;
		prtln ("requestedUrl: " + requestedUrl); */
		
		try {

			String command = req.getParameter("command");

			showParams(req);

            GoogleAuthHelper googleAuthHelper = (GoogleAuthHelper)session.getAttribute ("googleAuthHelper");
            if (googleAuthHelper == null) {
                googleAuthHelper = new GoogleAuthHelper(FAKE_USER_ID);
                session.setAttribute ("googleAuthHelper", googleAuthHelper);
            }

            if (command == null || command.trim().length() == 0) {
                return doGoogleAuth(googleAuthHelper, mapping, form, req, response);
            }

			throw new Exception ("Unrecognized Command: \"" + command + "\"");
					
		} catch (Throwable t) {
            t.printStackTrace();
			prtln ("OAuth exception: " + t.getMessage());
			return mapping.findForward("auth.page");
		}

	}



    private ActionForward doGoogleAuth (GoogleAuthHelper googleAuthHelper,
                                        ActionMapping mapping,
                                        ActionForm form,
                                        HttpServletRequest req,
                                        HttpServletResponse response) {
        prtln ("\n-----------------\ngoogleAuth()");
        ActionErrors errors = new ActionErrors();
        HttpSession session = req.getSession(true);
        UserBean currentUser = (UserBean) session.getAttribute("currentUser");
        try {
            if ((req.getParameter("code") == null || req.getParameter("state") == null) && req.getParameter("error") == null) {

				/*
				 * set the secure state token in session to be able to track what we sent to ORCID
				 */
//				googleAuthHelper = new GoogleAuthHelper(currentUser.getUserId());
                googleAuthHelper = new GoogleAuthHelper(FAKE_USER_ID);
                session.setAttribute ("googleAuthHelper", googleAuthHelper);
                prtln ("instantiated new GoogleAuthHelper");
                prtln (" - error param (should be null): " + req.getParameter("error"));

                String stateToken = googleAuthHelper.getStateToken(); // must be done AFTER instantiating new helper
                session.setAttribute("state", stateToken);
                prtln (" - set state in session context: " + (String)session.getAttribute ("state"));

                // build authorize URL now and redirect to it

                String access_type = "online";
                String orcid_auth_url = AUTHORIZE_URL + "?" +
                        "client_id=" + URLEncoder.encode(CLIENT_ID) + "&" +
                        "redirect_uri=" + URLEncoder.encode("http://localhost:8080/orcid/oauth.do") + "&" +
                        "response_type=code&" +
                        "state=" + URLEncoder.encode(stateToken) + "&" +
                        "access_type=" + access_type + "&" +
                        "scope=" + URLEncoder.encode("/orcid-works/create");


                // redirect to login url - context is sent in param
                prtln ("auth url: " + orcid_auth_url);
                return new ActionForward (orcid_auth_url, true);

            } else {

                // WILL STATE ALWAYS be populated at this point?
                // could we make a method called "handleGoogleConsent"

				/* params to worry about
				- code
				- error
				- state - important cause it contains context
				*/

                // parse context and state out of stateParam if possible
                String stateParam = req.getParameter("state");
                String context = null;
                String state = stateParam;
                if (stateParam != null) {
                    prtln ("splitting up stateParam: \"" + stateParam + "\"");
                    int contextStart = stateParam.indexOf("context;");
                    prtln (" - contextStart: " + contextStart);
                    if (contextStart != -1) {
                        state = stateParam.substring(0, contextStart);
                        prtln (" - state: " + state);
                        context = stateParam.substring(contextStart + "context;".length(), stateParam.length());
                        prtln (" - context: " + context);
                    }
                }

                // error param set by google when user does not give consent
                if (req.getParameter("error") != null && state != null) {
                    session.setAttribute ("googleUserInfo", null);
                    context = context + "&googleAuthError=consent_denied";
                    return new ActionForward (context, true);
                }

                if (req.getParameter("code") != null && state != null
                        && state.equals(session.getAttribute("state"))) {

                    // OKAY - we're authenticated!

                    prtln ("GOT CODE!");
                    session.removeAttribute("state");

					/* code is the googleAuthCode, which we can use to get authenticated user's
					   profile information.
					*/


                    GoogleUserInfo googleUserInfo = googleAuthHelper.initGoogleUserInfo(req.getParameter("code"));

                    prtln ("GoogleUserInfo afer getting userInfoJson: " + googleUserInfo.toString());

					/* now we could extract from user's profile info and persist the info */

                    // prtln ("userInfoJson is a " + userInfoJson.getClass().getName());
                    // prtln (userInfoJson);
                    JSONObject json = new JSONObject (googleUserInfo.getProfile());
                    String userInfoJson = json.toString();
                    prtln ("userInfoJson: " + userInfoJson);

					/*
						make sure 1) - there is googleId in currentUser
					              2) - currentUser.googleId matches googleUserInfo.id
				    */

                    String googleProfileId = googleUserInfo.getId();

                    String ccsUserGoogleId = currentUser.getUserGoogleId();
                    prtln (" - ccsUserGoogleId: " + ccsUserGoogleId);

					/*
						if currentUser did not have googleId set - do it now with googleProfileId,
						which is the google account user verified with.

						if currentUser has googleId other than googleEmail, we don't want to let
						user login using that google credentials.
						-- Q: should we remove the user's credentials??
					*/
                    if (ccsUserGoogleId == null || ccsUserGoogleId.trim().length() == 0) {
						/*
							this user wants to connect googleProfileId with ccsAccount.
							we confirm the id and forward to confirm page.
							if the googleId is not unique, there is a flag set in the
							request
						*/

                        session.setAttribute ("googleUserInfo", null);
                        req.setAttribute ("tmpGoogleUserInfo", googleUserInfo);
                        req.setAttribute ("context", context);
                        prtln ("set tmpGoogleUserInfo in request context");
                        prtln (" --> " + googleUserInfo.toString());
                        return mapping.findForward ("google.id.confirm");
                    }

                    else if (!ccsUserGoogleId.equals (googleProfileId)) {
                        prtln ("ccsUserGoogleId does NOT match googleProfileId");
                        // should we remove the credentials now??
                        googleAuthHelper.deleteUserCredential();
                        prtln (" CRED DELETED");
                        // session.setAttribute ("googleAuthError", "invalid");
                        req.setAttribute ("googleAuthError", "invalid");
                        googleUserInfo = null;

                    }

                    else if (ccsUserGoogleId.equals (googleProfileId)) {
                        // SET googleUserInfo as sessionAttribute
                        session.setAttribute ("googleUserInfo", googleUserInfo);
                        if (googleUserInfo != null)
                            prtln ("put googleUserInfo into session context:\n" + googleUserInfo.toString());
                        else
                            prtln ("put EMPTY googleUserInfo into session context");
                    }

					/* If we overloaded state param to include a context url, we have parsed the
					   context out and can forward/redirect eventually.
					*/

                    if (context != null)
                        return new ActionForward (context, true);
                    else
                        return mapping.findForward("auth.page");
                }

                else  {
                    prtln ("apparently state parameter (" + state + " is NOT session state (" +
                            session.getAttribute("state") + ")");
                }
            }

            prtln("execute() returning default forward...");

            // Default forwarding:
            saveErrors(req, errors);
            return mapping.findForward("auth.page");
        } catch (NullPointerException e) {
            prtln("OAuthAction caught exception.");
            e.printStackTrace();
            return mapping.findForward("auth.page");
        } catch (Throwable e) {
            prtln("OAuthAction caught exception: " + e);
            e.printStackTrace();
            return mapping.findForward("auth.page");
        }
    }


    /**
     * usage
     *
     *
     *
      * @param googleAuthHelper
     * @param mapping
     * @param form
     * @param req
     * @param response
     * @return
     */
	private ActionForward doGoogleProxy (GoogleAuthHelper googleAuthHelper,
										 ActionMapping mapping,
										 ActionForm form,
										 HttpServletRequest req,
										 HttpServletResponse response) {
									 
		prtln ("PROXY");
		String apiUrl = req.getParameter ("apiUrl");
		String postBody = req.getParameter ("postBody");
		String method = req.getParameter ("method");
		String batchBody = req.getParameter ("batchBody");
		String boundary = req.getParameter ("boundary");
		
		JSONObject postBodyJson = null;
		if (postBody != null && postBody.trim().length() > 1) {
			try {
				postBodyJson = new JSONObject(postBody);
			} catch (Throwable t) {
				prtln ("ERROR: pre-processing postBody: " + t.getMessage());
			}
		}
		prtln ("- apiUrl: " + apiUrl);

		String resp = null;
		try {
			if ("DELETE".equals(method)) {
				prtln ("deleting ...");
				int responseCode = GoogleAPIRequestor.delete(apiUrl);
				resp = "{ " + Integer.toString(responseCode) + "}";
				prtln ("resp: " + resp);
			}
			else if (batchBody != null && batchBody.trim().length() > 0) {
				prtln ("BATCH BODY: " + batchBody);
				prtln (batchBody);
				
				/* JSONObject jsonResp = new JSONObject();
				jsonResp.put("WARN", "not implemented");
				resp = jsonResp.toString(); */
				
				resp = GoogleAPIRequestor.batch(apiUrl, boundary, batchBody);
				prtln ("RESPONSE (not yet json ...");
				prtln (resp);
				prtln ("-------------");
			}
			else if (postBodyJson != null) {
				if ("PATCH".equals(method))
					resp = GoogleAPIRequestor.patch(apiUrl, postBodyJson);
				else
					resp = GoogleAPIRequestor.post(apiUrl, postBodyJson);
			}
			else if ("POST".equals(method)) {
				resp = GoogleAPIRequestor.post(apiUrl, null);
			}
			else {
				resp = TimedURLConnection.importURL(apiUrl, 3000);
			}
/* 			prtln ("resp: " + resp);
			prtln (" -->"+(int)resp.trim().charAt(0)+"<--"); */
			JSONObject json = new JSONObject(resp);
			
			req.setAttribute ("apiData", json.toString());
		} catch (Exception e) {
			req.setAttribute ("error", e.getMessage());
			prtln ("PROXY error: " + e.getMessage());
			// e.printStackTrace();
		} catch (Throwable e) {
			req.setAttribute ("error", e.getMessage());
			prtln ("Unknown PROXY error: " + e.getMessage());
			e.printStackTrace();
		}
		
		return mapping.findForward ("json.response");
	
	}


    private ActionForward doGoogleRefreshToken (GoogleAuthHelper googleAuthHelper,
                                                ActionMapping mapping,
                                                ActionForm form,
                                                HttpServletRequest req,
                                                HttpServletResponse response) {

        boolean debugwas = debug;
        debug=false;
        prtln ("REFRESH");
        HttpSession session = req.getSession(true);
        UserBean currentUser = (UserBean) session.getAttribute("currentUser");
        GoogleUserInfo googleUserInfo = (GoogleUserInfo)session.getAttribute("googleUserInfo");
        try {
            // session.setAttribute("googleAuthError", null);
            req.setAttribute("googleAuthError", null);
            if (googleAuthHelper.getUserCredential() == null)
                prtln ("no Credential found");
            else {
                if (debug) {
                    prtln ("CREDENTIAL FOUND");
                    prtln ("authenticatedUser.userGoogleId: " + currentUser.getUserGoogleId());
                    prtln ("BEFORE");
                    googleAuthHelper.showCredential();
                }
                try {
                    // Refresh the token
                    if (googleAuthHelper.refreshToken()) {
                        if (debug) { // to be able to turn off showCredential
                            prtln ("- Success");
                            prtln ("AFTER");
                            googleAuthHelper.showCredential();
                            prtln ("access_token sanity check");
                            prtln (googleAuthHelper.getAccessToken());
                        }
                        if (googleUserInfo == null) {
                            prtln ("googleUserInfo == null -> creating");
                            googleUserInfo = new GoogleUserInfo();
                            googleUserInfo.setId(currentUser.getUserId());
                        }
                        googleUserInfo.setAccessToken (googleAuthHelper.getUserCredential().getAccessToken());
                    }
                    else
                        prtln ("- Did not refresh token");

                    String tokenData = "{\"access_token\":\"" + googleAuthHelper.getAccessToken() + "\"," +
                            "\"expires_in\":\"" + googleAuthHelper.getUserCredential().getExpiresInSeconds() + "\"}";
                    req.setAttribute ("apiData", tokenData);
                    // prtln ("apiData: " + json.toString());
                    session.setAttribute("googleUserInfo", googleUserInfo);
                    prtln ("googleUserInfo updated in session context");

                    // SANITY CHECK
                    GoogleUserInfo testInfo = (GoogleUserInfo)session.getAttribute("googleUserInfo");
                    prtln ("sanity check access_token from session googleUserInfo \n  - " + testInfo.getAccessToken());

                } catch (Exception e) {
                    e.printStackTrace();
                    googleUserInfo = null;
                    // prtln ("Could not get googleUserInfo: " + e);
                    prtln ("Could not reuse googleUserInfo: " + e);
                    if (e.getMessage().startsWith ("googleUserInfo was invalid"))
                        req.setAttribute("googleAuthError", "invalid");
                    // session.setAttribute("googleAuthError", "invalid");
                }
            }

        } catch (Exception e) {
            req.setAttribute ("error", e.getMessage());
            prtln ("REFRESH error: " + e.getMessage());
            // e.printStackTrace();
        } catch (Throwable e) {
            req.setAttribute ("error", e.getMessage());
            prtln ("Unknown REFRESH error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            debug=debugwas;
            System.out.println(" - set debug to " + debug);
        }

        return mapping.findForward ("json.response");
    }


	// ---------------------- Debug info --------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}

	private void showParams(HttpServletRequest req) {
		prtln ("Params");
		Enumeration params = req.getParameterNames();
		String paramName = null;
		String[] paramValues = null;
		while (params.hasMoreElements()) {
			paramName = (String) params.nextElement();
			if (true || paramName.equals("command") || paramName.equals("apiUrl")) {
				paramValues = req.getParameterValues(paramName);
				if (paramValues.length == 1)
					prtln (" - " + paramName + ": " + paramValues[0]);
				else
					prtln (" - " + paramName + ": MULTIVALUE");
			}
		}
		prtln ("---");
	}
	
	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " Error OAuthAction: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			// System.out.println(getDateStamp() + " OAuthAction: " + s);
			System.out.println("OAuthAction: " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the object
	 *
	 * @param  db  The new debug value
	 */
	public static void setDebug(boolean db) {
		debug = db;
	}
}

