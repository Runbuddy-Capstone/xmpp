package com.example.xmpp_runbuddy;

import android.util.Log;

import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;

class ItemEventCoordinator  implements ItemEventListener {
    @Override
    public void handlePublishedItems(ItemPublishEvent items) {
        for(Object i : items.getItems()) {
            Log.d("myapp", "handlePublishedItems: " + i.toString());
        }
    }
}