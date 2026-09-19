package com.decoupledx.reservation.webui;

import java.util.Map;
import java.util.stream.Collectors;

import com.decoupledx.reservation.content.adapter.api.SiteContentBlock;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.decoupledx.reservation.content.adapter.api.ContentApi;

import lombok.RequiredArgsConstructor;

/**
 * Exposes every editable site-content block (key -> body) to all views so the
 * public pages render admin-configured text. Views fall back to a bundled
 * default when a block is empty.
 */
@ControllerAdvice
@RequiredArgsConstructor
class ContentAdvice {

    private final ContentApi contentApi;

    @ModelAttribute("siteContent")
    Map<String, String> content() {
        return contentApi.all().stream()
                .collect(Collectors.toMap(SiteContentBlock::key, SiteContentBlock::body));
    }
}