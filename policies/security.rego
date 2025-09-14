package kubernetes.security

import rego.v1

# Deny containers using :latest tag
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    endswith(container.image, ":latest")
    msg := sprintf("Container '%s' uses ':latest' tag which is not allowed in production", [container.name])
}

# Deny containers without explicit tag
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    not contains(container.image, ":")
    msg := sprintf("Container '%s' must specify an explicit image tag", [container.name])
}

# Require resource requests for CPU
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    not container.resources.requests.cpu
    msg := sprintf("Container '%s' must specify CPU resource requests", [container.name])
}

# Require resource requests for memory
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    not container.resources.requests.memory
    msg := sprintf("Container '%s' must specify memory resource requests", [container.name])
}

# Require resource limits for CPU
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    not container.resources.limits.cpu
    msg := sprintf("Container '%s' must specify CPU resource limits", [container.name])
}

# Require resource limits for memory
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    not container.resources.limits.memory
    msg := sprintf("Container '%s' must specify memory resource limits", [container.name])
}

# Require liveness probe
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    not container.livenessProbe
    msg := sprintf("Container '%s' must specify a liveness probe", [container.name])
}

# Require readiness probe
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    not container.readinessProbe
    msg := sprintf("Container '%s' must specify a readiness probe", [container.name])
}

# Disallow privileged containers
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    container.securityContext.privileged == true
    msg := sprintf("Container '%s' cannot run in privileged mode", [container.name])
}

# Disallow running as root (UID 0)
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    container.securityContext.runAsUser == 0
    msg := sprintf("Container '%s' cannot run as root user (UID 0)", [container.name])
}

# Require non-root user
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    not container.securityContext.runAsNonRoot
    msg := sprintf("Container '%s' must specify runAsNonRoot: true", [container.name])
}

# Disallow hostNetwork
deny contains msg if {
    input.kind == "Deployment"
    input.spec.template.spec.hostNetwork == true
    msg := "Deployment cannot use hostNetwork"
}

# Disallow hostPID
deny contains msg if {
    input.kind == "Deployment"
    input.spec.template.spec.hostPID == true
    msg := "Deployment cannot use hostPID"
}

# Disallow hostIPC
deny contains msg if {
    input.kind == "Deployment"
    input.spec.template.spec.hostIPC == true
    msg := "Deployment cannot use hostIPC"
}

# Require security context for pod
deny contains msg if {
    input.kind == "Deployment"
    not input.spec.template.spec.securityContext
    msg := "Deployment must specify a pod security context"
}

# Disallow allowPrivilegeEscalation
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    container.securityContext.allowPrivilegeEscalation == true
    msg := sprintf("Container '%s' cannot allow privilege escalation", [container.name])
}

# Require readOnlyRootFilesystem
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    not container.securityContext.readOnlyRootFilesystem
    msg := sprintf("Container '%s' should use read-only root filesystem", [container.name])
}

# Disallow dangerous capabilities
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    dangerous_caps := {"SYS_ADMIN", "NET_ADMIN", "SYS_TIME", "SYS_MODULE"}
    cap := container.securityContext.capabilities.add[_]
    cap in dangerous_caps
    msg := sprintf("Container '%s' cannot add dangerous capability '%s'", [container.name, cap])
}

# Require dropping ALL capabilities
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    not "ALL" in container.securityContext.capabilities.drop
    msg := sprintf("Container '%s' must drop ALL capabilities", [container.name])
}

# Validate service account
deny contains msg if {
    input.kind == "Deployment"
    input.spec.template.spec.serviceAccountName == "default"
    msg := "Deployment should not use the default service account"
}

# Require automountServiceAccountToken to be false if not needed
warn contains msg if {
    input.kind == "Deployment"
    not input.spec.template.spec.automountServiceAccountToken == false
    msg := "Consider setting automountServiceAccountToken: false if service account token is not needed"
}

# Validate image registry
deny contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    not startswith(container.image, "ghcr.io/hopngo")
    not startswith(container.image, "gcr.io/hopngo")
    not startswith(container.image, "registry.hopngo.com")
    msg := sprintf("Container '%s' must use approved registry (ghcr.io/hopngo, gcr.io/hopngo, or registry.hopngo.com)", [container.name])
}

# Require specific labels
deny contains msg if {
    input.kind == "Deployment"
    required_labels := {"app", "version", "environment"}
    label := required_labels[_]
    not input.metadata.labels[label]
    msg := sprintf("Deployment must have label '%s'", [label])
}

# Validate environment label values
deny contains msg if {
    input.kind == "Deployment"
    env_label := input.metadata.labels.environment
    not env_label in {"development", "staging", "production"}
    msg := sprintf("Environment label must be one of: development, staging, production. Got: '%s'", [env_label])
}

# Require replica count constraints
deny contains msg if {
    input.kind == "Deployment"
    input.metadata.labels.environment == "production"
    input.spec.replicas < 2
    msg := "Production deployments must have at least 2 replicas for high availability"
}

# Validate resource limits are not too high
warn contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    cpu_limit := container.resources.limits.cpu
    cpu_limit_num := to_number(trim_suffix(cpu_limit, "m")) / 1000
    cpu_limit_num > 2
    msg := sprintf("Container '%s' has high CPU limit (%s). Consider if this is necessary.", [container.name, cpu_limit])
}

warn contains msg if {
    input.kind == "Deployment"
    container := input.spec.template.spec.containers[_]
    memory_limit := container.resources.limits.memory
    memory_limit_bytes := to_number(trim_suffix(memory_limit, "Gi")) * 1024 * 1024 * 1024
    memory_limit_bytes > 4 * 1024 * 1024 * 1024  # 4Gi
    msg := sprintf("Container '%s' has high memory limit (%s). Consider if this is necessary.", [container.name, memory_limit])
}