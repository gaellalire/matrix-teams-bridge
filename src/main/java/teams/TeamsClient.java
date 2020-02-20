package teams;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.aad.msal4j.DeviceCode;
import com.microsoft.aad.msal4j.DeviceCodeFlowParameters;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.UserNamePasswordParameters;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IUserRequestBuilder;

public class TeamsClient {

	public static final Logger LOGGER = LoggerFactory.getLogger(TeamsClient.class);

	public static final String CLIENT_ID = "631c912f-e484-4f1e-9bb1-70f21f6ba40e";

	// https://login.microsoftonline.com/common/oauth2/authorize?client_id=631c912f-e484-4f1e-9bb1-70f21f6ba40e&response_type=code

	// https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps/ApplicationMenuBlade/CallAnAPI/appId/631c912f-e484-4f1e-9bb1-70f21f6ba40e/objectId/a1358689-dcf0-4a01-b6d1-9947737adcf7/isMSAApp//defaultBlade/Overview/servicePrincipalCreated/true

	enum Authent {
		TOKEN,
		DEVICE,
		USERPASSWORD;
	}

	public static Authent currentAuthent = Authent.USERPASSWORD;

	public static void main(String[] args) throws Exception {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		Set<String> scopes = new HashSet<>();
		scopes.add("User.Read");
		scopes.add("Chat.Read");
		scopes.add("Chat.ReadWrite");

		String accessToken;
		switch (currentAuthent) {
		case TOKEN:
			accessToken = args[0];
			break;
		case DEVICE: {
			PublicClientApplication app = PublicClientApplication.builder(CLIENT_ID)
					.authority("https://login.microsoftonline.com/common/")
					.build();

			Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> {
				LOGGER.info("deviceCode {}", deviceCode.message());
			};

			CompletableFuture<IAuthenticationResult> future = app.acquireToken(
					DeviceCodeFlowParameters.builder(
							scopes,
							deviceCodeConsumer)
							.build());

			IAuthenticationResult authenticationResult = future.join();
			accessToken = authenticationResult.accessToken();
		}
			break;
		case USERPASSWORD: {
			PublicClientApplication app = PublicClientApplication
					.builder(CLIENT_ID)
					.authority("https://login.microsoftonline.com/organizations")
					.build();

			CompletableFuture<IAuthenticationResult> acquireToken = app.acquireToken(UserNamePasswordParameters.builder(scopes, args[0], args[1].toCharArray()).build());
			IAuthenticationResult authenticationResult = acquireToken.join();
			accessToken = authenticationResult.accessToken();
		}
			break;
		default:
			throw new Exception("Unknown auth " + currentAuthent);
		}

		IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(new IAuthenticationProvider() {

			@Override
			public void authenticateRequest(IHttpRequest request) {
				request.addHeader("Authorization", "Bearer " + accessToken);
			}

		}).buildClient();

		IUserRequestBuilder me = graphClient.me();
		// https://docs.microsoft.com/en-us/graph/api/chat-list?view=graph-rest-beta&tabs=http
		GenericRequest genericRequest = new GenericRequest("https://graph.microsoft.com/beta/me/chats", me.getClient());
		JsonObject rawObject = genericRequest.get();
		JsonArray jsonArray = rawObject.get("value").getAsJsonArray();
		jsonArray.forEach((JsonElement e ) -> {

			String id = e.getAsJsonObject().get("id").getAsString();
			LOGGER.info("ID {}", id);
			GenericRequest messagesRequest = new GenericRequest("https://graph.microsoft.com/beta/me/chats/"+id+"/messages", me.getClient());
			JsonObject jsonObject = messagesRequest.get();
			JsonArray messageArray = jsonObject.get("value").getAsJsonArray();
			messageArray.forEach((JsonElement message ) -> {
				JsonElement content = message.getAsJsonObject().get("body").getAsJsonObject().get("content");
				LOGGER.info("{}", content);
			});

			LOGGER.info("==============");
		});

		// https://docs.microsoft.com/en-us/graph/api/chatmessage-get?view=graph-rest-beta&tabs=http

	}

}
