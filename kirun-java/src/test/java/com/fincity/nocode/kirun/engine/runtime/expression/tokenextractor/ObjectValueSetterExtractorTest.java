package com.fincity.nocode.kirun.engine.runtime.expression.tokenextractor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class ObjectValueSetterExtractorTest {

    @Test
    void ObjectValueSetterExtractorTest1() {

        Gson gson = new Gson();

        var store = gson.fromJson(
                """
                        {"name":"Kiran","addresses":[{"city":"Bangalore","state":"Karnataka","country":"India"},{"city":"Kakinada","state":"Andhra Pradesh","country":"India"},{"city":"Beaverton","state":"Oregon"}],"phone":{"home":"080-23456789","office":"080-23456789","mobile":"080-23456789","mobile2":"503-23456789"},"plain":[1,2,3,4]}
                        """,
                JsonObject.class);

        ObjectValueSetterExtractor objExtractor = new ObjectValueSetterExtractor(store, "Store");

        assertEquals(objExtractor.getValue("Store.name"), new JsonPrimitive("Kiran"));

        objExtractor.setValue("Store.name", new JsonPrimitive("Surendhar"), null, null);

        assertEquals(objExtractor.getValue("Store.name"), new JsonPrimitive("Surendhar"));

        JsonObject newStore = objExtractor.getStore().getAsJsonObject();

        assertEquals(newStore.get("name"), new JsonPrimitive("Surendhar"));

        objExtractor.setValue("Store.addresses[0].city", new JsonPrimitive("Bengaluru"), null, null);

        assertEquals(objExtractor.getValue("Store.addresses[0].city"), new JsonPrimitive("Bengaluru"));

        objExtractor.setValue("Store.plain[0]", new JsonPrimitive("123"), null, null);

        JsonArray arr = new JsonArray();
        arr.add(new JsonPrimitive("123"));
        arr.add(new JsonPrimitive(2));
        arr.add(new JsonPrimitive(3));
        arr.add(new JsonPrimitive(4));

        assertEquals(objExtractor.getValue("Store.plain"), arr);

        objExtractor.setValue("Store.plain[0]", new JsonPrimitive(1), false, null);

        assertEquals(objExtractor.getValue("Store.plain[0]"), arr.get(0));

        assertEquals(objExtractor.getValue("Store.plain"), arr);

        objExtractor.setValue("Store.plain", null, true, true);

        var storeObject = objExtractor.getValue("Store");

        Set<String> keys = objExtractor.getValue("Store").isJsonObject() ? storeObject.getAsJsonObject().keySet()
                : null;

        Set<String> expectedKeys = Set.of("name", "addresses", "phone");

        assertEquals(keys, expectedKeys);

        objExtractor.setValue("Store.plain", new JsonPrimitive("plainString"), false, false);

        assertEquals(objExtractor.getValue("Store.plain"), new JsonPrimitive("plainString"));

    }

    @Test
    void setNewKeyInObjectTest() {

        Gson gson = new Gson();

        var store = gson.fromJson("""
                {}
                """, JsonObject.class);
        

        ObjectValueSetterExtractor objExtractor = new ObjectValueSetterExtractor(store, "Obj");


        objExtractor.setValue("Obj.plain", new JsonPrimitive("plainString"), false, false);

        assertEquals(objExtractor.getValue("Obj.plain"), new JsonPrimitive("plainString"));


    }

}
