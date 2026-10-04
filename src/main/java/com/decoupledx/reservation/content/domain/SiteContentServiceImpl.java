package com.decoupledx.reservation.content.domain;

import com.decoupledx.reservation.content.adapter.api.SiteContentBlock;
import com.decoupledx.reservation.content.adapter.persistence.SiteTextDataValue;
import com.decoupledx.reservation.content.domain.port.ContentService;
import com.decoupledx.reservation.content.domain.port.SiteContentRepository;
import com.decoupledx.reservation.shared.TransactionRunner;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class SiteContentServiceImpl implements ContentService {

    private final SiteContentRepository siteContentRepository;
    private final TransactionRunner tx;
    private final Clock clock;

    @Override
    public List<SiteContentBlock> all() {
        return siteContentRepository.findAll().stream()
                .map(data -> new SiteContentBlock(data.key(), data.body()))
                .toList();
    }

    @Override
    public String get(String key) {
        return siteContentRepository.findByKey(key)
                .map(SiteTextDataValue::body)
                .orElse("");
    }

    @Override
    public void update(String key, String body) {
        tx.run(() -> {
            SiteText text = siteContentRepository.findByKey(key)
                    .map(data -> SiteText.reconstitute(data.key(), data.body(), data.updatedAt()))
                    .map(t -> {
                        t.updateBody(body, clock.instant());
                        return t;
                    })
                    .orElseGet(() -> SiteText.create(key, body, clock.instant()));
            siteContentRepository.save(text.toDataValue());
        });
    }
}
