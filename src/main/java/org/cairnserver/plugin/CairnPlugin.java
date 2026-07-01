package org.cairnserver.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CairnPlugin {

    private boolean enabled = false;

    public abstract void onEnable();
    public abstract void onDisable();
    public abstract String getName();

    public final Logger getLogger() {
        return LoggerFactory.getLogger("plugin." + getName());
    }

    final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}