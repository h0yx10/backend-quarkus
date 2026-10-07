package com.events.support;
import javax.sql.DataSource;
public class DatabaseTestClient {
    private final DataSource source;
    public DatabaseTestClient(DataSource source) { this.source=source; }
    public DataSource getDataSource() { return source; }
    public void execute(String sql) { try(var c=source.getConnection();var s=c.createStatement()) { s.execute(sql); } catch(Exception ex) { throw new DatabaseException(ex); } }
    public int update(String sql,Object... args) {
        try(var c=source.getConnection();var s=c.prepareStatement(sql)) {
            for(int i=0;i<args.length;i++) s.setObject(i+1,args[i]); return s.executeUpdate();
        } catch(Exception ex) { throw new DatabaseException(ex); }
    }
    public <T> T queryForObject(String sql,Class<T> type,Object... args) {
        try(var c=source.getConnection();var s=c.prepareStatement(sql)) {
            for(int i=0;i<args.length;i++) s.setObject(i+1,args[i]);
            try(var rows=s.executeQuery()) {
                rows.next(); Object value=rows.getObject(1);
                if(type==Integer.class) value=((Number)value).intValue();
                if(type==Long.class) value=((Number)value).longValue();
                return type.cast(value);
            }
        } catch(Exception ex) { throw new DatabaseException(ex); }
    }
    public static class DatabaseException extends RuntimeException { public DatabaseException(Throwable ex) { super(ex); } }
}
