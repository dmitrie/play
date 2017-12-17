package play.mvc;

import org.junit.Test;
import play.Play;
import play.PlayBuilder;
import play.mvc.Http.Cookie;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Session;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;
import static play.mvc.Scope.Session.TS_KEY;

public class SessionTest {

    @org.junit.Before
    public void playBuilderBefore() {
        new PlayBuilder().build();
    }

    private static void mockRequestAndResponse() {
        Request.current.set(new Request());
        Response.current.set(new Response());
    }

    public static void setConstant(String constantName, Object value) {
        try {
            /*
             * Set a value to final static constant using reflection.
             */
            Field field = Scope.class.getField(constantName);
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            // Set the new value
            field.set(null, value);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSessionManipulationMethods() {
        mockRequestAndResponse();
        Session session = Session.restore();
        assertFalse(session.changed);

        session.change();
        assertTrue(session.changed);

        // Reset
        session.changed = false;
        session.put("username", "Alice");
        assertTrue(session.changed);

        session.changed = false;
        session.remove("username");
        assertTrue(session.changed);

        session.changed = false;
        session.clear();
        assertTrue(session.changed);

        session.changed = false;
        session.reset();
        assertTrue(session.changed);
    }

    @Test
    public void testReset() throws Exception {
        setConstant("COOKIE_EXPIRE", "1h");
        mockRequestAndResponse();
        Session session = Session.restore();
        String timeStamp = session.get(TS_KEY);
        session.put("foo", "bar");

        session.reset();

        assertEquals(1, session.data.size());
        assertEquals(timeStamp, session.data.get(TS_KEY));
    }

    @Test
    public void testSendOnlyIfChanged() {
        // Mock secret
        Play.secretKey = "0112358";

        Session session;
        setConstant("SESSION_SEND_ONLY_IF_CHANGED", true);
        mockRequestAndResponse();

        // Change nothing in the session
        session = Session.restore();
        session.save();
        assertNull(Response.current().cookies.get(Scope.COOKIE_PREFIX + "_SESSION"));

        mockRequestAndResponse();
        // Change the session
        session = Session.restore();
        session.put("username", "Bob");
        session.save();

        Cookie sessionCookie = Response.current().cookies.get(Scope.COOKIE_PREFIX + "_SESSION");
        assertNotNull(sessionCookie);
        assertTrue(sessionCookie.value.contains("username"));
        assertTrue(sessionCookie.value.contains("Bob"));
    }

    @Test
    public void testSendAlways() {
        Session session;
        setConstant("SESSION_SEND_ONLY_IF_CHANGED", false);

        mockRequestAndResponse();

        // Change nothing in the session
        session = Session.restore();
        session.save();
        assertNotNull(Response.current().cookies.get(Scope.COOKIE_PREFIX + "_SESSION"));
    }

    @org.junit.After
    public void restoreDefault() {
        boolean SESSION_SEND_ONLY_IF_CHANGED = Play.configuration.getProperty("application.session.sendOnlyIfChanged", "false").toLowerCase().equals("true"); 
        setConstant("SESSION_SEND_ONLY_IF_CHANGED", SESSION_SEND_ONLY_IF_CHANGED);

        String COOKIE_EXPIRE = Play.configuration.getProperty("application.session.maxAge");
        setConstant("COOKIE_EXPIRE", COOKIE_EXPIRE);
    }
}
