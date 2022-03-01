package com.example.xmpp_runbuddy;

import android.util.Log;

import org.jivesoftware.smackx.pubsub.ItemDeleteEvent;
import org.jivesoftware.smackx.pubsub.listener.ItemDeleteListener;

// Class that logs item deletion events.
public class ItemDeleteCoordinator implements ItemDeleteListener {
    // Name of the leafNode this coordinator belongs to.
    private String leafName = null;
    public ItemDeleteCoordinator(String leafName) {
        super();
        this.leafName = leafName;
    }
    @Override
    public void handleDeletedItems(ItemDeleteEvent items) {
        Log.i("myapp", String.format("Deleting %d items in %s: %s", items.getItemIds().size(),
               this.leafName, items.toString()));
    }

    @Override
    public void handlePurge() {
        Log.i("myapp", String.format("Deleting all items from the node %s.", this.leafName));
    }
}
