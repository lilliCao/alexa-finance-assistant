package amosalexa;

import java.util.HashMap;
import java.util.Map;

/**
 * This singleton class helps to store arbitrary objects and link them to a session.
 * TODO: Implement removal of sessions and objects
 */
public class SessionStorage {

    private static SessionStorage instance;

    public static final String CURRENTDIALOG = "CURRENTDIALOG";

    /**
     * This class contains session related values.
     */
    public class Storage {
        private Map<String,Object> storage;

        private Storage() {
            storage = new HashMap<>();
        }

        public void put(String key, Object value) {
            storage.put(key, value);
        }

        public Object get(String key) {
            if (!storage.containsKey(key)) {
                return null;
            }
            return storage.get(key);
        }

        public boolean containsKey(String key) {
            return storage.containsKey(key);
        }

        public int size(){
            return storage.size();
        }

        public Object remove(String key) {
            return storage.remove(key);
        }
    }

    private Map<String,Storage> sessionStorage;

    private SessionStorage() {
        sessionStorage = new HashMap<>();
    }

    public static SessionStorage getInstance() {
        if(SessionStorage.instance == null) {
            SessionStorage.instance = new SessionStorage();
        }
        return SessionStorage.instance;
    }

    /**
     * Returns a Storage object or creates it if it doesn't exist yet.
     * @param sessionId
     * @return Storage object
     */
    public Storage getStorage(String sessionId) {
        if (sessionStorage.containsKey(sessionId)) {
            return sessionStorage.get(sessionId);
        }

        Storage storage = new Storage();
        sessionStorage.put(sessionId, storage);
        return storage;
    }

    /**
     * Puts an arbitrary object into the session storage and links it to a session.
     * @param sessionId
     * @param key
     * @param value
     */
    public void putObject(String sessionId, String key, Object value) {
        Storage storage = getStorage(sessionId);
        storage.put(key, value);
    }

    /**
     * Retrieves an arbitrary object that has been stored before.
     * @param sessionId
     * @param key
     * @return
     */
    public Object getObject(String sessionId, String key) {
        Storage storage = getStorage(sessionId);
        return storage.get(key);
    }

    /**
     * Removes and returns a Storage object that has been stored before.
     * @param sessionId
     * @return Storage object
     */
    public Object removeStorage(String sessionId) {
        return sessionStorage.remove(sessionId);
    }

}
