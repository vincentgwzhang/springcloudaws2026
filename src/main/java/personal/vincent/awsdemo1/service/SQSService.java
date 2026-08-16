package personal.vincent.awsdemo1.service;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeNameForSends;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Service to handle AWS SQS message operations.
 * Demonstrates sending messages to SQS queues.
 *
 * This service is only activated when spring.cloud.aws.sqs.enabled=true
 * and proper AWS credentials are configured.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "true")
public class SQSService {

	private final SqsAsyncClient sqsAsyncClient;

	public SQSService(SqsAsyncClient sqsAsyncClient) {
		this.sqsAsyncClient = sqsAsyncClient;
	}

	/**
	 * Send a message to the specified SQS queue.
	 *
	 * @param queueUrl the SQS queue URL
	 * @param message  the message payload to send
	 * @return the message ID from SQS
	 */
	public String sendMessage(String queueUrl) {
		try {
            String message = new SimpleDateFormat().format(new Date()) + ": Sending message";
			log.info("Sending message to queue: {}", queueUrl);
			log.debug("Message content: {}", message);
			
			sendWithTraceHeader(queueUrl, message);
			
			log.info("Message sent successfully to: {}", queueUrl);
			return "Message sent to queue: " + queueUrl;
		} catch (Exception e) {
			log.error("Failed to send message to queue: {}", queueUrl, e);
			throw new RuntimeException("Failed to send SQS message", e);
		}
	}

	/**
	 * Send a JSON message to the specified SQS queue.
	 *
	 * @param queueUrl the SQS queue URL
	 * @param jsonPayload the JSON message payload
	 * @return success message
	 */
	public String sendJsonMessage(String queueUrl, String jsonPayload) {
		try {
			log.info("Sending JSON message to queue: {}", queueUrl);
			
			sendWithTraceHeader(queueUrl, jsonPayload);
			
			log.info("JSON message sent successfully to: {}", queueUrl);
			return "JSON message sent to queue: " + queueUrl;
		} catch (Exception e) {
			log.error("Failed to send JSON message to queue: {}", queueUrl, e);
			throw new RuntimeException("Failed to send SQS JSON message", e);
		}
	}

	private void sendWithTraceHeader(String queueUrl, String payload) {
		Map<String, String> propagationFields = new HashMap<>();
		GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(
			Context.current(), propagationFields, Map::put);

		String xRayTraceHeader = propagationFields.entrySet().stream()
			.filter(entry -> "x-amzn-trace-id".equalsIgnoreCase(entry.getKey()))
			.map(Map.Entry::getValue)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException(
				"OpenTelemetry did not produce X-Amzn-Trace-Id; attach the upstream OTel Java agent "
					+ "and configure OTEL_PROPAGATORS=tracecontext,baggage,xray"));

		MessageSystemAttributeValue traceAttribute = MessageSystemAttributeValue.builder()
			.dataType("String")
			.stringValue(xRayTraceHeader)
			.build();

		sqsAsyncClient.sendMessage(SendMessageRequest.builder()
			.queueUrl(queueUrl)
			.messageBody(payload)
			.messageSystemAttributes(Map.of(
				MessageSystemAttributeNameForSends.AWS_TRACE_HEADER, traceAttribute))
			.build()).join();
	}
}
