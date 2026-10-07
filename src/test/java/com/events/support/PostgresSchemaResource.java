package com.events.support;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.sql.DriverManager;
import java.nio.file.*;
import java.util.Map;
public class PostgresSchemaResource implements QuarkusTestResourceLifecycleManager {
    public Map<String,String> start() {
        String url=System.getenv("TEST_DB_URL"),user=System.getenv().getOrDefault("TEST_DB_USERNAME","events_test"),pass=System.getenv().getOrDefault("TEST_DB_PASSWORD","");
        if(url==null) throw new IllegalStateException("postgres-it requiere TEST_DB_URL y una base vacia events_test_*.");
        try(var c=DriverManager.getConnection(url,user,pass);var s=c.createStatement()) {
            try(var db=s.executeQuery("select current_database()")) { db.next();if(!db.getString(1).startsWith("events_test_")) throw new IllegalStateException("Solo una base desechable events_test_*."); }
            s.execute(Files.readString(Path.of("docs/schema.sql")));
        } catch(Exception ex) { throw new IllegalStateException("No se pudo preparar PostgreSQL desechable.",ex); }
        return Map.of("quarkus.datasource.jdbc.url",url,"quarkus.datasource.username",user,"quarkus.datasource.password",pass);
    }
    public void stop() { }
}
