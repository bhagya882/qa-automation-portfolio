# Test Scenarios – Project 01

| Scenario | Description | Test cases |
|---|---|---|
| TS-01 Registration | A new visitor can create an account. Invalid or duplicate input is rejected. The account can be deleted | TC-REG-01 … 06 |
| TS-02 Login & session | Valid users log in. Invalid, empty or locked credentials are rejected with a generic message. Protected pages need a session. Logout ends it | TC-LOG-01 … 14 |
| TS-03 Search & sort | A shopper finds products by keyword regardless of case. An empty result is communicated. Input is encoded. Lists can be sorted | TC-SRCH-01 … 08 |
| TS-04 Product details | Details match the listing. A product can be added or removed from its page. Unknown products give a proper 404 | TC-PD-01 … 06 |
| TS-05 Cart | Items can be added, removed, kept across navigation/reload and reset. The empty cart state is clear | TC-CART-01 … 10 |
| TS-06 Checkout | Shipping info is validated. Totals are correct (items + 8% tax). Cancel paths keep the cart. Checkout needs login and items | TC-CO-01 … 09 |
| TS-07 Order history | After purchase the shopper sees a confirmation, can get an invoice and can see past orders | TC-OH-01 … 03 |
| TS-08 Front-end hygiene | No console errors, failed requests or broken images on key pages | TC-DT-01 … 04 |
