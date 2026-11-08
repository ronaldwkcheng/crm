package com.ronald.crm.graphql.config;

import graphql.GraphQLContext;
import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import graphql.execution.CoercedVariables;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Custom scalars for types the GraphQL spec has no built-in mapping for: {@code Long} (JPA
 * {@code @Version}/id fields), {@code DateTime} (audit timestamps), and {@code JSON} (the
 * schema-less instance property map and filter/report values driven by metadata at runtime).
 */
@Configuration
public class GraphQlScalarConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return builder -> builder
                .scalar(longScalar())
                .scalar(dateTimeScalar())
                .scalar(jsonScalar());
    }

    private GraphQLScalarType longScalar() {
        return GraphQLScalarType.newScalar()
                .name("Long")
                .description("A 64-bit signed integer, used for JPA ids and optimistic-lock version numbers")
                .coercing(new Coercing<Long, Long>() {
                    @Override
                    public Long serialize(Object input, GraphQLContext graphQLContext, Locale locale) {
                        return toLong(input, CoercingSerializeException::new);
                    }

                    @Override
                    public Long parseValue(Object input, GraphQLContext graphQLContext, Locale locale) {
                        return toLong(input, CoercingParseValueException::new);
                    }

                    @Override
                    public Long parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext graphQLContext, Locale locale) {
                        if (input instanceof IntValue intValue) {
                            return intValue.getValue().longValueExact();
                        }
                        if (input instanceof StringValue stringValue) {
                            return toLong(stringValue.getValue(), CoercingParseLiteralException::new);
                        }
                        throw new CoercingParseLiteralException("Expected an Int or String literal for Long but got: " + input);
                    }

                    private Long toLong(Object input, java.util.function.Function<String, RuntimeException> onError) {
                        try {
                            if (input instanceof Long l) {
                                return l;
                            }
                            if (input instanceof Number n) {
                                return n.longValue();
                            }
                            return Long.parseLong(input.toString());
                        } catch (NumberFormatException e) {
                            throw onError.apply("Not a valid Long: " + input);
                        }
                    }
                })
                .build();
    }

    private GraphQLScalarType dateTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("An ISO-8601 instant, e.g. 2026-11-07T10:15:30Z")
                .coercing(new Coercing<Instant, String>() {
                    @Override
                    public String serialize(Object input, GraphQLContext graphQLContext, Locale locale) {
                        if (input instanceof Instant instant) {
                            return instant.toString();
                        }
                        throw new CoercingSerializeException("Expected an Instant but got: " + input);
                    }

                    @Override
                    public Instant parseValue(Object input, GraphQLContext graphQLContext, Locale locale) {
                        try {
                            return Instant.parse(input.toString());
                        } catch (DateTimeParseException e) {
                            throw new CoercingParseValueException("Not a valid ISO-8601 instant: " + input);
                        }
                    }

                    @Override
                    public Instant parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext graphQLContext, Locale locale) {
                        if (input instanceof StringValue stringValue) {
                            try {
                                return Instant.parse(stringValue.getValue());
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseLiteralException("Not a valid ISO-8601 instant: " + stringValue.getValue());
                            }
                        }
                        throw new CoercingParseLiteralException("Expected a String literal for DateTime but got: " + input);
                    }
                })
                .build();
    }

    private GraphQLScalarType jsonScalar() {
        return GraphQLScalarType.newScalar()
                .name("JSON")
                .description("An arbitrary JSON value: used for instance property maps, filter/report values, "
                        + "and anything else shaped by runtime metadata rather than a fixed GraphQL type")
                .coercing(new Coercing<Object, Object>() {
                    @Override
                    public Object serialize(Object input, GraphQLContext graphQLContext, Locale locale) {
                        return input;
                    }

                    @Override
                    public Object parseValue(Object input, GraphQLContext graphQLContext, Locale locale) {
                        return input;
                    }

                    @Override
                    public Object parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext graphQLContext, Locale locale) {
                        return parseLiteralValue(input, variables);
                    }
                })
                .build();
    }

    private Object parseLiteralValue(Value<?> value, CoercedVariables variables) {
        if (value instanceof StringValue v) {
            return v.getValue();
        }
        if (value instanceof IntValue v) {
            return v.getValue();
        }
        if (value instanceof FloatValue v) {
            return v.getValue();
        }
        if (value instanceof BooleanValue v) {
            return v.isValue();
        }
        if (value instanceof NullValue) {
            return null;
        }
        if (value instanceof graphql.language.VariableReference ref) {
            return variables.get(ref.getName());
        }
        if (value instanceof ArrayValue v) {
            return v.getValues().stream().map(item -> parseLiteralValue(item, variables)).toList();
        }
        if (value instanceof ObjectValue v) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (ObjectField field : v.getObjectFields()) {
                result.put(field.getName(), parseLiteralValue(field.getValue(), variables));
            }
            return result;
        }
        throw new CoercingParseLiteralException("Unsupported JSON literal: " + value);
    }
}
