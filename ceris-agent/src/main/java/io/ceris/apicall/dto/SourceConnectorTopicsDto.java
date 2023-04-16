package io.ceris.apicall.dto;

import java.util.List;

public record SourceConnectorTopicsDto(String connector, List<String> topics) {}
