package com.decoupledx.reservation.webui.reserve;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

record SlotSelection(LocalDate date, LocalTime start, int duration,
                     List<LocalTime> timeOptions) {
}
