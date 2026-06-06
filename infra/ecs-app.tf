resource "aws_security_group" "alb" {
  name        = "skills-${var.environment}-alb"
  description = "Allow HTTPS inbound to ALB"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "app" {
  name        = "skills-${var.environment}-app"
  description = "Allow inbound from ALB on port 8080"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "tls_private_key" "api" {
  algorithm = "RSA"
  rsa_bits  = 2048
}

resource "tls_self_signed_cert" "api" {
  private_key_pem = tls_private_key.api.private_key_pem

  subject {
    common_name  = "skills-${var.environment}.local"
    organization = "skills"
  }

  validity_period_hours = 8760
  early_renewal_hours   = 720

  allowed_uses = [
    "digital_signature",
    "key_encipherment",
    "server_auth",
  ]
}

resource "aws_acm_certificate" "api" {
  private_key      = tls_private_key.api.private_key_pem
  certificate_body = tls_self_signed_cert.api.cert_pem
}

data "aws_iam_policy_document" "ecs_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "execution" {
  name               = "skills-${var.environment}-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

resource "aws_iam_role_policy_attachment" "execution_policy" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "task" {
  name               = "skills-${var.environment}-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

data "aws_iam_policy_document" "task" {
  statement {
    effect    = "Allow"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_rds_cluster.main.master_user_secret[0].secret_arn]
  }
}

resource "aws_iam_role_policy" "task" {
  role   = aws_iam_role.task.id
  policy = data.aws_iam_policy_document.task.json
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/skills-${var.environment}"
  retention_in_days = 30
}

resource "aws_ecs_cluster" "main" {
  name = "skills-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name       = aws_ecs_cluster.main.name
  capacity_providers = ["FARGATE"]
}

resource "aws_ecs_task_definition" "app" {
  family                   = "skills-${var.environment}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "app"
      image     = var.image_uri
      essential = true

      portMappings = [
        { containerPort = 8080, protocol = "tcp" }
      ]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = var.environment },
        { name = "SPRING_DATA_REDIS_HOST", value = aws_elasticache_replication_group.main.primary_endpoint_address },
        { name = "SPRING_DATA_REDIS_PORT", value = tostring(var.redis_port) },
        { name = "SPRING_DATA_REDIS_SSL_ENABLED", value = "true" },
      ]

      secrets = [
        { name = "SPRING_DATASOURCE_URL", valueFrom = "${aws_rds_cluster.main.master_user_secret[0].secret_arn}:url::" },
        { name = "SPRING_DATASOURCE_USERNAME", valueFrom = "${aws_rds_cluster.main.master_user_secret[0].secret_arn}:username::" },
        { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = "${aws_rds_cluster.main.master_user_secret[0].secret_arn}:password::" },
      ]

      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health/readiness || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.app.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_lb" "main" {
  name               = "skills-${var.environment}"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id
}

resource "aws_lb_target_group" "app" {
  name        = "skills-${var.environment}"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/actuator/health/readiness"
    matcher             = "200"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.main.arn
  port              = 443
  protocol          = "HTTPS"
  certificate_arn   = aws_acm_certificate.api.arn
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

resource "aws_ecs_service" "app" {
  name            = "skills-${var.environment}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = "app"
    container_port   = 8080
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  depends_on = [aws_lb_listener.https]
}
