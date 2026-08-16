package personal.vincent.awsdemo1.config;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Configuration for AWS SQS integration.
 * This configuration is activated when AWS credentials are available
 * and spring.cloud.aws.sqs.enabled=true is set.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "true")
public class SQSConfiguration {

	/**
	 * Creates the async client using the AWS SDK default region and credentials
	 * provider chains. This supports ~/.aws/credentials locally and an EC2
	 * instance profile in production.
	 *
	 * @return AWS SQS async client
	 */
	@Bean
	public SqsAsyncClient sqsAsyncClient() {
		log.info("Initializing AWS SQS async client");
		return SqsAsyncClient.create();
	}

	/**
	 * Creates an SqsTemplate bean for sending and receiving SQS messages.
	 *
	 * @param sqsAsyncClient the AWS SQS async client
	 * @return SqsTemplate instance
	 */
	@Bean
	public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
		log.info("Initializing SqsTemplate for AWS SQS operations");
		return SqsTemplate.newTemplate(sqsAsyncClient);
	}
}
