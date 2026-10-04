package com.decoupledx.reservation.webui.reserve;

import java.math.BigDecimal;

record MoneyView(BigDecimal amount, String currency) {
}
