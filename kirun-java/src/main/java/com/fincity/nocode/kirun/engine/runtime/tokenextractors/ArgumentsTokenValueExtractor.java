package com.fincity.nocode.kirun.engine.runtime.tokenextractors;

import java.util.Map;

import com.fincity.nocode.kirun.engine.runtime.expression.tokenextractor.TokenValueExtractor;
import com.google.gson.JsonElement;

public class ArgumentsTokenValueExtractor extends TokenValueExtractor {

	public static final String PREFIX = "Arguments.";

	private final Map<String, JsonElement> arguments;

	public ArgumentsTokenValueExtractor(Map<String, JsonElement> arguments) {

		this.arguments = arguments;
	}

	@Override
	protected JsonElement getValueInternal(String token) {

		String[] parts = token.split("\\.");

		String key = parts[1];
		int bIndex = key.indexOf('[');
		int fromIndex = 2;
		if (bIndex != -1) {
			key = parts[1].substring(0, bIndex);
			parts[1] = parts[1].substring(bIndex);
			fromIndex = 1;
		}

		return retrieveElementFrom(token, parts, fromIndex, arguments.get(key));
	}

	@Override
	public String getPrefix() {
		return PREFIX;
	}
}
