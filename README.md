# amt-deltaker-bff

## Utvikling
**Lint fix:** 
```
./gradlew ktlintFormat build
```
**Build:**
```
./gradlew build
```

### Kjør lokalt

Start kafka og database:
```shell
docker-compose up -d
```

Sett opp runtime configuration med disse miljøvariablene og kjør via intellij:
```shell
DB_USERNAME=myuser
DB_PASSWORD=mypassword
DB_DATABASE=mydb
DB_HOST=localhost
DB_PORT=5432
```

eller kjør:
```shell
export DB_USERNAME=myuser && 
export DB_PASSWORD=mypassword && 
export DB_DATABASE=mydb && 
export DB_HOST=localhost && 
export DB_PORT=5432 && 
./gradlew run
```

