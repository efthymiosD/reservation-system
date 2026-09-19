package com.decoupledx.reservation.content.domain.service;

import java.time.Clock;
import java.util.List;

import com.decoupledx.reservation.content.adapter.api.ContentApi;
import com.decoupledx.reservation.content.adapter.api.SiteContentBlock;
import com.decoupledx.reservation.content.domain.model.SiteText;
import com.decoupledx.reservation.content.domain.port.SiteContentRepository;
import com.decoupledx.reservation.shared.TransactionRunner;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SiteContentService implements ContentApi {

    private final SiteContentRepository siteContentRepository;
    private final TransactionRunner tx;
    private final Clock clock;

    @Override
    public List<SiteContentBlock> all() {
        return siteContentRepository.findAll().stream()
                .map(text -> new SiteContentBlock(text.getKey(), text.getBody()))
                .toList();
    }

    @Override
    public String get(String key) {
        return siteContentRepository.findByKey(key).map(SiteText::getBody).orElse("");
    }

    @Override
    public void update(String key, String body) {
        tx.run(() -> {
            SiteText text = siteContentRepository.findByKey(key)
                    .map(existing -> {
                        existing.updateBody(body, clock.instant());
                        return existing;
                    })
                    .orElseGet(() -> SiteText.create(key, body, clock.instant()));
            siteContentRepository.save(text);
        });
    }
}