package com.events.infrastructure.config;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.jackson.ObjectMapperCustomizer;
import com.fasterxml.jackson.databind.*;
import java.util.TimeZone;
@ApplicationScoped
public class JacksonConfig implements ObjectMapperCustomizer {
    public void customize(ObjectMapper mapper) {
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setTimeZone(TimeZone.getTimeZone("America/Bogota"));
    }
}
