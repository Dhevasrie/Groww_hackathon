# [Project Name] — Code by Groww Submission

<!-- One-line description of what this does. Fill in once you know the problem. -->

## Problem Statement

<!-- Paste/paraphrase the actual problem here in 2-3 sentences, so a reader
     has context before looking at anything else. -->

## The 100-Word Pitch

<!-- Draft this EARLY, not last. What did you build, how did you design it,
     and what's the thinking behind your key choices? This is likely the
     first thing a judge reads. -->

## Architecture & Key Decisions

<!-- This section is where you demonstrate judgement — it matters more than
     the code itself. For each major decision, briefly say WHAT you chose
     and WHY, and what you chose NOT to do.

     Examples of things to cover:
     - Why this data model / schema shape
     - How you handled [specific edge case] and why
     - What you deliberately left out of scope, and why
     - Any trade-off you made under time pressure
-->

- **Decision:** ...
  **Why:** ...
- **Decision:** ...
  **Why:** ...

## Tech Stack

- Backend: Java 17, Spring Boot 3.3
- Database: H2 (in-memory) <!-- swap if you moved to Postgres/MySQL -->
- Build tool: Maven

## Setup Instructions

### Prerequisites
- JDK 17+
- Maven 3.9+ (or use the included `./mvnw` wrapper)

### Run locally

```bash
# Clone the repo
git clone <your-repo-url>
cd hackathon-starter

# Run the app
./mvnw spring-boot:run
```

The app will start on `http://localhost:8080`.

### Verify it's running

```bash
curl http://localhost:8080/api/health
# {"status":"UP"}
```

### Run tests

```bash
./mvnw test
```

### Explore the DB (dev only)

H2 console available at `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:hackathondb`, user: `sa`, no password)

## API Overview

<!-- List your actual endpoints once built. Example format below. -->

| Method | Endpoint          | Description        |
|--------|-------------------|---------------------|
| GET    | `/api/items`      | List all items      |
| GET    | `/api/items/{id}` | Get item by ID       |
| POST   | `/api/items`      | Create a new item    |
| DELETE | `/api/items/{id}` | Delete an item        |

## Edge Cases Handled

<!-- Explicitly call these out — judges are told to look for this. -->

- ...
- ...

## What I'd Do With More Time

<!-- Shows self-awareness and scoping judgement, not weakness. -->

- ...
- ...
