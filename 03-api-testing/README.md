# Project 03 — REST API Testing and Automated Regression

Tests positive and negative API paths, status codes, response schemas, authentication, validation and error handling
of an e-commerce REST API. The regression runs two ways:

* **Postman + Newman** collection, with JUnit and HTML reports
* **REST Assured + TestNG** (Java), with JSON Schema contracts and Allure reporting

Both run in CI on every pull request (`.github/workflows/pr-quality-gate.yml` → job `api`).

**Target:** [DummyJSON](https://dummyjson.com/docs), a public fake REST API published for testing and
prototyping. Write calls are simulated, so tests leave no data behind. The demo login (`emilys`) is published in
DummyJSON's docs. It isn't real personal data, and tokens are fetched at runtime and never stored.

## Tools
Java 21+ · Maven (wrapper included) · REST Assured 5.5.7 · TestNG 7.11 · json-schema-validator · AssertJ ·
Allure 2.35 (+ allure-rest-assured) · Postman collection v2.1 · Newman 6.2.1 + newman-reporter-htmlextra · Node 20.

## Layout
```
03-api-testing/
├── postman/
│   ├── DemoShop-API.postman_collection.json   # folders: Auth, Products, Carts, Users, Negative & validation,
│   │                                          #          Data-driven products, Known defects
│   ├── dummyjson.postman_environment.json     # baseUrl, demo creds, runtime token vars (empty)
│   └── data/products.csv                      # iteration data for the data-driven folder
├── package.json                               # npm scripts wrapping Newman
├── scripts/collect-evidence.js                # copies reports to docs/evidence and redacts JWTs
├── src/test/java/com/qaportfolio/api/
│   ├── client/ApiSpecs.java                   # Request/ResponseSpecification builders, response-time SLA
│   ├── client/AuthClient.java                 # login once per thread, authenticated() spec
│   ├── client/RateLimitFilter.java            # waits on HTTP 429 using x-ratelimit-reset
│   ├── model/                                 # records: LoginRequest/Response, Product, ProductPage, Cart, CartRequest
│   └── tests/                                 # AuthTests, ProductReadTests, ProductWriteTests, CartTests, UserTests
├── src/test/resources/
│   ├── schemas/*.json                         # JSON Schema (draft-04) contracts
│   ├── testdata/*.json                        # data-provider inputs
│   └── config/default.properties              # baseUrl, credentials override points, SLA
├── testng.xml / testng-smoke.xml
└── docs/  test-plan.md · test-cases.md · defects/ · execution-summary.md · evidence/
```

## How to run
From the repository root:
```bash
# REST Assured: every test, including known-defect tests that reproduce real defects (13 expected failures)
./mvnw -pl 03-api-testing -am test -DtestFailureIgnore=true

# PR/CI mode: exclude known defects, retry once, fail on any failure
./mvnw -pl 03-api-testing -am test -DexcludedGroups=known-defect -Dretry.count=1

# Smoke subset
./mvnw -pl 03-api-testing -am test -Dsuite=testng-smoke.xml

# Allure HTML report (results from every module are collected in ./target/allure-results)
npx allure-commandline@2.32.0 serve target/allure-results
```
Newman, from `03-api-testing/`:
```bash
npm ci
npm test                 # regression + data-driven → target/newman/*.xml|html
npm run test:defects     # Known defects folder (expected to fail) → target/newman-defects/
npm run evidence         # copy reports to docs/evidence/ with tokens redacted
```
To use Postman, import the collection and environment, select the environment, and run the collection.

### Behind a TLS-inspecting corporate proxy
If you see `PKIX path building failed` (Java) or `unable to get local issuer certificate` (Node), the
proxy's root CA isn't in the runtime's trust store. Trust the OS store instead of disabling TLS checks:
```bash
# macOS: Java reads the login/system keychain
./mvnw -pl 03-api-testing test -Djavax.net.ssl.trustStoreType=KeychainStore
# Node/Newman: export the keychain roots once
security find-certificate -a -p /Library/Keychains/System.keychain \
  /System/Library/Keychains/SystemRootCertificates.keychain > ~/corp-ca.pem
NODE_EXTRA_CA_CERTS=~/corp-ca.pem npm test
```
GitHub-hosted runners don't need this.

## Configuration
`src/test/resources/config/default.properties`. Any key can be overridden with `-Dkey=value` or an env var
(`api.baseUrl` → `API_BASEURL`). The keys are `api.baseUrl`, `api.username`, `api.password`, `api.maxResponseMs`
and `retry.count`.

## Results
See [docs/execution-summary.md](docs/execution-summary.md): REST Assured has 53/53 passing in PR mode, plus 13
known-defect reproductions. Newman has 177/177 assertions passing. There are **11 defects**, 3 of them Critical
([register](docs/defects/README.md)).

## Known limitations
* DummyJSON writes are simulated, so persistence (create → read back) can't be verified.
* The public instance is shared and rate limited (100 requests per window). Suites stay small and back off on 429.
  Load testing is done in project 05 against our own app, never here.
* Defects are in a third-party service and can't be fixed by us. They stay as `known-defect` tests that run
  nightly, so a fix would show up as a passing test.
* Response-time assertions measure from the client, so network latency is included.
