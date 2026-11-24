package com.qaportfolio.saucedemo.model;

/** A SauceDemo public demo account (see testdata/users.json). */
public record User(String key, String username, String password, boolean canLogin, String expectedError, String notes) {
    @Override
    public String toString() {
        return username;
    }
}
