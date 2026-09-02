package com.decoupledx.reservation.resource.adapter.in.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decoupledx.reservation.resource.domain.model.CreateResourceCommand;
import com.decoupledx.reservation.resource.domain.model.ResourceGroupInfo;
import com.decoupledx.reservation.resource.domain.model.ResourceGroupId;
import com.decoupledx.reservation.resource.domain.model.ResourceId;
import com.decoupledx.reservation.resource.domain.model.ResourceInfo;
import com.decoupledx.reservation.resource.domain.service.ResourceService;
import com.decoupledx.reservation.resource.domain.model.ResourceType;
import com.decoupledx.reservation.venue.domain.service.VenueService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
class ResourceAdminController {

    private final ResourceService resourceService;
    private final VenueService venueService;

    @GetMapping("/resources")
    List<ResourceResponse> listResources() {
        return resourceService.findResources(venueService.singleVenueId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/resource-groups")
    List<ResourceGroupResponse> listResourceGroups() {
        return resourceService.findResourceGroups(venueService.singleVenueId()).stream()
                .map(group -> new ResourceGroupResponse(
                        group.id().value(), group.name(), group.type().name()))
                .toList();
    }

    @PostMapping("/resources")
    @ResponseStatus(HttpStatus.CREATED)
    ResourceResponse create(@Valid @RequestBody CreateResourceRequest request) {
        CreateResourceCommand command = new CreateResourceCommand(
                venueService.singleVenueId(),
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

    record CreateResourceRequest(
            @NotNull UUID groupId,
            @NotBlank String name,
            @NotBlank String code,
            @NotBlank String type) {
    }

    record RenameRequest(@NotBlank String name) {
    }

    record ResourceResponse(
            UUID id,
            UUID groupId,
            String name,
            String code,
            String type,
            String status) {
    }

    record ResourceGroupResponse(UUID id, String name, String type) {
    }
}
