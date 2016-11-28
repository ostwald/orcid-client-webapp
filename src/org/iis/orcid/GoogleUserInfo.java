package org.iis.orcid;


import com.google.api.client.auth.oauth2.Credential;

import org.json.*;

import java.io.Serializable;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
Attributes
	- credential
	- id
	- email
	- picture
	- error
	
Methods
	- update (profile) - extract googleId, googleEmail, picture (googleId validates?)
 */
public final class GoogleUserInfo implements Serializable {

	private static boolean debug = true;
	
	private String access_token = null;
	private String id = null;
	private String email = null;
	private String profile = null;
	private String error = null;
	private String picture = null;
	private String name = null;
	
	/**
	 * Constructor initializes the Google Authorization Code Flow with CLIENT ID, SECRET, and SCOPE 
	 */
	public GoogleUserInfo() {
	}
	
	public String getAccessToken () {
		return this.access_token;
	}
	
	public void setAccessToken (String access_token) {
		this.access_token = access_token;
	}
	
	public String getId () {
		return this.id;
	}
	
	public void setId (String id) {
		this.id = id;
	}
	
	public String getProfile () {
		return this.profile;
	}
	
	public void setProfile (String profile) {
		this.update (profile);
	}
	
	public String getEmail () {
		return this.email;
	}
	
	public void setEmail (String email) {
		this.email = email;
	}	
	
	public String getPicture () {
		return this.picture;
	}
	
	public String getName () {
		return this.name;
	}

	
/* 	public String getError () {
		return this.error;
	} */
	
	public boolean validate (String googleUserId) throws Exception {
		prtln ("VALIDATE");
		if (googleUserId == null || googleUserId.trim().length() == 0)
			throw new Exception ("supplied googleUserId was empty");
		if (this.id != null && this.id.equals(googleUserId))
			return true;
		throw new Exception ("I am for a different user!");
	}
	
	private void update (String profileJsonString) {
		// prtln ("\nupdate ()");
		JSONObject json = null;
		try {
			
			// error handling done down stream (see OAuthAction)
			//    - if profile.has("error") ....
			
			json = new JSONObject (profileJsonString);
			this.profile = json.toString();
			if (json.has("id"))
				this.id = json.getString("id");
			if (json.has("email"))
				this.email = json.getString("email");
			if (json.has("picture"))
				this.picture = json.getString("picture");
			if (json.has("name"))
				this.name = json.getString("name");		
			
		} catch (Throwable t) {
			prtln ("updateGoogleUserInfo error: " + t.getMessage());
		}
		// prtln (" ... after update: " + this.toString() + "\n");
	}
	
	public String toString () {
		String s = "";
		s += this.email + " - " + this.id;
		try {
			JSONObject profileJson = new JSONObject (this.profile);
			s += "\n" + profileJson.toString();
		} catch (Throwable t) {
			prtln ("WARNING: could not get profile: " + t.getMessage());
		}
		return s;
	}
		
	
	private static void prtln (String s) {
		System.out.println (s);
	}
}
