package com.decoupledx.reservation.content.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decoupledx.reservation.content.adapter.api.ContentApi;
import com.decoupledx.reservation.content.adapter.api.SiteContentBlock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/content")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class ContentAdminController {

    private final ContentApi contentApi;

    @GetMapping
    List<SiteContentBlock> all() {
        return contentApi.all();
    }

    @PutMapping("/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable String key, @Valid @RequestBody UpdateBodyRequest request) {
        contentApi.update(key, request.body());
    }

    record UpdateBodyRequest(@NotNull String body) {
    }
}