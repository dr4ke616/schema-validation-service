# Schema Validation Service

[![Scala CI](https://github.com/dr4ke616/schema-validation-service/actions/workflows/scala.yml/badge.svg)](https://github.com/dr4ke616/validation-service/actions/workflows/scala.yml)

Schema Validation Service is a REST service for validating JSON documents against JSON Schemas.

This REST-service allows users to upload JSON Schemas and store them at unique URI and then validate JSON documents against those URIs.

The service exposes three routes:

- `POST /schema/<schema_id>`: Upload a JSON Schema with unique `schema_id`
- `GET /schema/<schema_id>`: Retrieve a JSON Schema with unique `schema_id`
- `POST /validate/<schema_id>`: Validate a JSON document against the JSON Schema identified by `schema_id`

The system is build using scala3 on top of `cats` and `cats-effect`. Http requests are served with `http4s`, and `doobie` for database actions.

The service can run in two modes, one which uses `sqlite` as the backing store, and the other with an in-memory store. This mode can be configured by setting the `BACKEND` environment variable to either `sqlite` (default) or `in-mem`. Subsequent iterations can be done to support more production ready datastores as needed. The intention for now, and for simplicity sake, is the service can run with no external dependencies.

## Application

The application has a few environment variables that can be set before running, either with `sbt` or on `docker`:

| Env Variable  | Default        |
| ------------- | -------------- |
| `HTTP_PORT`   | `8080`         |
| `HTTP_HOST`   | `0.0.0.0`      |
| `BACKEND`     | `sqlite`       |
| `SQLITE_FILE` | `validator.db` |

### Running

```shell
sbt run
```

### Tests

```shell
sbt test
```

### Docker

To publish a local docker image, using `sbt-native-packager` with:

```shell
sbt docker:publishLocal
```

Then running with docker we can run:

```shell
docker run -p 8080:8080 --rm schema-validation-service:0.1.0-SNAPSHOT
```

#### Known Docker Issues

As `sqlite` is used as for the database, write permissions are needed for the JVM process to successfully write to the database file. [`sbt-native-packager`](https://www.scala-sbt.org/sbt-native-packager/) has strict file permissions, and seems quiet difficult to customize it. See [this issue](https://github.com/sbt/sbt-native-packager/issues/1402) for a similar problem. One option might be to build the `jar`, and define the `Dockerfile` manually. Alternatively, we should also be able to mount a directory from the host machine. Finally, as more of a workaround, we can write to the `/opt/docker/bin/` directory which is writable.

## Example Requests

Create a schema called `my-schema`:

```shell
curl -i -XPOST http://localhost:8080/schema/my-schema -d '
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "source": {
      "type": "string"
    },
    "destination": {
      "type": "string"
    },
    "timeout": {
      "type": "integer",
      "minimum": 0,
      "maximum": 32767
    },
    "chunks": {
      "type": "object",
      "properties": {
        "size": {
          "type": "integer"
        },
        "number": {
          "type": "integer"
        }
      },
      "required": ["size"]
    }
  },
  "required": ["source", "destination"]
}
'
```

Retrieve it:

```shell
curl -i -XGET http://localhost:8080/schema/my-schema
```

Validate it:

```shell
curl -i -XPOST http://localhost:8080/schema/my-schema -d '
{
  "source": "/home/alice/image.iso",
  "destination": "/mnt/storage",
  "timeout": null,
  "chunks": {
    "size": 1024,
    "number": null
  }
}
'
```

See [`exercise.sh`](./exercise.sh) for some working examples (requires the use of `jq`).

## Next Steps(?)

- Update github actions to publish docker image to dockerhub
- Update github actions to deploy to heroku
- Update datastore to use something like Postgres, DynamoDB, MongoDB, etc.
- Expand to support:
  - Other schema types
  - Schema versions
