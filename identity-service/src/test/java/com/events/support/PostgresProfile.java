package com.events.support;
import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;
public class PostgresProfile implements QuarkusTestProfile {
    public Map<String,String> getConfigOverrides() {
        return Map.of("quarkus.datasource.db-kind","postgresql","quarkus.hibernate-orm.schema-management.strategy","validate","quarkus.hibernate-orm.sql-load-script","no-file");
    }
}
