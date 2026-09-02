package com.decoupledx.reservation.resource.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.decoupledx.reservation.resource.domain.model.ResourceGroupId;
import com.decoupledx.reservation.resource.domain.model.ResourceStatus;
import com.decoupledx.reservation.resource.domain.model.ResourceType;
import com.decoupledx.reservation.venue.domain.model.VenueId;
import com.decoupledx.reservation.resource.domain.model.Resource;

class ResourceTest {

    private static final Instant NOW = Instant.parse("2026-08-29T10:00:00Z");

    private Resource newResource() {
        return Resource.create(ResourceGroupId.random(), VenueId.random(),
                "Field 1", "FIELD-01", ResourceType.FOOTBALL_FIELD, NOW);
    }

    @Test
    void createsActiveResource() {
        Resource resource = newResource();
        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
        assertThat(resource.isActive()).isTrue();
        assertThat(resource.getCode()).isEqualTo("FIELD-01");
    }

    @Test
    void deactivatesAndActivates() {
        Resource resource = newResource();
        resource.deactivate();
        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.INACTIVE);
        assertThat(resource.isActive()).isFalse();
        resource.activate();
        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.ACTIVE);
    }

    @Test
    void renames() {
        Resource resource = newResource();
        resource.rename("Pitch A");
        assertThat(resource.getName()).isEqualTo("Pitch A");
    }

    @Test
    void rejectsBlankName() {
        Resource resource = newResource();
        assertThatThrownBy(() -> resource.rename(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
