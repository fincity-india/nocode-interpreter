package com.fincity.nocode.kirun.engine.function.system.math;

import static com.fincity.nocode.kirun.engine.namespaces.Namespaces.MATH;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fincity.nocode.kirun.engine.exception.KIRuntimeException;
import com.fincity.nocode.kirun.engine.function.reactive.AbstractReactiveFunction;
import com.fincity.nocode.kirun.engine.json.schema.Schema;
import com.fincity.nocode.kirun.engine.model.Event;
import com.fincity.nocode.kirun.engine.model.EventResult;
import com.fincity.nocode.kirun.engine.model.FunctionOutput;
import com.fincity.nocode.kirun.engine.model.FunctionSignature;
import com.fincity.nocode.kirun.engine.model.Parameter;
import com.fincity.nocode.kirun.engine.runtime.reactive.ReactiveFunctionExecutionParameters;
import com.fincity.nocode.kirun.engine.util.string.StringFormatter;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import reactor.core.publisher.Mono;

public class Hypotenuse extends AbstractReactiveFunction {

	private static final String VALUE = "value";

	private static final FunctionSignature SIGNATURE = new FunctionSignature().setName("Hypotenuse")
			.setNamespace(MATH)
			.setParameters(Map.of(VALUE, new Parameter().setParameterName(VALUE)
					.setSchema(Schema.ofNumber(VALUE))
					.setVariableArgument(true)))
			.setEvents(Map.ofEntries(Event.outputEventMapEntry(Map.of(VALUE, Schema.ofDouble(VALUE)))));

	@Override
	public FunctionSignature getSignature() {
		return SIGNATURE;
	}

	@Override
	protected Mono<FunctionOutput> internalExecute(ReactiveFunctionExecutionParameters context) {

		JsonElement jsonElement = context.getArguments()
				.get(VALUE);

		if (!jsonElement.isJsonArray()) {
			throw new KIRuntimeException(StringFormatter.format("Expected an array but found $", jsonElement));
		}

		if (jsonElement.getAsJsonArray()
				.isEmpty()) {

			return Mono.just(new FunctionOutput(List.of(EventResult.outputOf(Map.of(VALUE, new JsonPrimitive(0))))));
		}

		Iterator<JsonElement> iterator = jsonElement.getAsJsonArray()
				.iterator();

		double sumOfSqaures = 0;

		while (iterator.hasNext()) {
			double value = iterator.next()
					.getAsDouble();
			sumOfSqaures += value * value;
		}

		JsonPrimitive element = new JsonPrimitive(Math.sqrt(sumOfSqaures));

		return Mono.just(new FunctionOutput(List.of(EventResult.outputOf(Map.of(VALUE, element)))));
	}
}
