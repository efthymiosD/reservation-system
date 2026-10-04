package com.decoupledx.reservation.content.adapter.api;

import com.decoupledx.reservation.content.domain.port.ContentService;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Module external-facing facade implementing {@link ContentApi} on top of the
 * content inbound port. The only bean other modules use for this module.
 */
@RequiredArgsConstructor
class ContentApiImpl implements ContentApi {

    private final ContentService contentService;

    @Override
    public List<SiteContentBlock> all() {
        return contentService.all();
    }

    @Override
    public String get(String key) {
        return contentService.get(key);
    }

    @Override
    public void update(String key, String body) {
        contentService.update(key, body);
    }
}
