package com.qaportfolio.saucedemo.model;

import java.math.BigDecimal;

/** Product as shown on a listing/cart row. */
public record Product(String name, String description, BigDecimal price) {
}
