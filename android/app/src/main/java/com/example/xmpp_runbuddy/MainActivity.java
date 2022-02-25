package com.example.xmpp_runbuddy;

import android.os.Bundle;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.android.AndroidSmackInitializer;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.PublishModel;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.form.ConfigureForm;
import org.jivesoftware.smackx.pubsub.form.FillableConfigureForm;
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

        System.out.println("Setting up XMPP");
        AndroidSmackInitializer.initialize(getApplicationContext());

        try {
            xmppConnection = new XMPPTCPConnection("ryan", "IAmErr0r", "ryanmj.xyz");
            xmppConnection.connect().login();
            psManager = PubSubManager.getInstanceFor(xmppConnection);
            hasConnection = true;


            // TODO update the docs about fillable configure form.
            // also FormType
            FillableConfigureForm form = (new ConfigureForm(DataForm.builder().build())).getFillableForm();
            form.setAccessModel(AccessModel.open);
            form.setDeliverPayloads(false);
            form.setNotifyRetract(true);
            form.setPersistentItems(true);
            form.setPublishModel(PublishModel.open);
            LeafNode leaf = psManager.createNode("testNode");
            leaf.sendConfigurationForm(form);


            // TODO Update doc about publish
            // TODO simplepayload constructor is deprecated
            // TODO spelling error in simple payload
            leaf.publish(new PayloadItem<ExtensionElement>("test" + System.currentTimeMillis(),
                    new SimplePayload("book", "pubsub:test:book", "Sneed")));

        } catch (XmppStringprepException e) {
            hasConnection = false;
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

        System.out.println("Done setting up XMPP");
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
}
