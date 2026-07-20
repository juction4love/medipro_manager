# Contributing

MediPro is developed by **Bimal Tech Solution** for pharmacy ERP deployments in Nepal.

## Internal contributors

1. Clone the repository
2. Follow [docs/DATABASE-ASSETS.md](docs/DATABASE-ASSETS.md) to obtain `catalog.db` before building
3. Copy `keystore.properties.example` → `keystore.properties` (local only, never commit)
4. Add `app/google-services.json` from Firebase Console (never commit)
5. Branch from `master`, open PR with clear description

## Code conventions

- Kotlin + Jetpack Compose + Clean Architecture (feature → domain ← data)
- Hilt for DI; ViewModels use use cases, not DAOs directly
- Room migrations: never use destructive fallback in production
- Bug fixes only on frozen modules unless approved for a release gate

## Commit messages

- `fix:` crash / data integrity
- `feat:` new capability (usually deferred post-pilot)
- `docs:` documentation
- `release:` version bumps

## Questions

See [SECURITY.md](SECURITY.md) or contact bimal.lamichhane@gmail.com
