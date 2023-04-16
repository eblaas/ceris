package io.ceris.apicall.dto;

import io.confluent.kafka.schemaregistry.client.SchemaMetadata;

public record SchemaDto(SchemaMetadata keySchema, SchemaMetadata valueSchema) {}
