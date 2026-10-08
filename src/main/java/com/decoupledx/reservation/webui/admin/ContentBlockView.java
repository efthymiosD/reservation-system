package com.decoupledx.reservation.webui.admin;

/**
 * One text block for one editor language: its storage key (master key for
 * English, prefixed for other languages) and the built-in default used as the
 * input placeholder when the admin leaves it empty.
 */
record ContentBlockView(String key, String storageKey, String label, String body, String defaultBody) {

    ContentBlockView withStorageKey(String storageKey) {
        return new ContentBlockView(key(), storageKey, label(), body(), defaultBody());
    }
}
