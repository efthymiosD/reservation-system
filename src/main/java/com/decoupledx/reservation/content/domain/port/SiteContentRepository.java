package com.decoupledx.reservation.content.domain.port;

import com.decoupledx.reservation.content.adapter.persistence.SiteTextDataValue;
import java.util.List;
import java.util.Optional;

public interface SiteContentRepository {

    Optional<SiteTextDataValue> findByKey(String key);

    List<SiteTextDataValue> findAll();

    SiteTextDataValue save(SiteTextDataValue text);
}
