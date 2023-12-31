package splunk.implementation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.logging.LogLevel;
import com.mendix.logging.LogMessage;
import com.mendix.logging.LogSubscriber;
import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;
import com.splunk.logging.HttpEventCollectorErrorHandler;
import com.splunk.logging.HttpEventCollectorErrorHandler.ErrorCallback;
import com.splunk.logging.HttpEventCollectorEventInfo;
import com.splunk.logging.HttpEventCollectorSender;

import splunk.proxies.constants.Constants;

public class SplunkLogSubscriber extends LogSubscriber {
	
	private final ILogNode LOGGER = Core.getLogger("Splunk");
	private HttpEventCollectorSender hecSender;
	private LogLevel minimalLogLevel;
	
	public SplunkLogSubscriber() {
		super(SplunkLogSubscriber.class.getSimpleName(), LogLevel.valueOf(Constants.getMinimalLogLevel()));
		minimalLogLevel = LogLevel.valueOf(Constants.getMinimalLogLevel());
		
		
		Map<String, String> metadata = new HashMap<>();
		metadata.put("instance_index", System.getenv("CF_INSTANCE_INDEX"));
		metadata.put("environment_id", System.getenv("ENVIRONMENT"));
		final String vcapApplication = System.getenv("VCAP_APPLICATION");
		if (vcapApplication != null) {
			final JSONObject vcapApplicationObject = new JSONObject(vcapApplication);

	        final JSONArray appliationUrisArray = vcapApplicationObject.getJSONArray("application_uris");
	        final String hostname = appliationUrisArray.getString(0);
			metadata.put("application_name", hostname);
		}
				
		metadata.put("runtime_version", Core.getConfiguration().RUNTIME_VERSION.toString());
		metadata.put("model_version", Core.getModelVersion());

		String tags = System.getenv("TAGS");
		if (tags != null) {
			try {
				JSONArray arr = new JSONArray(tags);
				for (int i = 0; i < arr.length(); i++) {
					String tag = arr.getString(i);
					String[] kv = tag.split("[:]");
					if (kv.length != 2) {
						LOGGER.warn("Unable to interpret environment TAG: " + tag);
						continue;
					}
					metadata.put(kv[0], kv[1]);
				}
			} catch (Exception e) {
				LOGGER.error("Unable to parse environment TAGS: " + e.getMessage(), e);
			}
		}
		
		String channel = null, type = null;
		HttpEventCollectorSender.TimeoutSettings timeoutSettings = new HttpEventCollectorSender.TimeoutSettings();

		hecSender = new HttpEventCollectorSender(
				Constants.getEndpoint(),
				Constants.getToken(),
				channel, // channel
				type, // type
				0L, // delay
				0L, // maxEventsBatchCount
				0L, // maxEventsBatchSize
				"sequential",
				metadata,
				timeoutSettings
				);
		HttpEventCollectorErrorHandler.onError(new ErrorCallback() {
			@Override
			public void error(List<HttpEventCollectorEventInfo> msges, Exception ex) {
				LOGGER.error("Error while sending message to Splunk.", ex);
			}
		});
		
		LOGGER.info("Forwarding logs to Splunk.");
	}

	@Override
	public void processMessage(LogMessage msg) {
		if (msg.node == LOGGER) return; // avoid infinite loop, don't act on messages which have errors generated by this own module
		if (msg.level.ordinal() >= minimalLogLevel.ordinal()) {
			String exceptionMessage = null;
			if (msg.cause != null) {
				exceptionMessage = ExceptionUtils.getStackTrace(msg.cause);
			}
			hecSender.send(
					msg.timestamp,
					msg.level.name().toLowerCase(), // severity
					msg.message.toString(), // message
					msg.node.name(), // logger name
					null, // thread name
					null, // properties
					exceptionMessage,
					null // marker
					);
		}
	}

	
	
}
