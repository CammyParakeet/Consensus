# Consensus

**Consensus** is a Minecraft (Paper 1.21.6) plugin for creating and managing in-game polls  
Players can vote through interactive **books**, view results in real time, and server staff can configure persistence backends (FlatFile or SQL)

---

## Features
- Create polls with multiple answers, tooltips, and limits (single/multi-choice)
- Interactive book UI for voting and results
- Poll builder flow powered by Papers **Dialog API**
- Flexible storage backends:
    - **FlatFile (JSON)** — simple, human-readable poll storage
    - **SQLite / MySQL** — backed by JDBI and connection pooling
- Clean Guice-based modular architecture

---

## Downloads

Two JARs are shipped with each release:

- **Consensus.jar**  
  Standard build. Uses FlatFile by default. If you want SQL, you must provide the JDBC driver manually

- **Consensus-with-sqlite.jar**  
  Bundled with the `org.xerial:sqlite-jdbc` driver. Recommended if you plan to use SQLite without adding the driver yourself

---

## Installation
1. Download either `Consensus.jar` or `Consensus-with-sqlite.jar` from [releases](.todo/releases)
2. Place it in your server’s `plugins/` folder
3. Restart the server to generate configuration files

---

## Configuration

The main settings live in `config.yml`:

```yaml
storage:
  backend: FLATFILE  # FLATFILE | SQLITE | MYSQL
  flatfile:
    dir: playerdata/polls
  sql:
    jdbcUrl: "jdbc:sqlite:${plugin.data}/polls.db"
    username: ""
    password: ""
    pool:
      maxSize: 6
      minIdle: 2
```

Notes:
- `${plugin.data}` expands to the plugin’s data folder path
- FlatFile is recommended for lightweight servers
- Use SQLite/MySQL for larger deployments

---

## Usage

- Start a new poll with the builder: (The id is an **optional** string e.g /poll create my_poll)
```
/poll create [id]
```

- Players will receive an interactive **book** with the poll question and answers
- Results can be viewed by anyone with permission:
```
/poll results
```

### Browsing Polls
- Open the **polls menu**:  
```
/polls
```

- Displays recent polls in a **paginated inventory**
- Icons show the question, open/closed badge, and remaining/elapsed time
- Bottom navigation lets you flip pages and toggle between **Active**, **Closed**, and **All**


---

## Building

This project uses **Gradle + Kotlin DSL**

To build both JARs:
```bash
./gradlew shadowWithSQLite
```

Artifacts:
- `build/libs/Consensus.jar`
- `build/libs/Consensus-with-sqlite.jar`

---
