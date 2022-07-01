package com.fincity.nocode.kirun.engine.runtime;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fincity.nocode.kirun.engine.function.math.Abs;
import com.fincity.nocode.kirun.engine.function.system.GenerateEvent;
import com.fincity.nocode.kirun.engine.function.system.If;
import com.fincity.nocode.kirun.engine.function.system.context.Create;
import com.fincity.nocode.kirun.engine.function.system.context.Set;
import com.fincity.nocode.kirun.engine.function.system.loop.RangeLoop;
import com.fincity.nocode.kirun.engine.json.JsonExpression;
import com.fincity.nocode.kirun.engine.json.schema.Schema;
import com.fincity.nocode.kirun.engine.model.Event;
import com.fincity.nocode.kirun.engine.model.EventResult;
import com.fincity.nocode.kirun.engine.model.FunctionDefinition;
import com.fincity.nocode.kirun.engine.model.Parameter;
import com.fincity.nocode.kirun.engine.model.ParameterReference;
import com.fincity.nocode.kirun.engine.model.Statement;
import com.fincity.nocode.kirun.engine.repository.KIRunFunctionRepository;
import com.fincity.nocode.kirun.engine.repository.KIRunSchemaRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class KIRuntimeTest {

	@Test
	void test() {

		// Testing the logic for Fibonacci series.

		Integer num = 7;
		JsonArray array = new JsonArray(7);
		int a = 0, b = 1;
		array.add(a);
		array.add(b);

		for (int i = 2; i < num; i++) {
			int t = a + b;
			a = b;
			b = t;
			array.add(t);
		}

		var create = new Create().getSignature();
		var arrayOfIntegerSchema = new JsonObject();
		arrayOfIntegerSchema.addProperty("name", "ArrayType");
		arrayOfIntegerSchema.addProperty("type", "ARRAY");
		arrayOfIntegerSchema.add("defaultValue", new JsonArray());
		var integerSchema = new JsonObject();
		integerSchema.addProperty("name", "EachElement");
		integerSchema.addProperty("type", "INTEGER");
		arrayOfIntegerSchema.add("items", integerSchema);
		var createArray = new Statement("createArray").setNamespace(create.getNamespace())
		        .setName(create.getName())
		        .setParameterMap(Map.of("name", List.of(ParameterReference.of(new JsonPrimitive("a"))), "schema",
		                List.of(ParameterReference.of(arrayOfIntegerSchema))));

		var rangeLoop = new RangeLoop().getSignature();
		var loop = new Statement("loop").setNamespace(rangeLoop.getNamespace())
		        .setName(rangeLoop.getName())
		        .setParameterMap(Map.of("from", List.of(ParameterReference.of(new JsonPrimitive(0))), "to",
		                List.of(ParameterReference.of("Arguments.Count"))))
		        .setDependentStatements(List.of("Steps.createArray.output"));

		var resultObj = new JsonObject();
		resultObj.add("name", new JsonPrimitive("result"));
		resultObj.add("value", new JsonExpression("Context.a"));

		var generate = new GenerateEvent().getSignature();
		var outputGenerate = new Statement("outputStep").setNamespace(generate.getNamespace())
		        .setName(generate.getName())
		        .setParameterMap(Map.of("eventName", List.of(ParameterReference.of(new JsonPrimitive("output"))),
		                "results", List.of(ParameterReference.of(resultObj))))
		        .setDependentStatements(List.of("Steps.loop.output"));

		var ifFunction = new If().getSignature();
		var ifStep = new Statement("if").setNamespace(ifFunction.getNamespace())
		        .setName(ifFunction.getName())
		        .setParameterMap(Map.of("condition", List.of(
		                ParameterReference.of("Steps.loop.iteration.index = 0 or Steps.loop.iteration.index = 1"))));

		var set = new Set().getSignature();
		var set1 = new Statement("setOnTrue").setNamespace(set.getNamespace())
		        .setName(set.getName())
		        .setParameterMap(Map.of("name",
		                List.of(ParameterReference.of(new JsonPrimitive("Context.a[Steps.loop.iteration.index]"))),
		                "value", List.of(ParameterReference.of("Steps.loop.iteration.index"))))
		        .setDependentStatements(List.of("Steps.if.true"));

		var set2 = new Statement("setOnFalse").setNamespace(set.getNamespace())
		        .setName(set.getName())
		        .setParameterMap(Map.of("name",
		                List.of(ParameterReference.of(new JsonPrimitive("Context.a[Steps.loop.iteration.index]"))),
		                "value",
		                List.of(ParameterReference.of(
		                        "Context.a[Steps.loop.iteration.index - 1] + Context.a[Steps.loop.iteration.index - 2]"))))
		        .setDependentStatements(List.of("Steps.if.false"));

		StepVerifier
		        .create(new KIRuntime(
		                ((FunctionDefinition) new FunctionDefinition()
		                        .setSteps(Map.ofEntries(Statement.ofEntry(createArray), Statement.ofEntry(loop),
		                                Statement.ofEntry(outputGenerate), Statement.ofEntry(ifStep),
		                                Statement.ofEntry(set1), Statement.ofEntry(set2)))
		                        .setNamespace("Test")
		                        .setName("Fibonacci")
		                        .setEvents(Map.ofEntries(Event.outputEventMapEntry(
		                                Map.of("result", Schema.ofArray("result", Schema.ofInteger("result"))))))
		                        .setParameters(Map.of("Count", new Parameter().setParameterName("Count")
		                                .setSchema(Schema.ofInteger("count"))))),
		                new KIRunFunctionRepository(), new KIRunSchemaRepository())
		                .execute(new FunctionExecutionParameters()
		                        .setArguments(Map.of("Count", new JsonPrimitive(num)))))
		        .expectNext(new EventResult().setName("output")
		                .setResult(Map.of("result", array)))
		        .verifyComplete();
	}

//	@Test
	void testSingleFunctionCall() {

		var abs = new Abs().getSignature();

		var genEvent = new GenerateEvent().getSignature();

		var resultObj = new JsonObject();
		resultObj.add("name", new JsonPrimitive("result"));
		resultObj.add("value", new JsonExpression("Steps.first.output.value"));

		Flux<EventResult> out = new KIRuntime(((FunctionDefinition) new FunctionDefinition().setNamespace("Test")
		        .setName("SingleCall")
		        .setParameters(Map.of("Value", new Parameter().setParameterName("Value")
		                .setSchema(Schema.ofInteger("Value")))))
		        .setSteps(Map.ofEntries(Statement.ofEntry(new Statement("first").setNamespace(abs.getNamespace())
		                .setName(abs.getName())
		                .setParameterMap(Map.of("value", List.of(ParameterReference.of("Arguments.Value"))))), Statement
		                        .ofEntry(new Statement("second").setNamespace(genEvent.getNamespace())
		                                .setName(genEvent.getName())
		                                .setParameterMap(Map.of("eventName",
		                                        List.of(ParameterReference.of(new JsonPrimitive("output"))), "results",
		                                        List.of(ParameterReference.of(resultObj))))))),
		        new KIRunFunctionRepository(), new KIRunSchemaRepository())
		        .execute(new FunctionExecutionParameters().setArguments(Map.of("Value", new JsonPrimitive(-10))));

		StepVerifier.create(out)
		        .expectNext(new EventResult().setName("output")
		                .setResult(Map.of("result", new JsonPrimitive(10))))
		        .verifyComplete();
	}
}
