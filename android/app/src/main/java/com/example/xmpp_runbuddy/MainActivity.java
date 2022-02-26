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
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.FormNodeType;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.PublishModel;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.form.ConfigureForm;
import org.jivesoftware.smackx.pubsub.form.FillableConfigureForm;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.TextSingleFormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GeneratedPluginRegistrant.registerWith(new FlutterEngine(this));



        Log.i("myapp", "Setting up XMPP");
        new AsyncCaller(this).execute();

        Log.i("myapp", "Done setting up XMPP");
        new MethodChannel(getFlutterEngine().getDartExecutor().getBinaryMessenger(), CHANNEL)
            .setMethodCallHandler(new MethodChannel.MethodCallHandler() {
                @Override
                public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
                    if(methodCall.method.equals("getMessage")) {

                        result.success("Sneed!");
                    }
                }
            });
    }

    private class AsyncCaller extends AsyncTask<Void, Void, Void>
    {
        private MainActivity mainActivity = null;
        public AsyncCaller(MainActivity ma) {
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
                mainActivity.hasConnection = true;


                // TODO update the docs about fillable configure form.
                // also FormType

                LeafNode leaf = psManager.createNode("testNode");


                // TODO Update doc about publish
                // TODO simplepayload constructor is deprecated
                // TODO spelling error in simple payload
                leaf.publish(new PayloadItem<ExtensionElement>("test" + System.currentTimeMillis(),
                        new SimplePayload("\"<data xmlns='https://example.org'>This is the payload</data>\"")));

            } catch (XmppStringprepException e) {
                mainActivity.hasConnection = false;
            } catch (SmackException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XMPPException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch(Exception e ) {
                Log.d("myapp", Log.getStackTraceString(e));
            }

            return null;
        }
    }
}