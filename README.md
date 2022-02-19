# Akka gRPC quickstart

*Highly opinionated* fork of the Lightbend Akka gRPC G8 template.

Out of the box, it provides:

* Fully functional Akka gRPC service
* Basic GitHub Actions workflow
* Setup for structured logging
* Basic healthcheck
* Customized build.sbt
* Scalafmt setup
* Docker image creation and publishing support
* Always latest stable releases of required dependencies
* Support for code coverage
* Scalafix linter and rules
* ScalaSteward setup (with Pull Request auto-merge)

## Usage

Prerequisites:
- JDK 11+
- sbt 1.13.13+

Assuming that you have access to the private repository, in the
console, type:

```sh
sbt new git@github.com/umatrangolo/akka-grpc-quickstart-scala.g8.git
```

This template will prompt for a set of parameters asking for the
desired versions of all the dependencies; you can just press enter on
the proposed choices given that they are the latest stable versions
found on the Maven repository.

However, these are the ones that you will need to fill correctly:

| Name | Description |
|------|-------------|
|name  | Becomes the name of the project (e.g. foo-svc) |
|description | Used to bootstrap README.md for the app |
|organization | The organization owning this app (e.g. example) |
|codeowners | Content of the GitHub CODEOWNERS file |
|package | Starting package (e.g. foo.bar) |
|docker_maintainer| Email of the maintainer of this app |
|docker_package_name| Specifies the package name for Docker (e.g. gonitro/foo-svc) |

## Running

The generated service is runnable out of the box and it is ready to be
deployed wout any required further intervention. To start
locally:

```
sbt run
```

that listens on port 9000 and answering on a simple healthcheck.
```
grpcurl -v --plaintext  localhost:9000 grpc.health.v1.Health/Check
```

More options are in the generated service README.

## CI/CD setup

The template will use GitHub actions to implement a basic CI/CD
pipeline. The (optional) CD is implemented by just pushing the Docker
image to AWS ECR after each merge to master/main.

We have three different pipelines with their jobs:

* **CI**
  - *lint*: lints your code (e.g. checks formatting)
  - *test*: compiles and runs tests checking also for coverage
* **CD**
  - *build*: builds and pushes the Docker image
* **Scala Steward**
  - *scala-steward*: each Sunday it opens a lot of PRs proposing
    dependencies updates (that will be auto-merged on a green build)

## References

* [g8](http://www.foundweekends.org/giter8/)
* [GitHub actions](https://docs.github.com/en/free-pro-team@latest/actions/reference/workflow-syntax-for-github-actions)
* [SBT Coverage](https://github.com/scoverage/sbt-scoverage)
* [SBT Dot Env](https://github.com/mefellows/sbt-dotenv)
* [Logstash Logback Encoder](https://github.com/logstash/logstash-logback-encoder)
* [Scalafmt](https://scalameta.org/scalafmt/)
* [Scalafix](https://scalacenter.github.io/scalafix/)
* [Scala Steward](https://github.com/scala-steward-org/scala-steward)

ugo.matrangolo@gmail.com
