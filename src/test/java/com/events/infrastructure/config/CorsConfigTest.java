package com.events.infrastructure.config;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import static org.assertj.core.api.Assertions.*;
@QuarkusTest
class CorsConfigTest {
    @Test
    void acceptsPreflightFromConfiguredOrigins() {
        for(String origin:new String[]{"http://localhost:4200","https://frontend-pi-olive-30.vercel.app"}) {
            for(String path:new String[]{"/api/events","/api/today","/api/capacity"}) {
                var r=RestAssured.given().header("Origin",origin).header("Access-Control-Request-Method","GET")
                        .header("Access-Control-Request-Headers","content-type").options(path);
                assertThat(r.statusCode()).isEqualTo(200);
                assertThat(r.header("Access-Control-Allow-Origin")).isEqualTo(origin);
                assertThat(r.header("Access-Control-Allow-Credentials")).isEqualTo("true");
            }
        }
    }
    @Test
    void rejectsUnconfiguredOrigin() {
        var r=RestAssured.given().header("Origin","https://another-app.vercel.app").header("Access-Control-Request-Method","GET").options("/api/events");
        assertThat(r.statusCode()).isEqualTo(403);assertThat(r.header("Access-Control-Allow-Origin")).isNull();
    }
}
