package com.decoupledx.reservation.content.adapter.persistence;

import com.decoupledx.reservation.content.domain.port.SiteContentRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SiteContentPersistenceAdapter implements SiteContentRepository {

    private final SiteContentJpaRepository siteContent;

    @Override
    public Optional<SiteTextDataValue> findByKey(String key) {
        return siteContent.findById(key).map(this::toDomain);
    }

    @Override
    public List<SiteTextDataValue> findAll() {
        return siteContent.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public SiteTextDataValue save(SiteTextDataValue text) {
        siteContent.findById(text.key())
                .ifPresentOrElse(
                        entity -> entity.updateFrom(text.body(), text.updatedAt()),
                        () -> siteContent.saveAndFlush(toEntity(text)));
        siteContent.flush();
        return text;
    }

    private SiteContentEntity toEntity(SiteTextDataValue text) {
        return new SiteContentEntity(text.key(), text.body(), text.updatedAt());
    }

    private SiteTextDataValue toDomain(SiteContentEntity entity) {
        return new SiteTextDataValue(entity.getKey(), entity.getBody(), entity.getUpdatedAt());
    }
}
