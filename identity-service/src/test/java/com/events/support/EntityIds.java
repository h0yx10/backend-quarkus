package com.events.support;
public final class EntityIds {
    private EntityIds() { }
    public static void setField(Object object,String name,Object value) {
        try { var field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value); }
        catch(ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
}
