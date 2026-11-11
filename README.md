# URL Shortener for Client Support

A URL shortener for client support teams. Short links are easier to read to a client over the phone or paste into an email, which helps support direct clients to a specific screen while debugging an issue.

A support agent pastes a long URL (or up to 5 of them) into a web page and either types a custom code or lets the app generate one. The app returns a short link like `http://localhost:8080/k7Xp9m`, and the agent copies it or reads it to the client. When the client opens the short link, the app records the visit and sends them to the long URL. If the link has several URLs, the client sees a page listing them, with an "Open all" button. Support managers can see on a dashboard which links clients actually use, and when. Other programs can do the same things through a JSON API.

Planning notes are in [PLAN.md](PLAN.md). Every AI-assisted task, its acceptance criteria and my review result are in [AI_LOG.md](AI_LOG.md).

---

## Setup

**You need:** Java 21. Maven is not required, because the project includes the Maven wrapper (`./mvnw`).

```bash
# 1. Go to the project folder (keep the quotes if the path has spaces)
cd "path/to/url-shortener-mine"

# 2. If Java 21 isn't on your PATH, point JAVA_HOME at it, for example:
export JAVA_HOME="$(echo ~/.local/jdk/jdk-21*/Contents/Home)"

# 3. Build and run all tests
./mvnw clean verify

# 4. Start the app
./mvnw spring-boot:run
```

When the log shows `Started UrlShortenerApplication`, open:

| Page | URL |
|------|-----|
| Create a short link | http://localhost:8080 |
| Analytics dashboard | http://localhost:8080/dashboard |
| API docs (try the API in the browser) | http://localhost:8080/swagger-ui.html |

Stop the app with **Ctrl+C**. **Ctrl+Z** only pauses it, and a paused copy keeps the database locked.

**Troubleshooting:**
- **Start-up fails with "Unable to determine Dialect" or "Database may be already in use."** Another copy of the app is still running and has the database file open. Stop it, then start again. To find it: `lsof -iTCP:8080 -sTCP:LISTEN`.
- **"Port 8080 was already in use."** Same cause: another copy is running.

Data is stored in `data/urlshortener.mv.db`. Delete that file to start with an empty database.

---

## Architecture

### Components

| Layer | Class | What it does |
|-------|-------|--------------|
| Web | `HomeController` + `home.html` | The shorten form: 1–5 long URLs, optional custom code, unusual-ending warning, result with a Copy button |
| | `RedirectController` + `open-all.html` | `GET /{code}`: records a visit, then redirects to the long URL (1 URL), shows the "Open all" page (2–5 URLs), or shows "This page doesn't exist" |
| | `DashboardController` + `dashboard.html`, `link-detail.html` | Visit counts by long URL and by short link; each link's visit dates and times |
| API | `LinkApiController` | JSON endpoints under `/api/links` (see [API](#api)) |
| | `ApiErrorHandler` | Turns errors into `{"error", "message"}` with the right HTTP status |
| Links | `LinkService` | Creates links: 1–5 URLs; custom code (up to 12 letters, numbers and dashes; blocked if taken or reserved) or random code (regenerated if taken) |
| | `CodeGenerator` | Random 6-character codes without easily confused characters |
| | `UrlListParser` | Splits the form's box into URLs by new lines and commas |
| | `UrlNormalizer` | Adds `https://` when the agent leaves it off |
| | `UrlEndingChecker` | Flags unusual endings (anything other than .com .org .gov .net .edu .io); shared by the form and the API |
| Visits | `VisitTracker` | Records one visit per opening of a short link |
| | `VisitRepository`, `LinkSummary`, `LongUrlSummary` | Dashboard queries: counts per link (including 0) and grouped by long URL |
| Storage | `ShortLink`, `Visit`, plus repositories | H2 file database via Spring Data JPA (see [Data model](#data-model)) |

The web form and the API are two front doors to the same `LinkService`, so every rule (the 5-URL limit, custom-code rules, taken and reserved codes, the unusual-ending check) works the same way in both.

**Tools:** Java 21, Spring Boot 3.3 (Web, Data JPA, Thymeleaf), springdoc-openapi (Swagger UI), H2 (file mode), JUnit 5, MockMvc, AssertJ.

### Control flow

**Creating a link (web form; the API follows the same steps)**

```
Agent submits form ─► HomeController
    ├─ UrlListParser: split by new lines and commas, drop blanks
    ├─ no URLs? ───────────────────────────────► "Please enter a URL"
    ├─ unusual ending and not confirmed? ──────► warning + "Yes, create it" (nothing saved)
    └─ LinkService.create
         ├─ UrlNormalizer: add https:// if missing; drop duplicates
         ├─ more than 5 URLs? ─────────────────► "up to 5 URLs" (nothing saved)
         ├─ custom code? ─ over 12 characters or not letters/numbers/dashes ─► "Custom codes can be..." (nothing saved)
         │               ├─ reserved or taken ──► "already taken / reserved" (nothing saved)
         │               └─ free ─────────────► save
         └─ no custom code ─► CodeGenerator ─► taken? regenerate ─► save
    ─► page shows the short URL + Copy button
```

**Opening a link**

```
Client opens /{code} ─► RedirectController
    ├─ code not found ─► 404 "This page doesn't exist"
    └─ found ─► VisitTracker records the visit (link + time)
              ├─ 1 URL ─► 302 redirect to the long URL (https:// added if an old link lacks it)
              └─ 2–5 URLs ─► "Open all" page listing each URL
```

### API

Full contract: [docs/openapi.json](docs/openapi.json) (OpenAPI 3). While the app is running, the same contract is live at http://localhost:8080/v3/api-docs, and you can try each endpoint at http://localhost:8080/swagger-ui.html.

| Endpoint | Success | Errors |
|----------|---------|--------|
| `POST /api/links` | **201** with `code`, `shortUrl`, `longUrls`, `createdAt`, and a `Location` header | **400** `no_urls`, `too_many_urls`, `invalid_code`, `unusual_url_ending`, `invalid_json`; **409** `code_taken` |
| `GET /api/links/{code}` | **200** with the link's details | **404** `not_found` |
| `GET /api/links/{code}/visits` | **200** with `visitCount` and `visits` (times, newest first) | **404** `not_found` |

Example request:

```json
POST /api/links
{ "urls": ["google.com", "example.org"], "customCode": "reset" }
```

An unusual ending such as `google.cm` is rejected with **400** `unusual_url_ending` unless the request includes `"confirmed": true`. This is the API's version of the form's "Yes, create it" button. Every error has the same shape:

```json
{ "error": "code_taken", "message": "The code \"reset\" is already taken. Please choose another." }
```

### Data model

Three tables, created by Hibernate from the entity classes.

| Table | Columns | Notes |
|-------|---------|-------|
| `short_link` | `id` (PK), `code` (unique, up to 64), `long_url` (up to 2048), `created_at` | One row per short link. `long_url` holds the first (or only) URL |
| `short_link_extra_url` | `short_link_id` (FK → `short_link`), `position`, `url` | URLs 2–5 of a multi-URL link, in the order they were entered |
| `visit` | `id` (PK), `short_link_id` (FK → `short_link`), `visited_at` | One row per opening of a short link |

Keeping the first URL in `short_link.long_url` meant that links created before the multi-URL feature needed no data migration.

### Execution approach

I planned first ([PLAN.md](PLAN.md)): who uses the system, the end-to-end flow, the features, the build order and the three scenarios. Then I built it one task at a time, in dependency order. Storage came first, then the form (code generator, then saving, then the page), then the redirect, then visit tracking, then the dashboard. Later enhancements (multi-URL links, then the JSON API) were added on top of the working app without changing its existing behaviour. For each task I set the acceptance criteria, the AI drafted the code and tests, and I reviewed the result and ran the hand checks before accepting it. Every task is logged in [AI_LOG.md](AI_LOG.md), including where I changed or rejected what the AI produced. Each task from row 16 on is its own git commit.

### Key decisions

| Decision | Choice | Why |
|----------|--------|-----|
| What analytics to track | Visits to each short link, with date and time | Shows how useful support's short links are and how often clients use them |
| v1 scope | Shorten form + redirect | Without the redirect, every short link gives the client an error |
| Storage | H2 file database in the project folder | Links must survive a restart, because a client may call back the next day |
| Random code format | 6 characters, mixed case, no `0 O o 1 l I` | Easy to read aloud; 56 characters gives about 30 billion codes |
| Custom code already taken | Block and tell the agent | We don't want to override another support agent's short URL |
| Random code already taken | Quietly regenerate | The agent didn't choose it, so an error would only confuse them |
| Redirect type | 302 (temporary), not 301 | Browsers cache 301s and skip the server, which would make visit counts too low |
| Invalid URLs | Warn and ask for confirmation, rather than block or allow silently | Real sites with other endings (e.g. `.co.uk`) still work, and typos are caught before the agent reads a bad link to a client |
| Custom code rules | Up to 12 characters: letters, numbers and dashes only | <!-- your reason --> |
| Reserved codes `dashboard`, `swagger-ui.html` | Blocked as custom codes | A short link at those addresses could never be reached, because an app page already uses them |
| Multi-URL links | Up to 5 URLs, split by new lines or any comma | Simple for agents; 1 URL behaves exactly as before |
| Visits to a multi-URL link | 1 for the short link, +1 for each of its URLs in "By long URL" | <!-- your reason --> |
| API and unusual endings | Reject unless `"confirmed": true` | Same rule as the form, and a program calling the API won't read a warning, so a typo would quietly become a live link |

<!-- What I'd do differently: in your own words, e.g. whether you would design the API before the web form next time, and why you built it in this order. -->

---

## The three scenarios

### Greenfield: v1, built from scratch
The shorten form, with custom or random codes, and the redirect. Then visit tracking and the dashboard. See AI_LOG rows 4–11.

### Brownfield: debugging and enhancing the existing code
- **Bug fix:** a URL typed without `https://`, such as `google.com`, sent the client to `localhost:8080/google.com`, which showed "This page doesn't exist." I found it while testing the redirect and deliberately left it for this scenario. **Fix:** add `https://` when saving, and also when redirecting, so links saved before the fix work too. (Row 12)
- **Enhancement:** a dashboard view by long URL that lists every short link pointing to the same URL, with total visits. (Row 13)
- **Enhancement:** one short link for up to 5 URLs, with an "Open all" page. Impacted: `ShortLink` (new extra-URL table, no migration for old links), `LinkService`, the form (now a text box that takes several URLs), `RedirectController` plus a new page, the dashboard queries and pages, and 3 existing tests. Verified against a copy of the real database that old links still work. (Row 17)
- **Enhancement:** a JSON API with OpenAPI docs, added as a new layer over the existing `LinkService`. The warning text moved from the form's controller into `UrlEndingChecker` so the form and API share it; the form's behaviour is unchanged and all earlier tests still pass. (Row 18)
- **Bug fix:** custom codes had almost no rules, so an agent could create a link the client couldn't open (`reset password`, `help/login`). Fix: custom codes must be up to 12 letters, numbers and dashes, in both the form and the API. Existing links with old-style codes still open. (Row 20)

### Ambiguous: what counts as an invalid URL?
- **Options:** strict (allow only common endings), loose (allow anything), or warn.
- **Chosen:** **warn.** Usual endings (`.com .org .gov .net .edu .io`) are created immediately. Anything else shows *"Are you sure this is what you meant?"*, and the agent either fixes the URL or clicks **Yes, create it**.
- **Evidence it was the right call:** while testing, `google.cm`, which looks like a typo, turned out to be a real site (Google Cameroon). Strict rules would have blocked it. (Row 14)

---

## Testing approach

**75 automated tests**, run with `./mvnw clean verify`. Each test maps to an acceptance criterion I set before the code was written. Most are labelled `// Check N` to match the AI_LOG row.

| Level | What's covered | Examples |
|-------|----------------|----------|
| Unit | Pure logic, no Spring | `CodeGeneratorTest` (1,000 codes: length and banned characters), `UrlNormalizerTest`, `UrlEndingCheckerTest`, `UrlListParserTest` |
| Integration (Spring + H2) | Saving and looking up links | `ShortLinkRepositoryTest`, `LinkServiceTest` (taken codes; a forced random-code clash regenerates) |
| Web (MockMvc + real database) | Pages and redirects end to end | `HomeControllerTest`, `RedirectControllerTest`, `VisitTrackingTest`, `DashboardTest`, `MultiUrlTest`, `CustomCodeRulesTest` |
| API (MockMvc + real database) | JSON requests, status codes, error shape | `LinkApiTest` |

Tests use their own database under `target/test-data/`, so they never touch real data.

**Hand checks** (things automated tests can't easily cover): the Copy button puts the link on the clipboard; a link made in the form opens the real site in a browser; links still work after restarting the app; "Open all" opens every tab; the dashboard looks right; the API works from Swagger UI.

**Lesson from review:** once, the AI reported a passing build when 4 new tests had actually errored. After that, I checked the full test summary (`Tests run: N, Failures: 0, Errors: 0`) rather than trusting a short "OK."

---

## Limitations and trade-offs

- **No login.** Anyone who can reach the app or the API can create links and see the dashboard. Fine for a local prototype; a real deployment would need authentication.
- **Mixed-case codes** can be confusing over the phone ("capital K or lowercase k?"). This is accepted for v1; codes could be made one case, at the cost of fewer possible codes.
- **Custom codes made only of dashes** (e.g. `---`) are allowed, since they fit the rule; they're valid but hard to read aloud.
- **A URL containing a comma gets split** into two URLs, because any comma separates URLs in the form. The API takes a list, so it isn't affected.
- **Near-identical URLs count separately** on the dashboard, e.g. `https://google.com` vs `https://google.com/`.
- **The ending check looks only at the last part of the site name.** It can't tell whether a site actually exists, and every `.co.uk` site triggers the warning.
- **Unlimited retries for random codes.** If nearly every code were taken, creating a link could loop for a long time. Not a risk at about 30 billion codes; I chose not to add a retry cap for v1.
- **H2 file database, single process.** Only one copy of the app can run at a time, because the file is locked. A production version would use a database server such as PostgreSQL.
- **`docs/openapi.json` is a saved copy.** If the API changes, it must be saved again from `/v3/api-docs`; the live Swagger page is always current.
- **Visit times are shown in the server's time zone** on the dashboard; the API returns them in UTC.
- **Links never expire and can't be deleted** from the UI or the API.
- **Every visit is counted**, including repeat clicks by the same client and link-preview bots.

---

## Project layout

```
src/main/java/com/example/urlshortener/
  link/    ShortLink, LinkService, CodeGenerator, UrlListParser, UrlNormalizer, UrlEndingChecker
  visit/   Visit, VisitTracker, dashboard queries and summaries
  web/     HomeController, RedirectController, DashboardController
  api/     LinkApiController, request/response records, ApiErrorHandler
src/main/resources/
  templates/   home, open-all, dashboard, link-detail, not-found pages
  application.properties
src/test/      unit, integration, web and API tests (see Testing approach)
docs/openapi.json  the API contract (OpenAPI 3)
PLAN.md        my plan: users, flow, features, build order, scenarios
AI_LOG.md      every AI task, my acceptance criteria and review result, and my decisions
```
