SHELL := /bin/bash # Use bash syntax
KUBE_CONTEXT := "<cluster_name_goes_here>" # ensure to apply all resources to integration cluster only

.PHONY: all build run-local deploy-integration refresh-gcs

build:
	rm -rf build/libs
	gradle build
	jar -cf build/libs/resources.jar src/main/resources
	
refresh-gcs:
	gcloud config set project "$gcp_project"
	gsutil cp build/libs/req_server-0.0.1-SNAPSHOT.jar gs://example-mock-server-artifacts/example_mock_server.jar
	gsutil cp build/libs/resources.jar gs://example-mock-server-artifacts/resources.jar
	
deploy-integration: refresh-gcs
# Set kube context
	kubectl config use-context $(KUBE_CONTEXT)
	kubectl config set-context --current --namespace=tracking-exp
# Clean up old resources
	kubectl delete deployment --ignore-not-found=true example-mock-server
	kubectl delete secret example-mock-server-gcs-key --ignore-not-found=true
# Deploy new resources
	kubectl create secret generic example-mock-server-gcs-key --from-file=key.json=./src/main/resources/client.json
	kubectl apply -f k8s/deployment.yaml
	kubectl apply -f k8s/service.yaml
	kubectl apply -f k8s/pdb.yaml

run-local:
	trap 'kill %1; kill %2' SIGINT
	java -jar build/libs/req_server-0.0.1-SNAPSHOT.jar
