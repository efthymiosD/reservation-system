package com.decoupledx.reservation.content.adapter.in.web;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves uploaded site images (e.g. the hero photo) from the configured upload
 * directory as /uploads/**. The directory is created on first upload by the
 * admin panel.
 */
@Configuration
class SiteContentWebConfig implements WebMvcConfigurer {

    private final String uploadDir;

    SiteContentWebConfig(@Value("${app.content.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + Path.of(uploadDir).toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}