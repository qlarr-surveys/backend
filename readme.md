# Qlarr Backend

## Build Jar

```
./gradlew assemble
```

## Run Jar

```
java -jar -D{ENV_VAR_NAME}={ENV_VAR_VALUE} qlarr-backend-0.0.1-SNAPSHOT.jar
```

## Run the Project locally

### start the database docker container

```
docker compose up -d
```

### shutdown docker container
```
docker compose down
```
or
```
docker compose stop
```

### start the service with your ide or

```
./gradlew bootRun --args='--spring.profiles.active={profile}'
```

### Configure PGADMIN

To access the PGADMIN on the browser [Click here](http://localhost:5050/browser)

1. Use the **PGADMIN_DEFAULT_PASSWORD** to unlock saved password
2. Create new server **Register** => **Server**
3. The **Server** require a name
4. Connection Hostname/address type **postgres-db**
5. Username field type **postgres-db** username
6. Password field type **postgres-db** password# qlarr-backend-core
