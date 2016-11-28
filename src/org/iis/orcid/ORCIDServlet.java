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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.SAXReader;
import org.dom4j.Element;
import org.dom4j.Attribute;
import org.dom4j.Node;

import java.text.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;


import org.dlese.dpc.webapps.tools.*;

import org.iis.orcid.GoogleAuthHelper;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.extensions.java6.auth.oauth2.FileCredentialStore;


/**
 *  Servlet and initialization for the Curriculum Customization Service.
 *
 * @author    John Weatherley
 */
public class ORCIDServlet extends HttpServlet {
	private boolean debug = false;

	final static int VALID_REQUEST = 0;
	final static int REQUEST_EXCEPTION = -1;
	final static int UNRECOGNIZED_REQUEST = 1;
	final static int NO_REQUEST_PARAMS = 2;
	final static int INITIALIZING = 3;
	final static int NOT_INITIALIZED = 4;
	final static int INVALID_CONTEXT = 5;


	private boolean isInitialized = false;


	/**  Constructor for the ORCIDServlet object */
	public ORCIDServlet() { }


	/**
	 *  The standard <code>HttpServlet</code> init method, called only when the servlet is first loaded.
	 *
	 * @param  config
	 * @exception  ServletException
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
	
		if (isInitialized) {
			System.out.println("CCS has already been initialized. Call to ORCIDServlet.init() aborted...");
			return;
		}
		isInitialized = true;

		ServletContext servletContext = getServletContext();
	
		try {
			setUpGoogleAuth (servletContext);
		} catch (Exception e) {
			prtlnErr ("setUpGoogleAuth error: " + e);
		}
		
		// Set the startup date and log the time:
		servletContext.setAttribute("ccsStartUpDate", new Date());
		System.out.println("\n\n" + getDateStamp() + " ORCIDServlet started." + "\n\n");
	}


	/**
	 *  Initialize GoogleAuth and credentialStore using initParams
	 *
	 * @param  servletContext  The ServletContext
	 */
	private void setUpGoogleAuth(ServletContext servletContext) throws Exception {
		servletContext.setAttribute ("GoogleIntegrationEnabled", false);
		String googleClientId = servletContext.getInitParameter("googleClientId");
		if (googleClientId != null && googleClientId.length() > 0) {
			GoogleAuthHelper.setClientId(googleClientId);
			prtln ("googleClientId: " + googleClientId);
			
			String googleClientSecret = servletContext.getInitParameter("googleClientSecret");
			if (googleClientSecret != null && googleClientSecret.length() > 0) {
				GoogleAuthHelper.setClientSecret(googleClientSecret);
				prtln ("googleClientSecret: " + googleClientSecret);
			}
			else
				throw new Exception ("Expected googleClientSecret initParam");
		
			String googleCallbackUri = servletContext.getInitParameter("googleCallbackUri");
			if (googleCallbackUri != null && googleCallbackUri.length() > 0) {
				GoogleAuthHelper.setCallbackUri(googleCallbackUri);
				prtln ("googleCallbackUri: " + googleCallbackUri);
			}
			else
				throw new Exception ("Expected googleCallbackUri initParam");	
			
			String googleCredentialStorePath = servletContext.getInitParameter("googleCredentialStore");
			if (googleCredentialStorePath != null && googleCredentialStorePath.length() > 0) {
				try {
					prtln ("googleCredentialStorePath: " + googleCredentialStorePath);
					FileCredentialStore credentialStore = new FileCredentialStore(new File (googleCredentialStorePath), new JacksonFactory());
					GoogleAuthHelper.setCredentialStore(credentialStore);
					prtln ("set GoogleAuthHelper.CredentialStore");
				} catch (IOException ioe) {
					throw new Exception ("Could not instantiate credentialStore for GoogleAuthHelper: " + ioe.getMessage());
				}
				
			}
			else {
				throw new Exception ("Expected googleCredentialStore initParam");
			}
			
			servletContext.setAttribute ("GoogleIntegrationEnabled", true);
			
		}
	}
	

	/**  Shut down sequence. */
	public void destroy() {
		System.out.println("\n\n" + getDateStamp() + " ORCIDServlet stopped." + "\n\n");
	}


	/**
	 *  Standard doPost method forwards to doGet
	 *
	 * @param  request
	 * @param  response
	 * @exception  ServletException
	 * @exception  java.io.IOException
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		 throws ServletException, IOException {
		doGet(request, response);
	}


	/**
	 *  The standard required servlet method, just parses the request header for known parameters. The <code>doPost</code>
	 *  method just calls this one. See {@link HttpServlet} for details.
	 *
	 * @param  request
	 * @param  response
	 * @exception  ServletException
	 * @exception  java.io.IOException
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		 throws ServletException, IOException {
		PrintWriter out = response.getWriter();

		int result = handleRequest(request, response, out);
		switch (result) {

						case VALID_REQUEST:
							response.setContentType("text/html");
							// processed a request okay
							return;
						case UNRECOGNIZED_REQUEST:
							// no recognized parameters
							response.setContentType("text/html");
							out.println("Called with unrecognized parameter(s)...");
							return;
						case NO_REQUEST_PARAMS:
							// no paramters
							response.setContentType("text/html");
							out.println("Request did not contain a parameter...");
							return;
						case INITIALIZING:
							response.setContentType("text/html");
							out.println("System is initializing...");
							out.println(" ... initializtion may take less than a second or several minutes.");
							out.println(" ... please try request again.");
							return;
						case NOT_INITIALIZED:
							out.println("System is not initialized...");
							out.println(" ... the server may need to be restarted,");
							out.println(" ... or there is a problem with configuration.");
							out.println("");
							out.println("Please inform support@your.org.");
							out.println("");
							out.println("Thank You");
							return;
						case INVALID_CONTEXT:
							response.setContentType("text/html");
							out.println("A request was recieved, but the context can not be identified...");
							out.println(" ... either  unable to initialize the catalog context," +
								" or the servlet container is in an invalid state.");
							return;
						default:
							// an exception occurred
							response.setContentType("text/html");
							out.println("An unexpected exception occurred processing request...");
							return;
		}
	}


	/**
	 *  Used to provide explicit command parameter processing.
	 *
	 * @param  request   The request
	 * @param  response  The response
	 * @param  out       The output
	 * @return
	 */
	private int handleRequest(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		try {
			Enumeration paramNames = request.getParameterNames();
			if (paramNames.hasMoreElements()) {
				while (paramNames.hasMoreElements()) {
					String paramName = (String) paramNames.nextElement();
					String[] paramValues = request.getParameterValues(paramName);
					// this next section can use an interface and hashmap -
					// see pg 228 of JavaServerPages
					if (paramValues.length == 1) {
						if (paramName.equals("command")) {
							if (paramValues[0].equals("stop")) {
								//fileIndexingService.stopTester();
							}
							return VALID_REQUEST;
							//if(paramValues[0].equals("start"))
							//	fileIndexingService.startTester();

						}
//  								if (paramName.equals("query")) {
//  									//catalog.reinit();
//  									//response.setContentType("text/html");
//  									//PrintWriter out = response.getWriter();
//  									//out.println("CatalogAdmin called Catalog.reinit() ");
//  									//out.println("See Catalog Activity Log for messages.");
//  									return VALID_REQUEST;
//  								}
//  								if (paramName.equals("unlock")) {
//  									//releaseLock(paramValues[0], request, response);
//  									return VALID_REQUEST;
//  								}
					}
				}
				return UNRECOGNIZED_REQUEST;
			}
			return NO_REQUEST_PARAMS;
			//}
			//else if (catalog.initializing())
			//	return CATALOG_INITIALIZING;
			//else
			//	return CATALOG_NOT_INITIALIZED;
			//}
			//return INVALID_CONTEXT;
		} catch (Throwable t) {
			return REQUEST_EXCEPTION;
		}
	}


	/**
	 *  Gets the absolute path to a given file or directory. Assumes the path passed in is eithr already absolute
	 *  (has leading slash) or is relative to the context root (no leading slash). If the string passed in does
	 *  not begin with a slash ("/"), then the string is converted. For example, an init parameter to a config
	 *  file might be passed in as "WEB-INF/conf/serverParms.conf" and this method will return the corresponding
	 *  absolute path "/export/devel/tomcat/webapps/myApp/WEB-INF/conf/serverParms.conf." <p>
	 *
	 *  If the string that is passed in already begings with "/", nothing is done. <p>
	 *
	 *  Note: the super.init() method must be called prior to using this method, else a ServletException is
	 *  thrown.
	 *
	 * @param  fname                 An absolute or relative file name or path (relative the the context root).
	 * @return                       The absolute path to the given file or path.
	 * @exception  ServletException  An exception related to this servlet
	 */
	private String getAbsolutePath(String fname)
		 throws ServletException {
		return GeneralServletTools.getAbsolutePath(fname, getServletContext());
	}


	/**
	 *  Gets the absolute path to a given file or directory. Assumes the path passed in is eithr already absolute
	 *  (has leading slash) or is relative to the context root (no leading slash). If the string passed in does
	 *  not begin with a slash ("/"), then the string is converted. For example, an init parameter to a config
	 *  file might be passed in as "WEB-INF/conf/serverParms.conf" and this method will return the corresponding
	 *  absolute path "/export/devel/tomcat/webapps/myApp/WEB-INF/conf/serverParms.conf." <p>
	 *
	 *  If the string that is passed in already begings with "/", nothing is done. <p>
	 *
	 *  Note: the super.init() method must be called prior to using this method, else a ServletException is
	 *  thrown.
	 *
	 * @param  fname    An absolute or relative file name or path (relative the the context root).
	 * @param  docRoot  The context document root as obtained by calling getServletContext().getRealPath("/");
	 * @return          The absolute path to the given file or path.
	 */
	private String getAbsolutePath(String fname, String docRoot) {
		return GeneralServletTools.getAbsolutePath(fname, docRoot);
	}


	/**
	 *  Return a string for the current time and date, sutiable for display in log files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	public static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " ORCIDServlet error: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			System.out.println(getDateStamp() + " ORCIDServlet: " + s);
		}
	}


	/**
	 *  Sets the debug attribute of the ORCIDServlet object
	 *
	 * @param  db  The new debug value
	 */
	public final void setDebug(boolean db) {
		debug = db;
	}
}

