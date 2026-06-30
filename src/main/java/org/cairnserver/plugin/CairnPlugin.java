package org.cairnserver.plugin;

public interface CairnPlugin {
    void onEnable();
    void onDisable();
    String getName();
}
