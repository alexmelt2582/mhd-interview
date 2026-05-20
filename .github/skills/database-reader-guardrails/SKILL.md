---
name: database-reader-guardrails
description: Safe database reading workflow for MySQL, PostgreSQL, openGauss, and Redis. Use whenever the user asks to query SQL, inspect Redis, read a database, check schemas, list tables, verify columns, or troubleshoot database data. Always verify the target database, schema or table, columns, or Redis keys and types exist before querying, and never invent object names.
---

# Database Reader Guardrails

## Purpose

Use this skill when the task involves reading data from MySQL, PostgreSQL, openGauss, or Redis.

The core rule is simple: verify first, query second. Do not assume the database, schema, table, column, key, or field exists just because the user mentioned it.

## Default behavior

Follow this workflow unless the user explicitly provides verified metadata from the current environment:

1. Identify the engine: MySQL, PostgreSQL, openGauss, or Redis.
2. Identify the target database and object names from the user request.
3. Check the current connection target before running business queries.
4. Verify the requested database exists.
5. Verify the requested schema and table exist.
6. Verify every referenced column exists.
7. For Redis, verify the DB index, key existence, key type, and requested hash fields or structure.
8. Only after the checks pass, run the narrowest possible read-only query.
9. In the final response, state what was verified and what query or command was used.

If any check fails, stop and report the exact missing database object instead of guessing.

## Non-negotiable rules

- Never fabricate database names, schema names, table names, column names, Redis keys, or Redis fields.
- Never skip existence checks when the schema is not already verified in the current session.
- Prefer read-only commands first. Do not write, update, delete, truncate, or alter unless the user explicitly asks for it.
- Use the currently connected database intentionally. Always confirm which database is active before querying SQL data.
- Keep queries narrow. Select only needed columns and add LIMIT when the user did not ask for a full export.
- If the user asks for a dangerous or destructive statement, ask for confirmation and explain the risk.

## Output contract

When reporting a database lookup, keep the answer structured:

1. Engine and target: what database system and target database or Redis DB was used.
2. Verification: which existence checks passed.
3. Query or command: the exact SQL or Redis command used when useful.
4. Result: the concise answer or returned rows summary.
5. Blockers: any missing database, schema, table, column, key, or field.

## Engine-specific workflow

### MySQL

Always verify in this order:

1. Confirm active database.
2. Confirm target database exists.
3. Confirm target table exists.
4. Confirm referenced columns exist.
5. Run the business query.

Useful checks:

```sql
SELECT DATABASE() AS current_database;

SELECT SCHEMA_NAME
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'target_db';

SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'target_db'
  AND TABLE_NAME = 'target_table';

SELECT COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'target_db'
  AND TABLE_NAME = 'target_table'
  AND COLUMN_NAME IN ('col_a', 'col_b');
```

MySQL notes:

- If the user did not specify a database, inspect the current one first and say so.
- If the query spans multiple tables, verify each table and each referenced join column.
- If the environment is Windows PowerShell, prefer explicit executable paths when the workspace already uses them.

### PostgreSQL

Always verify in this order:

1. Confirm active database.
2. Confirm target database exists.
3. Confirm target schema exists.
4. Confirm target table exists.
5. Confirm referenced columns exist.
6. Run the business query.

Useful checks:

```sql
SELECT current_database() AS current_database;

SELECT datname
FROM pg_database
WHERE datname = 'target_db';

SELECT schema_name
FROM information_schema.schemata
WHERE schema_name = 'public';

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name = 'target_table';

SELECT column_name
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'target_table'
  AND column_name IN ('col_a', 'col_b');
```

PostgreSQL notes:

- Do not assume the schema is public.
- If the user provides an unqualified table name, check the search_path only if needed, but prefer confirming the schema explicitly.

### openGauss

Treat openGauss like PostgreSQL first, then adapt only if the environment exposes engine-specific catalog differences.

Use the same verification sequence as PostgreSQL:

1. Confirm active database.
2. Confirm target database exists.
3. Confirm target schema exists.
4. Confirm target table exists.
5. Confirm referenced columns exist.
6. Run the business query.

Preferred checks are still based on information_schema and pg_catalog-compatible views unless the environment requires a different catalog.

openGauss notes:

- Prefer compatibility views before engine-specific system tables.
- If a PostgreSQL catalog query fails, explain that the environment may expose a variant and inspect available schemas or tables before continuing.

### Redis

Redis does not have tables and columns, so translate the verification rule into the Redis model:

1. Confirm target logical DB index.
2. Confirm the key exists.
3. Confirm the key type.
4. If it is a hash, verify the requested field exists.
5. If it is a list, set, zset, stream, or string, choose the matching read command.
6. Run the narrowest possible read.

Useful checks:

```text
SELECT 0
EXISTS user:1001
TYPE user:1001
HKEYS user:1001
HEXISTS user:1001 nickname
```

Redis notes:

- Never run GET against a non-string key before checking TYPE.
- For hashes, use HEXISTS before claiming a field exists.
- For lists, sets, zsets, and streams, inspect the key type first and then use LRANGE, SMEMBERS, ZRANGE, or XRANGE as appropriate.

## Query planning checklist

Before any read query, quickly check:

- Which engine am I using?
- Which database am I connected to?
- Did I verify the database exists?
- Did I verify the schema and table exist?
- Did I verify every column used in SELECT, WHERE, JOIN, ORDER BY, and GROUP BY?
- If Redis: did I verify DB index, key existence, key type, and requested field existence?
- Is the query read-only and as narrow as possible?

## Response examples

### Example 1: MySQL

User request: Query the latest 5 rows from service in apipark.

Good response pattern:

- Engine and target: MySQL, database apipark.
- Verification: confirmed apipark exists, confirmed service exists, confirmed id, name, prefix, create_at exist.
- Query used: SELECT id, name, prefix FROM service ORDER BY create_at DESC LIMIT 5;
- Result: summarize or present the rows.

### Example 2: PostgreSQL or openGauss

User request: Count yesterday's orders from public.orders.

Good response pattern:

- Engine and target: PostgreSQL, database shop, schema public.
- Verification: confirmed database shop exists, confirmed schema public exists, confirmed orders exists, confirmed created_at exists.
- Query used: SELECT COUNT(*) FROM public.orders WHERE created_at >= CURRENT_DATE - INTERVAL '1 day' AND created_at < CURRENT_DATE;
- Result: provide the count.

### Example 3: Redis

User request: Read nickname from user:1001.

Good response pattern:

- Engine and target: Redis DB 0.
- Verification: confirmed key user:1001 exists, confirmed key type is hash, confirmed field nickname exists.
- Command used: HGET user:1001 nickname.
- Result: provide the field value.

## Failure handling

If a verification step fails, respond with the missing object and stop.

Examples:

- Database missing: target database not found, so the business query was not executed.
- Table missing: database exists, but the requested table does not exist.
- Column missing: table exists, but one or more requested columns are absent.
- Redis key missing: logical DB is reachable, but the key does not exist.
- Redis type mismatch: the key exists, but the requested command does not match the key type.

## Practical guidance

- Prefer information_schema for SQL existence checks because it is portable and easier to explain.
- For openGauss, start with PostgreSQL-compatible catalog queries.
- When the user already supplied the exact schema result from the live database in the same conversation, you may reuse it instead of rechecking, but say that you are relying on verified metadata from the session.
- If the environment has a repository-specific command for database access, follow that command rather than inventing a new connection pattern.
