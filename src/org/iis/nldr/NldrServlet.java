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
package org.iis.nldr;

import org.dlsciences.nldr.gAnalytics;
import org.dlsciences.nldr.gaPayload;

import java.util.regex.*;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.text.*;
import org.dom4j.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;

import org.dlese.dpc.webapps.tools.GeneralServletTools;
import org.dlese.dpc.xml.Dom4jUtils;

/**
 *  Servlet that implements the citableUrl scheme, in which citableURLs are
 *  either redirected to NLDR landingPages (views) or, when the citableURL
 *  refers to a digital asset, binary content is streamed to the requestor.
 *
 * @author    Jonathan Ostwald
 */
public final class NldrServlet extends HttpServlet {
	private static boolean debug = true;
	
	/* private String ncsDDSWSBaseUrl; */
	private File assetDirectory;
	private String landingPageBaseUrl;
	private Map accessionNumberMap;
	private Pattern accessionNumberPattern;

	/**  Constructor for the NldrServlet object */
	public NldrServlet() { }


	/**
	 *  Initialize the servlet with values for assetDirectoryPath, which refers to
	 *  the location of digital assets, and landingPageBaseUrl, which is used to
	 *  construct URLs to resource landing pages.
	 *
	 * @param  config                the servlet config
	 * @exception  ServletException  if required config param is not found
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext servletContext = getServletContext();

		String assetDirectoryPath = (String) servletContext.getInitParameter("assetDirectoryPath");
		if (assetDirectoryPath == null || assetDirectoryPath.trim().length() == 0)
			throw new ServletException("initParamter \"assetDirectoryPath\" not found");

		assetDirectory = new File(GeneralServletTools.getAbsolutePath(assetDirectoryPath, servletContext));
		if (!assetDirectory.exists())
			throw new ServletException("assetDirectory does not exist at: " + assetDirectoryPath);
		prtln("assetDirectoryPath: " + assetDirectoryPath);

		landingPageBaseUrl = (String) servletContext.getInitParameter("landingPageBaseUrl");
		if (landingPageBaseUrl == null || landingPageBaseUrl.trim().length() == 0)
			throw new ServletException("initParamter \"landingPageBaseUrl\" not found");
		prtln("landingPageBaseUrl: " + landingPageBaseUrl);
		
		File accessionMapData = new File(GeneralServletTools.getAbsolutePath("WEB-INF/data/accessionNumberMappings.xml", servletContext));
		if (!accessionMapData.exists())
			throw new ServletException("accessionMapData not found");
		this.accessionNumberMap = getAccessionNumberMap (accessionMapData);
		
		accessionNumberPattern = Pattern.compile ("DR000[0-9]{3}");
		// prtln (this.accessionNumberMap.size() + " accession number mappings read");
		
		prtln ("NldrServlet initialized");
	}

	private Map getAccessionNumberMap (File accessionMapData) throws ServletException {
		Map map = new HashMap();
		Document doc = null;
		try {
			doc = Dom4jUtils.getXmlDocument(accessionMapData);
		} catch (Exception e) {
			throw new ServletException ("getAccessionNumberMap Error: " + e.getMessage());
		}
		for (Iterator i=doc.selectNodes("/accessionNumberMappings/mapping").iterator();i.hasNext();) {
			Element e = (Element)i.next();
			map.put (e.attributeValue("drNumber"), e.attributeValue("queryString"));
		}
		return map;
	}

	
	private void showRequestInfo (HttpServletRequest request) {
		prtln ("path info");
		prtln ("\trequestURI: " + request.getRequestURI());
		prtln ("\tpathInfo: " + request.getPathInfo());
/* 		prtln ("paramater names");
		Enumeration pnames = request.getParameterNames();
		while (pnames.hasMoreElements()){
			prtln ((String)pnames.nextElement());
		}
		
		prtln ("attribute names");
		Enumeration anames = request.getAttributeNames();
		while (anames.hasMoreElements()){
			prtln ((String)anames.nextElement());
		} */
	}
	

	/**
	 *  Serve the asset content or forward to NLDR View for handling, depending on
	 *  parameters present in request.<P>
	 *
	 *  NOTE: eventually, the foward to NLDRs will be done by apache rewrite rule,
	 *  so this servlet will receive asset requests only.
	 *
	 * @param  request
	 * @param  response
	 * @exception  ServletException
	 * @exception  IOException
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// prtln("doGet()");

		ServletContext servletContext = getServletContext();
		HttpSession session = request.getSession(true);

		// showRequestInfo (request);
		
		try {

			/*
			legal paths:
				resource:
					collections/{resourceId}
				asset:
					assets/{collId}/{assetId}
				DRnum
					{DRNum}
			*/
			String path = request.getPathInfo();
			if (path == null)
				path = "";
			prtln("path: " + path);

			String[] pathSegments = path.split("/");

			// prtln(pathSegments.length + " path segments");

			if (pathSegments.length == 0) {
				// no path - treat as 404
				forwardToViews404 (request, response);
				return;
			}
			
			String seg1 = null;
			String seg2 = null;
			String seg3 = null;
			
			if (pathSegments.length > 1)
				seg1 = pathSegments[1]; 

			if (pathSegments.length > 2)
				seg2 = pathSegments[2];

			if (pathSegments.length > 3)
				seg3 = pathSegments[3];

 			// prtln("seg1: " + seg1);
			// prtln("seg2: " + seg2);
			// prtln("seg3: " + seg3);

			if (seg1.equals("assets")) {
				// just send the whole servlet path:
				doServeContent (path, request, response);
				return;
			}
			
			Matcher drMatcher = accessionNumberPattern.matcher (seg1);
			if (drMatcher.matches()) {
				String accessionNumber = seg1;
				String queryPath = (String)this.accessionNumberMap.get(accessionNumber);
				if (queryPath == null) {
					forwardToViews404 (request, response);
				}
				else {
					String viewsAddress = landingPageBaseUrl + "?" + queryPath;
					// prtln("resolved DR path: " + viewsAddress);
					redirectToAddress (viewsAddress, request, response);
				}
				return;
			}
			// resources have only two seqments (collections/${id})
			if (seg3 == null) {
				String collId = seg1;
				String itemId = seg2;
				forwardToViews(itemId, request, response);
				return;
			}
			else {
				throw new Exception ("unparsable path");
			}
		} catch (NullPointerException e) {
			prtlnErr("NldrServlet caught unknown exception. \t\n(remoteAddr: " + request.getRemoteAddr() + ")");
			e.printStackTrace();
			forwardToViews404 (request, response);
			return;
		} catch (Throwable e) {
			prtlnErr(e.getMessage() + " \n\t(remoteAddr: " + request.getRemoteAddr() + ")");
			e.printStackTrace();
			forwardToViews404 (request, response);
			return;
		}
	}

	/*
	http://localhost/nldr/assets/ncar-books/cover-jpgs/atlas_of_the_oceans.jpg
	http://localhost/nldr/assets/soars/2001_Kelly_Resa.pdf
	http://localhost/nldr/assets/staffnotes/asset-000-000-001-462.pdf
	http://localhost/nldr/assets/foo.jpg
	http://localhost/nldr/collections/fooberry/foo.jpg
	*/

	/** Serve binary content or forward to approprate JSP for handling. */
	private void doServeContent(String path,
	                            HttpServletRequest request,
	                            HttpServletResponse response) throws ServletException, IOException {

		// prtln("doServeContent!");
		ServletContext servletContext = getServletContext();

		String[] pathSegments = path.split("/");
		
		// assetId is last segment
		// we discard the first two segments ("" & "assets") when constructing the assetPath
		String assetId = pathSegments[pathSegments.length-1];
		String assetPath = "";
		for (int i=2;i<pathSegments.length-1;i++) {
			assetPath += "/" + pathSegments[i];
		}
		
		// prtln ("assetId: " + assetId);
		// prtln ("assetPath: " + assetPath);
		
		File fileToRead = new File(assetDirectory, assetPath + "/" + assetId);
		// prtln("fileToRead: " + fileToRead);

		if (!fileToRead.exists() || fileToRead.isDirectory()) {
			prtlnErr("Resource file not found in repository for path=" + path);
			
			forwardToViews404(request, response);
			return;
		}

		// get fileName and size from file on disk
		String fileName = fileToRead.getName();
		// prtln("fileName: " + fileName);
		
		String sizeBytes = String.valueOf(fileToRead.length());
		// prtln("sizeBytes: " + sizeBytes);

		// Get the mime type from the servlet config (defined in global or webapp web.xml). If not available, use default.
		String contentType = servletContext.getMimeType(fileName.toLowerCase());

		// The default contentType to use if unknown, per rfc1341:
		if (contentType == null)
			contentType = "application/octet-stream";
		// prtln("contentType: " + contentType);

		// Set the headers:
		response.reset();

		response.setContentType(contentType);

		// pdf, image, text are not setAsAttachment
		if (setAsAttachment(contentType, fileName)) {
			// prtln("Attachment");
			response.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		}
		else {
			// prtln ("NOT attachment");
			response.addHeader("Content-Disposition", "filename=\"" + fileName + "\"");
		}
		if (sizeBytes != null)
			response.addHeader("Content-Length", sizeBytes);

		// Generate Last-Modified header in HTTP-date format rfc1123-date
		Date lastModifiedDate = new Date(fileToRead.lastModified());

		SimpleDateFormat df = new SimpleDateFormat("E', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' GMT'");
		df.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
		String datestg = df.format(lastModifiedDate);
		response.addHeader("Last-Modified", datestg);

		// Stream the binary content:
		OutputStream responseOutputStream = response.getOutputStream();
		BufferedInputStream contentInputStream = new BufferedInputStream(new FileInputStream(fileToRead));
		ByteArrayOutputStream contentOutputStream = new ByteArrayOutputStream(2048);
		int contentByte;
		while ((contentByte = contentInputStream.read()) != -1)
			contentOutputStream.write(contentByte);
		contentInputStream.close();
		contentOutputStream.writeTo(responseOutputStream);
		recordHit(path, request);
		return;
	}

	private void recordHit (String path, HttpServletRequest request) {
		String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String context = request.getContextPath();
        String doc_url = scheme+"://"+serverName+":"+new Integer(serverPort).toString()+context+path;
        String user_agent = request.getHeader("user-agent");
        String referrer = request.getHeader("referer");

        gaPayload ga_payload_data = new gaPayload();

        ga_payload_data.setUserAgent(user_agent);
        ga_payload_data.setProtocolVersion(new Integer(1).toString());
        ga_payload_data.setTrackingId("UA-17806895-9");
        ga_payload_data.setClientId();
        ga_payload_data.setHitType("pageview");
        ga_payload_data.setDocumentReferrer(referrer);
        ga_payload_data.setDocumentLocationUrl(doc_url);
        ga_payload_data.setDocumentTitle(path);
        ga_payload_data.setCacheBuster(true);

        gAnalytics ga = new gAnalytics(ga_payload_data);
        prtln ("calling ga.recordHit()");
        Boolean success = ga.recordHit();
    }

	/* Determine if the content should be set as an attachment for download or inline for display inthe browser */
	private boolean setAsAttachment(String contentType, String fileName) {
		boolean isAttachment = true;

		// prtln("setAsAttachment");
		// prtln("  contentType: " + contentType);

		// image, text and pdf are NOT delivered as attachment
		if (contentType.startsWith("image") || contentType.startsWith("text") || contentType.startsWith("application/pdf"))
			isAttachment = false;

		// These may be redundent, but for good measure:
		else if (fileName != null) {
			fileName = fileName.toLowerCase();
			if (fileName.endsWith(".pdf") || fileName.endsWith(".jpg") || fileName.endsWith(".gif") || fileName.endsWith(".xml"))
				isAttachment = false;
		}

		// prtln("setAsAttachment(contentType:'" + contentType + "', fileName:'" + fileName + "'): " + isAttachment);
		return isAttachment;
	}


	/*
		http://localhost/nldr/monographs/MONOGRAPH-000-000-000-670
	*/
	
	/**
	 *  http://nldr.library.ucar.edu/views/index.php?collId=<collKey>&itemId=
	 *  <resourceID>&format=<xmlFormat>
	 *
	 * @param  itemId    item ID
	 * @param  assetId   asset ID
	 * @param  request   the request
	 * @param  response  the response
	 */
	private void forwardToViews(String itemId, HttpServletRequest request,
	                            HttpServletResponse response) {

		String viewsAddress = landingPageBaseUrl;
		
		if (itemId != null) {
			// viewsAddress += "?itemId=" + itemId;
			viewsAddress += itemId;
		}

		redirectToAddress (viewsAddress, request, response);

	}

	private void forwardToViews404 (HttpServletRequest request, HttpServletResponse response) {
		String views404Address = landingPageBaseUrl + "404.php";

		redirectToAddress (views404Address, request, response);
	
	}
	
	private void redirectToAddress (String address, HttpServletRequest request, HttpServletResponse response) {
		try {
			// prtln("redirecting to: " + address);
			response.sendRedirect(address);
		} catch (IllegalStateException ise) {
			prtlnErr("Got IllegalStateException trying to forward request to " + address);
			if (response.isCommitted())
				prtln ("response has already been committed (this should not be the case!");
			ise.printStackTrace();
		} catch (Throwable t) {
			prtlnErr("Unable to forward request to " + address + ": " + t);
		}
	}
	
	// ---------------------- Debug info --------------------

	/**
	 *  Return a string for the current time and date, sutiable for display in log
	 *  files and output to standout:
	 *
	 * @return    The dateStamp value
	 */
	protected final static String getDateStamp() {
		return
			new SimpleDateFormat("MMM d, yyyy h:mm:ss a zzz").format(new Date());
	}


	/**
	 *  Output a line of text to error out, with datestamp.
	 *
	 * @param  s  The text that will be output to error out.
	 */
	private final void prtlnErr(String s) {
		System.err.println(getDateStamp() + " NldrServlet Error: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			// System.out.println(getDateStamp() + " NldrServlet: " + s);
			System.out.println("NldrServlet: " + s);
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

