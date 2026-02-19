package io.quarkus.gamemanager.observability;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.logging.Log;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter.MeterProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.observability.api.event.AiServiceStartedEvent;

@ApplicationScoped
public class InteractionObserver implements ChatModelListener {
  private MeterProvider<Counter> totalInputTokenCounter;
	private MeterProvider<Counter> totalOutputTokenCounter;
	private MeterProvider<Counter> totalTokenCounter;
  private MeterProvider<Counter> interactionStartedCounter;

  public InteractionObserver(OTelBuildConfig otelConfig, MeterRegistry meterRegistry) {
		if (isOtelMetricsEnabled(otelConfig)) {
			this.totalInputTokenCounter = createMeterProvider(meterRegistry, "token.input.count", "Total input token count", "tokens");
			this.totalOutputTokenCounter = createMeterProvider(meterRegistry, "token.output.count", "Total output token count", "tokens");
			this.totalTokenCounter = createMeterProvider(meterRegistry, "token.total.count", "Total token count", "tokens");
      this.interactionStartedCounter = createMeterProvider(meterRegistry, "interaction.started", "Total interactions started", "count");
		}
	}

  @Override
	public void onResponse(ChatModelResponseContext responseContext) {
		Log.debugf("Response received: %s", responseContext.chatResponse());

		var modelName = responseContext.chatRequest().modelName();
		incrementTotals(responseContext.chatResponse().tokenUsage(), modelName);
	}

  public void interactionStarted(@Observes AiServiceStartedEvent event) {
    Log.debugf("Interaction started: %s", event);

    var tags = Tags.of(
        Tag.of("interfaceName", event.invocationContext().interfaceName()),
        Tag.of("methodName", event.invocationContext().methodName())
    );

    incrementMeter(this.interactionStartedCounter, tags, 1);
  }

	private static MeterProvider<Counter> createMeterProvider(MeterRegistry meterRegistry, String name, String description, String unit) {
		return Counter.builder(name)
			.description(description)
			.baseUnit(unit)
			.withRegistry(meterRegistry);
	}

	private boolean isOtelEnabled(OTelBuildConfig otelConfig) {
		return otelConfig.enabled();
	}

	private boolean isOtelMetricsEnabled(OTelBuildConfig otelConfig) {
		return isOtelEnabled(otelConfig) && otelConfig.metrics().enabled().orElse(false);
	}

  private void incrementTotals(TokenUsage tokenUsage, String modelName) {
		var modelNameTags = Tags.of("modelName", modelName);
		incrementMeter(this.totalInputTokenCounter, tokenUsage.inputTokenCount());
		incrementMeter(this.totalInputTokenCounter, modelNameTags, tokenUsage.inputTokenCount());
		incrementMeter(this.totalOutputTokenCounter, tokenUsage.outputTokenCount());
		incrementMeter(this.totalOutputTokenCounter, modelNameTags, tokenUsage.outputTokenCount());
		incrementMeter(this.totalTokenCounter, tokenUsage.totalTokenCount());
		incrementMeter(this.totalTokenCounter, modelNameTags, tokenUsage.totalTokenCount());
	}

  private void incrementMeter(MeterProvider<Counter> meter, int incrementBy) {
		incrementMeter(meter, Tags.empty(), incrementBy);
	}

	private void incrementMeter(MeterProvider<Counter> meter, Tags tags, int incrementBy) {
		if (meter != null) {
			meter.withTags(tags).increment(incrementBy);
		}
	}
}
