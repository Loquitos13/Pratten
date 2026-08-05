package beringela.software.redis;

/** Canais Redis Pub/Sub partilhados entre instâncias. */
public final class RedisChannelNames {

    public static final String SYNC_EVENTS = "pratten.sync.events";
    public static final String PLATFORM_NOTIFICATIONS = "pratten.platform.notifications";

    public static final String LOGIN_KEY_PREFIX = "pratten:login:";
    public static final String SYNC_CONN_KEY_PREFIX = "pratten:sync:connections:";

    private RedisChannelNames() {
    }
}
