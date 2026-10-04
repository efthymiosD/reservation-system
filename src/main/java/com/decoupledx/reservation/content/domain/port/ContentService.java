package com.decoupledx.reservation.content.domain.port;

import com.decoupledx.reservation.content.adapter.api.SiteContentBlock;
import java.util.List;

/**
 * Inbound port for the content module: editable site-content blocks.
 */
public interface ContentService {

    List<SiteContentBlock> all();

    String get(String key);

    void update(String key, String body);
}
