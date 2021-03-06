package uk.nhs.ciao.transport.dts.route;

import static uk.nhs.ciao.logging.CiaoCamelLogMessage.camelLogMsg;
import static uk.nhs.ciao.transport.dts.processor.DTSDataFilePoller.*;
import static uk.nhs.ciao.transport.dts.route.DTSHeaders.*;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.util.toolbox.AggregationStrategies;

import uk.nhs.ciao.camel.BaseRouteBuilder;
import uk.nhs.ciao.dts.ControlFile;
import uk.nhs.ciao.logging.CiaoCamelLogger;
import uk.nhs.ciao.transport.dts.processor.DTSDataFilePoller;
import uk.nhs.ciao.transport.dts.processor.DTSFileHousekeeper;

/**
 * 
 * Route to receive control and data file pairs from the DTS IN folder and publish
 * the data payload.
 * <p>
 * File management of the IN directory is handled by this route.
 */
public class DTSMessageReceiverRoute extends BaseRouteBuilder {
	private static final CiaoCamelLogger LOGGER = CiaoCamelLogger.getLogger(DTSMessageReceiverRoute.class);
	
	private String dtsMessageReceiverUri;
	private String payloadDestinationUri;
	private DTSFileHousekeeper fileHousekeeper = new DTSFileHousekeeper();
	private DTSFileHousekeeper errorFileHousekeeper = new DTSFileHousekeeper();
	
	// optional properties
	private long dataFilePollingInterval = 200;
	private int dataFileMaxAttempts = 100; // == 20 seconds
	
	/**
	 * URI where incoming DTS messages are received from
	 * <p>
	 * input only
	 */
	public void setDTSMessageReceiverUri(final String dtsMessageReceiverUri) {
		this.dtsMessageReceiverUri = dtsMessageReceiverUri;
	}
	
	/**
	 * URI where outgoing payload messages are sent to
	 * <p>
	 * output only
	 */
	public void setPayloadDestinationUri(final String payloadDestinationUri) {
		this.payloadDestinationUri = payloadDestinationUri;
	}
	
	/**
	 * Housekeeper called after processing an incoming file
	 * <p>
	 * Only files intended for this application will have housekeeping applied.
	 */
	public void setFileHousekeeper(final DTSFileHousekeeper fileHousekeeper) {
		this.fileHousekeeper = fileHousekeeper;
	}
	
	/**
	 * Housekeeper called if an error occurs while processing an incoming file
	 * <p>
	 * Only files intended for this application will have housekeeping applied.
	 */
	public void setErrorFileHousekeeper(final DTSFileHousekeeper errorFileHousekeeper) {
		this.errorFileHousekeeper = errorFileHousekeeper;
	}

	/**
	 * Time to wait between poll attempts when waiting for a DTS data file
	 */
	public void setDataFilePollingInterval(final int dataFilePollingInterval) {
		this.dataFilePollingInterval = dataFilePollingInterval;
	}
	
	/**
	 * Maximum polling attempts to make while waiting for a DTS data file
	 */
	public void setDataFileMaxAttempts(final int dataFileMaxAttempts) {
		this.dataFileMaxAttempts = dataFileMaxAttempts;
	}
	
	@Override
	public void configure() throws Exception {
		final ScheduledExecutorService executorService = getContext().getExecutorServiceManager()
				.newSingleThreadScheduledExecutor(this, "data-file-poller");

		from(dtsMessageReceiverUri)
			.onCompletion()
				.onFailureOnly()
					.process(new ErrorHousekeeping())
				.end()
			.end()
			
			.process(LOGGER.info(camelLogMsg("Received incoming DTS control file")
					.fileName(header(Exchange.FILE_NAME))))
			.setHeader("controlFileName").header(Exchange.FILE_NAME)
			//.convertBodyTo(ControlFile.class)
			
			// Wait for the associated data file
			.setHeader(HEADER_DTS_FOLDER_NAME).header(Exchange.FILE_PARENT)
			.setHeader(HEADER_DATA_FILE_NAME, regexReplaceAll(
					simple("${header.CamelFileName}"), "(..*)\\.ctl", "$1.dat"))
			.process(createDataFilePoller(executorService, dataFilePollingInterval, dataFileMaxAttempts))
			
			// Publish the payload (using multicast to maintain original message)
			.multicast(AggregationStrategies.useOriginal())
				.shareUnitOfWork()
				.pipeline()
					.setProperty("dtsControlFile").body(ControlFile.class)
					.setBody().header(HEADER_DATA_FILE)
					.convertBodyTo(byte[].class)
					
					// Store the control file properties - it may be required for future DTS exchanges (especially the workflowId)
					.removeHeaders("*")
					.setHeader(HEADER_WORKFLOW_ID).simple("${property.dtsControlFile.getWorkflowId}")
					.setHeader(HEADER_FROM_DTS).simple("${property.dtsControlFile.getFromDTS}")
					.setHeader(HEADER_TO_DTS).simple("${property.dtsControlFile.getToDTS}")
					.removeProperty("dtsControlFile")
					
					.to(payloadDestinationUri)
				.end()
			.end()
			
			.process(LOGGER.info(camelLogMsg("Published incoming DTS data payload")
				.fileName(header(HEADER_DATA_FILE_NAME))
				.workflowId("${body.getWorkflowId}")
				.fromDTS("${body.getFromDTS}")
				.toDTS("${body.getToDTS}")))
			
			// If successful delete the pair of files
			.process(new SuccessHousekeeping())
		.end();
	}
	
	// protected scope (for unit tests)
	
	protected DTSDataFilePoller createDataFilePoller(final ScheduledExecutorService executorService,
			final long pollingInterval, final int maxAttempts) {
		return new DTSDataFilePoller(executorService, pollingInterval, maxAttempts);
	}

	/**
	 * If successful, housekeeping is applied to the control and data files
	 */
	private class SuccessHousekeeping implements Processor {
		@Override
		public void process(final Exchange exchange) throws Exception {
			if (fileHousekeeper == null) {
				return;
			}
			
			final File controlFile = exchange.getIn().getHeader(Exchange.FILE_PATH, File.class);
			fileHousekeeper.cleanup(controlFile);
			
			final File dataFile = exchange.getIn().getHeader(DTSDataFilePoller.HEADER_DATA_FILE_PATH, File.class);
			fileHousekeeper.cleanup(dataFile);
		}
	}
	
	/**
	 * If unsuccessful, error housekeeping is applied to the control and data files
	 */
	private class ErrorHousekeeping implements Processor {
		@Override
		public void process(final Exchange exchange) throws Exception {
			if (errorFileHousekeeper == null) {
				return;
			}
			
			final File controlFile = exchange.getIn().getHeader(Exchange.FILE_PATH, File.class);
			errorFileHousekeeper.cleanup(controlFile);
			
			final File dataFile = exchange.getIn().getHeader(DTSDataFilePoller.HEADER_DATA_FILE_PATH, File.class);
			errorFileHousekeeper.cleanup(dataFile);
		}
	}
}
