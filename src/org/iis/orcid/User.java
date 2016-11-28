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

/**
 *  Holds data about an authenticated user. <p>
 *
 *  Approach taken from Struts: The Complete Reference by James Holmes. See
 *  http://www.devarticles.com/c/a/Java/Securing-Struts-Applications/
 *
 * @author    John Weatherley
 */
public interface User {
	/**
	 *  Gets the userId, which is unique for this user and must not change once it is set.
	 *
	 * @return    The userId value
	 */
	public String getUserId();


	/**
	 *  Gets the username, typically the name used to login. The username can be changed.
	 *
	 * @return    The username value
	 */
	public String getUsername();


	/**
	 *  Gets the firstName attribute of the User object
	 *
	 * @return    The firstName value
	 */
	public String getFirstName();

	public String getEmail();
	

	/**
	 *  Gets the lastName attribute of the User object
	 *
	 * @return    The lastName value
	 */
	public String getLastName();

	/**
	 *  Gets the dispay name, which is first, last, username.
	 *
	 * @return    The display value
	 */
	public String getDisplayname();

	public void setUserGoogleId(String userGoogleId);
	
	public String getUserGoogleId();
	
}

