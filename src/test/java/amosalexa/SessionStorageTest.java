package amosalexa;

import org.junit.Test;

import static org.junit.Assert.*;

public class SessionStorageTest {
    @Test
    public void getObject() throws Exception {
        SessionStorage sessionStorage = SessionStorage.getInstance();

        sessionStorage.putObject("sessid123", "key1", "val1");
        sessionStorage.putObject("sessid123", "key2", "val2");
        sessionStorage.putObject("sessid456", "key3", "val3");

        assertEquals(sessionStorage.getObject("sessid123", "key1"), "val1");
        assertEquals(sessionStorage.getObject("sessid123", "key2"), "val2");
        assertEquals(sessionStorage.getObject("sessid456", "key3"), "val3");
    }

}