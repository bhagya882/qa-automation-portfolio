package com.qaportfolio.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductPage(List<Product> products, int total, int skip, int limit) {
}
