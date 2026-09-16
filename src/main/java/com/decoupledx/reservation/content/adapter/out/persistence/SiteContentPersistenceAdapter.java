package com.decoupledx.reservation.content.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.decoupledx.reservation.content.domain.model.SiteText;
import com.decoupledx.reservation.content.domain.port.SiteContentRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SiteContentPersistenceAdapter implements SiteContentRepository {

    private final SiteContentJpaRepository siteContent;

    @Override
    public Optional<SiteText> findByKey(String key) {
        return siteContent.findById(key).map(this::toDomain);
    }

    @Override
    public List<SiteText> findAll() {
        return siteContent.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public SiteText save(SiteText text) {
        siteContent.findById(text.getKey())
                .ifPresentOrElse(
                        entity -> entity.updateFrom(text.getBody(), text.getUpdatedAt()),
                        () -> siteContent.saveAndFlush(toEntity(text)));
        siteContent.flush();
        return text;
    }

    private SiteContentEntity toEntity(SiteText text) {
        return new SiteContentEntity(text.getKey(), text.getBody(), text.getUpdatedAt());
    }

    private SiteText toDomain(SiteContentEntity entity) {
        return SiteText.reconstitute(entity.getKey(), entity.getBody(), entity.getUpdatedAt());
    }
}