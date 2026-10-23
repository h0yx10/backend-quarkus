package com.events.support;
import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;
/** Binding/validation tests use real REST resources with mocked use-case ports. */
public class UnsecuredProfile implements QuarkusTestProfile {
    public Map<String,String> getConfigOverrides() { return Map.of("quarkus.http.auth.permission.api.policy","permit","quarkus.http.auth.permission.organizador.policy","permit","quarkus.http.auth.permission.admin.policy","permit"); }
}
