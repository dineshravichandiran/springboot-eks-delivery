# One-time setup for ECR push and EKS promotion

Everything here needs your own AWS account. Until it is done the workflow still runs the tests, the SonarQube gate, the
image build and the chart checks; the push and deploy jobs stay off because `DEPLOY_ENABLED` is not set.

## 1. ECR repository and the GitHub OIDC role

```
aws cloudformation deploy \
  --stack-name items-service-ci \
  --template-file infra/ecr-and-github-oidc.yml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides EksClusterName=<your-cluster>
```

If the account already has the `token.actions.githubusercontent.com` OIDC provider, add
`CreateOidcProvider=false ExistingOidcProviderArn=<arn>`. The stack outputs the repository URI and the role ARN.

## 2. Let the role into the cluster, only in three namespaces

```
kubectl create namespace dev; kubectl create namespace qa; kubectl create namespace prod

aws eks create-access-entry --cluster-name <your-cluster> \
  --principal-arn <role-arn-from-stack> --type STANDARD
aws eks associate-access-policy --cluster-name <your-cluster> \
  --principal-arn <role-arn-from-stack> \
  --policy-arn arn:aws:eks::aws:cluster-access-policy/AmazonEKSEditPolicy \
  --access-scope type=namespace,namespaces=dev,qa,prod
```

The role can then deploy to dev, qa and prod and nothing else in the cluster.

## 3. GitHub settings (Settings > Secrets and variables > Actions > Variables)

| Variable | Value |
|---|---|
| `DEPLOY_ENABLED` | `true` (turn it on last) |
| `AWS_ROLE_ARN` | role ARN from the stack |
| `AWS_REGION` | for example `ap-south-1` |
| `ECR_REPOSITORY` | `items-service` |
| `EKS_CLUSTER_NAME` | your cluster |

Settings > Environments: create `dev`, `qa` and `prod`. On `prod` add **Required reviewers** (yourself) so the last step waits
for an approval.

## 4. Make the quality gate block merges

Settings > Branches > add a rule for `main`: **Require a pull request before merging**, **Require status checks to pass**
and select `test-and-gate`, `helm` and `infra`. From then on a pull request whose SonarQube gate fails cannot be merged.

## Cluster endpoint note

The deploy job runs on a GitHub-hosted runner, which needs to reach the EKS API. If the endpoint is private or restricted to
your own IP, run `scripts/promote.sh <dev|qa|prod> <commit-sha> <ecr-uri>` from a machine that can reach it instead.
