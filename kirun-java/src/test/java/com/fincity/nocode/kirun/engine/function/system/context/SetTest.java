package com.fincity.nocode.kirun.engine.function.system.context;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fincity.nocode.kirun.engine.json.schema.Schema;
import com.fincity.nocode.kirun.engine.repository.KIRunFunctionRepository;
import com.fincity.nocode.kirun.engine.repository.KIRunSchemaRepository;
import com.fincity.nocode.kirun.engine.runtime.ContextElement;
import com.fincity.nocode.kirun.engine.runtime.FunctionExecutionParameters;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class SetTest {

	@Test
	void test() {

		Set setFunction = new Set();

		FunctionExecutionParameters fep = new FunctionExecutionParameters(new KIRunFunctionRepository(), new KIRunSchemaRepository());

		Map<String, ContextElement> contextMap = new HashMap<>();
		contextMap.put("a", new ContextElement(Schema.ofAny("test"), new JsonObject()));
		fep.setContext(contextMap);
		fep.setArguments(Map.of("name", new JsonPrimitive("Context.a.b"), "value", new JsonPrimitive(20)));

		setFunction.execute(fep);

		assertEquals(new JsonPrimitive(20), contextMap.get("a")
		        .getElement()
		        .getAsJsonObject()
		        .get("b"));

		fep.setArguments(Map.of("name", new JsonPrimitive("Context.a.c[2].d"), "value", new JsonPrimitive(25)));

		setFunction.execute(fep);

		assertEquals(new JsonPrimitive(25), contextMap.get("a")
		        .getElement()
		        .getAsJsonObject()
		        .get("c")
		        .getAsJsonArray()
		        .get(2)
		        .getAsJsonObject()
		        .get("d"));

	}
	
	@Test
	void testFirstLevelArray() {

		Set setFunction = new Set();

		FunctionExecutionParameters fep = new FunctionExecutionParameters(new KIRunFunctionRepository(), new KIRunSchemaRepository());

		Map<String, ContextElement> contextMap = new HashMap<>();
		contextMap.put("a", new ContextElement(Schema.ofAny("test"), new JsonArray()));
		fep.setContext(contextMap);
		fep.setArguments(Map.of("name", new JsonPrimitive("Context.a[1]"), "value", new JsonPrimitive(240)));

		setFunction.execute(fep);

		assertEquals(new JsonPrimitive(240), contextMap.get("a")
		        .getElement()
		        .getAsJsonArray()
		        .get(1));

	}

}
