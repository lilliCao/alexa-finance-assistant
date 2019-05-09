package api.banking;

import api.aws.DynamoDbClient;
import api.aws.DynamoDbMapper;
import configuration.ConfigurationAMOS;
import model.db.User;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


public class AuthenticationAPI {

	public static class AuthResponse {
		private String access_token;
		private String expires_in;

		public AuthResponse() {}

		public String getAccess_token() {
			return access_token;
		}

		public void setAccess_token(String access_token) {
			this.access_token = access_token;
		}

		public String getExpires_in() {
			return expires_in;
		}

		public void setExpires_in(String expires_in) {
			this.expires_in = expires_in;
		}
	}

	private static DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * Logger
	 */
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(AccountAPI.class);

	private static final String AUTH_URL = ConfigurationAMOS.authTokenApi;

	private static ConcurrentHashMap<String, User> accessTokenUsers = new ConcurrentHashMap<>();

	/**
	 * Returns the access token for the given user id.
	 *
	 * @param userId the user id
	 * @return the access token
	 */
	public static String getAccessToken(String userId) {
		if(!accessTokenUsers.containsKey(userId)) {
			User user = (User)DynamoDbMapper.getInstance().load(User.class, userId);
			accessTokenUsers.put(user.getId(), user);
		}
		return accessTokenUsers.get(userId).getAccessToken();

	}

	/**
	 * Updates the access token if necessary.
	 *
	 * @param userId the user id
	 */
	public static void updateAccessToken(String userId) {
		if(!accessTokenUsers.containsKey(userId)) {
			User user = (User)DynamoDbMapper.getInstance().load(User.class, userId);//(model.db.User) DynamoDbClient.instance.getItem(model.db.User.TABLE_NAME, userId, model.db.User.factory);
			accessTokenUsers.put(user.getId(), user);
		}

		model.db.User user = accessTokenUsers.get(userId);

		if(user == null) {
			user = new model.db.User();
			user.setBalanceLimit(0);
			user.setId(userId);

			// Update the user object in the db
			DynamoDbMapper.getInstance().save(user);
			//DynamoDbClient.instance.putItem(model.db.User.TABLE_NAME, user);
		}

		if(shouldRefresh(user)) {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
			map.add("username", "anton");
			map.add("password", "anton");
			map.add("grant_type", "password");
			map.add("client_id", "public_auth");

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

			AuthResponse authResponse = new RestTemplate().exchange(AUTH_URL, HttpMethod.POST, request, AuthResponse.class).getBody();

			log.info("AuthToken: " + authResponse.getAccess_token());

			user.setAccessToken(authResponse.getAccess_token());

			// Add expiry time (seconds) to current time, store this value as a String in the User object
			int expiresInSeconds = Integer.valueOf(authResponse.getExpires_in());
			DateTime expiryDateTime = DateTime.now().withZone(DateTimeZone.UTC).plusSeconds(expiresInSeconds);
			String dtStr = fmt.print(expiryDateTime);
			user.setAccessTokenExpiryTime(dtStr);
			log.info("new expiry time: " + dtStr);

			// Store this token in our HashMap
			accessTokenUsers.put(user.getId(), user);

			// Update the user object in the db
			DynamoDbMapper.getInstance().save(user);
			//DynamoDbClient.instance.putItem(model.db.User.TABLE_NAME, user);
		}
	}

	/**
	 * Checks if this user's access token must be refreshed. A token must be refreshed if it is null or has expired.
	 *
	 * @param user the user
     * @return true if the token must be refreshed, false otherwise
	 */
	private static boolean shouldRefresh(User user) {
		if(user.getAccessToken() == null) {
			return true;
		}

		try {
			DateTime validUntil = fmt.parseDateTime(user.getAccessTokenExpiryTime()).withZone(DateTimeZone.UTC);

			DateTime now = DateTime.now().withZone(DateTimeZone.UTC);

			log.info("now: " + now + " - validUntil: " + validUntil);

			if(validUntil.minusMinutes(10).isBefore(now)) {
				return true;
			}
		} catch(Exception e) {
			return true;
		}

		return false;
	}
}
