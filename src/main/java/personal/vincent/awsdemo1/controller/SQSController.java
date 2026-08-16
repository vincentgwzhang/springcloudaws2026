package personal.vincent.awsdemo1.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import personal.vincent.awsdemo1.service.SQSService;

/**
 * REST Controller for AWS SQS operations.
 * Provides endpoints to send messages to SQS queues.
 *
 * This controller is only activated when spring.cloud.aws.sqs.enabled=true
 * and proper AWS credentials are configured.
 */
@Slf4j
@RestController
@RequestMapping("/api/sqs")
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "true")
public class SQSController {

	private final SQSService sqsService;
	private final String queueUrl;

	public SQSController(
		SQSService sqsService,
		@Value("${app.aws.sqs.queue-url}") String queueUrl) {
		this.sqsService = sqsService;
		if (queueUrl == null || queueUrl.isBlank()) {
			throw new IllegalArgumentException("app.aws.sqs.queue-url must be configured");
		}
		this.queueUrl = queueUrl;
	}

	/**
	 * Send a plain text message to an SQS queue.
	 *
	 * @param message  the message to send
	 * @return response entity with success message
	 */
	@PostMapping("/send")
	public ResponseEntity<String> sendMessage() {
		log.info("Received request to send message to queue: {}", queueUrl);
		try {
			String result = sqsService.sendMessage(queueUrl);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			log.error("Error sending message", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Error sending message: " + e.getMessage());
		}
	}

	/**
	 * Send a JSON message to an SQS queue.
	 *
	 * @param jsonPayload the JSON message payload
	 * @return response entity with success message
	 */
	@PostMapping("/send-json")
	public ResponseEntity<String> sendJsonMessage(
		@RequestParam String jsonPayload) {
		
		log.info("Received request to send JSON message to queue: {}", queueUrl);
		
		try {
			String result = sqsService.sendJsonMessage(queueUrl, jsonPayload);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			log.error("Error sending JSON message", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Error sending JSON message: " + e.getMessage());
		}
	}

	/**
	 * Health check endpoint for SQS module.
	 *
	 * @return simple ok response
	 */
	@PostMapping("/health")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok("SQS module is running");
	}
}
