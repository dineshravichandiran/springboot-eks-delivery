#!/usr/bin/env bash
# Promotes one image tag to one environment from a machine that can reach the cluster API
# (use this when the EKS API endpoint is private or restricted and GitHub-hosted runners cannot reach it).
# Usage: scripts/promote.sh <dev|qa|prod> <image-tag> <ecr-repository-uri>
set -euo pipefail

ENVIRONMENT="${1:?usage: promote.sh <dev|qa|prod> <image-tag> <ecr-repository-uri>}"
TAG="${2:?image tag required (the commit SHA that passed CI)}"
REPOSITORY="${3:?ECR repository URI required}"

case "$ENVIRONMENT" in dev|qa|prod) ;; *) echo "environment must be dev, qa or prod" >&2; exit 1 ;; esac
cd "$(dirname "$0")/.."

helm upgrade --install items-service helm/items-service \
  --namespace "$ENVIRONMENT" \
  -f "helm/items-service/values-${ENVIRONMENT}.yaml" \
  --set image.repository="$REPOSITORY" \
  --set image.tag="$TAG" \
  --atomic --wait --timeout 5m

kubectl -n "$ENVIRONMENT" rollout status deployment/items-service --timeout=120s
helm -n "$ENVIRONMENT" history items-service | tail -3
