package com.example.xmpp_runbuddy;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.android.AndroidSmackInitializer;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.jivesoftware.smackx.pubsub.form.FillableConfigureForm;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class MainActivity extends FlutterActivity {
    public final String CHANNEL = "runbuddy/xmpp";
    private PubSubManager psManager = null;
    private AbstractXMPPConnection xmppConnection = null;
    private boolean hasConnection = false;
    private LeafNode testLeaf = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GeneratedPluginRegistrant.registerWith(new FlutterEngine(this));

        Log.i("myapp", "Setting up XMPP");

        // Init XMPP connection.
        try {
            hasConnection = new AsyncInitXMPP(this).execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        MainActivity ma = this;
        new MethodChannel(getFlutterEngine().getDartExecutor().getBinaryMessenger(), CHANNEL)
            .setMethodCallHandler(new MethodChannel.MethodCallHandler() {
                @Override
                public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
                    if(methodCall.method.equals("getMessage")) {
                        ArrayList<String> payloads = (ArrayList<String>)methodCall.arguments();
                        try {
                            result.success(new AsyncPubSubXMPP(ma, payloads).execute().get());
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
    }

    // Initialize the publish and subscribe connection. MUST be done asynchronously.
    private class AsyncInitXMPP extends AsyncTask<Void, Void, Boolean>
    {
        private MainActivity mainActivity = null;
        public AsyncInitXMPP(MainActivity ma) {
            super();
            mainActivity = ma;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // TODO process dialog maybe
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            AndroidSmackInitializer.initialize(getApplicationContext());

            try {
                mainActivity.xmppConnection = new XMPPTCPConnection("ryan", "IAmErr0r", "ryanmj.xyz");
                mainActivity.xmppConnection.connect().login();
                mainActivity.psManager = PubSubManager.getInstanceFor(xmppConnection);

                // TODO update the docs about fillable configure form.
                // also FormType

                FillableConfigureForm form = psManager.getDefaultConfiguration().getFillableForm();
                Log.d("myapp", "waiting for subs...");
                try {
                    mainActivity.testLeaf = psManager.getOrCreateLeafNode("testNode");
                } catch(Exception e) {
                    Log.e("myapp", "error in getting node: " + e.getMessage());
                    return new Boolean(false);
                }

                Log.i("myapp", "Adding event listeners");
                mainActivity.testLeaf.addItemEventListener(new ItemEventCoordinator());
                Subscription subs = mainActivity.testLeaf.subscribe(JidCreate.from("ryan@ryanmj.xyz/testNode"));

                mainActivity.testLeaf.addItemDeleteListener(new ItemDeleteCoordinator(mainActivity.testLeaf.getId()));

                Log.i("myapp", "Deleting other test pubs");
                mainActivity.testLeaf.deleteAllItems();

                mainActivity.hasConnection = true;
            } catch (XmppStringprepException e) {
                Log.e("myapp", "XMPPStringprep: " + Log.getStackTraceString(e));
            } catch (SmackException e) {
                Log.e("myapp", "Smack:" + Log.getStackTraceString(e));
            } catch (IOException e) {
                Log.e("myapp", "IO: " + Log.getStackTraceString(e));
            } catch (XMPPException e) {
                Log.e("myapp", "XMPP: " + Log.getStackTraceString(e));
            } catch (InterruptedException e) {
                Log.e("myapp", "Interrupted: " + Log.getStackTraceString(e));
            } catch(Exception e) {
                Log.e("myapp", "General exception" + Log.getStackTraceString(e));
            }

            if(mainActivity.hasConnection) {
                Log.i("myapp", "We have established a connection successfully!");
            } else {
                Log.e("myapp", "We have failed to establish a connection.");
            }
            return new Boolean(true);
        }
    }

    // Do periodic publish and subscribe to/from the server.
    private class AsyncPubSubXMPP extends AsyncTask<Void, Void, HashMap<String, String>>
    {
        private MainActivity mainActivity = null;
        private ArrayList<String> payloads = null;
        public AsyncPubSubXMPP(MainActivity ma, ArrayList<String> payloads) {
            super();
            mainActivity = ma;
            this.payloads = payloads;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // TODO process dialog maybe
        }

        @Override
        protected HashMap<String, String> doInBackground(Void... voids) {
            // Do nothing if no connection.
            if(!mainActivity.hasConnection) {
                return new HashMap<String, String>() {{
                    put("ERROR", "No connection");
                }};
            }
            // TODO Update doc about publish
            // TODO simplepayload constructor is deprecated
            // TODO spelling error in simple payload
            ArrayList<Item> items = null;
            try {
                // Publish to the test node.
                for(String payload : payloads) {
                    Log.e("myapp", "PUBBINGGajdgkljasdklgjalsdg " + payload);
                    mainActivity.testLeaf.publish(new PayloadItem<ExtensionElement>("test" + System.currentTimeMillis(),
                            new SimplePayload(payload)));
                }
                // Get the items from the remote node.
                items = new ArrayList<Item>(mainActivity.testLeaf.getItems());
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Log.i("myapp", "The size is " + items.size());
            for(int i = 0; i < items.size(); i++) {
                Log.i("myapp", items.toString());
            }

            HashMap<String, String> resultingMap = new HashMap<String, String>();
            for(Item i : items) {
                resultingMap.put(i.getId(), i.toXML().toString());
            }
            return resultingMap;
        }
    }
}