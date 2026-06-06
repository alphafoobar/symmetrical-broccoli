# Aurora Serverless v2 PostgreSQL
# Aurora Serverless is the preferred managed PostgreSQL option for this deployment:
# - Automatically scales compute capacity up/down based on load
# - Pauses to near-zero cost when idle (suitable for dev/staging environments)
# - No patching required for minor versions
# - Aurora Serverless v2 removed the cold-start latency of v1

resource "aws_db_subnet_group" "main" {
  name       = "skills-${var.environment}"
  subnet_ids = aws_subnet.private[*].id
}

resource "aws_security_group" "rds" {
  name        = "skills-${var.environment}-rds"
  description = "Allow inbound PostgreSQL from the application"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 5432
    to_port         = 5432
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

resource "aws_rds_cluster" "main" {
  cluster_identifier          = "skills-${var.environment}"
  engine                      = "aurora-postgresql"
  engine_mode                 = "provisioned"
  engine_version              = "16.4"
  database_name               = var.db_name
  master_username             = "skills_admin"
  manage_master_user_password = true
  db_subnet_group_name        = aws_db_subnet_group.main.name
  vpc_security_group_ids      = [aws_security_group.rds.id]
  deletion_protection         = true
  storage_encrypted           = true
  backup_retention_period     = 7
  skip_final_snapshot         = false
  final_snapshot_identifier   = "skills-${var.environment}-final"

  serverlessv2_scaling_configuration {
    min_capacity = 0.5
    max_capacity = 4.0
  }
}

resource "aws_rds_cluster_instance" "main" {
  cluster_identifier = aws_rds_cluster.main.id
  instance_class     = "db.serverless"
  engine             = aws_rds_cluster.main.engine
  engine_version     = aws_rds_cluster.main.engine_version
}
