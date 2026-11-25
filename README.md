# Spring Boot Delivery Pipeline: Maven, SonarQube Gate, ECR and Helm Promotion on EKS

A small Spring Boot service (an in-memory "items" REST API) with the delivery pipeline around it: Maven build and unit
tests, a SonarQube quality gate, a Docker image pushed to ECR, and one Helm chart promoted DEV, then QA, then PROD on
Amazon EKS with the same image tag.

| | |
|---|---|
| Built with | Java 21, Spring Boot 3.5.16, Maven, JUnit 5, Mockito, JaCoCo, SonarQube Community Build, Docker, GitHub Actions, Amazon ECR, Amazon EKS, Helm, CloudFormation |
| Status | **written; first CI run pending** (see "What has and has not run") |
| License | All rights reserved, view only |

## How a change flows

```
pull request ──> test-and-gate ──> image ──> (main only) push to ECR ──> deploy DEV ──> deploy QA ──> deploy PROD
                 unit tests         build +    immutable tag =            helm          helm         helm, needs
                 SonarQube gate     smoke      commit SHA                 --atomic      --atomic     reviewer approval
                 (blocks merge)     test
```

* **Unit tests and quality gate (`test-and-gate`).** `mvn verify` runs the unit tests with JaCoCo coverage. In the same job
  the workflow starts a real SonarQube server in Docker, creates a quality gate, runs the analysis and waits for the verdict
  (`sonar.qualitygate.wait=true`). If the gate fails, the job fails. The job summary lists every condition with its actual
  value and the test count.
* **Quality gate conditions.** The gate fails when coverage is below 80%, duplicated lines are above 3%, or the reliability
  or security rating is worse than A. The server is created fresh for each run, so every run judges the whole code base
  (there is no "new code" baseline to compare against).
* **Blocking merges.** The job is only a blocker once it is a required status check on `main`. That is a repository
  setting, not code: see [docs/PROMOTION.md](docs/PROMOTION.md).
* **Image.** Built from the exact jar that passed the tests, run as a non-root user (uid 10001) and smoke-tested in CI:
  readiness probe UP, an item created and read back.
* **One chart, three values files.** `helm/items-service/values-dev.yaml`, `values-qa.yaml` and `values-prod.yaml` change
  replicas, resources, autoscaling and disruption budgets. The same image tag (the commit SHA) is promoted; nothing is
  rebuilt per environment. ECR tags are immutable.
* **Promotion.** `helm upgrade --install --atomic --wait` per environment into its own namespace, with a port-forward smoke
  test. QA and PROD are GitHub Environments, so PROD can require a human approval.

## Layout

| Path | What it is |
|---|---|
| `app/` | Spring Boot service, 20 unit tests, JaCoCo, Dockerfile |
| `helm/items-service/` | Chart and one values file per environment |
| `.github/workflows/ci.yml` | tests + SonarQube gate, image build and push, chart validation, infra lint, DEV to QA to PROD |
| `.github/workflows/deploy.yml` | reusable per-environment Helm deploy |
| `infra/ecr-and-github-oidc.yml` | ECR repository and the IAM role GitHub Actions assumes through OIDC (no stored AWS keys) |
| `scripts/` | SonarQube provisioning, test and gate summaries, a manual `promote.sh` |
| `docs/PROMOTION.md` | one-time setup: AWS, EKS access, GitHub variables and environments, branch protection |

## What has and has not run

Nothing in this repository has been executed yet; the first GitHub Actions run is the first test of every part below.
This table is updated from real runs only.

| Part | Status |
|---|---|
| Unit tests (20 written) | not yet run |
| SonarQube gate in CI | not yet run |
| Container build and smoke test | not yet run |
| Helm lint and kubeconform for DEV, QA, PROD | not yet run |
| `cfn-lint` and `checkov` on the infra template | run locally: cfn-lint clean, checkov 11 passed, 0 failed, 2 skipped (each explained in the template) |
| Push to ECR | not run (needs an AWS account and the setup in docs/PROMOTION.md) |
| Helm deploy and promotion on EKS | not run (needs a cluster) |

## Known limits

* Data is in memory and per pod, so replicas do not share items. It exists to give the pipeline something real to build and
  test, not to be a product.
* There is no ingress or TLS; the smoke tests use a port-forward.
* GitHub-hosted runners cannot reach an EKS API endpoint that is private or restricted to your own IP. In that case use
  `scripts/promote.sh` from a machine that can, or a self-hosted runner inside the VPC.
* The ephemeral SonarQube in CI keeps no history, so there is no trend or "new code" gate. A long-lived server would add that.
* Spring Boot 3.5 is used because it is the line I could pin and test confidently; moving to 4.x is a dependency upgrade,
  not a redesign.
