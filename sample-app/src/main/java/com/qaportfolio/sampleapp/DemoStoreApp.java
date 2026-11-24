package com.qaportfolio.sampleapp;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * "Demo Store" — a tiny e-commerce REST API that this repository OWNS, so it can legitimately be load-tested
 * (project 05) and security-tested (project 06).
 *
 * <p>Two modes:
 * <ul>
 *   <li><b>secure</b> (default): proper access control, security headers, escaped output, generic errors,
 *       login rate limiting.</li>
 *   <li><b>insecure</b> ({@code --insecure} or {@code APP_MODE=insecure}): seeded, documented vulnerabilities
 *       for the security lab — IDOR on orders, missing headers, reflected XSS on /search, stack traces in
 *       errors, no login throttling, session cookie without flags. NEVER deploy this mode publicly.</li>
 * </ul>
 *
 * <p>Endpoint {@code /api/reports/sales} contains a deliberate serialisation bottleneck for the perf project.
 */
public final class DemoStoreApp {

    private final boolean insecure;
    private final Store store = new Store();
    private final Sessions sessions = new Sessions();
    private final Metrics metrics = new Metrics();
    private final LoginThrottle throttle = new LoginThrottle(5, 60_000);

    public DemoStoreApp(boolean insecure) {
        this.insecure = insecure;
    }

    public static void main(String[] args) {
        boolean insecure = List.of(args).contains("--insecure")
                || "insecure".equalsIgnoreCase(System.getenv("APP_MODE"));
        int port = Integer.parseInt(Objects.requireNonNullElse(System.getenv("PORT"), "7070"));
        new DemoStoreApp(insecure).start(port);
    }

    public Javalin start(int port) {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.before(ctx -> ctx.attribute("t0", System.nanoTime()));
        app.after(this::afterRequest);

        app.get("/health", ctx -> ctx.json(Map.of("status", "UP", "mode", insecure ? "insecure" : "secure")));
        app.get("/metrics", ctx -> ctx.contentType("text/plain; version=0.0.4").result(metrics.prometheus()));

        app.post("/api/auth/login", this::login);
        app.post("/api/auth/logout", this::logout);

        app.get("/api/products", this::listProducts);
        app.get("/api/products/{id}", this::getProduct);

        app.get("/api/cart", ctx -> ctx.json(store.cart(requireUser(ctx).username())));
        app.post("/api/cart/items", this::addToCart);
        app.delete("/api/cart", ctx -> {
            store.clearCart(requireUser(ctx).username());
            ctx.status(HttpStatus.NO_CONTENT);
        });

        app.post("/api/orders", this::placeOrder);
        app.get("/api/orders", ctx -> ctx.json(store.ordersFor(requireUser(ctx).username())));
        app.get("/api/orders/{id}", this::getOrder);

        app.get("/api/admin/users", ctx -> {
            Sessions.User u = requireUser(ctx);
            if (!"ADMIN".equals(u.role())) {
                throw new ApiError(403, "Forbidden");
            }
            ctx.json(Store.USERS.keySet());
        });

        app.get("/api/reports/sales", ctx -> ctx.json(store.salesReport()));
        app.get("/search", this::searchPage);

        app.exception(ApiError.class, (e, ctx) -> ctx.status(e.status).json(Map.of("error", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            if (insecure) {
                // Seeded flaw: information disclosure via stack trace
                StringBuilder sb = new StringBuilder(e.toString());
                for (StackTraceElement el : e.getStackTrace()) {
                    sb.append("\n  at ").append(el);
                }
                ctx.json(Map.of("error", e.getMessage() == null ? "error" : e.getMessage(), "trace", sb.toString()));
            } else {
                ctx.json(Map.of("error", "Internal server error"));
            }
        });

        app.start(port);
        System.out.printf("Demo Store running on http://localhost:%d (mode=%s)%n", port, insecure ? "INSECURE" : "secure");
        return app;
    }

    // ---------------------------------------------------------------- handlers

    private void login(Context ctx) {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = parseBody(ctx);
        String username = str(body.get("username"));
        String password = str(body.get("password"));
        if (username.isBlank() || password.isBlank()) {
            throw new ApiError(400, "username and password are required");
        }
        String client = ctx.ip();
        if (!insecure && throttle.isBlocked(client + "|" + username)) {
            throw new ApiError(429, "Too many login attempts, try again later");
        }
        Store.Account acc = Store.USERS.get(username);
        if (acc == null || !acc.password().equals(password)) {
            throttle.recordFailure(client + "|" + username);
            // Same message for unknown user and wrong password (no user enumeration)
            throw new ApiError(401, "Invalid credentials");
        }
        throttle.reset(client + "|" + username);
        String token = sessions.create(username, acc.role());
        String cookie = insecure
                ? "SESSION=" + token + "; Path=/"
                : "SESSION=" + token + "; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=1800";
        ctx.header("Set-Cookie", cookie);
        ctx.json(Map.of("token", token, "username", username, "role", acc.role(), "expiresInSeconds", 1800));
    }

    private void logout(Context ctx) {
        String token = bearer(ctx);
        if (token != null) {
            sessions.invalidate(token);
        }
        ctx.status(HttpStatus.NO_CONTENT);
    }

    private void listProducts(Context ctx) {
        String q = ctx.queryParam("search");
        int page = parseIntParam(ctx.queryParam("page"), 1);
        int size = parseIntParam(ctx.queryParam("size"), 10);
        if (page < 1 || size < 1 || size > 100) {
            throw new ApiError(400, "page must be >= 1 and size between 1 and 100");
        }
        List<Store.Product> all = store.search(q);
        int from = Math.min((page - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total", all.size());
        res.put("page", page);
        res.put("size", size);
        res.put("items", all.subList(from, to));
        ctx.json(res);
    }

    private void getProduct(Context ctx) {
        int id = parseIntParam(ctx.pathParam("id"), -1);
        Store.Product p = store.product(id);
        if (p == null) {
            throw new ApiError(404, "Product not found");
        }
        ctx.json(p);
    }

    private void addToCart(Context ctx) {
        Sessions.User u = requireUser(ctx);
        Map<String, Object> body = parseBody(ctx);
        int productId = parseIntParam(str(body.get("productId")), -1);
        int qty = parseIntParam(str(body.get("quantity")), 1);
        if (qty < 1 || qty > 20) {
            throw new ApiError(400, "quantity must be between 1 and 20");
        }
        if (store.product(productId) == null) {
            throw new ApiError(404, "Product not found");
        }
        ctx.status(201).json(store.addToCart(u.username(), productId, qty));
    }

    private void placeOrder(Context ctx) {
        Sessions.User u = requireUser(ctx);
        Store.Order order = store.checkout(u.username());
        if (order == null) {
            throw new ApiError(409, "Cart is empty");
        }
        ctx.status(201).json(order);
    }

    private void getOrder(Context ctx) {
        Sessions.User u = requireUser(ctx);
        Store.Order o = store.order(parseIntParam(ctx.pathParam("id"), -1));
        boolean allowed = o != null && (insecure || o.username().equals(u.username()) || "ADMIN".equals(u.role()));
        if (!allowed) {
            // 404 rather than 403 so order IDs of other users can't be probed
            throw new ApiError(404, "Order not found");
        }
        ctx.json(o);
    }

    private void searchPage(Context ctx) {
        String q = Objects.requireNonNullElse(ctx.queryParam("q"), "");
        String shown = insecure ? q : Html.escape(q); // Seeded flaw in insecure mode: reflected XSS
        StringBuilder rows = new StringBuilder();
        for (Store.Product p : store.search(q)) {
            rows.append("<li>").append(Html.escape(p.name())).append(" &mdash; $").append(p.price()).append("</li>");
        }
        ctx.html("""
                <!doctype html><html lang="en"><head><meta charset="utf-8"><title>Search - Demo Store</title></head>
                <body><main><h1>Search results</h1>
                <form action="/search"><label for="q">Search</label> <input id="q" name="q"> <button>Go</button></form>
                <p>Results for: %s</p><ul>%s</ul></main></body></html>""".formatted(shown, rows));
    }

    // ---------------------------------------------------------------- cross-cutting

    private void afterRequest(Context ctx) {
        if (!insecure) {
            ctx.header("X-Content-Type-Options", "nosniff");
            ctx.header("X-Frame-Options", "DENY");
            ctx.header("Referrer-Policy", "no-referrer");
            ctx.header("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'; form-action 'self'");
            ctx.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            ctx.header("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
            if (ctx.path().startsWith("/api/")) {
                ctx.header("Cache-Control", "no-store");
            }
        } else {
            ctx.header("Server", "DemoStore/0.1 (Javalin; Java " + Runtime.version() + ")");
        }
        Long t0 = ctx.attribute("t0");
        if (t0 != null) {
            String route;
            try {
                route = ctx.endpointHandlerPath();
            } catch (RuntimeException e) {
                route = "unmatched";
            }
            metrics.record(ctx.method().name(), route, ctx.statusCode(), System.nanoTime() - t0);
        }
    }

    private Sessions.User requireUser(Context ctx) {
        String token = bearer(ctx);
        Sessions.User u = token == null ? null : sessions.find(token);
        if (u == null) {
            throw new ApiError(401, "Authentication required");
        }
        return u;
    }

    private static String bearer(Context ctx) {
        String h = ctx.header("Authorization");
        if (h != null && h.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return h.substring(7).trim();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseBody(Context ctx) {
        try {
            Map<String, Object> m = ctx.bodyAsClass(Map.class);
            return m == null ? Map.of() : m;
        } catch (RuntimeException e) {
            throw new ApiError(400, "Malformed JSON body");
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static int parseIntParam(String s, int fallback) {
        if (s == null || s.isBlank()) {
            return fallback;
        }
        try {
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new ApiError(400, "Invalid number: " + Html.escape(s));
        }
    }

    static final class ApiError extends RuntimeException {
        final int status;

        ApiError(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    static final class Html {
        static String escape(String s) {
            StringBuilder sb = new StringBuilder(s.length());
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '<' -> sb.append("&lt;");
                    case '>' -> sb.append("&gt;");
                    case '&' -> sb.append("&amp;");
                    case '"' -> sb.append("&quot;");
                    case '\'' -> sb.append("&#x27;");
                    default -> sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
