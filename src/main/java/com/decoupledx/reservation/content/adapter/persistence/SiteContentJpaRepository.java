package com.decoupledx.reservation.content.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SiteContentJpaRepository extends JpaRepository<SiteContentEntity, String> {
}