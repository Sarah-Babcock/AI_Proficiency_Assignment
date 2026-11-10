# URL Shortener for Client Support

A URL shortener for client support teams. Short links are easier to read to a client over the phone or paste into an email, which helps support direct clients to a specific screen while debugging an issue.

A support agent pastes a long URL into a web page and either types a custom code or lets the app generate one. The app returns a short link like `http://localhost:8080/k7Xp9m`, and the agent copies it or reads it to the client. When the client opens the short link, the app records the visit and sends them to the long URL. Support managers can see on a dashboard which links clients actually use, and when.

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
| Web | `HomeController` + `home.html` | The shorten form: long URL, optional custom code, unusual-ending warning, result with a Copy button |
| | `RedirectController` | `GET /{code}`: records a visit, then redirects to the long URL, or shows "This page doesn't exist" |
| | `DashboardController` + `dashboard.html`, `link-detail.html` | Visit counts by long URL and by short link; each link's visit dates and times |
| Links | `LinkService` | Creates links: custom code (blocked if taken or reserved) or random code (regenerated if taken) |
| | `CodeGenerator` | Random 6-character codes without easily confused characters |
| | `UrlNormalizer` | Adds `https://` when the agent leaves it off |
| | `UrlEndingChecker` | Flags unusual endings (anything other than .com .org .gov .net .edu .io) |
| Visits | `VisitTracker` | Records one visit per redirect |
| | `VisitRepository`, `LinkSummary`, `LongUrlSummary` | Dashboard queries: counts per link (including 0) and grouped by long URL |
| Storage | `ShortLink`, `Visit`, plus repositories | H2 file database via Spring Data JPA |

**Tools:** Java 21, Spring Boot 3.3 (Web, Data JPA, Thymeleaf), H2 (file mode), JUnit 5, MockMvc, AssertJ.

### Control flow

**Creating a link**

```
Agent submits form ─► HomeController
    ├─ URL empty? ─────────────────────────────► "Please enter a URL"
    ├─ unusual ending and not confirmed? ──────► warning + "Yes, create it" (nothing saved)
    └─ LinkService.create
         ├─ UrlNormalizer: add https:// if missing
         ├─ custom code? ─ reserved or taken ──► "already taken / reserved" (nothing saved)
         │               └─ free ─────────────► save
         └─ no custom code ─► CodeGenerator ─► taken? regenerate ─► save
    ─► page shows the short URL + Copy button
```

**Opening a link**

```
Client opens /{code} ─► RedirectController
    ├─ code not found ─► 404 "This page doesn't exist"
    └─ found ─► VisitTracker records the visit (link + time)
              ─► 302 redirect to the long URL (https:// added if an old link lacks it)
```

### Execution approach

I planned first ([PLAN.md](PLAN.md)): who uses the system, the end-to-end flow, the features, the build order and the three scenarios. Then I built it one task at a time, in dependency order. Storage came first, then the form (code generator, then saving, then the page), then the redirect, then visit tracking, then the dashboard. For each task I set the acceptance criteria, the AI drafted the code and tests, and I reviewed the result and ran the hand checks before accepting it. Every task is logged in [AI_LOG.md](AI_LOG.md), including where I changed or rejected what the AI produced.

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
| Reserved code `dashboard` | Blocked as a custom code | A short link `/dashboard` could never be reached, because the dashboard page would take over that address |

---

## The three scenarios

### Greenfield: v1, built from scratch
The shorten form, with custom or random codes, and the redirect. Then visit tracking and the dashboard. See AI_LOG rows 4–11.

### Brownfield: debugging and enhancing the existing code
- **Bug fix:** a URL typed without `https://`, such as `google.com`, sent the client to `localhost:8080/google.com`, which showed "This page doesn't exist." I found it while testing the redirect and deliberately left it for this scenario. **Fix:** add `https://` when saving, and also when redirecting, so links saved before the fix work too. (Row 12)
- **Enhancement:** a dashboard view by long URL that lists every short link pointing to the same URL, with total visits. (Row 13)

### Ambiguous: what counts as an invalid URL?
- **Options:** strict (allow only common endings), loose (allow anything), or warn.
- **Chosen:** **warn.** Usual endings (`.com .org .gov .net .edu .io`) are created immediately. Anything else shows *"Are you sure this is what you meant?"*, and the agent either fixes the URL or clicks **Yes, create it**.
- **Evidence it was the right call:** while testing, `google.cm`, which looks like a typo, turned out to be a real site (Google Cameroon). Strict rules would have blocked it. (Row 14)

---

## Testing approach

**42 automated tests**, run with `./mvnw clean verify`. Each test maps to an acceptance criterion I set before the code was written. Most are labelled `// Check N` to match the AI_LOG row.

| Level | What's covered | Examples |
|-------|----------------|----------|
| Unit | Pure logic, no Spring | `CodeGeneratorTest` (1,000 codes: length and banned characters), `UrlNormalizerTest`, `UrlEndingCheckerTest` |
| Integration (Spring + H2) | Saving and looking up links | `ShortLinkRepositoryTest`, `LinkServiceTest` (taken codes; a forced random-code clash regenerates) |
| Web (MockMvc + real database) | Pages and redirects end to end | `HomeControllerTest`, `RedirectControllerTest`, `VisitTrackingTest`, `DashboardTest` |

Tests use their own database under `target/test-data/`, so they never touch real data.

**Hand checks** (things automated tests can't easily cover): the Copy button puts the link on the clipboard; a link made in the form opens the real site in a browser; links still work after restarting the app; the dashboard looks right.

**Lesson from review:** once, the AI reported a passing build when 4 new tests had actually errored. After that, I checked the full test summary (`Tests run: N, Failures: 0, Errors: 0`) rather than trusting a short "OK."

---

## Limitations and trade-offs

- **No login.** Anyone who can reach the app can create links and see the dashboard. Fine for a local prototype; a real deployment would need authentication.
- **Mixed-case codes** can be confusing over the phone ("capital K or lowercase k?"). This is accepted for v1; codes could be made one case, at the cost of fewer possible codes.
- **No rules on custom codes** beyond "not taken" and "not `dashboard`." Spaces, slashes or very long codes aren't rejected, and a code containing `/` would not be reachable.
- **Near-identical URLs count separately** on the dashboard, e.g. `https://google.com` vs `https://google.com/`.
- **The ending check looks only at the last part of the site name.** It can't tell whether a site actually exists, and every `.co.uk` site triggers the warning.
- **Unlimited retries for random codes.** If nearly every code were taken, creating a link could loop for a long time. Not a risk at about 30 billion codes; I chose not to add a retry cap for v1.
- **H2 file database, single process.** Only one copy of the app can run at a time, because the file is locked. A production version would use a database server such as PostgreSQL.
- **Visit times are shown in the server's time zone.**
- **Links never expire and can't be deleted** from the UI.
- **Every visit is counted**, including repeat clicks by the same client and link-preview bots.

---

## Project layout

```
src/main/java/com/example/urlshortener/
  link/    ShortLink, LinkService, CodeGenerator, UrlNormalizer, UrlEndingChecker
  visit/   Visit, VisitTracker, dashboard queries and summaries
  web/     HomeController, RedirectController, DashboardController
src/main/resources/
  templates/   home, dashboard, link-detail, not-found pages
  application.properties
src/test/      unit, integration and web tests (see Testing approach)
PLAN.md        my plan: users, flow, features, build order, scenarios
AI_LOG.md      every AI task, my acceptance criteria and review result, and my decisions
```
