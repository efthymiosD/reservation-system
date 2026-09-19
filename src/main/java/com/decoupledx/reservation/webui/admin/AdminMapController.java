package com.decoupledx.reservation.webui.admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.decoupledx.reservation.content.adapter.api.ContentApi;
import com.decoupledx.reservation.resource.adapter.api.CreateResourceCommand;
import com.decoupledx.reservation.resource.adapter.api.ResourceApi;
import com.decoupledx.reservation.resource.adapter.api.ResourceInfo;
import com.decoupledx.reservation.venue.adapter.api.VenueApi;
import com.decoupledx.reservation.venue.adapter.api.VenueId;
import com.decoupledx.reservation.webui.reserve.VenueLayout;
import com.decoupledx.reservation.webui.reserve.VenueLayoutLoader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Admin field-layout page (ROLE_ADMIN gated at the security chain): a graphical
 * drag-and-drop grid editor for the reservation-page venue map. Field positions
 * and sizes are persisted as the {@code venue.map.layout} content block, and
 * field names/activity live in the resource module — all edited from here. The
 * form posts per-field inputs so it also works without JavaScript; the editor
 * JavaScript keeps those inputs in sync while fields are dragged.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
class AdminMapController {

    private final VenueApi venueApi;
    private final ContentApi contentApi;
    private final ResourceApi resourceApi;
    private final VenueLayoutLoader layoutLoader;
    private final ObjectMapper objectMapper;

    @GetMapping("/admin/map")
    String map(Model model) {
        UUID venueId = venueApi.singleVenueId();
        VenueLayout layout = layoutLoader.get();
        Map<UUID, VenueLayout.Placement> placements = layout.placements().stream()
                .collect(Collectors.toMap(VenueLayout.Placement::resourceId, placement -> placement));

        List<AdminMapModel.MapFieldView> fields = resourceApi.findResources(venueId).stream()
                .map(resource -> fieldView(resource, placements))
                .toList();

        model.addAttribute("page", new AdminMapModel(
                layout.canvasWidth(),
                layout.canvasHeight(),
                fields,
                venueId));
        return "admin/map";
    }

    @PostMapping("/admin/map/layout")
    String saveLayout(@RequestParam Map<String, String> params, RedirectAttributes redirect) {
        try {
            UUID venueId = venueApi.singleVenueId();
            ObjectNode layout = currentLayout();
            int canvasWidth = layout.path("canvasWidth").asInt(1200);
            int canvasHeight = layout.path("canvasHeight").asInt(400);

            for (ResourceInfo resource : resourceApi.findResources(venueId)) {
                String id = resource.id().value().toString();
                String name = params.get("name-" + id);
                if (name != null && !name.isBlank()) {
                    resourceApi.rename(resource.id().value(), name.trim());
                }
                if (params.containsKey("x-" + id) && params.containsKey("y-" + id)
                        && params.containsKey("width-" + id) && params.containsKey("height-" + id)) {
                    int x = parseInt(params.get("x-" + id), "position");
                    int y = parseInt(params.get("y-" + id), "position");
                    int width = parseInt(params.get("width-" + id), "size");
                    int height = parseInt(params.get("height-" + id), "size");
                    if (x < 0 || y < 0 || width < 1 || height < 1) {
                        throw new IllegalArgumentException(
                                "Every field needs a valid position (size at least 1, "
                                        + "position not negative).");
                    }
                    upsertPlacement(layout, resource.id().value(), x, y, width, height);
                    canvasWidth = Math.max(canvasWidth, x + width);
                    canvasHeight = Math.max(canvasHeight, y + height);
                }
            }

            layout.put("canvasWidth", canvasWidth);
            layout.put("canvasHeight", canvasHeight);
            saveLayout(layout);
            redirect.addFlashAttribute("message", "Field layout saved.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/map";
    }

    @PostMapping("/admin/map/fields")
    String addField(@RequestParam String name, RedirectAttributes redirect) {
        try {
            UUID venueId = venueApi.singleVenueId();
            List<ResourceInfo> resources = resourceApi.findResources(venueId);
            if (resources.isEmpty()) {
                throw new IllegalArgumentException("Cannot add a field before a field group exists.");
            }
            ResourceInfo template = resources.getFirst();
            String code = nextFieldCode(resources);
            resourceApi.createResource(new CreateResourceCommand(
                    VenueId.of(venueId), template.groupId(), name.trim(), code, template.type()));

            ResourceInfo created = resourceApi.findResources(venueId).stream()
                    .filter(resource -> resource.code().equals(code))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("The new field could not be found."));

            ObjectNode layout = currentLayout();
            int x = layout.path("canvasWidth").asInt(1200);
            int height = layout.path("canvasHeight").asInt(400);
            upsertPlacement(layout, created.id().value(), x, 0, 200, height);
            saveLayout(layout);
            redirect.addFlashAttribute("message", "Field added — drag it into place.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/map";
    }

    @PostMapping("/admin/map/field/{resourceId}/activate")
    String activateField(@PathVariable UUID resourceId, RedirectAttributes redirect) {
        resourceApi.activate(resourceId);
        redirect.addFlashAttribute("message", "Field activated.");
        return "redirect:/admin/map";
    }

    @PostMapping("/admin/map/field/{resourceId}/deactivate")
    String deactivateField(@PathVariable UUID resourceId, RedirectAttributes redirect) {
        resourceApi.deactivate(resourceId);
        redirect.addFlashAttribute("message", "Field deactivated.");
        return "redirect:/admin/map";
    }

    private AdminMapModel.MapFieldView fieldView(ResourceInfo resource,
            Map<UUID, VenueLayout.Placement> placements) {
        VenueLayout.Placement placement = placements.get(resource.id().value());
        return new AdminMapModel.MapFieldView(
                resource.id().value(),
                resource.name(),
                resource.code(),
                resource.isActive(),
                placement == null ? 0 : placement.x(),
                placement == null ? 0 : placement.y(),
                placement == null ? 200 : placement.width(),
                placement == null ? 400 : placement.height());
    }

    private int parseInt(String value, String kind) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Every field needs a valid " + kind + " (numeric).");
        }
    }

    private String nextFieldCode(List<ResourceInfo> resources) {
        int max = resources.stream()
                .map(ResourceInfo::code)
                .filter(code -> code != null && code.matches("FIELD-\\d+"))
                .mapToInt(code -> Integer.parseInt(code.substring(6)))
                .max()
                .orElse(0);
        return String.format("FIELD-%02d", max + 1);
    }

    private void upsertPlacement(ObjectNode layout, UUID resourceId,
                                 int x, int y, int width, int height) {
        ArrayNode placements = objectMapper.createArrayNode();
        for (JsonNode placement : layout.path("placements")) {
            if (!placement.path("resourceId").asString().equals(resourceId.toString())) {
                placements.add(placement);
            }
        }
        placements.add(objectMapper.createObjectNode()
                .put("resourceId", resourceId.toString())
                .put("x", x)
                .put("y", y)
                .put("width", width)
                .put("height", height));
        layout.set("placements", placements);
    }

    private ObjectNode currentLayout() {
        ObjectNode fromContent = parseLayoutJson(contentApi.get(VenueLayoutLoader.LAYOUT_KEY));
        if (fromContent != null) {
            return fromContent;
        }
        try (InputStream in = new ClassPathResource("webui/venue-layout.json").getInputStream()) {
            return (ObjectNode) objectMapper.readTree(in);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot load webui/venue-layout.json", exception);
        }
    }

    private ObjectNode parseLayoutJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return node instanceof ObjectNode objectNode ? objectNode : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private void saveLayout(ObjectNode layout) {
        contentApi.update(VenueLayoutLoader.LAYOUT_KEY, layout.toString());
    }
}