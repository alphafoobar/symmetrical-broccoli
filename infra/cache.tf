# ElastiCache Valkey
# Valkey is used instead of Redis because:
# - Valkey is the AWS-native, open-source fork of Redis (post Redis 7.2 licence change)
# - AWS ElastiCache for Valkey offers the same API as Redis but under the OSI-approved
#   BSD-3-Clause licence with full AWS managed service support
# - Functionally equivalent to Redis for Spring Data Redis / Spring Cache integration

resource "aws_security_group" "cache" {
  name        = "skills-${var.environment}-cache"
  description = "Allow inbound Valkey from the application"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_elasticache_subnet_group" "main" {
  name       = "skills-${var.environment}"
  subnet_ids = aws_subnet.private[*].id
}

resource "aws_elasticache_replication_group" "main" {
  replication_group_id = "skills-${var.environment}"
  description          = "Valkey cache for skills API"

  # Valkey 7.2 - Redis-compatible, OSI-licensed
  engine         = "valkey"
  engine_version = "7.2"
  node_type      = "cache.t4g.micro"

  num_cache_clusters         = 2
  automatic_failover_enabled = true
  multi_az_enabled           = true

  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.cache.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true

  tags = { Name = "skills-${var.environment}" }
}
