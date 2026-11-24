# Rollback Runbook (V5 → V2)

Flyway Community has no `undo` command. Rollback therefore uses versioned undo scripts in
`src/main/resources/db/undo/`, applied newest first. The procedure is verified automatically by `RollbackTest` (DB-40).

| Step | Script | Effect |
|---|---|---|
| 1 | `U5__indexes_and_reporting_view.sql` | Drops the reporting view and indexes; removes the V5 history row |
| 2 | `U4__migrate_and_cleanse_data.sql` | Deletes the migrated data, map and rejects; removes the V4 history row |
| 3 | `U3__normalised_schema.sql` | Drops the target tables; removes the V3 history row |

Legacy tables are never modified by V3–V5. After U3, the schema is byte-identical to V2 (checked by SHA-256
fingerprints). Running `flyway migrate` again re-applies V3–V5 with identical results.

```bash
# PostgreSQL example
psql "$DB_URL" -v ON_ERROR_STOP=1 -1 \
  -f src/main/resources/db/undo/U5__indexes_and_reporting_view.sql \
  -f src/main/resources/db/undo/U4__migrate_and_cleanse_data.sql \
  -f src/main/resources/db/undo/U3__normalised_schema.sql
```

On PostgreSQL, `-1` runs all three scripts in a single transaction, so a failed rollback leaves nothing half-done.
Take a backup (`pg_dump`) before both the migration and the rollback in any real environment.
