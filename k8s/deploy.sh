#!/bin/bash

# JavaDNS Kubernetes Deployment Script
set -euo pipefail

NAMESPACE=${NAMESPACE:-default}
IMAGE_TAG=${IMAGE_TAG:-latest}
IMAGE_NAME=${IMAGE_NAME:-javadns}

echo "🚀 Deploying JavaDNS to Kubernetes namespace: $NAMESPACE"

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl is not installed or not in PATH"
    exit 1
fi

# Check if we can connect to the cluster
if ! kubectl cluster-info &> /dev/null; then
    echo "❌ Cannot connect to Kubernetes cluster"
    exit 1
fi

# Create namespace if it doesn't exist
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Apply RBAC first
echo "📋 Creating RBAC resources..."
kubectl apply -f rbac.yaml -n $NAMESPACE

# Apply ConfigMap
echo "📋 Creating ConfigMap..."
kubectl apply -f configmap.yaml -n $NAMESPACE

# Apply Deployment
echo "📋 Creating Deployment..."
if [[ "$IMAGE_TAG" != "latest" ]]; then
    sed "s|image: javadns:latest|image: $IMAGE_NAME:$IMAGE_TAG|g" deployment.yaml | kubectl apply -f - -n $NAMESPACE
else
    kubectl apply -f deployment.yaml -n $NAMESPACE
fi

# Apply Services
echo "📋 Creating Services..."
kubectl apply -f service.yaml -n $NAMESPACE

# Apply HPA (optional)
echo "📋 Creating HPA..."
kubectl apply -f hpa.yaml -n $NAMESPACE || echo "⚠️  HPA creation failed - metrics server might not be available"

# Wait for rollout
echo "⏳ Waiting for deployment to be ready..."
kubectl rollout status deployment/javadns-deployment -n $NAMESPACE --timeout=300s

# Show deployment status
echo "✅ Deployment completed!"
echo
echo "📊 Deployment Status:"
kubectl get pods,svc,hpa -l app=javadns -n $NAMESPACE

echo
echo "🔍 Service Endpoints:"
echo "HTTP API: kubectl port-forward svc/javadns-http-service 8080:8080 -n $NAMESPACE"
echo "DNS Service: kubectl port-forward svc/javadns-dns-service 5353:53 -n $NAMESPACE"

echo
echo "📝 Useful Commands:"
echo "View logs: kubectl logs -f deployment/javadns-deployment -n $NAMESPACE"
echo "Scale deployment: kubectl scale deployment javadns-deployment --replicas=5 -n $NAMESPACE"
echo "Delete deployment: kubectl delete -f . -n $NAMESPACE"