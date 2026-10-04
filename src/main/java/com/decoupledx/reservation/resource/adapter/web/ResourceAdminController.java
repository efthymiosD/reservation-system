package com.decoupledx.reservation.resource.adapter.web;

import com.decoupledx.reservation.resource.adapter.api.*;
import com.decoupledx.reservation.resource.domain.port.ResourceService;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class ResourceAdminController {

    private final ResourceService resourceService;
    private final VenueApi venueService;

    @GetMapping("/resources")
    List<ResourceResponse> listResources() {
        return resourceService.findResources(VenueId.of(venueService.singleVenueId())).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/resource-groups")
    List<ResourceGroupResponse> listResourceGroups() {
        return resourceService.findResourceGroups(VenueId.of(venueService.singleVenueId())).stream()
                .map(group -> new ResourceGroupResponse(
                        group.id().value(), group.name(), group.type().name()))
                .toList();
    }

    @PostMapping("/resources")
    @ResponseStatus(HttpStatus.CREATED)
    ResourceResponse create(@Valid @RequestBody CreateResourceRequest request) {
        CreateResourceCommand command = new CreateResourceCommand(
                VenueId.of(venueService.singleVenueId()),
                ResourceGroupId.of(request.groupId()),
                request.name(),
                request.code(),
                ResourceType.valueOf(request.type()));
        return toResponse(resourceService.createResource(command));
    }

    @PostMapping("/resources/{resourceId}/activate")
    ResourceResponse activate(@PathVariable UUID resourceId) {
        return toResponse(resourceService.activate(ResourceId.of(resourceId)));
    }

    @PostMapping("/resources/{resourceId}/deactivate")
    ResourceResponse deactivate(@PathVariable UUID resourceId) {
        return toResponse(resourceService.deactivate(ResourceId.of(resourceId)));
    }

    @PatchMapping("/resources/{resourceId}")
    ResourceResponse rename(@PathVariable UUID resourceId, @Valid @RequestBody RenameRequest request) {
        return toResponse(resourceService.rename(ResourceId.of(resourceId), request.name()));
    }

    private ResourceResponse toResponse(ResourceInfo resource) {
        return new ResourceResponse(
                resource.id().value(),
                resource.groupId().value(),
                resource.name(),
                resource.code(),
                resource.type().name(),
                resource.status().name());
    }

}
