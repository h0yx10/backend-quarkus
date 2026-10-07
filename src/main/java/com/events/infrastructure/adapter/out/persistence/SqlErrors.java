package com.events.infrastructure.adapter.out.persistence;
public final class SqlErrors {
    private SqlErrors() { }
    public static boolean hasState(Throwable error, String state) {
        for(Throwable c=error;c!=null;c=c.getCause()) if(c instanceof java.sql.SQLException s && state.equals(s.getSQLState())) return true;
        return false;
    }
}
