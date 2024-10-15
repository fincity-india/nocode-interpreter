package com.fincity.nocode.kirun.engine.runtime.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fincity.nocode.kirun.engine.repository.reactive.KIRunReactiveFunctionRepository;
import com.fincity.nocode.kirun.engine.repository.reactive.KIRunReactiveSchemaRepository;
import com.fincity.nocode.kirun.engine.runtime.expression.tokenextractor.TokenValueExtractor;
import com.fincity.nocode.kirun.engine.runtime.reactive.ReactiveFunctionExecutionParameters;
import com.fincity.nocode.kirun.engine.runtime.tokenextractors.ArgumentsTokenValueExtractor;
import com.fincity.nocode.kirun.engine.runtime.tokenextractors.OutputMapTokenValueExtractor;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class ExpressionEvaluatorTest {

	@Test
	void test() {

		JsonObject phone = new JsonObject();
		phone.addProperty("phone1", "1234");
		phone.addProperty("phone2", "5678");
		phone.addProperty("phone3", "5678");

		JsonObject address = new JsonObject();
		address.addProperty("line1", "Flat 202, PVR Estates");
		address.addProperty("line2", "Nagvara");
		address.addProperty("city", "Benguluru");
		address.addProperty("pin", "560048");
		address.add("phone", phone);

		JsonArray arr = new JsonArray();
		arr.add(10);
		arr.add(20);
		arr.add(30);

		JsonObject obj = new JsonObject();
		obj.add("studentName", new JsonPrimitive("Kumar"));
		obj.add("math", new JsonPrimitive(20));
		obj.add("isStudent", new JsonPrimitive(true));
		obj.add("address", address);
		obj.add("array", arr);
		obj.add("num", new JsonPrimitive(1));

		Map<String, Map<String, Map<String, JsonElement>>> output = Map.of("step1",
				Map.of("output", Map.of("name", new JsonPrimitive("Kiran"), "obj", obj)));

		ReactiveFunctionExecutionParameters parameters = new ReactiveFunctionExecutionParameters(
				new KIRunReactiveFunctionRepository(),
				new KIRunReactiveSchemaRepository()).setArguments(Map.of())
				.setContext(Map.of())
				.setSteps(output);

		assertEquals(new JsonPrimitive(10), new ExpressionEvaluator("3 + 7").evaluate(parameters.getValuesMap()));
		assertEquals(new JsonPrimitive("asdf333"),
				new ExpressionEvaluator("\"asdf\"+333").evaluate(parameters.getValuesMap()));
		assertEquals(new JsonPrimitive(422),
				new ExpressionEvaluator("10*11+12*13*14/7").evaluate(parameters.getValuesMap()));
		assertEquals(new JsonPrimitive(true),
				new ExpressionEvaluator("34 >> 2 = 8 ").evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(true),
				new ExpressionEvaluator("34 >> 2 = 8 ").evaluate(parameters.getValuesMap()));

		assertEquals(null, new ExpressionEvaluator("Steps.step1.output.name1").evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(true),
				new ExpressionEvaluator("\"Kiran\" = Steps.step1.output.name ").evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(true),
				new ExpressionEvaluator("null = Steps.step1.output.name1 ").evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(true),
				new ExpressionEvaluator("Steps.step1.output.obj.phone.phone2 = Steps.step1.output.obj.phone.phone2 ")
						.evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(true),
				new ExpressionEvaluator(
						"Steps.step1.output.obj.address.phone.phone2 != Steps.step1.output.address.obj.phone.phone1 ")
						.evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(32),
				new ExpressionEvaluator("Steps.step1.output.obj.array[Steps.step1.output.obj.num +1]+2")
						.evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(60), new ExpressionEvaluator(
				"Steps.step1.output.obj.array[Steps.step1.output.obj.num +1]+Steps.step1.output.obj.array[Steps.step1.output.obj.num +1]")
				.evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(60), new ExpressionEvaluator(
				"Steps.step1.output.obj.array[Steps.step1.output.obj.num +1]+Steps.step1.output.obj.array[Steps.step1.output.obj.num +1]")
				.evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(32),
				new ExpressionEvaluator("Steps.step1.output.obj.array[-Steps.step1.output.obj.num + 3]+2")
						.evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(17.3533f),
				new ExpressionEvaluator("2.43*4.22+7.0987").evaluate(parameters.getValuesMap()));
	}

	@Test
	void testWithSquareAccessBracket() {

		JsonObject phone = new JsonObject();
		phone.addProperty("phone1", "1234");
		phone.addProperty("phone2", "5678");
		phone.addProperty("phone3", "5678");

		JsonObject address = new JsonObject();
		address.addProperty("line1", "Flat 202, PVR Estates");
		address.addProperty("line2", "Nagvara");
		address.addProperty("city", "Benguluru");
		address.addProperty("pin", "560048");
		address.add("phone", phone);

		JsonArray arr = new JsonArray();
		arr.add(10);
		arr.add(20);
		arr.add(30);

		JsonObject obj = new JsonObject();
		obj.add("studentName", new JsonPrimitive("Kumar"));
		obj.add("math", new JsonPrimitive(20));
		obj.add("isStudent", new JsonPrimitive(true));
		obj.add("address", address);
		obj.add("array", arr);
		obj.add("num", new JsonPrimitive(1));

		Map<String, Map<String, Map<String, JsonElement>>> output = Map.of("step1",
				Map.of("output", Map.of("name", new JsonPrimitive("Kiran"), "obj", obj)));

		ReactiveFunctionExecutionParameters parameters = new ReactiveFunctionExecutionParameters(
				new KIRunReactiveFunctionRepository(),
				new KIRunReactiveSchemaRepository()).setArguments(Map.of())
				.setContext(Map.of())
				.setSteps(output);

		assertEquals(new JsonPrimitive(true),
				new ExpressionEvaluator("Steps.step1.output.obj.phone.phone2 = Steps.step1.output.obj[\"phone\"][\"phone2\"] ")
						.evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(true),
				new ExpressionEvaluator("Steps.step1.output.obj[\"phone\"][\"phone2\"] = Steps.step1.output.obj[\"phone\"][\"phone2\"] ")
						.evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(true),
				new ExpressionEvaluator(
						"Steps.step1.output.obj[\"address\"].phone[\"phone2\"] != Steps.step1.output.address.obj.phone.phone1 ")
						.evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(true),
				new ExpressionEvaluator(
						"Steps.step1.output.obj[\"address\"][\"phone\"][\"phone2\"] != Steps.step1.output.address.obj.phone.phone1 ")
						.evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(true),
				new ExpressionEvaluator(
						"Steps.step1.output.obj[\"address\"][\"phone\"][\"phone2\"] != Steps.step1.output[\"address\"][\"phone\"][\"phone2\"] ")
						.evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(32),
				new ExpressionEvaluator("Steps.step1.output.obj.array[Steps.step1.output.obj[\"num\"] +1]+2")
						.evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(60), new ExpressionEvaluator(
				"Steps.step1.output.obj.array[Steps.step1.output.obj[\"num\"] +1]+Steps.step1.output.obj.array[Steps.step1.output.obj[\"num\"] +1]")
				.evaluate(parameters.getValuesMap()));

		assertEquals(new JsonPrimitive(60), new ExpressionEvaluator(
				"Steps.step1.output.obj.array[Steps.step1.output.obj.num +1]+Steps.step1.output.obj.array[Steps.step1.output.obj.num +1]")
				.evaluate(parameters.getValuesMap()));
	}

	@Test
	void deepTest() {

		var cobj = new JsonObject();
		var dobj = new JsonObject();

		cobj.addProperty("a", 2);
		dobj.addProperty("a", 2);

		var cbArray = new JsonArray();
		cbArray.add(true);
		cbArray.add(false);
		cobj.add("b", cbArray);

		var dbArray = new JsonArray();
		dbArray.add(true);
		dbArray.add(false);
		dobj.add("b", dbArray);

		var ccObj = new JsonObject();
		ccObj.addProperty("x", "kiran");

		var dcObj = new JsonObject();
		dcObj.addProperty("x", "kiran");

		cobj.add("c", ccObj);
		dobj.add("c", dcObj);

		ArgumentsTokenValueExtractor atv = new ArgumentsTokenValueExtractor(
				Map.of("a", new JsonPrimitive("kirun "), "b", new JsonPrimitive(2), "c", cobj, "d", dobj));
		Map<String, TokenValueExtractor> valuesMap = Map.of(atv.getPrefix(), atv);

		var ev = new ExpressionEvaluator("Arguments.a = Arugments.b");
		assertFalse(ev.evaluate(valuesMap)
				.getAsBoolean());

		ev = new ExpressionEvaluator("Arguments.c = Arguments.d");
		assertTrue(ev.evaluate(valuesMap)
				.getAsBoolean());

		ev = new ExpressionEvaluator("Arguments.e = null");
		assertTrue(ev.evaluate(valuesMap)
				.getAsBoolean());

		ev = new ExpressionEvaluator("Arguments.e != null");
		assertFalse(ev.evaluate(valuesMap)
				.getAsBoolean());

		ev = new ExpressionEvaluator("false = false");
		assertTrue(ev.evaluate(valuesMap)
				.getAsBoolean());

		ev = new ExpressionEvaluator("Arguments.e = false");
		assertTrue(ev.evaluate(valuesMap)
				.getAsBoolean());
	}

	@Test
	void testNullCoalescing() {

		var cobj = new JsonObject();
		cobj.addProperty("a", 2);

		var cbArray = new JsonArray();
		cbArray.add(true);
		cbArray.add(false);
		cobj.add("b", cbArray);

		var ccObj = new JsonObject();
		ccObj.addProperty("x", "kiran");

		var dcObj = new JsonObject();
		dcObj.addProperty("x", "kiran");

		cobj.add("c", ccObj);

		ArgumentsTokenValueExtractor atv = new ArgumentsTokenValueExtractor(Map.of("a", new JsonPrimitive("kirun "),
				"b", new JsonPrimitive(2), "b2", new JsonPrimitive(4), "c", cobj));
		Map<String, TokenValueExtractor> valuesMap = Map.of(atv.getPrefix(), atv);

		var ev = new ExpressionEvaluator("(Arguments.e ?? Arguments.b ?? Arguments.b1) + 4");
		assertEquals(6, ev.evaluate(valuesMap)
				.getAsInt());

		ev = new ExpressionEvaluator("(Arguments.e ?? Arguments.b2 ?? Arguments.b1) + 4");
		assertEquals(8, ev.evaluate(valuesMap)
				.getAsInt());
	}

	@Test
	void testNestedExpression() {

		var cobj = new JsonObject();
		cobj.addProperty("a", 2);

		var cbArray = new JsonArray();
		cbArray.add(true);
		cbArray.add(false);
		cobj.add("b", cbArray);

		var ccObj = new JsonObject();
		ccObj.addProperty("x", "Arguments.b2");

		cobj.add("c", ccObj);

		ArgumentsTokenValueExtractor atv = new ArgumentsTokenValueExtractor(Map.of("a", new JsonPrimitive("kirun "),
				"b", new JsonPrimitive(2), "b2", new JsonPrimitive(4), "c", cobj, "d", new JsonPrimitive("c")));
		Map<String, TokenValueExtractor> valuesMap = Map.of(atv.getPrefix(), atv);

		var ev = new ExpressionEvaluator("Arguments.{{Arguments.d}}.a + {{Arguments.{{Arguments.d}}.c.x}}");

		assertEquals(6, ev.evaluate(valuesMap)
				.getAsInt());

		ev = new ExpressionEvaluator(
				"'There are {{{{Arguments.{{Arguments.d}}.c.x}}}} boys in the class room...' * Arguments.b");

		assertEquals("There are 4 boys in the class room...There are 4 boys in the class room...",
				ev.evaluate(valuesMap)
						.getAsString());
	}

	@Test
	void testPartialPathEvaluation() {

		var cobj = new JsonObject();
		cobj.addProperty("a", 2);

		var cbArray = new JsonArray();
		cbArray.add(true);
		cbArray.add(false);
		cobj.add("b", cbArray);

		var ccObj = new JsonObject();
		ccObj.addProperty("x", "Arguments.b2");

		cobj.add("c", ccObj);

		var cKeysArray = new JsonArray();
		cKeysArray.add(new JsonPrimitive("a"));
		cKeysArray.add(new JsonPrimitive("e"));

		var keys2Obj = new JsonObject();
		keys2Obj.add("val", new JsonPrimitive(5));
		cKeysArray.add(keys2Obj);
		cobj.add("keys", cKeysArray);

		var eArray = new JsonArray();
		var e1 = new JsonObject();
		e1.add("name", new JsonPrimitive("Kiran"));
		e1.add("num", new JsonPrimitive(1));

		var e2 = new JsonObject();
		e2.add("name", new JsonPrimitive("Good"));
		e2.add("num", new JsonPrimitive(2));
		eArray.add(e1);
		eArray.add(e2);

		ArgumentsTokenValueExtractor atv = new ArgumentsTokenValueExtractor(
				Map.of("a", new JsonPrimitive("kirun "), "b", new JsonPrimitive(2), "b2", new JsonPrimitive(4), "c",
						cobj, "d", new JsonPrimitive("c"), "e", eArray));

		Map<String, TokenValueExtractor> valuesMap = Map.of(atv.getPrefix(), atv);

		var ev = new ExpressionEvaluator("(Arguments.f ?? Arguments.e)[1+1-1].num");
		assertEquals(new JsonPrimitive(2), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.c.keys[2].val + 3");
		assertEquals(new JsonPrimitive(8), ev.evaluate(valuesMap));

	}

	@Test
	void testEmptyStringwithNULLs() {

		ArgumentsTokenValueExtractor atv = new ArgumentsTokenValueExtractor(
				Map.of("a", new JsonPrimitive(""), "b", new JsonPrimitive("Kiran")));

		Map<String, TokenValueExtractor> valuesMap = Map.of(atv.getPrefix(), atv);

		var ev = new ExpressionEvaluator("Arguments.a = ''");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.a != ''");
		assertEquals(new JsonPrimitive(false), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.b != ''");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.b = ''");
		assertEquals(new JsonPrimitive(false), ev.evaluate(valuesMap));
	}

	@Test
	void testExpressionWithLogicalOperatorsAndAllTypesOfValues() {

		JsonObject job = new JsonObject();
		job.addProperty("a", 1);
		job.addProperty("b", "2");
		job.addProperty("c", true);
		job.add("d", JsonNull.INSTANCE);
		job.add("e", JsonNull.INSTANCE);

		JsonArray ja = new JsonArray();
		ja.add(new JsonPrimitive(1));
		ja.add(new JsonPrimitive(2));
		ja.add(new JsonPrimitive(true));
		ja.add(JsonNull.INSTANCE);
		ja.add(JsonNull.INSTANCE);

		JsonArray ja2 = new JsonArray();
		ja2.add(new JsonPrimitive(1));
		ja2.add(new JsonPrimitive(2));
		ja2.add(new JsonPrimitive(true));
		ja2.add(JsonNull.INSTANCE);
		ja2.add(JsonNull.INSTANCE);

		ArgumentsTokenValueExtractor atv = new ArgumentsTokenValueExtractor(
				Map.ofEntries(
						Map.entry("string", new JsonPrimitive("kirun ")),
						Map.entry("stringEmpty", new JsonPrimitive("")),
						Map.entry("number", new JsonPrimitive(122.2)),
						Map.entry("number1", new JsonPrimitive(1)),
						Map.entry("number0", new JsonPrimitive(0)),
						Map.entry("booleanTrue", new JsonPrimitive(true)),
						Map.entry("booleanFalse", new JsonPrimitive(false)),
						Map.entry("null", JsonNull.INSTANCE),
						Map.entry("undefined", JsonNull.INSTANCE),
						Map.entry("object", job),
						Map.entry("array", ja),
						Map.entry("array2", ja2),
						Map.entry("emptyArray", new JsonArray())));

		Map<String, TokenValueExtractor> valuesMap = Map.of(atv.getPrefix(), atv);

		var ev = new ExpressionEvaluator("not not Arguments.object");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("not not Arguments.stringEmpty");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("not not Arguments.number");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("not not Arguments.number0");
		assertEquals(new JsonPrimitive(false), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("not not Arguments.booleanTrue");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("not not Arguments.booleanFalse");
		assertEquals(new JsonPrimitive(false), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("not not Arguments.null");
		assertEquals(new JsonPrimitive(false), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("not not Arguments.undefined");
		assertEquals(new JsonPrimitive(false), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("not not Arguments.array");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("not not Arguments.emptyArray");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.object = true");
		assertEquals(new JsonPrimitive(false), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.object != true");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.stringEmpty = true");
		assertEquals(new JsonPrimitive(false), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.stringEmpty != false");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.number0 = true");
		assertEquals(new JsonPrimitive(false), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.number0 = false");
		assertEquals(new JsonPrimitive(false), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.array.length");
		assertEquals(new JsonPrimitive(5), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.object.length");
		assertEquals(new JsonPrimitive(5), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.object and Arguments.array");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.object or Arguments.null");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.object and Arguments.null");
		assertEquals(new JsonPrimitive(false), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.object ? 3 : 4");
		assertEquals(new JsonPrimitive(3), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("not Arguments.object ? 3 : 4");
		assertEquals(new JsonPrimitive(4), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.array = Arguments.array2");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.number0 ? 3 : 4");
		assertEquals(new JsonPrimitive(4), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("Arguments.string = Arguments.stringEmpty");
		assertEquals(new JsonPrimitive(false), ev.evaluate(valuesMap));

		ev = new ExpressionEvaluator("(Arguments.number1 = 0) or (Arguments.number1 = 1)");
		assertEquals(new JsonPrimitive(true), ev.evaluate(valuesMap));
	}

	@Test
	void additonTest() {

		var ev = new ExpressionEvaluator("1 + 2");
		assertEquals(new JsonPrimitive(3), ev.evaluate(Map.of()));
	}

	@Test
	void fullStoreTest() {

		ArgumentsTokenValueExtractor atv = new ArgumentsTokenValueExtractor(
				Map.of("a", new JsonPrimitive("kirun "), "b", new JsonPrimitive(2), "c", new JsonPrimitive(4)));

		JsonObject job = new JsonObject();
		job.addProperty("a", "kirun ");
		job.addProperty("b", 2);
		job.addProperty("c", 4);

		assertEquals(job, atv.getStore());

		OutputMapTokenValueExtractor omtv = new OutputMapTokenValueExtractor(Map.of("step1",
				Map.of("output", Map.of("name", new JsonPrimitive("Kiran"), "obj", new JsonPrimitive("obj")))));

		JsonObject job2 = new JsonObject();
		job2.addProperty("name", "Kiran");
		job2.addProperty("obj", "obj");

		JsonObject job3 = new JsonObject();
		job3.add("output", job2);

		JsonObject job4 = new JsonObject();
		job4.add("step1", job3);

		assertEquals(job4, omtv.getStore());
	}

	@Test
	void fullStore2Test() {

		JsonObject job = new JsonObject();
		job.addProperty("a", "kirun ");
		job.addProperty("b", 2);
		job.addProperty("c", 4);

		TestTokenValueExtractor tte = new TestTokenValueExtractor(job);

		ExpressionEvaluator ev;

		ev = new ExpressionEvaluator("Test.a");
		assertEquals(new JsonPrimitive("kirun "), ev.evaluate(Map.of(tte.getPrefix(), tte)));

		tte = new TestTokenValueExtractor(new JsonPrimitive(20));
		ev = new ExpressionEvaluator("Test");
		assertEquals(new JsonPrimitive(20), ev.evaluate(Map.of(tte.getPrefix(), tte)));

		ev = new ExpressionEvaluator("Test > 10");
		assertEquals(new JsonPrimitive(true), ev.evaluate(Map.of(tte.getPrefix(), tte)));
	}

	@Test
	void fullStoreNULLTest() {

		TestTokenValueExtractor tte = new TestTokenValueExtractor(JsonNull.INSTANCE);

		ExpressionEvaluator ev;

		ev = new ExpressionEvaluator("Test");
		assertEquals(JsonNull.INSTANCE, ev.evaluate(Map.of(tte.getPrefix(), tte)));
	}

	@Test
	void testingNumberEquality() {

		var json = """
				{
					"user": {
					"id": 265,
					"updatedBy": 0,
					"createdAt": 1714649,
					"updatedAt": 1714716,
					"clientId": 174,
					"emailId": "kiran.grandhi+buildingscustomer1@gmail.com",
					"firstName": "Kiran Kumar",
					"lastName": "Grandhi",
					"localeCode": "en-US",
					"passwordHashed": false,
					"accountNonExpired": true,
					"accountNonLocked": true,
					"credentialsNonExpired": true,
					"noFailedAttempt": 0,
					"statusCode": "ACTIVE",
					"stringAuthorities": [
						"Authorities.Logged_IN"
					]
					},
					"isAuthenticated": true,
					"loggedInFromClientId": 174,
					"loggedInFromClientCode": "BUILD",
					"clientTypeCode": "INDV",
					"clientCode": "KIRAN24",
					"urlClientCode": "BUILD",
					"urlAppCode": "kyc"
				}
				""";

		var jsonElement = new Gson().fromJson(json, JsonElement.class);

		JsonObject job = new JsonObject();
		job.add("auth", jsonElement);
		job.add("bigNumber", new JsonPrimitive(new BigInteger("11")));
		job.add("bigNumber2", new JsonPrimitive(11));

		TestTokenValueExtractor tte = new TestTokenValueExtractor(job);

		ExpressionEvaluator ev;

		ev = new ExpressionEvaluator("Test.auth.loggedInFromClientId = Test.auth.user.clientId");
		assertEquals(new JsonPrimitive(true), ev.evaluate(Map.of(tte.getPrefix(), tte)));

		ev = new ExpressionEvaluator("Test.bigNumber = Test.bigNumber2");
		assertEquals(new JsonPrimitive(true), ev.evaluate(Map.of(tte.getPrefix(), tte)));
	}
}

class TestTokenValueExtractor extends TokenValueExtractor {
	private JsonElement store;

	public TestTokenValueExtractor(JsonElement store) {
		this.store = store;
	}

	@Override
	protected JsonElement getValueInternal(String token) {
		return this.retrieveElementFrom(token, token.split(TokenValueExtractor.REGEX_DOT), 1, store);
	}

	@Override
	public String getPrefix() {
		return "Test.";
	}

	@Override
	public JsonElement getStore() {
		return this.store;
	}
}
