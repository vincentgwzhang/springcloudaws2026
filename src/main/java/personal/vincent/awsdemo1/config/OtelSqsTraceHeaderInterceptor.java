package personal.vincent.awsdemo1.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeNameForSends;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Adds the current OpenTelemetry context as the AWS X-Ray SQS system attribute
 * for every message sent through the configured SQS client.
 */
public class OtelSqsTraceHeaderInterceptor implements ExecutionInterceptor {

	private static final String X_AMZN_TRACE_ID = "x-amzn-trace-id";

	private final Supplier<TextMapPropagator> propagatorSupplier;

	public OtelSqsTraceHeaderInterceptor() {
		this(() -> GlobalOpenTelemetry.getPropagators().getTextMapPropagator());
	}

	OtelSqsTraceHeaderInterceptor(Supplier<TextMapPropagator> propagatorSupplier) {
		this.propagatorSupplier = propagatorSupplier;
	}

	@Override
	public SdkRequest modifyRequest(
		software.amazon.awssdk.core.interceptor.Context.ModifyRequest context,
		ExecutionAttributes executionAttributes) {

		SdkRequest request = context.request();
		if (request instanceof SendMessageRequest sendRequest) {
			return addTraceHeader(sendRequest);
		}
		if (request instanceof SendMessageBatchRequest batchRequest) {
			return addTraceHeader(batchRequest);
		}
		return request;
	}

	private SendMessageRequest addTraceHeader(SendMessageRequest request) {
		Map<MessageSystemAttributeNameForSends, MessageSystemAttributeValue> attributes =
			new HashMap<>(request.messageSystemAttributes());
		attributes.put(MessageSystemAttributeNameForSends.AWS_TRACE_HEADER, traceAttribute());
		return request.toBuilder().messageSystemAttributes(attributes).build();
	}

	private SendMessageBatchRequest addTraceHeader(SendMessageBatchRequest request) {
		MessageSystemAttributeValue traceAttribute = traceAttribute();
		List<SendMessageBatchRequestEntry> entries = new ArrayList<>(request.entries().size());

		for (SendMessageBatchRequestEntry entry : request.entries()) {
			Map<MessageSystemAttributeNameForSends, MessageSystemAttributeValue> attributes =
				new HashMap<>(entry.messageSystemAttributes());
			attributes.put(MessageSystemAttributeNameForSends.AWS_TRACE_HEADER, traceAttribute);
			entries.add(entry.toBuilder().messageSystemAttributes(attributes).build());
		}

		return request.toBuilder().entries(entries).build();
	}

	private MessageSystemAttributeValue traceAttribute() {
		Map<String, String> propagationFields = new HashMap<>();
		propagatorSupplier.get().inject(Context.current(), propagationFields, Map::put);

		String traceHeader = propagationFields.entrySet().stream()
			.filter(entry -> X_AMZN_TRACE_ID.equalsIgnoreCase(entry.getKey()))
			.map(Map.Entry::getValue)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException(
				"OpenTelemetry did not produce X-Amzn-Trace-Id; attach the upstream OTel Java agent "
					+ "and configure OTEL_PROPAGATORS=tracecontext,baggage,xray"));

		return MessageSystemAttributeValue.builder()
			.dataType("String")
			.stringValue(traceHeader)
			.build();
	}
}
