package org.mule.modules.jsonloggermodule.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.json.JSONException;
import org.json.JSONObject;
import org.mule.api.MuleContext;
import org.mule.api.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Route;
import okio.Buffer;

public class AnypointMQHelper {
	
	private static final Logger log = LoggerFactory.getLogger("org.mule.modules.jsonloggermodule.utils.AnypointMQHelper");
	
	/**
     * OkHttp client
     */
	private final OkHttpClient client;
	
	/**
     * Anypoint MQ parameters
     */
	private String clientAppId;
	private String clientSecret;
	private String endpoint;
	private String destination;
	private String accessToken;
	private String orgId;
	private String envId;
	
    /**
     * OS helper to interact with Mule Object Store Manager
     */
    private ObjectStoreHelper os;
	
	public AnypointMQHelper(String clientAppId, String clientSecret, String endpoint, String destination, Registry registry, MuleContext muleContext) {
		this.clientAppId = clientAppId;
		this.clientSecret = clientSecret;
		this.endpoint = endpoint;
		this.destination = destination;
		
		// INIT Object Store
		log.info("Initializing object store...");
		os = new ObjectStoreHelper(registry, muleContext);
		Validate.notNull(os.getObjectStore(), "Unable to acquire an object store.");
		log.info("Object store successfully acquired.");
		
		// SETUP Authentication for REST Call: This is called automatically when we receive a 401 authorization code.
		client = new OkHttpClient.Builder().authenticator(new Authenticator() {
			@Override
			public Request authenticate(Route route, Response response) throws IOException {
				log.info("Got 401 response. Triggering Authenticator...");
				log.debug("Authenticator - RESPONSE: " + response);
				log.debug("Authenticator - AUTHORIZATION: " + response.request().header("Authorization"));
				log.debug("Authenticator - CHALLENGES: " + response.challenges());

				int count = responseCount(response);

				if (count >= 3) {
					log.info("Authenticator: Max authentication attempts (3) exceeded");
					return null; // If we've failed 3 times, give up.
				} else {
					log.info("Authenticator: Authentication attempt " + count);
					log.info("Authenticator: Getting new access token...");
					refreshToken();
				}

				return response.request().newBuilder().header("Authorization", "bearer " + accessToken).build();
			}
		}).build();
	    
		// GET ACCESS TOKEN
		log.info("Getting access token");
		refreshToken();
	}
	
	private int responseCount(Response response) {
		int result = 1;
		while ((response = response.priorResponse()) != null) {
			result++;
		}
		return result;
	}
	
	public void refreshToken() {
		// STORE ACCESS TOKEN
		log.info("Refreshing accessToken...");
		try {
			HashMap<String, String> accessTokenMap = getAccessToken();
			os.store("accessTokenMap", accessTokenMap);
			log.debug("Stored accessTokenMap: " + os.retrieve("accessTokenMap").toString());
			this.orgId = accessTokenMap.get("orgId");
			this.envId = accessTokenMap.get("envId");
			this.accessToken = accessTokenMap.get("accessToken");
		} catch (Exception e) {
			log.error("Error obtaining access token: " + e);
		}
	}
	
	public HashMap<String,String> getAccessToken() throws JSONException, IOException {
		MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
		RequestBody body = RequestBody.create(mediaType, 
				"client_id=" + this.clientAppId + 
				"&client_secret=" + this.clientSecret +
				"&grant_type=client_credentials");
		Request request = new Request.Builder()
		  .url(this.endpoint + "/authorize")
		  .post(body)
		  .build();

		Response response = client.newCall(request).execute();
		JSONObject responseJson = new JSONObject(response.body().string());
		HashMap<String,String> responseMap = new HashMap<String,String>();
		responseMap.put("accessToken", responseJson.getString("access_token"));
		responseMap.put("orgId", responseJson.getJSONObject("simple_client").getString("orgId"));
		responseMap.put("envId", responseJson.getJSONObject("simple_client").getString("envId"));
		
		log.debug("access_token: " + responseMap.get("accessToken"));
		log.debug("orgId: " + responseMap.get("orgId"));
		log.debug("envId: " + responseMap.get("envId"));
		
		return responseMap;
	}
	
	public void send(String content) {
		MediaType mediaType = MediaType.parse("application/json");
		RequestBody body = RequestBody.create(mediaType, "[{\"headers\":{\"messageId\":\"" + UUID.randomUUID().toString() + "\"},\"properties\": {\"contentType\": \"application/json\"},\"body\":" + org.json.JSONObject.quote(content) + "}]");
		log.debug("OkHttp ThreadId sender: " + Thread.currentThread().getName());
		Request request = new Request.Builder()
			.url(this.endpoint + "/organizations/" + this.orgId + "/environments/" + this.envId + "/destinations/" + this.destination + "/messages")
			.put(body)
			.addHeader("authorization", "bearer " + this.accessToken)
	        .build();

		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				e.printStackTrace();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				try (ResponseBody responseBody = response.body()) {
					log.debug("OkHttp ThreadId receiver: " + Thread.currentThread().getId());
					log.debug("OkHttp ThreadName receiver: " + Thread.currentThread().getName());
					if (!response.isSuccessful()) {
						final Buffer buffer = new Buffer();
						call.request().body().writeTo(buffer);
						log.error("Request failed - RESPONSE: " + response);
						log.error("Request failed - CHALLENGES: " + response.challenges());
						log.error("Request failed - ERROR CODE: " + response.code());
						log.error("Request failed - REQUEST BODY: " + buffer.readUtf8());

						Headers responseHeaders = response.headers();
						for (int i = 0, size = responseHeaders.size(); i < size; i++) {
							log.error("Request failed - HEADERS: " + responseHeaders.name(i) + ": " + responseHeaders.value(i));
						}
						throw new IOException("Unexpected code " + response);
					}
				}
			}
		});
	}
}
