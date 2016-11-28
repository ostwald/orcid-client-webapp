package org.iis.orcid;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;

import com.google.api.client.extensions.java6.auth.oauth2.FileCredentialStore;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.json.JsonObjectParser;
// import com.google.appengine.api.users.UserServiceFactory;

import org.json.*;

import java.io.IOException;
import java.io.File;
import java.lang.Exception;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * A helper class for Google's OAuth2 authentication API.
 * @version 20130224
 * @author Matyas Danter
 */
public final class GoogleAuthHelper {

	/**
	 * set this up at https://code.google.com/apis/console/
	 */
	private static String CLIENT_ID = "";

	/**
	 * set this up at https://code.google.com/apis/console/
	 */
	private static String CLIENT_SECRET = "";
	
	/**
	 * Callback URI that google will redirect to after successful authentication. must match URI set up at console.
	 */
	private static String CALLBACK_URI = "";
	private static FileCredentialStore CREDENTIAL_STORE = null;
	
	// start google authentication constants
	
	private static List getScopeList() {
		List scope_list = new ArrayList();
		scope_list.add ("https://www.googleapis.com/auth/userinfo.profile");
		scope_list.add ("https://www.googleapis.com/auth/userinfo.email");
		scope_list.add ("https://www.googleapis.com/auth/calendar");
		scope_list.add ("https://www.googleapis.com/auth/drive");
		scope_list.add ("https://www.googleapis.com/auth/drive.file");
		return scope_list;
	}
	
	// private static final Iterable<String> SCOPE = Arrays.asList("https://www.googleapis.com/auth/userinfo.profile;https://www.googleapis.com/auth/userinfo.email".split(";"));
	private static final Iterable<String> SCOPE = getScopeList();
	private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v1/userinfo";
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final String TOKEN_URL = "https://sandbox.orcid.org/oauth/token";

    // end google authentication constants
	
	private String stateToken;
	
	private final GoogleAuthorizationCodeFlow flow;
	
	private GoogleTokenResponse googleTokenResponse = null;
	private String userId = null;
	private GoogleUserInfo googleUserInfo = null;
	
	/**
	 * Constructor initializes the Google Authorization Code Flow with CLIENT ID, SECRET, and SCOPE 
	 */
	public GoogleAuthHelper(String userId) {
		this.userId = userId;
		this.googleUserInfo = new GoogleUserInfo();
/*  	// works but no persistance	
		flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT,
				JSON_FACTORY, CLIENT_ID, CLIENT_SECRET, getScopeList()).build(); */
		
/* 		// never succussfully implemented
		flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT,
				JSON_FACTORY, CLIENT_ID, CLIENT_SECRET, SCOPE)
				.setCredentialStore(
					new JdoCredentialStore(JDOHelper.getPersistenceManagerFactory("transactions-optional")))
				.build(); */
				
//		GoogleAuthorizationCodeFlow.Builder builder =
//				new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT,
//						JSON_FACTORY, CLIENT_ID, CLIENT_SECRET, getScopeList());
//
//		builder.setCredentialStore(CREDENTIAL_STORE);
//		builder.setAccessType("offline"); // if not offline, then we don't get refresh token
//		builder.setApprovalPrompt("force");  // force google consent or not - REQUIRED TO GET REFRESH
//
//		flow = builder.build();
		flow = null;
		generateStateToken();

	}
	
	
	public String getCcsUserId () {
		return this.userId;
	}
	
	public GoogleUserInfo getGoogleUserInfo() {
		return this.googleUserInfo;
	}
	
	public Credential getUserCredential () {
		try {
			return flow.loadCredential (userId);
		} catch (IOException e) {
			prtln ("loadCredential Error: " + e.getMessage());
		}
		return null;
	}
	
	/**
	Called by oauth.do, uses authCode to obtain response and access token. 
	
	TODO: populates GoogleUserInfo
	- credential
	- update (jsonString) - googleId, googleEmail (googleId validates)
	- googleId
	- googleEmail
	
	*/
	public GoogleUserInfo initGoogleUserInfo (final String authCode) throws IOException {
		prtln ("initGoogleUserInfo()");
		try {
//			final GoogleTokenResponse response = flow.newTokenRequest(authCode).setRedirectUri(CALLBACK_URI).execute();
//			googleTokenResponse = response;
//			prtln ("ACCESS_TOKEN: " + response.getAccessToken());
//
//			googleUserInfo.setAccessToken (response.getAccessToken());
//
//			Credential credential = flow.createAndStoreCredential(response, userId);

            // build token url
            Credential credential = null;
            JSONObject post_data = new JSONObject();
            post_data.put("client_id", CLIENT_ID);
            post_data.put("client_secret", CLIENT_SECRET);
            post_data.put("grant_type", "authorization_code");
            post_data.put("code",authCode );
            post_data.put("redirect_uri", CALLBACK_URI);
            post_data.put("scope", "/orcid-works/create");
            prtln ("token request data");
            prtln (post_data.toString(2));
            String resp = GoogleAPIRequestor.post(TOKEN_URL, post_data);

            /*
            All in one approach - build url and pass everything that way
            - returns 415 - undigestable format
             */

            /* DOESNLT WORK
            String paramString = "client_id=" + CLIENT_ID;
            paramString += "&" + "client_secret=" + CLIENT_SECRET;
            paramString += "&" + "grant_type=" + "authorization_code";
            paramString += "&" + "code=" + authCode;
            paramString += "&" + "redirect_uri=" + URLEncoder.encode(CALLBACK_URI);

            prtln ("paramString: " + paramString);
            String allInOneUrl = TOKEN_URL+"?"+paramString;
            prtln ("allInOneUrl: " + allInOneUrl);
            String resp = GoogleAPIRequestor.post(allInOneUrl, null);
            */


//            prtln ("POST RESPONSE:" + resp);
            prtln ("POST RESPONSE");
            JSONObject jsonResp = new JSONObject(resp);
            prtln (jsonResp.toString(2));
            if (jsonResp.get("error") != null) {
                String err = "POST ERROR: " + jsonResp.getString("error") +
                              ": " + jsonResp.getString("error_description");
                prtln (err);
                return null;
//                throw new Exception(err);
            }

            prtln (" .. I want to call updateGoogleUserInfo!");
			return updateGoogleUserInfo (credential);
			
		} catch (Throwable t) {
			prtln ("initGoogleUserInfo error: " + t.getMessage());
			t.printStackTrace();
			throw new IOException (t.getMessage());
		}
	}
	
	/**
	 * Expects an Authentication Code, and makes an authenticated request for the user's profile information<p>
	 
	 The caller must valiate the googleUserInfo! - see GoogleUserInfo.validate()
	 
	 * @return JSON formatted user profile information
	 * @param authCode authentication code provided by google
	 */
	public GoogleUserInfo updateGoogleUserInfo (Credential credential) throws IOException {
		// prtln ("updateGoogleUserInfo");
		try {
			googleUserInfo.setAccessToken (credential.getAccessToken());
			
			final HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(credential);
			// Make an authenticated request
			final GenericUrl url = new GenericUrl(USER_INFO_URL);
			final HttpRequest request = requestFactory.buildGetRequest(url);
			
			request.getHeaders().setContentType("application/json");
			
			String userInfoJson = request.execute().parseAsString();
			googleUserInfo.setProfile (userInfoJson);
			
			return googleUserInfo;
		} catch (Throwable t) {
/* 			prtln ("getJsonData error: " + t.getMessage());
			prtln ("- cause: : " + t.getCause());
			prtln ("- toString(): : " + t.toString());
			t.printStackTrace(); */
			throw new IOException (t.getMessage());
		}
	}

	public static void setClientId(String id) {
		CLIENT_ID = id;
	}
	
	public static void setClientSecret (String secret) {
		CLIENT_SECRET = secret;
	}
	
	public static void setCallbackUri(String uri) {
		CALLBACK_URI = uri;
	}
	
	public static void setCredentialStore(FileCredentialStore store) throws IOException {
		CREDENTIAL_STORE = store;
	}	
	
	public static void setCredentialStoreOFF(String path) throws IOException {
		CREDENTIAL_STORE = new FileCredentialStore(new File (path), JSON_FACTORY);
	}	
	
	/**
	 * Builds a login URL based on client ID, secret, callback URI, and scope 
	 */
	public String buildLoginUrl() {
		return buildLoginUrl(null);
	}
	
	public String buildLoginUrl(String context) {
		
		final GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
		
		String state = stateToken;
		if (context != null)
			state += "context;"+context.trim();
		
		return url.setRedirectUri(CALLBACK_URI).setState(state).build();
	}
	
	public String getLoginUrl() {
		return buildLoginUrl();
	}
	
	/**
	 * Generates a secure state token 
	 */
	private void generateStateToken(){
		
		SecureRandom sr1 = new SecureRandom();
		stateToken = "orcid;"+sr1.nextInt();
	}
	
	/**
	 * Accessor for state token
	 */
	public String getStateToken(){
		return stateToken;
	}
	
	public String getAccessToken(){
		return getUserCredential().getAccessToken();
	}
	
	public boolean refreshToken() {
		prtln ("getRefreshToken()");
		try {
			Credential credential = getUserCredential();
			return credential.refreshToken();
		} catch (Exception e) {
			prtln ("getRefreshToken ERROR: " + e.getMessage());
		}
		return false;
	}

	public void deleteUserCredential () {
		try {
			CREDENTIAL_STORE.delete(userId, getUserCredential());
			prtln ("Credential Deleted");
			prtln (" - this should be null: " + getUserCredential());
		} catch (IOException e) {
			prtln ("couldn not deleteUserCredential: " +e.getMessage());
		}
	}

	public void showCredential () {
		prtln ("User Crendentials (obtained from flow)");
		showCredential (this.getUserCredential());
	}
	
	private void showCredential (Credential credential) {
		prtln ("---------------\ncredential");
		if (credential == null) {
			prtln (" - credential is EMPTY (null)");
			return;
		}
		if (credential.getRefreshToken() != null) 
			prtln ("- HAS refresh");
		else
			prtln ("- does NOT have refresh");
		prtln ("- access_token: " + credential.getAccessToken());
		prtln ("- expires in: " + credential.getExpiresInSeconds());
	}

	
	private static void prtln (String s) {
		System.out.println (s);
	}


	
}
