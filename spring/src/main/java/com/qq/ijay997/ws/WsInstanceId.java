package com.qq.ijay997.ws;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.UUID;

public final class WsInstanceId {

    private final String id;

    private WsInstanceId(String id) {
        this.id = id;
    }

    public static WsInstanceId create(String appName, String port) {
        String host = "unknown-host";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }
        String pid = "unknown-pid";
        try {
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            pid = jvmName.contains("@") ? jvmName.substring(0, jvmName.indexOf('@')) : jvmName;
        } catch (Exception ignored) {
        }
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String a = appName == null || appName.isEmpty() ? "app" : appName;
        String p = port == null || port.isEmpty() ? "0" : port;
        return new WsInstanceId(a + ":" + host + ":" + p + ":" + pid + ":" + rand);
    }

    public String getId() {
        return id;
    }
}

