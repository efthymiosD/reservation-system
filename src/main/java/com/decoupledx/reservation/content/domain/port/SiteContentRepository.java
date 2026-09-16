package com.decoupledx.reservation.content.domain.port;

import java.util.List;
import java.util.Optional;

import com.decoupledx.reservation.content.domain.model.SiteText;

public interface SiteContentRepository {

    Optional<SiteText> findByKey(String key);

    List<SiteText> findAll();

    SiteText save(SiteText text);
}