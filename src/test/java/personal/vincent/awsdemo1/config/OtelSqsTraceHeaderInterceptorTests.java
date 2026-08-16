package personal.vincent.awsdemo1.config;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeNameForSends;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OtelSqsTraceHeaderInterceptorTests {

	private static final String TRACE_HEADER =
		"Root=1-66c8a910-123456789012345678901234;Parent=1234567890123456;Sampled=1";

	private final OtelSqsTraceHeaderInterceptor interceptor =
		new OtelSqsTraceHeaderInterceptor(OtelSqsTraceHeaderInterceptorTests::fixedPropagator);

	@Test
	void addsTraceHeaderToSendMessage() {
		SendMessageRequest request = SendMessageRequest.builder()
			.queueUrl("queue-url")
			.messageBody("hello")
			.build();

		SendMessageRequest modified = (SendMessageRequest) modify(request);

		assertThat(modified.messageSystemAttributes()
			.get(MessageSystemAttributeNameForSends.AWS_TRACE_HEADER).stringValue())
			.isEqualTo(TRACE_HEADER);
	}

	@Test
	void addsTraceHeaderToEveryBatchEntry() {
		SendMessageBatchRequest request = SendMessageBatchRequest.builder()
			.queueUrl("queue-url")
			.entries(
				SendMessageBatchRequestEntry.builder().id("1").messageBody("one").build(),
				SendMessageBatchRequestEntry.builder().id("2").messageBody("two").build())
			.build();

		SendMessageBatchRequest modified = (SendMessageBatchRequest) modify(request);

		assertThat(modified.entries())
			.allSatisfy(entry -> assertThat(entry.messageSystemAttributes()
				.get(MessageSystemAttributeNameForSends.AWS_TRACE_HEADER).stringValue())
				.isEqualTo(TRACE_HEADER));
	}

	@Test
	void leavesNonSendRequestsUnchanged() {
		GetQueueUrlRequest request = GetQueueUrlRequest.builder().queueName("queue").build();

		assertThat(modify(request)).isSameAs(request);
	}

	private SdkRequest modify(SdkRequest request) {
		return interceptor.modifyRequest(() -> request, new ExecutionAttributes());
	}

	private static TextMapPropagator fixedPropagator() {
		return new TextMapPropagator() {
			@Override
			public Collection<String> fields() {
				return List.of("x-amzn-trace-id");
			}

			@Override
			public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
				setter.set(carrier, "x-amzn-trace-id", TRACE_HEADER);
			}

			@Override
			public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
				return context;
			}
		};
	}
}
