## Comandos para rodar a aplicação

Compilar:
    
```bash
mvn clean package -DskipTests
```

Subir o sistema:
```bash
docker compose up -d --build
```

Abrir o menu:

```bash
docker compose run --rm console
```


Parar:

```bash
docker compose down
```