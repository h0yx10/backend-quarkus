package com.events.support;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Small REST Assured adapter preserving the existing contract assertions during migration. */
public class HttpTestClient {
    public static Request get(String path) { return new Request("GET",path); }
    public static Request post(String path) { return new Request("POST",path); }
    public static Request patch(String path) { return new Request("PATCH",path); }
    public static Request put(String path) { return new Request("PUT",path); }
    public static Request delete(String path) { return new Request("DELETE",path); }
    public static class Request {
        final String method,path; String body,contentType; final Map<String,String> headers=new HashMap<>();
        Request(String method,String path) { this.method=method;this.path=path; }
        public Request header(String key,String value) { headers.put(key,value);return this; }
        public Request content(String value) { body=value;return this; }
        public Request contentType(String value) { contentType=value;return this; }
    }
    public Actions perform(Request request) {
        var given=RestAssured.given().headers(request.headers);
        if(request.body!=null) given.body(request.body);
        if(request.contentType!=null) given.contentType(request.contentType);
        return new Actions(given.request(request.method,request.path));
    }
    public interface Expectation { void check(Response response); }
    public static class Actions {
        final Response response;
        Actions(Response response) { this.response=response; }
        public Actions andExpect(Expectation e) { e.check(response);return this; }
        public Result andReturn() { return new Result(response); }
    }
    public record Result(Response getResponse) { }
    public static Expectation jsonPath(String path, Matcher<?> matcher) { return jsonPath(path).value(matcher); }
    public static Status status() { return new Status(); }
    public static class Status {
        private Expectation code(int value) { return r->assertEquals(value,r.statusCode(),r.body().asString()); }
        public Expectation isOk() { return code(200); }
        public Expectation isCreated() { return code(201); }
        public Expectation isUnauthorized() { return code(401); }
        public Expectation isForbidden() { return code(403); }
        public Expectation isNotFound() { return code(404); }
        public Expectation isBadRequest() { return code(400); }
        public Expectation isConflict() { return code(409); }
    }
    public static JsonExpectation jsonPath(String path) { return new JsonExpectation(path); }
    public record JsonExpectation(String path) {
        Object read(Response r) { return r.jsonPath().get(path.replaceFirst("^\\$\\.", "").replace(".length()", ".size()")); }
        @SuppressWarnings({"unchecked","rawtypes"})
        public Expectation value(Object expected) { return r->{
            Object actual=read(r);
            if(expected instanceof Matcher matcher) MatcherAssert.assertThat(actual,matcher);
            else if(expected instanceof Number a && actual instanceof Number b) assertEquals(a.doubleValue(),b.doubleValue(),0.000001);
            else assertEquals(expected,actual,r.body().asString());
        }; }
        public Expectation isEmpty() { return r->{Object value=read(r);assertTrue(value==null || value instanceof Collection<?> c && c.isEmpty() || "".equals(value),r.body().asString());}; }
        public Expectation isNotEmpty() { return r->assertNotNull(read(r),r.body().asString()); }
        public Expectation doesNotExist() { return r->assertNull(read(r),r.body().asString()); }
    }
}
