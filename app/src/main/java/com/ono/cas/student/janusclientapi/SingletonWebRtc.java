package com.ono.cas.student.janusclientapi;

import com.ono.cas.student.MyLifecycleHandler;
import com.ono.cas.student.webRtc.WebRTCEngine;

public class SingletonWebRtc {

    private static WebRTCEngine webRTCEngine;

    public synchronized static WebRTCEngine getWebRTCEngine() {
        if (null == webRTCEngine) {
            webRTCEngine = new WebRTCEngine(MyLifecycleHandler.activity);
        }
        return webRTCEngine;
    }

    public static void releaseInstance() {
        webRTCEngine = null;
    }

}
