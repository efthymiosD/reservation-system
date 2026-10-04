package com.decoupledx.reservation.content.adapter.web;

import com.decoupledx.reservation.content.adapter.api.ContentApi;
import com.decoupledx.reservation.content.adapter.api.SiteContentBlock;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

}
