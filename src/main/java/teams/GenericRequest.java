package teams;

import java.util.Collections;

import com.google.gson.JsonObject;
import com.microsoft.graph.core.IBaseClient;
import com.microsoft.graph.http.BaseRequest;
import com.microsoft.graph.http.HttpMethod;
import com.microsoft.graph.models.extensions.Entity;

public class GenericRequest extends BaseRequest {

	public GenericRequest(String requestUrl, IBaseClient client) {
		super(requestUrl, client, Collections.EMPTY_LIST, Entity.class);
	}

	public JsonObject get() {
		Entity send = send(HttpMethod.GET, null);
		return send.getRawObject();
	}

}
