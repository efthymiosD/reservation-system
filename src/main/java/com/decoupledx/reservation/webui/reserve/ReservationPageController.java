package com.decoupledx.reservation.webui.reserve;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

/**
 * The reservation page: date / start time / duration controls, the SVG venue map
 * with per-resource availability states, and the backend-computed slot price.
 * The page is public (browse before login); reserving requires authentication.
 */
@Controller
@RequiredArgsConstructor
class ReservationPageController {

    private final ReservationPageModelFactory pageFactory;

    @GetMapping("/reserve")
    String reserve(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam(required = false) Integer durationMinutes,
            Model model) {
        model.addAttribute("page", pageFactory.build(date, start, durationMinutes));
        return "reserve";
    }
}
