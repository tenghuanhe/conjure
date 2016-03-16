package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tenghuanhe on 2016/3/16.
 */
public class HostPort {
    public static final String ANY = "*";
    private static final Pattern ANY_PATTERN = Pattern.compile("\\*:(\\d{1,5})");
    private static final Pattern HOST_PORT_PATTERN =
            Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})");

    private final String host;
    private final int port;

    public HostPort(int port) {
        this.host = ANY;
        this.port = port;
    }

    public HostPort(String host, int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be in range 0 - 65535");
        }
        this.host = host;
        this.port = port;
    }

    public static HostPort decode(String addr) {
        Matcher m;
        m = HOST_PORT_PATTERN.matcher(addr);
        if (m.find()) {
            int port = Integer.parseInt(m.group(2));
            return new HostPort(m.group(1), port);
        } else {
            m = ANY_PATTERN.matcher(addr);
            if (m.find()) {
                int port = Integer.parseInt(m.group(1));
                return new HostPort(port);
            }
        }
        return null;
    }

    public boolean isAnyHost() {
        return ANY.equals(this.host);
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    @Override
    public String toString() {
        return this.host + ":" + this.port;
    }

}
