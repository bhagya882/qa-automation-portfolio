package com.qaportfolio.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Product(Integer id, String title, String category, BigDecimal price, Integer stock,
                      Double rating, List<String> tags) {
}
