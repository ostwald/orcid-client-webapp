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
package org.iis.orcid;

import java.util.*;
import java.io.Serializable;
/* import javax.servlet.http.HttpServletRequest; */

/* import org.apache.struts.action.Action; 
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.validator.ValidatorForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.util.MessageResources;
*/

/**
 *  Holds data about an authenticated user.
 *
 * @author    John Weatherley
 */
public class UserBean implements User, Serializable {
	private String userId = null;
	private String username = null;
	private String firstName = null;
	private String lastName = null;
	private String email = null;
	private String userGoogleId = null;



	/**  Resets the data in the form to default values. Needed by Struts if form is stored in session scope. */
	public void reset() {
		//roles = new String[0];
	}

	
	/**
	 *  Returns the value of email.
	 *
	 * @return    The email value
	 */
	public String getEmail() {
		return email;
	}


	/**
	 *  Sets the value of email, converting chars to lower case.
	 *
	 * @param  email  The value to assign email.
	 */
	public void setEmail(String email) {
		if (email == null)
			this.email = null;
		else
			this.email = email.trim().toLowerCase();
	}

	/**
	 *  Creates a User.
	 *
	 * @param  userId     User ID
	 * @param  username   The username
	 * @param  firstName  First name
	 * @param  lastName   Last name

	 */
	
	public UserBean(String userId, String username, String firstName, String lastName, String userGoogleId) {
		this.userId = userId;
		this.username = username;
		this.firstName = firstName;
		this.lastName = lastName;
		this.userGoogleId = userGoogleId;
	}


	/**
	 *  String representation
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		String rolesStr = "";

		return "firstName:" + firstName +
			" lastName:" + lastName +
			" email:" + email +
			" username:" + username +
			" userId:" + userId +
			" userGoogleId:" + userGoogleId;
	}


	/**  Constructor for the UserBean object */
	public UserBean() { }


	/**
	 *  Gets the userId, which is unique for this user.
	 *
	 * @return    The userId value
	 */
	public String getUserId() {
		return this.userId;
	}


	/**
	 *  Sets the userId attribute of the UserBean object
	 *
	 * @param  userId  The new userId value
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}


	/**
	 *  Gets the dispay name, which is first, last, username.
	 *
	 * @return    The display value
	 */
	public String getDisplayname() {
		String txt = ((firstName == null) ? "" : firstName);
		txt += " " + ((lastName == null) ? "" : lastName);
		txt += " (" + username + ")";
		return txt;
	}	
	
	/**
	 *  Gets the username, typically the name used to login.
	 *
	 * @return    The username value
	 */
	public String getUsername() {
		return this.username;
	}


	/**
	 *  Sets the username attribute of the UserBean object
	 *
	 * @param  username  The new username value
	 */
	public void setUsername(String username) {
		if(username == null)
			this.username = null;
		else
			this.username = username.trim();
	}


	/**
	 *  Gets the firstName attribute of the User object
	 *
	 * @return    The firstName value
	 */
	public String getFirstName() {
		return this.firstName;
	}


	/**
	 *  Sets the firstName attribute of the UserBean object
	 *
	 * @param  firstName  The new firstName value
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}


	/**
	 *  Gets the lastName attribute of the User object
	 *
	 * @return    The lastName value
	 */
	public String getLastName() {
		return this.lastName;
	}


	/**
	 *  Sets the lastName attribute of the UserBean object
	 *
	 * @param  lastName  The new lastName value
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setUserGoogleId (String userGoogleId) {
		this.userGoogleId = userGoogleId;
	}
	
	public String getUserGoogleId () {
		return this.userGoogleId;
	}
	
}

