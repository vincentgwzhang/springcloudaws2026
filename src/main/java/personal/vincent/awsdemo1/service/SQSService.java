package personal.vincent.awsdemo1.service;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

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

	private final SqsTemplate sqsTemplate;

	public SQSService(SqsTemplate sqsTemplate) {
		this.sqsTemplate = sqsTemplate;
	}

	/**
	 * Send a message to the specified SQS queue.
	 *
	 * @param queueUrl the SQS queue URL
	 * @return the message ID from SQS
	 */
	public String sendMessage(String queueUrl) {
		try {
            String message = new SimpleDateFormat().format(new Date()) + ": Sending message";
			log.info("Sending message to queue: {}", queueUrl);
			log.debug("Message content: {}", message);
			
			sqsTemplate.send(to -> to.queue(queueUrl).payload(message));
			
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
			
			sqsTemplate.send(to -> to.queue(queueUrl).payload(jsonPayload));
			
			log.info("JSON message sent successfully to: {}", queueUrl);
			return "JSON message sent to queue: " + queueUrl;
		} catch (Exception e) {
			log.error("Failed to send JSON message to queue: {}", queueUrl, e);
			throw new RuntimeException("Failed to send SQS JSON message", e);
		}
	}
}
