package com.qaportfolio.api.model;

import java.util.List;

public record CartRequest(Integer userId, Boolean merge, List<Item> products) {

    public record Item(int id, int quantity) {
    }

    public static CartRequest of(int userId, Item... items) {
        return new CartRequest(userId, null, List.of(items));
    }
}
