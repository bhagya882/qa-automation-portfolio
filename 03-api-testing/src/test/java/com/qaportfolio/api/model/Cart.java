package com.qaportfolio.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Cart(Integer id, List<Line> products, BigDecimal total, BigDecimal discountedTotal, Integer userId,
                   Integer totalProducts, Integer totalQuantity) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Line(Integer id, String title, BigDecimal price, Integer quantity, BigDecimal total) {
    }
}
