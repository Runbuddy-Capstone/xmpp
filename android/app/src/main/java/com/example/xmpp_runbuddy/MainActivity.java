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
        new AsyncInitXMPP(this).execute();

        MainActivity ma = this;
        new MethodChannel(getFlutterEngine().getDartExecutor().getBinaryMessenger(), CHANNEL)
            .setMethodCallHandler(new MethodChannel.MethodCallHandler() {
                @Override
                public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
                    if(methodCall.method.equals("getMessage")) {

                        new AsyncPubSubXMPP(ma).execute();

                        result.success("Sneed!");
                    }
                }
            });
    }

    // Initialize the publish and subscribe connection. MUST be done asynchronously.
    private class AsyncInitXMPP extends AsyncTask<Void, Void, Void>
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
        protected Void doInBackground(Void... voids) {
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
                    return null;
                }

                Log.d("myapp", "waiting for subs...");

                // TODO Update doc about publish
                // TODO simplepayload constructor is deprecated
                // TODO spelling error in simple payload
                mainActivity.testLeaf.publish(new PayloadItem<ExtensionElement>("test" + System.currentTimeMillis(),
                        new SimplePayload(String.format("<data xmlns='https://example.org'>RB Payload%d</data>", System.currentTimeMillis()))));

                Log.d("myapp", "waiting for subs...");
                mainActivity.testLeaf.addItemEventListener(new ItemEventCoordinator());
                Log.d("myapp", "waiting for subs...");
                Subscription subs = mainActivity.testLeaf.subscribe(JidCreate.from("ryan@ryanmj.xyz/testNode"));
                Log.d("myapp", "waiting for subs...");

                // Log deletion items.
                mainActivity.testLeaf.addItemDeleteListener(new ItemDeleteCoordinator(mainActivity.testLeaf.getId()));

                ArrayList<Item> items = new ArrayList<Item>(mainActivity.testLeaf.getItems());

                Log.i("myapp", "The size is " + items.size());
                for(int i = 0; i < items.size(); i++) {
                    Log.i("myapp", items.toString());
                }

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
            return null;
        }
    }

    // Do periodic publish and subscribe to/from the server.
    private class AsyncPubSubXMPP extends AsyncTask<Void, Void, Void>
    {
        private MainActivity mainActivity = null;
        public AsyncPubSubXMPP(MainActivity ma) {
            super();
            mainActivity = ma;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // TODO process dialog maybe
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Do nothing if no connection.
            if(!mainActivity.hasConnection) {
                return null;
            }
            return null;
        }
    }
}