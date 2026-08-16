package personal.vincent.awsdemo1.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import personal.vincent.awsdemo1.service.SQSService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SQSControllerTests {

	private static final String QUEUE_URL = "https://sqs.eu-west-1.amazonaws.com/123456789012/demo";

	@Test
	void sendsMessageToConfiguredQueue() {
		RecordingSqsService sqsService = new RecordingSqsService();
		SQSController controller = new SQSController(sqsService, QUEUE_URL);

		ResponseEntity<String> response = controller.sendMessage();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("sent");
		assertThat(sqsService.queueUrl).isEqualTo(QUEUE_URL);
	}

	@Test
	void sendsJsonMessageToConfiguredQueue() {
		RecordingSqsService sqsService = new RecordingSqsService();
		SQSController controller = new SQSController(sqsService, QUEUE_URL);

		ResponseEntity<String> response = controller.sendJsonMessage("{\"hello\":\"world\"}");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(sqsService.queueUrl).isEqualTo(QUEUE_URL);
		assertThat(sqsService.payload).isEqualTo("{\"hello\":\"world\"}");
	}

	@Test
	void rejectsMissingQueueConfiguration() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new SQSController(null, " "))
			.withMessage("app.aws.sqs.queue-url must be configured");
	}

	private static class RecordingSqsService extends SQSService {

		private String queueUrl;
		private String payload;

		RecordingSqsService() {
			super(null);
		}

		@Override
		public String sendMessage(String queueUrl) {
			this.queueUrl = queueUrl;
			return "sent";
		}

		@Override
		public String sendJsonMessage(String queueUrl, String jsonPayload) {
			this.queueUrl = queueUrl;
			this.payload = jsonPayload;
			return "sent";
		}
	}
}
