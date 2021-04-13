package com.ono.cas.student;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ono.cas.student.janusclientapi.IJanusGatewayCallbacks;
import com.ono.cas.student.janusclientapi.IJanusPluginCallbacks;
import com.ono.cas.student.janusclientapi.IPluginHandleWebRTCCallbacks;
import com.ono.cas.student.janusclientapi.JanusMediaConstraints;
import com.ono.cas.student.janusclientapi.JanusPluginHandle;
import com.ono.cas.student.janusclientapi.JanusServer;
import com.ono.cas.student.janusclientapi.JanusSupportedPluginPackages;
import com.ono.cas.student.janusclientapi.PluginHandleSendMessageCallbacks;
import com.ono.cas.student.janusclientapi.PluginHandleWebRTCCallbacks;
import com.ono.cas.student.janusclientapi.SingletonWebRtc;
import com.ono.cas.student.webRtc.WebRTCEngine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private boolean isVoiceEnable = true;
    private boolean isVideoEnable = true;
    private boolean isSpeakerEnable = true;

    public static final String TAG = "MainActivity";
    boolean ROOM_CREATION;

    private JanusServer janusServer;
    private WebRTCEngine webRTCEngine;

    public static final String REQUEST = "request";
    public static final String MESSAGE = "message";
    List<JanusPluginHandle> janusPluginHandles = new ArrayList<>();

    private JanusPluginHandle handle;
    private BigInteger myid;
    private BigInteger selectedUserHandleId;
    private BigInteger selectedUserFeedId;

    public static final String JANUS_URI = "wss://web.trango.io/janus";
    public static final String DEEP_LINK_URL = "https://web.trango.io";
    public static final String PUBLISHERS = "publishers";
    private static final int RC_CAMERA_AND_MIC = 1001;

    Button button_Start, button_Stop;

    private BigInteger roomid = new BigInteger("11223344");
    private String user_name = "StudentName";

    private static final String[] CAMERA_AND_MIC =
            {android.Manifest.permission.CAMERA,
                    android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.INTERNET,
                    android.Manifest.permission.ACCESS_NETWORK_STATE,
                    android.Manifest.permission.ACCESS_WIFI_STATE,
                    android.Manifest.permission.CHANGE_WIFI_STATE,
                    android.Manifest.permission.CHANGE_NETWORK_STATE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initComponents();
        initListeners();
    }


    private void initComponents() {
        button_Start = findViewById(R.id.button_Start);
        button_Stop = findViewById(R.id.button_Stop);
    }

    private void initListeners() {
        button_Start.setOnClickListener(this);
        button_Stop.setOnClickListener(this);
    }


    public void Start() {
//        ROOM_CREATION = true;

        webRTCEngine = SingletonWebRtc.getWebRTCEngine();
        janusServer = new JanusServer(new JanusGlobalCallbacks());
        janusServer.Connect();
    }

    @Override
    public void onClick(View view) {
        if (view.equals(button_Start)) {
            //todo start janus
            Start();
        } else if (view.equals(button_Stop)) {
            //todo stop publishing self video
            disconnectCall();
        }
    }

    public void disconnectCall() {
        runOnUiThread(() -> {
            if (!janusServer.attachedPlugins.isEmpty()) {
                janusServer.Destroy();
            }

            if (null != webRTCEngine) {
                webRTCEngine.release();
            }
        });
    }

    /**
     * Janus Util Methods
     */
    private List<JanusPluginHandle> getJanusPluginHandles() {
        List<JanusPluginHandle> janusPluginHandles = new ArrayList<>();
        for (BigInteger handleId : janusServer.attachedPlugins.keySet()) {
            JanusPluginHandle janusPluginHandle = janusServer.attachedPlugins.get(handleId);
            janusPluginHandles.add(janusPluginHandle);
        }
        return janusPluginHandles;
    }

    public void subscriberOnLeaving(JSONObject jsonObject) {
        /*
         * Get user if from json response using keyword
         * */
        if (jsonObject.has("leaving")) {
            String id = null;
            try {
                id = jsonObject.getString("leaving");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            BigInteger feedId = new BigInteger(id);

            /*
             * if un-published user is in main renderer.
             * then, remove that subscriber and show self user in main renderer.
             * */
            if (selectedUserFeedId.equals(feedId)) {

                selectedUserFeedId = myid;
                selectedUserHandleId = handle.id;
                JanusPluginHandle janusPluginHandle = janusServer.attachedPlugins.get(handle.id);
                janusPluginHandle.janusUserDetail.setSelected(true);

                /*
                 * update plugins
                 * */
                janusServer.attachedPlugins.put(handle.id, janusPluginHandle);
                janusServer.feedPlugins.put(myid, janusPluginHandle);
            }

            /*
             * Remove Subscriber from adapter and release its renderer.
             * */
            else {

                JanusPluginHandle janusPluginHandle = janusServer.feedPlugins.get(feedId);
                if (janusPluginHandle != null) {
                    BigInteger handleId = janusPluginHandle.janusUserDetail.getHandleId();
                    showToast(MyLifecycleHandler.activity, janusPluginHandle.janusUserDetail.getUserName() + " left the meeting.");
                    janusServer.feedPlugins.remove(feedId);
                    janusServer.attachedPlugins.remove(handleId);
                }
            }
        }
    }

    public void subscriberOnUnPublish(JSONObject jsonObject) {
        /*
         * Get user if from json response using keyword
         * */
        if (jsonObject.has("unpublished")) {
            String id = null;
            try {
                id = jsonObject.getString("unpublished");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            BigInteger feedId = new BigInteger(id);

            /*
             * if un-published user is in main renderer.
             * then, remove that subscriber and show self user in main renderer.
             * */
            if (selectedUserFeedId.equals(feedId)) {
                selectedUserFeedId = myid;
                selectedUserHandleId = handle.id;
                JanusPluginHandle janusPluginHandle = janusServer.attachedPlugins.get(handle.id);
                janusPluginHandle.janusUserDetail.setSelected(true);

                /*
                 * update plugins
                 * */
                janusServer.attachedPlugins.put(handle.id, janusPluginHandle);
                janusServer.feedPlugins.put(myid, janusPluginHandle);
            }
            /*
             * Remove Subscriber from adapter and release its renderer.
             * */
            else {
                JanusPluginHandle janusPluginHandle = janusServer.feedPlugins.get(feedId);
                if (janusPluginHandle != null) {
                    janusPluginHandle.detach();
                }

            }
        }
    }//[onSubscriberUnPublish]

    public void updateVideoInPluginHandle(BigInteger pubHandleId, boolean isVideoEnable) {
        JanusPluginHandle janusPluginHandle = janusServer.attachedPlugins.get(pubHandleId);
        if (janusPluginHandle != null) {
            janusPluginHandle.janusUserDetail.setVideoEnable(isVideoEnable);
            janusServer.attachedPlugins.put(pubHandleId, janusPluginHandle);
            janusServer.feedPlugins.put(janusPluginHandle.janusUserDetail.getFeedId(), janusPluginHandle);
        }
    }

    public void updateAudioInPluginHandle(BigInteger pubHandleId, boolean isAudioEnable) {
        JanusPluginHandle janusPluginHandle = janusServer.attachedPlugins.get(pubHandleId);
        if (janusPluginHandle != null) {
            janusPluginHandle.janusUserDetail.setMicEnable(isAudioEnable);
            janusServer.attachedPlugins.put(pubHandleId, janusPluginHandle);
            janusServer.feedPlugins.put(janusPluginHandle.janusUserDetail.getFeedId(), janusPluginHandle);
        }
    }

    class ListenerAttachCallbacks implements IJanusPluginCallbacks {
        private final BigInteger feedId;
        private final String displayName;
        private JanusPluginHandle listener_handle;

        public ListenerAttachCallbacks(BigInteger id, String displayName) {
            this.feedId = id;
            this.displayName = displayName;
        }

        public void success(JanusPluginHandle handle) {
            listener_handle = handle;
            listener_handle.janusUserDetail.setFeedId(feedId);
            janusServer.attachedPlugins.put(listener_handle.id, listener_handle);
            janusServer.feedPlugins.put(feedId, listener_handle);
            try {
                JSONObject body = new JSONObject();
                JSONObject msg = new JSONObject();
                body.put(REQUEST, "join");
                body.put("room", roomid);
                body.put("ptype", "subscriber");
                body.put("feed", feedId);
                msg.put(MESSAGE, body);
                handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
            } catch (Exception ex) {
//                Log.d(TAG, "success: Subscriber" + ex);
            }
        }

        @Override
        public void onMessage(JSONObject msg, JSONObject jsep) {
            try {
//                Log.d(TAG, "onMessage: Subscriber==> " + msg);
                String event = msg.getString("videoroom");
                if (event.equals("attached") && jsep != null) {
                    final JSONObject remoteJsep = jsep;
                    listener_handle.createAnswer(new IPluginHandleWebRTCCallbacks() {
                        @Override
                        public void onSuccess(JSONObject obj) {
                            try {
                                JSONObject mymsg = new JSONObject();
                                JSONObject body = new JSONObject();
                                body.put(REQUEST, "start");
                                body.put("room", roomid);
                                mymsg.put(MESSAGE, body);
                                mymsg.put("jsep", obj);
                                listener_handle.sendMessage(new PluginHandleSendMessageCallbacks(mymsg));
                            } catch (Exception ex) {
//                                Log.d(TAG, "onSuccess: " + ex);
                            }
                        }

                        @Override
                        public JSONObject getJsep() {
                            return remoteJsep;
                        }

                        @Override
                        public JanusMediaConstraints getMedia() {
                            JanusMediaConstraints cons = new JanusMediaConstraints();
                            cons.setVideo(null);
                            cons.setRecvAudio(true);
                            cons.setRecvVideo(true);
                            cons.setSendAudio(false);
                            return cons;
                        }

                        @Override
                        public Boolean getTrickle() {
                            return true;
                        }

                        @Override
                        public void disconnectedUser(BigInteger handleId) {
                            showToast(MyLifecycleHandler.activity, "Someother user got disconnected.");

                            /*
                             * Check if user exist in adapter and hashmaps
                             * then remove it from the hashmap and adapter
                             * */

                            runOnUiThread(() -> removeUserOnWebRtcError(handleId));

                        }

                        @Override
                        public void onCallbackError(String error) {
//                            Log.d(TAG, "onCallbackError: " + error);
                        }
                    });
                }
                /*
                 * if user again enable/disable video
                 * */
                else if (event.equals("event") && msg.getString("configured").equals("ok") && jsep != null) {
//                    Log.d(TAG, "ListenerAttachCallbacks ==> onMessage: " + jsep.toString());
                    JSONObject remoteJsep = jsep;
                    listener_handle.mySdp = null;
                    listener_handle.createAnswer(new IPluginHandleWebRTCCallbacks() {
                        @Override
                        public void onSuccess(JSONObject obj) {
                            try {
                                JSONObject mymsg = new JSONObject();
                                JSONObject body = new JSONObject();
                                body.put(REQUEST, "start");
                                body.put("room", roomid);
                                mymsg.put(MESSAGE, body);
                                mymsg.put("jsep", obj);
                                listener_handle.sendMessage(new PluginHandleSendMessageCallbacks(mymsg));
                            } catch (Exception ex) {
//                                Log.d(TAG, "onSuccess: CreateAnswer" + ex.toString());
                            }
                        }

                        @Override
                        public JSONObject getJsep() {
                            return remoteJsep;
                        }

                        @Override
                        public JanusMediaConstraints getMedia() {
                            JanusMediaConstraints cons = new JanusMediaConstraints();
                            cons.setVideo(null);
                            cons.setRecvAudio(true);
                            cons.setRecvVideo(true);
                            cons.setSendAudio(false);
                            return cons;
                        }

                        @Override
                        public Boolean getTrickle() {
                            return true;
                        }

                        @Override
                        public void disconnectedUser(BigInteger handleId) {
                        }

                        @Override
                        public void onCallbackError(String error) {
//                            Log.d(TAG, "onCallbackError: " + error);
                        }
                    });
                }
            } catch (Exception ex) {
//                Log.d(TAG, "onMessage: " + ex);
            }
        }

        @Override
        public void onLocalStream(MediaStream stream) {

        }

        @Override
        public void onRemoteStream(MediaStream stream) {
            MyLifecycleHandler.activity.runOnUiThread(() -> {
                View view = webRTCEngine.convertStreamIntoView(stream, true, MyLifecycleHandler.activity);
                JanusPluginHandle janusPluginHandle = janusServer.attachedPlugins.get(listener_handle.janusUserDetail.getHandleId());
                assert janusPluginHandle != null;
                janusPluginHandle.janusUserDetail.setUserName(displayName);
                janusPluginHandle.janusUserDetail.setView(view);
                janusPluginHandle.janusUserDetail.setSelected(false);
                janusPluginHandle.janusUserDetail.setVideoEnable(janusPluginHandle.janusUserDetail.isVideoEnable());
                janusPluginHandle.janusUserDetail.setMicEnable(janusPluginHandle.janusUserDetail.isMicEnable());

                Log.d(TAG, "onRemoteStream: " + janusPluginHandle.toString());

                janusServer.attachedPlugins.put(listener_handle.janusUserDetail.getHandleId(), janusPluginHandle);
                janusServer.feedPlugins.put(janusPluginHandle.janusUserDetail.getFeedId(), janusPluginHandle);
            });
        }

        @Override
        public void updateStream(MediaStream mediaStream) {
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataOpen(Object data) {

        }

        @Override
        public void onData(Object data) {

        }

        @Override
        public void onCleanup() {

        }

        @Override
        public void onDetached() {
//            Log.d(TAG, "onDetached: Subscriber");
        }

        @Override
        public JanusSupportedPluginPackages getPlugin() {
            return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM;
        }

        @Override
        public void onCallbackError(String error) {

        }
    }

    public class JanusGlobalCallbacks implements IJanusGatewayCallbacks {
        public void onSuccess() {
//            janusPublisherPluginCallbacks = new JanusPublisherPluginCallbacks();
//            janusServer.Attach(janusPublisherPluginCallbacks);
            janusServer.Attach(new JanusPublisherPluginCallbacks());
        }

        @Override
        public void onDestroy() {
        }

        @Override
        public String getServerUri() {
            return JANUS_URI;
        }

        @Override
        public List<PeerConnection.IceServer> getIceServers() {
            return new ArrayList<>();
        }

        @Override
        public Boolean getIpv6Support() {
            return Boolean.FALSE;
        }

        @Override
        public Integer getMaxPollEvents() {
            return 0;
        }

        @Override
        public void onCallbackError(String error) {

        }
    }

    public class JanusPublisherPluginCallbacks implements IJanusPluginCallbacks {

        public void publishOwnFeed(boolean isVideoEnable, boolean isAudioEnable) {
            if (null != handle) {
                handle.createOffer(new IPluginHandleWebRTCCallbacks() {
                    @Override
                    public void onSuccess(final JSONObject obj) {
                        try {
                            final JSONObject msg = new JSONObject();
                            final JSONObject body = new JSONObject();
                            body.put(REQUEST, "configure");
                            body.put("audio", true);
                            body.put("video", true);
                            body.put("videocodec", "vp8");
                            msg.put(MESSAGE, body);
                            msg.put("jsep", obj);
                            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
                        } catch (Exception ex) {
                            Log.e(TAG, "onSuccess: ", ex);
                        }
                    }

                    @Override
                    public JSONObject getJsep() {
                        return null;
                    }

                    @Override
                    public JanusMediaConstraints getMedia() {
                        final JanusMediaConstraints cons = new JanusMediaConstraints();
                        cons.setRecvAudio(false);
                        cons.setRecvVideo(false);
                        cons.setSendAudio(true);
                        return cons;
                    }

                    @Override
                    public Boolean getTrickle() {
                        return true;
                    }

                    @Override
                    public void disconnectedUser(BigInteger handleId) {
                        showToast(MyLifecycleHandler.activity, "Self user got disconnected.");

                        /*
                         * Todo
                         *  show reconnecting screen
                         *  clear adapter
                         *  clear all hash maps
                         *  call janus start
                         *  make socket connection.
                         * */
                        if (MyLifecycleHandler.activity.getClass().getSimpleName().equals("VideoRoomActivity")) {
                            runOnUiThread(() -> {
                                isReconnecting = true;
                                janusPluginHandles.clear();
                                janusServer.attachedPlugins.clear();
                                janusServer.feedPlugins.clear();
                            });
                        } else {
                            runOnUiThread(() -> {
                                handleCallDisconnectionScenarios();
                            });
                        }
                    }

                    @Override
                    public void onCallbackError(final String error) {

                    }
                });
            }
        }

        private void createRoom() {
            final JSONObject body = new JSONObject();
            final JSONObject msg = new JSONObject();
            try {
                body.put(REQUEST, "create");
                body.put("room", roomid);
                body.put("permanent", false);
                body.put("description", "Android_Room");
                body.put("is_private", false);
                body.put("publishers", 25);
                body.put("videocodec", "vp8");
                body.put("bitrate", 30000);

                msg.put(MESSAGE, body);
            } catch (final Exception e) {
                e.printStackTrace();
            }
            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
        }

        private void registerUsername() {
            if (handle != null) {
                final JSONObject obj = new JSONObject();
                final JSONObject msg = new JSONObject();
                try {
                    obj.put(REQUEST, "join");
                    obj.put("room", roomid);
                    obj.put("ptype", "publisher");
                    obj.put("display", user_name);
                    msg.put(MESSAGE, obj);
                } catch (final Exception ex) {
                    Log.e(TAG, "registerUsername: ", ex);
                }
                handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
            }
        }

        private void newRemoteFeed(final BigInteger id, final String displayName) { //todo attach the plugin as a listener
            janusServer.Attach(new ListenerAttachCallbacks(id, displayName));
        }

        @Override
        public void success(final JanusPluginHandle pluginHandle) {
            handle = pluginHandle;
            if (ROOM_CREATION) {
                this.createRoom();
            } else {
                this.registerUsername();
            }
        }

        @Override
        public void onMessage(final JSONObject msg, final JSONObject jsepLocal) {
            try {
                final String event = msg.getString("videoroom");
                if (event.equals("joined")) {
                    myid = new BigInteger(msg.getString("id"));
                    handle.janusUserDetail.setVideoEnable(isVideoEnable);
                    handle.janusUserDetail.setMicEnable(isVoiceEnable);
                    handle.janusUserDetail.setFeedId(myid);
                    janusServer.feedPlugins.put(myid, handle);
                    janusServer.attachedPlugins.put(handle.id, handle);

                    this.publishOwnFeed(isVideoEnable, isVoiceEnable);
                    if (msg.has(PUBLISHERS)) {
                        final JSONArray pubs = msg.getJSONArray(PUBLISHERS);
                        for (int i = 0; i < pubs.length(); i++) {
                            final JSONObject pub = pubs.getJSONObject(i);
                            final BigInteger tehId = new BigInteger(pub.getString("id"));
                            final String displayName = pub.getString("display");
                            this.newRemoteFeed(tehId, displayName);
                        }
                    }
                } else if (event.equals("destroyed")) {
//                    Log.d(VideoRoom.TAG, "onMessage: destroyed" + msg);
                } else if (event.equals("created")) {
                    this.registerUsername();
                } else if (event.equals("event")) {
                    if (msg.has(PUBLISHERS)) {
                        final JSONArray pubs = msg.getJSONArray(PUBLISHERS);
                        for (int i = 0; i < pubs.length(); i++) {
                            final JSONObject pub = pubs.getJSONObject(i);
                            final BigInteger tehId = new BigInteger(pub.getString("id"));
                            final String displayName = pub.getString("display");
                            this.newRemoteFeed(tehId, displayName);
                        }
                    } else if (msg.has("leaving")) {
                        subscriberOnLeaving(msg);
                    } else if (msg.has("unpublished")) {
                        subscriberOnUnPublish(msg);
                    } else {
                        //todo error
                        if (msg.has("error")) {
                            if (msg.has("error_code")) {
                                int errorCode = msg.getInt("error_code");
                                if (errorCode == 426) {
                                    runOnUiThread(() -> {
                                        showToast(MyLifecycleHandler.activity, "This room link has been already expired.");
                                        disconnectCall();
                                        janusServer = null;
                                        SingletonWebRtc.releaseInstance();
                                        finish();
                                    });
                                } else if (errorCode == 427) {
                                    runOnUiThread(() -> {
                                        showToast(MyLifecycleHandler.activity, "This room already exists.");
                                        this.registerUsername();
                                    });
                                }
                            }
                        }
                    }
                }
                if (jsepLocal != null) {
                    handle.handleRemoteJsep(new PluginHandleWebRTCCallbacks(null, jsepLocal, false));
                }
            } catch (final Exception ex) {
                Log.e(TAG, "onMessage: ", ex);
            }
        }

        @Override
        public void onLocalStream(final MediaStream stream) {
//            executor.execute(() -> webRTCEngine.toggleSpeaker(false));

            selectedUserHandleId = handle.id;
            selectedUserFeedId = myid;

            MyLifecycleHandler.activity.runOnUiThread(() -> {
                webRTCEngine.toggleSpeaker(isSpeakerEnable);
                View view = webRTCEngine.startPreview(true);
                handle.janusUserDetail.setUserName(user_name);
                handle.janusUserDetail.setView(view);
                handle.janusUserDetail.setSelected(true);
                handle.janusUserDetail.setVideoEnable(1 <= stream.videoTracks.size());
                handle.janusUserDetail.setMicEnable(1 <= stream.audioTracks.size());
                handle.myStream.videoTracks.get(0).setEnabled(isVideoEnable);
                handle.myStream.audioTracks.get(0).setVolume(0);// to remove the echo.
                janusServer.attachedPlugins.put(handle.janusUserDetail.getHandleId(), handle);
                janusServer.feedPlugins.put(myid, handle);

                if (isVideoEnable) {
                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
                    lp.addRule(RelativeLayout.CENTER_IN_PARENT);
                    view.setLayoutParams(lp);
                }

                if (!isVoiceEnable) {
                    webRTCEngine._localStream.audioTracks.get(0).setEnabled(false);
                }
            });
        }

        @Override
        public void onRemoteStream(final MediaStream stream) {

        }

        @Override
        public void updateStream(MediaStream mediaStream) {

        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataOpen(final Object data) {

        }

        @Override
        public void onData(final Object data) {

        }

        @Override
        public void onCleanup() {

        }

        @Override
        public JanusSupportedPluginPackages getPlugin() {
            return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM;
        }

        @Override
        public void onCallbackError(final String error) {
//            Log.d(TAG, "onCallbackError: Publisher" + error);
        }

        @Override
        public void onDetached() {
//            Log.d(TAG, "onDetached: Publisher");
        }
    }

    boolean isReconnecting;

    private void removeUserOnWebRtcError(BigInteger handleId) {
        if (selectedUserHandleId.equals(handleId)) {

            selectedUserFeedId = myid;
            selectedUserHandleId = handle.id;
            JanusPluginHandle janusPluginHandle = janusServer.attachedPlugins.get(handle.id);
            if (janusPluginHandle != null && janusPluginHandle.janusUserDetail != null) {
                janusPluginHandle.janusUserDetail.setSelected(true);

                /*
                 * update plugins
                 * */
                if (janusServer != null && janusServer.attachedPlugins != null && handle.id != null) {
                    janusServer.attachedPlugins.put(handle.id, janusPluginHandle);
                }
                if (janusServer != null && janusServer.feedPlugins != null && myid != null) {
                    janusServer.feedPlugins.put(myid, janusPluginHandle);
                }
            }
        }
        /*
         * Remove Subscriber from adapter and release its renderer.
         * */
        else {
            if (janusServer.attachedPlugins.containsKey(handleId)) {
                JanusPluginHandle janusPluginHandle = janusServer.attachedPlugins.get(handleId);
                if (janusPluginHandle.janusUserDetail != null && janusPluginHandle.janusUserDetail.getFeedId() != null) {
                    BigInteger feedId = janusPluginHandle.janusUserDetail.getFeedId();
                    if (janusPluginHandle != null) {
                        showToast(MyLifecycleHandler.activity, janusPluginHandle.janusUserDetail.getUserName() + " left the meeting.");
                        janusServer.feedPlugins.remove(feedId);
                        janusServer.attachedPlugins.remove(handleId);
                    }
                }
            }
        }
    }

    private void handleCallDisconnectionScenarios() {
        disconnectCall();
        janusServer = null;
        SingletonWebRtc.releaseInstance();
        finishAffinity();
    }

    public static void showToast(Context context, String message) {
        MyLifecycleHandler.activity.runOnUiThread(() ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}