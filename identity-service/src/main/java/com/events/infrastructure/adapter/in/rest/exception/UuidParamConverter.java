package com.events.infrastructure.adapter.in.rest.exception;
import jakarta.ws.rs.ext.*;
import jakarta.ws.rs.BadRequestException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.UUID;
@Provider
public class UuidParamConverter implements ParamConverterProvider {
    public <T> ParamConverter<T> getConverter(Class<T> type,Type generic,Annotation[] annotations) {
        if(type!=UUID.class) return null;
        return new ParamConverter<T>() {
            public T fromString(String value) {
                if(value==null) return null;
                try { return type.cast(UUID.fromString(value)); } catch(IllegalArgumentException ex) { throw new InvalidUuidException(value,ex); }
            }
            public String toString(T value) { return value.toString(); }
        };
    }
}
