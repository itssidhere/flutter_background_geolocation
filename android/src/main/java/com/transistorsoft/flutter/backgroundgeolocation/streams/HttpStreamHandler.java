package com.transistorsoft.flutter.backgroundgeolocation.streams;

import com.transistorsoft.locationmanager.adapter.BackgroundGeolocation;
import com.transistorsoft.locationmanager.adapter.callback.TSHttpResponseCallback;
import com.transistorsoft.locationmanager.http.HttpResponse;
import com.transistorsoft.locationmanager.location.TSLocation;
import com.transistorsoft.locationmanager.logger.TSLog;

import org.json.JSONObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;

import io.flutter.plugin.common.EventChannel;

public class HttpStreamHandler extends StreamHandler implements TSHttpResponseCallback {
    private static final int MAX_CACHE_SIZE = 10000;
    private DatabaseHelper dbHelper;

    public HttpStreamHandler() {
        mEvent = BackgroundGeolocation.EVENT_HTTP;
    }

    @Override
    public void onListen(Object args, EventChannel.EventSink eventSink) {
        super.onListen(args, eventSink);
        dbHelper = new DatabaseHelper(mContext);
        
        BackgroundGeolocation.getInstance(mContext).setBeforeInsertBlock(tsLocation -> {
            try {
                TSLog.logger.debug("[HttpStreamHandler] beforeInsertBlock called");
                String uuid = tsLocation.getUUID();
                String timestamp = tsLocation.getTimestamp();

                if (dbHelper.isDuplicate(uuid, "uuid")) {
                    TSLog.logger.debug("[HttpStreamHandler] Skipping location with duplicate UUID: " + uuid);
                    return null;
                }

                if (dbHelper.isDuplicate(timestamp, "timestamp")) {
                    TSLog.logger.debug("[HttpStreamHandler] Skipping location with duplicate timestamp: " + timestamp);
                    return null;
                }

                dbHelper.addEntry(uuid, timestamp);
                dbHelper.maintainCacheSize(MAX_CACHE_SIZE);
             
                JSONObject json = tsLocation.toJson();


                TSLog.logger.debug("[HttpStreamHandler] Location JSON: " + json.toString());

                return json;
            } catch (Exception e) {
                TSLog.logger.error(TSLog.error(e.getMessage()));
                return null;
            }
        });
        
        BackgroundGeolocation.getInstance(mContext).onHttp(this);
    }

    @Override
    public void onHttpResponse(HttpResponse response) {
        Map<String, Object> event = new HashMap<>();
        event.put("success", response.isSuccess());
        event.put("status", response.status);
        event.put("responseText", response.responseText);
        mEventSink.success(event);
    }
}