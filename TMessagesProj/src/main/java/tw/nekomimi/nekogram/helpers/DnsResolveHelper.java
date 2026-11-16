package tw.nekomimi.nekogram.helpers;

import org.telegram.messenger.FileLog;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * DNS resolution for the native layer. IPv4-only mode (freshGram).
 * Android's default resolver (getaddrinfo) often returns IPv6 first and this is not
 * usable on many VPN setups, which results in an endless "Updating..." state.
 */
public class DnsResolveHelper {

    public static String[] resolveIpv4Only(String host) {
        try {
            InetAddress[] all = InetAddress.getAllByName(host);
            ArrayList<String> v4 = new ArrayList<>();
            for (InetAddress a : all) {
                if (a instanceof Inet4Address) {
                    String ip = a.getHostAddress();
                    if (!v4.contains(ip)) {
                        v4.add(ip);
                    }
                }
            }
            if (v4.isEmpty()) {
                return new String[0];
            }
            return v4.toArray(new String[0]);
        } catch (Throwable e) {
            FileLog.e(e);
            return new String[0];
        }
    }
}
