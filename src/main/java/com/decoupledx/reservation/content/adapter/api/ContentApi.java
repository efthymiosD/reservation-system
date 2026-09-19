package com.decoupledx.reservation.content.adapter.api;

import java.util.List;

/**
 * Module API of the content module. This is the only type other modules may
 * depend on; the implementation stays module-internal. Carries the editable
 * text blocks for the public pages (home, about, contact) and the hero photo.
 */
public interface ContentApi {

    List<SiteContentBlock> all();

    /**
     * Resolves a block by key, or an empty string when the key is not set.
     * Views typically substitute a bundled default when empty.
     */
    String get(String key);

    void update(String key, String body);
}