# Refactorizare proxyobs

Clase noi pentru common-api, sedproxy, ratproxy și rvdrproxy, scrise după poze ale codului real. Nu sunt integrate și nu au fost compilate pe proiectul real; importurile și constantele presupuse sunt marcate `TODO`.

- [docs/LOGIC.md](docs/LOGIC.md): ce face fiecare variantă (legacy, cloud, nou) și unde diferă.
- [docs/COMMON-API.md](docs/COMMON-API.md): ce se șterge din common, ce se adaugă și echivalențele.
- [docs/MISSING.md](docs/MISSING.md): poze care lipsesc și întrebări deschise.

## Structură

| Folder | Conținut | Unde merge |
|---|---|---|
| `common-api/` | `ProxyContext`, validator, publisher, `KafkaPublisher`, agregare pe liste, teste | biblioteca common-api |
| `sedproxy/` | listener, `SedProcessingService`, `SedProcessorService`, clientul DES | sedproxy |
| `ratproxy/` | listener, `RatProcessingService`, `RatMappingService`, GET-ul de rezultate, test | ratproxy |
| `rvdrproxy/` | `IntegrityCheckService` | rvdrproxy |
| `snippets/` | fragmente de aplicat în alte clase (vis, cki, rat) | nu se compilează |
| `reference/` | codul existent, transcris din poze, doar ca exemplu | nu se integrează |

Clasele din common folosesc pachetul `com.bnp...`. Sed și rat folosesc `com.bnpp...`; rvdr și vis folosesc `com.bnp...`.
