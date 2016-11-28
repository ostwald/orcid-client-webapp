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
import java.net.URI;
import java.net.URLEncoder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;

import org.dlese.dpc.webapps.tools.GeneralServletTools;
import org.dlese.dpc.xml.Dom4jUtils;

/**
 *  Servlet that forwards citableURLs to ARK URLs when possible. These ARK URLs, in turn,
 	resolve to Islandora URLs.
 	
    When NOT possible it implements the NLDR citableUrl scheme, in which citableURLs are
 *  either redirected to NLDR landingPages (views) or, when the citableURL
 *  refers to a digital asset, binary content is streamed to the requestor.
 *
 * @author    Jonathan Ostwald
 */
public final class OS2ResolverServlet extends HttpServlet {
	private static boolean debug = true;
	
	private final static String ARK_URL_PREFIX = "http://n2t.net/";
	
	private File assetDirectory;
	private String landingPageBaseUrl;
	private Map<String,String> accessionNumberMap;
	private Map<String,String> arkMap;
	private Map<String,String>dilMap;
	private Pattern accessionNumberPattern;
	private Pattern queryStringPattern;

	/**  Constructor for the OS2ResolverServlet object */
	public OS2ResolverServlet() { }


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
		
		File arkMapData = new File(GeneralServletTools.getAbsolutePath("WEB-INF/data/arkMappings.xml", servletContext));
		if (!arkMapData.exists())
			throw new ServletException("arkMapData not found");
		this.arkMap = getArkMap (arkMapData);
		
		File dilMapData = new File(GeneralServletTools.getAbsolutePath("WEB-INF/data/dilMappings.xml", servletContext));
		if (!dilMapData.exists())
			throw new ServletException("dilMapData not found");
		this.dilMap = getDilMap (dilMapData);
		
		// Pattern to idenfity DR (Assesion) numbers
		accessionNumberPattern = Pattern.compile ("DR000[0-9]{3}");
		
		// pattern to pluck itemId from query string (e.g., "collId=technotes&amp;itemId=TECH-NOTE-000-000-000-018")
		queryStringPattern = Pattern.compile (".*itemId=([a-zA-Z0-9-]*)");

		prtln ("OS2ResolverServlet initialized");
	}

	/**
	* Maps accession_number to nldr_id
	*/
	private Map<String,String> getAccessionNumberMap (File accessionMapData) throws ServletException {
		Map map = new HashMap<String,String>();
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

	/**
	* Maps nldr_id to ark_id
	*/
	private Map<String,String> getArkMap (File arkMapData) throws ServletException {
		Map map = new HashMap<String,String>();
		Document doc = null;
		try {
			doc = Dom4jUtils.getXmlDocument(arkMapData);
		} catch (Exception e) {
			throw new ServletException ("getArkMap Error: " + e.getMessage());
		}
		for (Iterator i=doc.selectNodes("/arkMappings/mapping").iterator();i.hasNext();) {
			Element e = (Element)i.next();
			map.put (e.attributeValue("osmId"), e.attributeValue("arkId"));
		}
		return map;
	}
	
	/**
	* Maps dil_id (DIL portfolio ID) to opensky2 pid
	*/
	private Map<String,String> getDilMap (File dilMapData) throws ServletException {
		Map map = new HashMap<String,String>();
		Document doc = null;
		try {
			doc = Dom4jUtils.getXmlDocument(dilMapData);
		} catch (Exception e) {
			throw new ServletException ("getDilMap Error: " + e.getMessage());
		}
		for (Iterator i=doc.selectNodes("/dil_data/mapping").iterator();i.hasNext();) {
			Element e = (Element)i.next();
			map.put (e.attributeValue("dil"), e.attributeValue("pid"));
		}
		return map;
	}

	/**
	 * Resolve CitableUrls
	 *
	 *	Handle Request. Forward to ARK URL if possible, otherwise Serve the asset content or 
	 *  forward to NLDR View for handling, depending on
	 *  parameters present in request.
	 *
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
				DIL
					netpub/server.php?${query}
			*/
			String path = request.getPathInfo();
			if (path == null)
				path = "";
			System.out.println(getDateStamp() + " - " + path);

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
			
			if (seg1.equals("netpub")) {
				doDilGet (request, response);
				return;
			}
			
			/* we know it isn't an asset, so must be a resource
			look for DR Number or a resourceId. Either way we should get an osmId for the resource
			
			if this resource has an ark, forward to the arkUrl
			otherwise forward to views
			*/
			
			String osmId = null;
			
			// All we need is the itemId and they we can use ID -> Ark mapping
			Matcher drMatcher = accessionNumberPattern.matcher (seg1);
			if (drMatcher.matches()) {
				String accessionNumber = seg1;
				String queryPath = (String)this.accessionNumberMap.get(accessionNumber);
				
				if (queryPath != null) {
					// try to extract osmId ("itemId" of queryPath)
					Matcher qStrMatcher = queryStringPattern.matcher (queryPath);
					if (qStrMatcher.matches()) {
						osmId = qStrMatcher.group(1);
					}
				}

			}
			// resources have only two seqments and first is hard-coded (collections/${osmId})
			
			if (osmId == null && seg3 == null) {
				osmId = seg2;
			}
			
			if (osmId != null) {
				// look up ark
				String arkId = arkMap.get(osmId);
				prtln (" - ARK: " + arkId);
				if (arkId != null) {
					// FORWARD TO ARK URL - ARK_URL_PREFIX + arkId
					redirectToAddress (ARK_URL_PREFIX + arkId, request, response);
				} else {
					forwardToViews(osmId, request, response);
				}
				return;
			}
			else {
				throw new Exception ("unparsable path");
			}
		} catch (NullPointerException e) {
			prtlnErr("OS2ResolverServlet caught unknown exception. \t\n(remoteAddr: " + request.getRemoteAddr() + ")");
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

	/* exceptions forward to main DIL collection page in opensky2 
	
	http://www.fin.ucar.edu/netpub/server.np?find&field=Keywords&op=contains&value=CCOPE&catalog=catalog&site=imagelibrary&sorton=Filename&template=results.np&sorton=Item+ID&ascending=0

	to:

	https://opensky.ucar.edu/islandora/search/CCOPE?type=dismax&f[0]=mods_extension_collectionKey_ms%3A%22cimg%22
	
	*/
	public void doDilGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String queryString = request.getQueryString();
		prtln ("DO_DIL_GET() queryString: " + queryString);
				
		try {
		
			String catalog = request.getParameter("catalog");
			String site = request.getParameter("site");
			
			if (!("catalog".equals(catalog) && "imagelibrary".equals(site)))
				throw new ServletException ("bad catalog or site param(s) in " + queryString);
			
			String field = request.getParameter("field");
			String op = request.getParameter("op");
			String value = request.getParameter("value");
			String find = request.getParameter("find");
			
			if (find != null) {
				prtln ("- FIND REQUEST");
				prtln ("  - value: " + value);
				// prtln ("  - field: " + field);
				// prtln ("  - op: " + op);
				
				// https://opensky.ucar.edu/islandora/search/CCOPE?type=dismax&f[0]=mods_extension_collectionKey_ms%3A%22cimg%22
				
				String os2Url = "https://opensky.ucar.edu/islandora/search/"+ value +"?type=dismax&f[0]=mods_extension_collectionKey_ms%3A%22cimg%22";
				redirectToAddress (os2Url, request, response);
				prtln ("REDIRECTED to OpenSky2 URL: " + os2Url);
				return;
			}
			
			if ("itemid".equals(field)) { // metadata view
				
				String pid = dilMap.get(value);
				if (pid == null)
					throw new Exception ("PID not found for DIL_ID (value): " + value);
				// make url and forward and return
				String os2Url = "https://opensky.ucar.edu/islandora/object/"+URLEncoder.encode(pid);
				redirectToAddress (os2Url, request, response);
				prtln ("REDIRECTED to OpenSky2 URL: " + os2Url);
				return;
			}
	
			// original param holds dil_id
			String original = request.getParameter("original");
			
			if (original != null) { // image view/download
				String pid = dilMap.get(original);
				if (pid == null)
					throw new Exception ("PID not found for DIL_ID (original): " + original); 
				
				String os2Url = null;
				if (request.getParameter("download") != null)  {
					os2Url = "https://opensky.ucar.edu/islandora/object/" +URLEncoder.encode(pid) + "/datastream/OBJ/download";
					prtln ("DOWNLOAD");
				}
				else {
					os2Url = "https://opensky.ucar.edu/islandora/object/"+URLEncoder.encode(pid) + "/datastream/JPG/view";
				}
				redirectToAddress (os2Url, request, response);
				prtln ("REDIRECTED to OpenSky2 URL: " + os2Url);
				return;
			}				
			
			throw new Exception ("Could not handle DIL request: " + queryString);
			
		} catch (Exception e) {
			// forward to DIL collection page
			prtlnErr ("ERROR: doDilGet() - " + e);
			String dil_home_url = "https://opensky.ucar.edu/islandora/object/opensky%3Aimagegallery";
			redirectToAddress (dil_home_url, request, response);
			prtln ("REDIRECTED to DIL collection in OpenSky2: " + dil_home_url);
		}
		
	}
	
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
	* Debugging
	*/
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
		System.err.println(getDateStamp() + " OS2ResolverServlet Error: " + s);
	}


	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to
	 *  true.
	 *
	 * @param  s  The String that will be output.
	 */
	private final void prtln(String s) {
		if (debug) {
			// System.out.println(getDateStamp() + " OS2ResolverServlet: " + s);
			System.out.println("OS2ResolverServlet: " + s);
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

