# RecAPPy BOOK 🍲

Aplikace pro správu a sdílení receptů. Podporuje přihlašování, role uživatelů a nahrávání obrázků receptů.

## Funkce
- Uživatelská registrace a přihlášení (JWT)
- Role: admin vs běžný uživatel
- Admin může přidávat, upravovat a mazat recepty
- Uživatelé mohou prohlížet recepty podle kategorií
- Ukládání obrázků receptů
- „Tip na dnešní den“

## Technologie
- Java 21, Spring Boot 3.4.5
- Klient: nativní iOS app (SwiftUI, repo `recappy-book-ios`)
- Databáze: SQLite (vývoj), PostgreSQL (produkce)
- Autentizace: JWT

## Jak spustit
Backend běží na `https://recappy-book.onrender.com`. Lokálně: `./mvnw spring-boot:run` (viz `docker-compose.yml` pro Postgres).