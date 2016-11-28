var accessTokenExpiration = null;

function makeProxyError (errorMsg) {
	var responseCode;
	var msg;
	var details_pat = /.* Details:\s?(.*)/
	var details = details_pat.exec(errorMsg)
	if (details) {
			msg = details[1]
			// log ("details: " + msg)
			var code_pat = /response code: ([0-9]*)/
			responseCode = code_pat.exec(msg)
			// log ("makeProxyError responseCode: " + (responseCode && responseCode[1]))
	}
	return {'msg':msg || errorMsg, 'responseCode':responseCode && responseCode[1]}
}	

function forgetGoogleId () {
	new Ajax.Request(contextPath + '/oauth.do', {
			parameters: {command: 'forget'},
			onSuccess: function(transport) {
				log ("FORGET responseText: " + transport.responseText.strip());
				if(transport.responseText.isJSON()) {
					log ("isJSON");
					try {
						var resp = transport.responseText.evalJSON();
					} catch (error) {
						log ("couldnt eval resp: " + error);
					}
						
					
					if (resp.success) {
						log ("here's where we would refresh display");
						try {
							USER_GOOGLE_INFO.accessToken = null;
							CALENDAR_PROXY = null;
							// updateScheduleDisplay();
						} catch (error) {
							log ("ERROR - couldn't reset display: " + error);
						}
					}

					else if (resp.error) {
						log ("error: " + resp.error);
					}
				}
			}
	});
}
			

function googleProxy (request, callback, options) {
	
	var stopped = false;
	
	function execute () {
		
		if (stopped) {
			log ("googleProxy is stopped");
			return;
		}
		
		// log ("googleProxy Execute()");
		// Iinsert access token
		args.set('access_token', USER_GOOGLE_INFO.accessToken);		
		var apiUrl = request+'?'+args.toQueryString();
		
		if (options && options['boundary'])
			log ('googleProxy() apiUrl: ' + apiUrl);
		
		new Ajax.Request(contextPath + '/oauth.do', {
			parameters: {
				apiUrl: apiUrl, 
				command: command, 
				postBody:postBody, 
				method:method, 
				metadata:metadata,
				batchBody:batchBody,
				boundary:boundary 
			},
			onSuccess: function(transport) {
 				if (batchBody) {
					log ("batchBodyResponse");
					log ("responseText: " + transport.responseText.strip());
				}
				if(transport.responseText.isJSON()) {
					try {
						var responseJson = transport.responseText.evalJSON();
						//log ('responseJson: ' + JSON.stringify(responseJson, undefined, 2));
						
						if (responseJson) {
							
							if (responseJson.error) {
								log ("googleProxy received an Error: " + responseJson.error);
								
								// makeProxyError - parse error for responseCode
								responseJson.error = makeProxyError (responseJson.error);
								log (" - error msg: " + responseJson.error.msg);
								
								
								// HANDLE 401 - unauthorized
								if (responseJson.error.responseCode) {
									log (" - AUTH Error: responseCode " + responseJson.error.responseCode);
									// 401 Unauthorized, 400 Invalid HTTP
									if ( responseJson.error.responseCode == '401' || responseJson.error.responseCode == '400') {
										USER_GOOGLE_INFO.accessToken = null;
									}
									
									if ( responseJson.error.responseCode == '502' ) {
										log ("WARN: Google returned 502 - stopping!");
										stopped = true;
										return;
									}
										
								}
							}
							try {
								// log ("-- GOOGLE PROXY calling callback");
								callback(responseJson);
							} catch (error) {
								log ("WARN: the GoogleProxy callback experienced an error: " + error);
							}
						}
						else {
							log ("no response");
						}
					} catch (e) {
						log ("error processing as JSON: " + e);
					}
					return;
				}
				else {
					log ("response is not json");
				}
			},
			onFailure: function (transport) {
				log ("something went wrong");
			}
		});
	}
		
	// log ("GOOGLE_PROXY - " + USER_GOOGLE_INFO.accessToken);
	
	var args = $H(options);
	var postBody = args.unset('postBody');
	var batchBody = args.unset('batchBody');
	var boundary = args.unset('boundary');
	var metadata = args.unset('metadata');
	var method = args.unset('method');
	var command = args.unset('command') || 'proxy';
	checkAuth(execute);
	// execute();
}

/**
Validates the accessToken and then calls the provided callback function
with the results of the validation call (as json)

@method checkAuth
@param callback 
*/
function checkAuth (callback) {
	// log ("CHECK_AUTH()");
	
	callback = callback || function () {}
	
	/* 	
		Here we want to be monitoring the user_activities so we don't blindly 
		refresh access token if there hasn't been any activity ..., because doing so
		will reset the session's timeOut interval (and the session will never timeout!!)
	*/
	
	/**
	Do a tokenInfo request (don't use googleProxy, though, because googleProxy calls checkAuth!)
	
	@method execute
	*/
	function execute (exe_calback) {
		// log ("checkAuth execute");
		var request = "https://www.googleapis.com/oauth2/v1/tokeninfo";
		var args = $H();
		args.set('access_token', USER_GOOGLE_INFO.accessToken);		
		var apiUrl = request+'?'+args.toQueryString();
		
		new Ajax.Request(contextPath + '/oauth.do', {
			parameters: {
				apiUrl: apiUrl, 
				command: 'proxy'
			},
			onSuccess: function(transport) {
				if(transport.responseText.isJSON()) {
					try {
						var resp = transport.responseText.evalJSON();
						if (resp) {
							if (resp.error) {
								// auth failed - we assume its a bad token
								accessToken = null
								USER_GOOGLE_INFO.accessToken = null;
								try {
									log ("calling refreshAccessToken from CHECK_AUTH interal callback");
									refreshAccessToken (function () {
										execute(exe_calback)
									});
								} catch (error) {
									log ("ERROR: calling refreshAccessToken: " + error);
								}
								return; // it's all in refreshAccessToken's hands now ...
				
							}
							if (resp.expires_in) {
								setRefreshTimer (resp.expires_in - 10);
							}
							
							if (exe_calback) {
								// log ('calling exe_calback');
								exe_calback (resp);
							}
						}
					} catch (error) {
						log ("ERROR: could not process tokenInfo response: " + error);
					}
				}
				else {
					log ("WARN: tokenInfo response was not json: " + transport.responseText.trim());
				}
			},
			onFailure: function (transport) {
				log ("WARN: something went wrong with tokenInfo request");
			}
		});
	}
	
	// log ("- userGoogleId: " + userGoogleId);
	if (!USER_GOOGLE_INFO.id || USER_GOOGLE_INFO.id.empty()) {
		log ("userGoogleId is unknown")
		USER_GOOGLE_INFO.accessToken = null;
		return;
	}
	
	// log ("- accessToken: " + USER_GOOGLE_INFO.accessToken);
	if (USER_GOOGLE_INFO.accessToken == null || !Object.isString(USER_GOOGLE_INFO.accessToken) || USER_GOOGLE_INFO.accessToken.empty()) {
		try {
			refreshAccessToken (function () {
					execute(callback)
			});
		} catch (error) {
			log ("ERROR: could not refresh accessToken");
		}
	}
	else {
		try {
			// pass the function to be called by googleProxy after the it does 
			// some book keeping with response.
			execute(function (resp) {
				if (resp.error) {
					log ("token request got error: " + resp.error.responseCode)
					try {
						log ("trying again after refresh ...");
						refreshAccessToken (function () {
								log ("calling execute after refresh ...");
								execute(callback)
						});
					} catch (error) {
						log ("ERROR trying after refresh: " + error);
					}
				}
				callback(resp);
			});
		} catch (error) {
			log ('token request error: ' + error)
		}
	}
}

var refreshTimeout = null;

/**
Side effects
- accessToken is updated
- refreshTimer is set

@method refreshAccessToken
@param callback {function} called after successful refresh
*/
function refreshAccessToken (callback) {
	log ("REFRESH_AccessToken");
	new Ajax.Request(contextPath + '/oauth.do?command=token', {
		onSuccess: function (transport) {
			if(transport.responseText.isJSON()) {
				try {
					var responseJson = transport.responseText.evalJSON();
					log ('REFRESH responseJson: ' + JSON.stringify(responseJson, undefined, 2));
					setRefreshTimer (responseJson.expires_in - 10);
					USER_GOOGLE_INFO.accessToken = responseJson.access_token;
					if (callback)
						callback(responseJson);
					
				} catch (error) {
					log ("could not parse responseJson");
				}
			}
			else {
				// log ("response was not json ..." + transport.responseText);
				// CREATE AN ERROR RESP AND CALL CALLBACK WITHIT
				if (callback) {
					var errResp = {error: {
									msg: 'The response is not JSON: ' + transport.responseText,
									responseCode:9999
								}
					}
					callback (errResp);
				}
			}
		}
	});
}

/**
Send a "client" command request to server, get JSP
in response.

@method sendClientRequest
*/
function sendClientRequest(callback) {
	log ("sendClientRequest()");
	var params = {command:'client'}

	new Ajax.Request(contextPath + '/oauth.do', {
		parameters: params,
		onSuccess: function(transport) {
			// log ("Calendar Client responseText: " + transport.responseText.strip());
			try {
				if(transport.responseText.isJSON()) {
					// log ("isJSON");
					try {
						var resp = transport.responseText.evalJSON();
					} catch (error) {
						throw ("couldnt eval resp: " + error);
					}
						
					if (resp.error) {
						throw ("error: " + resp.error);
					}
					log ("activeCalendars saved");
				}
				else {
					// DO STUFF HERE
					if (callback)
						callback (transport.responseText);
				}
			} catch (error) {
				log ("sendClientRequest ERROR: processing response: " + error);
			}
		}
	});
}


/**

Refresh the access_token before it expires.

DISABLED - this is not needed since we now checkAuth, which will refresh token when necessary, as part of googleProxy

@method setRefreshTimer
*/
function setRefreshTimer(seconds) {
	if (true) {
		// log ("GoogleAuth Refresh Timer is Disabled");
		return;
	}
	else {
		if (refreshTimeout)
			clearTimeout(refreshTimeout);
		refreshTimeout = setTimeout ( function () {
			refreshAccessToken();
		}, (seconds * 1000));
	}
	
	var expiresInMinutes = Math.floor(seconds / 60);
	log ("setRefreshTimer for " + expiresInMinutes + " mins ...");
	if ($('expires_in')) {
		$('expires_in').update ("expires in " + expiresInMinutes + " min");
	}
}

// utils
function formatDate (date) {
	date = moment(date);
	return date.format("dddd, MMM Do YYYY, h:mma")
}

function validateDateTime (dateTime) {
	var mdate = moment(dateTime);
	if (!mdate.isValid())
		throw ("Invalid date format - try YYYY-MM-DDtHH:MM");
	return mdate;
}

