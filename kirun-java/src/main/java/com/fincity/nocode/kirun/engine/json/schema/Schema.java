package com.fincity.nocode.kirun.engine.json.schema;

import static com.fincity.nocode.kirun.engine.json.schema.type.SchemaType.*;
import static java.util.Map.entry;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fincity.nocode.kirun.engine.json.schema.array.ArraySchemaType;
import com.fincity.nocode.kirun.engine.json.schema.object.AdditionalPropertiesType;
import com.fincity.nocode.kirun.engine.json.schema.string.StringFormat;
import com.fincity.nocode.kirun.engine.json.schema.string.StringSchema;
import com.fincity.nocode.kirun.engine.json.schema.type.SchemaType;
import com.fincity.nocode.kirun.engine.json.schema.type.Type;
import com.fincity.nocode.kirun.engine.model.FunctionSignature;
import com.fincity.nocode.kirun.engine.namespaces.Namespaces;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Schema implements Serializable {

	private static final String REQUIRED_STRING = "required";
	private static final String VERSION_STRING = "version";
	private static final String NAMESPACE_STRING = "namespace";

	private static final long serialVersionUID = 4041990622586726910L;

	public static final Schema SCHEMA = new Schema().setNamespace(Namespaces.SYSTEM).setVersion(1)
			.setType(Type.of(SchemaType.OBJECT))
			.setProperties(Map.ofEntries(
					entry(NAMESPACE_STRING, Schema.of(NAMESPACE_STRING, STRING)),
					entry(VERSION_STRING, Schema.of(VERSION_STRING, INTEGER)),
					entry("ref", Schema.of("ref", STRING)),
					entry("type", Schema.ofArray("type", new Schema().setType(Type.of(STRING))
									.setEnums(List.of(new JsonPrimitive("INTEGER"), new JsonPrimitive("LONG"),
											new JsonPrimitive("FLOAT"), new JsonPrimitive("DOUBLE"),
											new JsonPrimitive("STRING"), new JsonPrimitive("OBJECT"),
											new JsonPrimitive("ARRAY"), new JsonPrimitive("BOOLEAN"),
											new JsonPrimitive("NULL"))))),
					entry("anyOf", Schema.ofArray("anyOf", Schema.ofRef("#/"))),
					entry("allOf", Schema.ofArray("allOf", Schema.ofRef("#/"))),
					entry("oneOf", Schema.ofArray("oneOf", Schema.ofRef("#/"))),

					entry("not", Schema.ofRef("#/")),
					entry("title", Schema.of("title", STRING)),
					entry("description", Schema.of("description", STRING)),
					entry("id", Schema.of("id", STRING)),
					entry("examples",  Schema.ofArray("examples", Schema.of("example", INTEGER, LONG, FLOAT, DOUBLE, STRING, OBJECT, ARRAY, BOOLEAN, NULL))),
					entry("defaultValue", Schema.of("defaultValue", INTEGER, LONG, FLOAT, DOUBLE, STRING, OBJECT, ARRAY, BOOLEAN, NULL)),
					entry("comment", Schema.of("comment", STRING)),
					entry("enums", Schema.ofArray("enums", Schema.of("enum", STRING))),
					entry("constant", Schema.of("constant", INTEGER, LONG, FLOAT, DOUBLE, STRING, OBJECT, ARRAY, BOOLEAN, NULL)),
					
					entry("pattern", Schema.of("pattern", STRING)),
					entry("format", Schema.of("format", STRING).setEnums(List.of(new JsonPrimitive("DATETIME"),
							new JsonPrimitive("TIME"),
							new JsonPrimitive("DATE"),
							new JsonPrimitive("EMAIL"),
							new JsonPrimitive("REGEX")))),
					entry("minLength", Schema.of("minLength", INTEGER)),
					entry("maxLength", Schema.of("maxLength", INTEGER)),
					
					entry("multipleOf", Schema.of("multipleOf", LONG)),
					entry("minimum", Schema.of("minimum", INTEGER, LONG, DOUBLE, FLOAT)),
					entry("maximum", Schema.of("maximum", INTEGER, LONG, DOUBLE, FLOAT)),
					entry("exclusiveMinimum", Schema.of("exclusiveMinimum", INTEGER, LONG, DOUBLE, FLOAT)),
					entry("exclusiveMaximum", Schema.of("exclusiveMaximum", INTEGER, LONG, DOUBLE, FLOAT)),
					
					entry("properties", Schema.of("properties", OBJECT).setAdditionalProperties(new AdditionalPropertiesType().setSchemaValue(Schema.ofRef("#/")))),
					entry("additionalProperties", Schema.of("additionalProperties", BOOLEAN, OBJECT).setRef("#/")),
					entry(REQUIRED_STRING, Schema.ofArray(REQUIRED_STRING, Schema.of(REQUIRED_STRING, STRING)))
			))
			.setRequired(List.of(NAMESPACE_STRING, VERSION_STRING));

	public static Schema of(String id, SchemaType... type) {
		return new Schema().setType(Type.of(type)).setId(id).setTitle(id);
	}

	public static Schema ofRef(String ref) {
		return new Schema().setRef(ref);
	}
	
	public static Schema ofArray(String id, Schema ...itemSchemas) {
		return new Schema().setType(Type.of(SchemaType.ARRAY)).setId(id).setTitle(id)
				.setItems(ArraySchemaType.of(itemSchemas));
	}

	private String namespace;

	private int version;

	private String ref;

	private Type type;
	private List<Schema> anyOf;
	private List<Schema> allOf;
	private List<Schema> oneOf;
	private Schema not;

	private String title;
	private String description;
	private String id;
	private List<JsonElement> examples; // NOSONAR - JSON Element for some reason is not serialised.
	private JsonElement defaultValue; // NOSONAR - JSON Element for some reason is not serialised.
	private String comment;
	private List<JsonElement> enums; // NOSONAR - JSON Element for some reason is not serialised.
	private JsonElement constant; // NOSONAR - JSON Element for some reason is not serialised.

	// String
	private String pattern;
	private StringFormat format;
	private Integer minLength;
	private Integer maxLength;

	// Number
	private Long multipleOf;
	private Number minimum;
	private Number maximum;
	private Number exclusiveMinimum;
	private Number exclusiveMaximum;

	// Object
	private Map<String, Schema> properties;
	private AdditionalPropertiesType additionalProperties;
	private List<String> required;
	private StringSchema propertyNames;
	private Integer minProperties;
	private Integer maxProperties;
	private Map<String, List<String>> dependencies;
	private Map<String, Schema> patternProperties;

	// Array
	private ArraySchemaType items;
	private Schema contains;
	private Integer minItems;
	private Integer maxItems;
	private Boolean uniqueItems;
	private List<FunctionSignature> methods;

	private Map<String, Schema> definitions;
}
