package com.anjoola.sharewear.util;

import org.json.JSONObject;

/**
 * Callback after something is sent to the server.
 */
public interface ServerConnectionCallback {
    public void callback(JSONObject json);
}
