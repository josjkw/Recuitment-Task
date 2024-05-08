# Description

## Architecture solution

Architecture solution chosen is hexagonal + onion. This solution enables domain to be library agnostic while outer layers (eg: implementation details) are completely disconnected from domain logic.
It is also convenient for replacing or extending individual layers without impacting the others.

## Technical stack overview

- Effect system : ZIO 
- Database interaction : doobie + tzio + flyway (migrations)
- Endpoints and swagger : tapir
- Serialisation / Deserialization : circe
- Type transformation : chimney
- Http server / client : http4s / zio
- Testing : testcontainers (kafka and postgres) / ziotest

## Testing

### Unit testing

Unit testing doesn't necessitate the launch of specific services or applications. Instead, test containers are spawned for Kafka / Postgres.
It ensures that tests remain isolated and independent of external dependencies, enhancing their reliability and reproducibility. 
It also simplifies the setup process, as we don't need to manually configure and manage these services during testing.

There are also a few suites that would be mandatory to have (BoardServiceSpec, EndpointsSpec)

### Integration testing

To launch the integration testing it is necessary to start the application (App) and the docker compose in docker-compose/docker-compose.yaml
Once this is up and running, the suite can be launched.

Again a few tests / suite could be added for a more thorough testing. 

## Error handling

Error handling is rudimentary; there are no dedicated entities to manage errors. While it would be beneficial to have them, for simplicity's sake, I currently return a string.
