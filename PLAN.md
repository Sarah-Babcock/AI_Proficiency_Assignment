This product is a url shortener that is being used to make communication with client support to clients easier to understand and help direct clients to specific screens when needed. 

The system is used by client support who would like the urls shortened so that when communicating via email or over the phone it is easier to communicate where the client needs to go in the url when helping the client debug an issue. I would ask the product owner what kind of analytics are most important and how best would they be displayed? In a dashboard? Frequency of which sites are being visited? Frequency of when the sites are being visited? Do links ever expire? Should the shortened url be random or should the person creating the shortened url be able to create what they want the wording to be? What is an invaid url? Should the url be confirmed that it is a valid site before the url is shortened? 

The features include a url shortener that takes a url and allows the customer to either generate what they want their shortened url to be or shortens it for them randomly. It should look for confusing scenarios such as 0o and lI when generating randomly. This initial version should include analytics that keep track of how many times a specific url is visited



## How it works (flow)

1. The support agent goes to a website where they can submit the long URL.
2. They choose to have the short URL randomly generated, or type what they want it to be.
3. They click Generate, and the system creates a short URL that is more readable and easier to communicate.
4. They tell the short URL to the client over the phone.
5. The client enters the short URL, and the system sends them to the right website.
6. The analytics tool records that the site was visited.

## Features

**v1 (must have)**
1. Shorten form: a web page where the agent enters a long URL and either types a custom short code or gets a random one.
2. Redirect: when the client visits the short URL, they're sent to the long URL.

**Later**
3. Visit tracking: each visit records the short URL and the long URL.
4. Analytics dashboard: the support team can see the visit numbers.

## Decisions

- **Analytics counts visits, not how often a URL is shortened.**
  Why: so we can see how useful the short links that support provides are, and how often clients use them.
- **v1 is the shorten form and the redirect only.**
  Why: without the redirect, every short link gives the client an error, so both are needed for a working product.
- **Tracking comes after v1.**
  Trade-off: visits before tracking is added aren't counted.

## Build order

0. Storage: a place to save each short code with its long URL (the form writes to it, the redirect reads from it).
1. Shorten form
2. Redirect
3. Visit tracking
4. Analytics dashboard

Each step depends on the one before it: the redirect needs stored links to look up, and the dashboard needs tracked visits to show.

## Scenarios

- **Greenfield:** v1, the shorten form and the redirect, built from scratch.
- **Brownfield:** add URL validation to the existing form.
  - Known bug to fix here (found while testing the redirect): a URL typed without `https://` (e.g. `google.com`) sends the client to `localhost:8080/google.com`, which shows "This page doesn't exist".
- **Ambiguous:** what counts as an invalid URL?
  - Options: strict (only common endings like .com, .org, .gov), loose (allow anything), or warn.
  - Chose **warn**: for unusual endings, show an "Are you sure this is what you meant?" confirmation before creating the link; it's created only if the agent confirms.
  - Why: strict would block real sites support may need (.io, .co.uk, internal pages); warning still catches typos before the agent reads a bad link to the client.
