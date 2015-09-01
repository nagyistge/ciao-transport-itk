package uk.nhs.ciao.transport.spine.route;

import static org.apache.camel.builder.PredicateBuilder.*;

import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.hazelcast.HazelcastComponent;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Route which wraps a hazelcast map / cache with type-conversion of values to/from JSON.
 */
public class HazelcastCacheRoute extends BaseRouteBuilder {
	private static final Logger LOGGER = LoggerFactory.getLogger(HazelcastCacheRoute.class);
	
	private String uri;
	private String targetUri;
	private Class<?> entryType;
	
	/**
	 * The uri this route consumes requests from
	 */
	public void setUri(final String uri) {
		this.uri = uri;
	}
	
	/**
	 * The uri of the backing hazelcast map
	 * 
	 * @see HazelcastComponent
	 */
	public void setTargetUri(final String targetUri) {
		this.targetUri = targetUri;
	}
	
	/**
	 * The Java class of map entries.
	 * <p>
	 * This is the object form of incoming/outgoing entries for this route.
	 * The entries will be stored in the Hazelcast map as String-encoded JSON
	 * marshalled/unmarshalled by Jackson using <code>entryType</code>.
	 */
	public void setEntryType(final Class<?> entryType) {
		this.entryType = entryType;
	}
		
	@Override
	public void configure() throws Exception {
		from(uri)
			.choice()
				.when(or(isEqualTo(header(HazelcastConstants.OPERATION), constant(HazelcastConstants.PUT_OPERATION)),
						isEqualTo(header(HazelcastConstants.OPERATION), constant(HazelcastConstants.UPDATE_OPERATION))))
					.marshal().json(JsonLibrary.Jackson)
					.log(LoggingLevel.DEBUG, LOGGER, "Request JSON body: ${body}")
				.endChoice()
			.end()
			.to(ExchangePattern.InOut, targetUri)
			.log(LoggingLevel.DEBUG, LOGGER, "Response JSON body: ${body}")
			.choice()
				.when().body()
					.unmarshal().json(JsonLibrary.Jackson, entryType)
				.endChoice()
			.end()
		.end();
	}
}