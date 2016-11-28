/*
 *  Copyright 2002-2009
 *
 *  Digital Learning Sciences (DLS)
 *  University Corporation for Atmospheric Research (UCAR)
 *  P.O. Box 3000
 *  Boulder, CO 80307-3000
 *
 *  AND
 *
 *  Institute of Cognitive Science
 *  University of Colorado at Boulder
 *  Campus Box 594
 *  Boulder, CO 80309-0594
 *
 *  This file is part of the Curriculum Customization Service (CCS) software
 */
package org.iis.orcid;

import java.util.*;
import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import org.dom4j.*;

import org.apache.http.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.mime.*; // HttpMultipart;
import org.apache.http.entity.mime.content.*;
import org.apache.http.impl.client.*;

import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.message.BasicHeader;
import org.apache.http.entity.StringEntity;

import org.json.*;
import org.iis.orcid.GoogleAuthHelper;

public class GoogleAPIRequestor  {
	
	private static int CONNECTION_TIMEOUT = 5; // secs
	
	public static void main (String[] args) throws Exception {
		String postData = "";
		// JSONObject json = new JSONObject (postData);
		
		String cal_id = "jonathan.ostwald@gmail.com";
		String event_id = "qbqfcs1eat9386smeth1vpm3sg";
		String baseUrl = "https://www.googleapis.com/calendar/v3/calendars/" + cal_id + "/events/" + event_id;
		String access_token = "ya29.AHES6ZQxjKyb27pnm7A44qr28RUfTe_nAYWO8nwjj7uxYX77G5X4iX8";
		String url = baseUrl + "?access_token=" + access_token;
		try {
			int response = delete (url);
			prtln ("delete response: " + response);
		} catch (Throwable t) {
			prtln ("post ERROR: " + t.getMessage());
		}
	}
	
	public static int delete (String urlStr) throws Exception {
		prtln ("delete()");
		URL url = new URL(urlStr);
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		httpCon.setDoOutput(true);
		httpCon.setRequestProperty(
			"Content-Type", "application/x-www-form-urlencoded" );
		httpCon.setRequestMethod("DELETE");
		// httpCon.connect();
		int responseCode = httpCon.getResponseCode();
		prtln ("delete request sent");
		return responseCode;
	}
	
	public static String serializeJson (JSONObject jsonObject) throws Exception {
		JSONStringer json = new JSONStringer();
		StringBuilder sb=new StringBuilder();
		
		if (jsonObject!=null) {
			Iterator<String> itKeys = jsonObject.keys();
			if(itKeys.hasNext())
				json.object();
			while (itKeys.hasNext()) 
			{
				String k=itKeys.next();
				json.key(k).value(jsonObject.get(k));
				// prtln (k + ": "+jsonObject.get(k).toString());
			}             
		}
		json.endObject();
		return json.toString();
	}
	
	/**
	If metadata is present then we do a multipart request
	
	org.apache.http.entity.mime.MultipartEntity
	http://hc.apache.org/httpcomponents-client-ga/httpmime/apidocs/index.html?org/apache/http/entity/mime/MultipartEntity.html
	
	approach: 
	http://stackoverflow.com/questions/1378920/how-can-i-make-a-multipart-form-data-post-request-using-java
	
	*/
	public static String upload (String url, String content, String metadata) throws Throwable {
		prtln ("upload() with metadata");
		
		if (metadata == null || metadata.trim().length() == 0) {
			prtln (" - Content Only");
			return uploadContentOnly(url, content);
		}
		
		prtln (" - MultiPart with metadata");
		
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url);
		
		Charset utf8 = Charset.forName("UTF-8");
		
		StringBody contentPart = new StringBody(content, "text/plain", utf8);
		
		// we want a jsonBody??
		// MIGHT HAVE TO MANIPULATE METADATA - see "post()" below
		
		JSONObject metadataJson = null;
		try {
			metadataJson = new JSONObject(metadata);
		} catch (Throwable t) {
			prtln ("ERROR: pre-processing metadataJson = new JSONObject(metadata);: " + t.getMessage());
		}
		
		
		StringBody metadataPart = new StringBody(serializeJson(metadataJson), 
												 "application/json", 
												 utf8);
		
		MultipartEntity reqEntity = new MultipartEntity();
		reqEntity.addPart("metadata", metadataPart);
		reqEntity.addPart("content", contentPart);
		
		httppost.setEntity(reqEntity);
		
		HttpResponse response = null;
		try {
			response = httpclient.execute(httppost); 
		} catch(SocketException se) {
			prtln("SocketException: " + se);
			throw se;
		}
	
		StringBuilder sb=new StringBuilder();
		InputStream in = response.getEntity().getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = null;
		while((line = reader.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();		
	}
	
	
	/**
	execute simple upload of content only, returning the json response 
	
	called from 
	*/
	public static String uploadContentOnly(String url, String content) throws Throwable {
		prtln ("upload()");
		HttpPost request = new HttpPost(url);		
		
		StringEntity entity = new StringEntity(content);
		entity.setContentType("text/plain;charset=UTF-8");
		entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"text/plain;charset=UTF-8"));
		request.setHeader("Accept", "application/json");
		
		request.setEntity(entity); 
		
		HttpResponse response =null;
		DefaultHttpClient httpClient = new DefaultHttpClient();
		
		HttpConnectionParams.setSoTimeout(httpClient.getParams(), CONNECTION_TIMEOUT*1000); 
		HttpConnectionParams.setConnectionTimeout(httpClient.getParams(),CONNECTION_TIMEOUT*1000); 
		try {
			response = httpClient.execute(request); 
		} catch(SocketException se) {
			prtln("SocketException: " + se);
			throw se;
		}
	
		StringBuilder sb=new StringBuilder();
		InputStream in = response.getEntity().getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = null;
		while((line = reader.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}

	public static String batch(String url, String boundary, String batchBody) throws Throwable {
		prtln ("batch()");
		prtln (" - boundary: " + boundary);
		HttpPost request = new HttpPost(url);
	
		StringEntity entity = new StringEntity(batchBody);
		request.setHeader("Authorization", "ya29.AHES6ZTEO6yO4sRuhpBtrIsS2s5gDwLcjhnK9QN6wRXa9xAT");
		// request.setHeader("Content-Type", "multipart/mixed; boundary="+boundary);
		// request.setHeader("Content-Length", String.valueOf(entity.getContentLength()));
		entity.setContentType("multipart/mixed; boundary="+boundary);
		request.setEntity(entity); 
		
		HttpResponse response =null;
		DefaultHttpClient httpClient = new DefaultHttpClient();
		
		HttpConnectionParams.setSoTimeout(httpClient.getParams(), CONNECTION_TIMEOUT*1000); 
		HttpConnectionParams.setConnectionTimeout(httpClient.getParams(),CONNECTION_TIMEOUT*1000); 
		prtln ("executing request");
		try {
			response = httpClient.execute(request); 
		} catch(SocketException se) {
			prtln("SocketException: " + se);
			throw se;
		}


		int statusCode = response.getStatusLine().getStatusCode();
		prtln ("-- STATUS CODE: " + statusCode);

		if (statusCode == 204) {
			// prtln ("no content, returning empty string");
			return "{}";
		}
	
		InputStream in = response.getEntity().getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder sb=new StringBuilder();
		String line = null;
		while((line = reader.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}
		
		
	public static String post(String url, JSONObject postJsonData) throws Throwable {
		prtln ("post()");
		HttpPost request = new HttpPost(url);
		
		JSONStringer json = null;
		
		if (postJsonData != null) {
		
			json = new JSONStringer();
			
			if (postJsonData!=null) {
				Iterator<String> itKeys = postJsonData.keys();
				if(itKeys.hasNext())
					json.object();
				while (itKeys.hasNext()) 
				{
					String k=itKeys.next();
					json.key(k).value(postJsonData.get(k));
					// prtln (k + ": "+postJsonData.get(k).toString());
				}             
			}
			json.endObject();
		}
		
		if (json != null) {
			
			StringEntity entity = new StringEntity(json.toString());
			entity.setContentType("application/json;charset=UTF-8");
			entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json;charset=UTF-8"));
			request.setHeader("Accept", "application/json");
            prtln("POST Payload: " + json.toString());
			request.setEntity(entity); 
		}
		
		HttpResponse response =null;
		DefaultHttpClient httpClient = new DefaultHttpClient();
		
		HttpConnectionParams.setSoTimeout(httpClient.getParams(), CONNECTION_TIMEOUT*1000); 
		HttpConnectionParams.setConnectionTimeout(httpClient.getParams(),CONNECTION_TIMEOUT*1000); 
		try {
			response = httpClient.execute(request); 
		} catch(SocketException se) {
			prtln("SocketException: " + se);
			throw se;
		}


		int statusCode = response.getStatusLine().getStatusCode();
	    prtln ("-- STATUS CODE: " + statusCode);

		if (statusCode == 204) {
			// prtln ("no content, returning empty string");
			return "{}";
		}
	
		InputStream in = response.getEntity().getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder sb=new StringBuilder();
		String line = null;
		while((line = reader.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}
	
	public static String patch(String url, JSONObject postJsonData) throws Throwable {
		prtln ("patch()");
		HttpPatch request = new HttpPatch(url);
		JSONStringer json = new JSONStringer();
		StringBuilder sb=new StringBuilder();
		
		// prtln ("postJsonData: " + postJsonData.toString(2));
		
		if (postJsonData!=null) {
			Iterator<String> itKeys = postJsonData.keys();
			if(itKeys.hasNext())
				json.object();
			while (itKeys.hasNext()) 
			{
				String k=itKeys.next();
				json.key(k).value(postJsonData.get(k));
				// prtln (k + ": "+postJsonData.get(k).toString());
			}             
		}
		json.endObject();
		
		
		StringEntity entity = new StringEntity(json.toString());
		entity.setContentType("application/json;charset=UTF-8");
		entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json;charset=UTF-8"));
		request.setHeader("Accept", "application/json");
		request.setEntity(entity); 
		
		HttpResponse response =null;
		DefaultHttpClient httpClient = new DefaultHttpClient();
		
		HttpConnectionParams.setSoTimeout(httpClient.getParams(), CONNECTION_TIMEOUT*1000); 
		HttpConnectionParams.setConnectionTimeout(httpClient.getParams(),CONNECTION_TIMEOUT*1000); 
		try {
			response = httpClient.execute(request); 
		} catch(SocketException se) {
			prtln("SocketException: " + se);
			throw se;
		}
	
		InputStream in = response.getEntity().getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = null;
		while((line = reader.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}

	
	private static void prtln (String s) {
		System.out.println(s);
	}
		
		
}


