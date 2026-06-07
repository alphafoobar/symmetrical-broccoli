locals {
  api_dashboard_name = "${var.project}-${var.environment}-api"
}

resource "aws_cloudwatch_dashboard" "api" {
  dashboard_name = local.api_dashboard_name

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          region = var.aws_region
          title  = "API response time"
          view   = "timeSeries"
          period = 60
          stat   = "p95"
          yAxis = {
            left = {
              label = "seconds"
              min   = 0
            }
          }
          metrics = [
            [
              "AWS/ApplicationELB",
              "TargetResponseTime",
              "LoadBalancer",
              aws_lb.main.arn_suffix,
              "TargetGroup",
              aws_lb_target_group.app.arn_suffix,
              {
                label = "p50"
                stat  = "p50"
              },
            ],
            [
              ".",
              ".",
              ".",
              ".",
              ".",
              ".",
              {
                label = "p95"
                stat  = "p95"
              },
            ],
            [
              ".",
              ".",
              ".",
              ".",
              ".",
              ".",
              {
                label = "max"
                stat  = "Maximum"
              },
            ],
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          region = var.aws_region
          title  = "API traffic and errors"
          view   = "timeSeries"
          period = 60
          stat   = "Sum"
          yAxis = {
            left = {
              label = "requests"
              min   = 0
            }
            right = {
              label = "error %"
              min   = 0
            }
          }
          metrics = [
            [
              "AWS/ApplicationELB",
              "RequestCount",
              "LoadBalancer",
              aws_lb.main.arn_suffix,
              "TargetGroup",
              aws_lb_target_group.app.arn_suffix,
              {
                id    = "requests"
                label = "requests"
              },
            ],
            [
              ".",
              "HTTPCode_Target_4XX_Count",
              ".",
              ".",
              ".",
              ".",
              {
                id    = "target4xx"
                label = "target 4xx"
              },
            ],
            [
              ".",
              "HTTPCode_Target_5XX_Count",
              ".",
              ".",
              ".",
              ".",
              {
                id    = "target5xx"
                label = "target 5xx"
              },
            ],
            [
              "AWS/ApplicationELB",
              "HTTPCode_ELB_5XX_Count",
              "LoadBalancer",
              aws_lb.main.arn_suffix,
              {
                id    = "elb5xx"
                label = "load balancer 5xx"
              },
            ],
            [
              {
                expression = "IF(requests > 0, 100 * (target5xx + elb5xx) / requests, 0)"
                id         = "error_rate"
                label      = "5xx error rate"
                yAxis      = "right"
              },
            ],
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 8
        height = 6
        properties = {
          region = var.aws_region
          title  = "Target health"
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          metrics = [
            [
              "AWS/ApplicationELB",
              "HealthyHostCount",
              "LoadBalancer",
              aws_lb.main.arn_suffix,
              "TargetGroup",
              aws_lb_target_group.app.arn_suffix,
              {
                label = "healthy targets"
                stat  = "Minimum"
              },
            ],
            [
              ".",
              "UnHealthyHostCount",
              ".",
              ".",
              ".",
              ".",
              {
                label = "unhealthy targets"
                stat  = "Maximum"
              },
            ],
          ]
        }
      },
      {
        type   = "metric"
        x      = 8
        y      = 6
        width  = 8
        height = 6
        properties = {
          region = var.aws_region
          title  = "ECS saturation"
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          yAxis = {
            left = {
              label = "percent"
              min   = 0
              max   = 100
            }
          }
          metrics = [
            [
              "AWS/ECS",
              "CPUUtilization",
              "ClusterName",
              aws_ecs_cluster.main.name,
              "ServiceName",
              aws_ecs_service.app.name,
              { label = "CPU" },
            ],
            [
              ".",
              "MemoryUtilization",
              ".",
              ".",
              ".",
              ".",
              { label = "memory" },
            ],
          ]
        }
      },
      {
        type   = "metric"
        x      = 16
        y      = 6
        width  = 8
        height = 6
        properties = {
          region = var.aws_region
          title  = "Aurora database"
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          metrics = [
            [
              "AWS/RDS",
              "CPUUtilization",
              "DBClusterIdentifier",
              aws_rds_cluster.main.cluster_identifier,
              { label = "CPU %" },
            ],
            [
              ".",
              "DatabaseConnections",
              ".",
              ".",
              { label = "connections" },
            ],
            [
              ".",
              "Deadlocks",
              ".",
              ".",
              {
                label = "deadlocks"
                stat  = "Sum"
              },
            ],
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 12
        width  = 12
        height = 6
        properties = {
          region = var.aws_region
          title  = "Aurora I/O latency"
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          yAxis = {
            left = {
              label = "seconds"
              min   = 0
            }
          }
          metrics = [
            [
              "AWS/RDS",
              "ReadLatency",
              "DBClusterIdentifier",
              aws_rds_cluster.main.cluster_identifier,
              { label = "read latency" },
            ],
            [
              ".",
              "WriteLatency",
              ".",
              ".",
              { label = "write latency" },
            ],
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 12
        width  = 12
        height = 6
        properties = {
          region = var.aws_region
          title  = "Aurora capacity and storage"
          view   = "timeSeries"
          period = 60
          stat   = "Average"
          metrics = [
            [
              "AWS/RDS",
              "ServerlessDatabaseCapacity",
              "DBClusterIdentifier",
              aws_rds_cluster.main.cluster_identifier,
              { label = "ACUs" },
            ],
            [
              ".",
              "VolumeBytesUsed",
              ".",
              ".",
              { label = "volume bytes used" },
            ],
          ]
        }
      },
    ]
  })
}
