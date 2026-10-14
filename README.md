# 🚀 Deploy a Java Application on AWS Using 3-Tier Architecture

![AWS Architecture](https://imgur.com/3XF0tlJ.png)

A hands-on cloud and DevOps project demonstrating how to deploy a Java web application on AWS using a secure, scalable, and highly available three-tier architecture.

The project covers VPC networking, Application Load Balancing, EC2 Auto Scaling, Amazon RDS for MySQL, IAM, monitoring, and application deployment using Maven.

> **Project status:** This README describes the target architecture and implementation guide. Update the project status and architecture diagram to reflect the infrastructure actually deployed in your AWS account.

---

## 📑 Table of Contents

- [Project Overview](#-project-overview)
- [Architecture Overview](#-architecture-overview)
- [AWS Services Used](#-aws-services-used)
- [Prerequisites](#-prerequisites)
- [Infrastructure Setup](#-infrastructure-setup)
- [Database Configuration](#-database-configuration)
- [Java Application Setup](#-java-application-setup)
- [Deployment on AWS](#-deployment-on-aws)
- [Load Balancing and Auto Scaling](#-load-balancing-and-auto-scaling)
- [Monitoring and Logging](#-monitoring-and-logging)
- [Security Best Practices](#-security-best-practices)
- [Troubleshooting](#-troubleshooting)
- [Cost Optimization](#-cost-optimization)
- [Future Enhancements](#-future-enhancements)
- [Contributing](#-contributing)
- [Author](#-author)

---

## 🎯 Project Overview

### Introduction

This project demonstrates how to deploy a Java-based web application on Amazon Web Services using a three-tier architecture.

The infrastructure separates incoming traffic, application processing, and persistent data storage into distinct layers. This improves security, simplifies maintenance, and provides a foundation for scaling the application as demand increases.

### Key Features

- **High Availability:** Deploy application instances across multiple Availability Zones.
- **Load Balancing:** Distribute incoming HTTP/HTTPS traffic using an Application Load Balancer.
- **Auto Scaling:** Automatically adjust application capacity based on demand.
- **Private Database:** Keep Amazon RDS MySQL inaccessible directly from the public internet.
- **Secure Networking:** Apply subnet isolation and least-privilege security group rules.
- **Monitoring:** Collect application logs and infrastructure metrics with Amazon CloudWatch.
- **Automated Deployment:** Build and package the Java application using Apache Maven.
- **Extensibility:** Add Jenkins CI/CD, Terraform, CloudFront, and other services as needed.

## 🏗️ Architecture Overview

### Three-Tier Design

#### 1. Presentation Tier

The presentation tier handles incoming client traffic.

- Internet-facing Application Load Balancer (ALB).
- HTTPS termination using an AWS Certificate Manager certificate.
- Optional Amazon CloudFront distribution for static content.
- Public subnets distributed across at least two Availability Zones.

#### 2. Application Tier

The application tier processes business logic and communicates with the database.

- Amazon EC2 instances running the Java application.
- Spring Boot with embedded Tomcat, or an appropriately configured external servlet container.
- EC2 Auto Scaling Group for horizontal scaling.
- Private application subnets across multiple Availability Zones.
- IAM instance role for authorized access to AWS services.

#### 3. Data Tier

The data tier stores application data.

- Amazon RDS for MySQL.
- Private database subnets.
- Multi-AZ deployment for database availability.
- Automated backups and point-in-time recovery.
- Security groups allowing MySQL traffic only from the application tier.

### Architecture Diagram

```text
                       Internet Users
                             |
                             v
                  +----------------------+
                  | Application Load     |
                  | Balancer (HTTPS)     |
                  +----------------------+
                             |
                 +-----------+-----------+
                 |                       |
                 v                       v
          Availability Zone A     Availability Zone B
          +----------------+      +----------------+
          | Private App    |      | Private App    |
          | EC2 Instance   |      | EC2 Instance   |
          +----------------+      +----------------+
                 |                       |
                 +-----------+-----------+
                             |
                             v
                  +----------------------+
                  | Amazon RDS MySQL     |
                  | Private DB Subnets   |
                  | Multi-AZ             |
                  +----------------------+

                  VPC spanning two AZs
```

**Networking model:** The ALB is placed in public subnets, application instances in private subnets, and the database in private database subnets. NAT Gateways may be used when private instances require outbound internet access for updates or external services. They are not required for inbound application traffic.

## ☁️ AWS Services Used

| Service | Purpose |
|---|---|
| Amazon VPC | Isolated network environment |
| EC2 | Hosts the Java application |
| Application Load Balancer | Distributes incoming requests |
| EC2 Auto Scaling | Maintains and adjusts application capacity |
| Amazon RDS for MySQL | Managed relational database |
| IAM | Identity and access management |
| CloudWatch | Metrics, logs, and alarms |
| AWS Certificate Manager | TLS certificates for HTTPS |
| Systems Manager | Secure instance administration |
| Amazon S3 | Optional artifact or backup storage |
| CloudFront | Optional content delivery |
| Route 53 | Optional custom domain and DNS management |

---

## 🧰 Prerequisites

Before starting, ensure you have:

- An AWS account with permissions to create the required resources.
- AWS CLI v2 installed and configured.
- Git installed.
- Java 17 or a compatible version for your application.
- Apache Maven installed.
- A working Java/Spring Boot application.
- Basic knowledge of networking, security groups, and Linux commands.

### Install AWS CLI

For Linux:

```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

For macOS:

```bash
brew install awscli
```

For Windows, follow the official [AWS CLI installation guide](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html).

Verify your installation:

```bash
aws --version
aws sts get-caller-identity
aws configure
```

Use an IAM Identity Center session or another supported credential mechanism where possible. Never commit access keys or credentials to GitHub.

### Install Java and Maven

Verify your development environment:

```bash
java -version
javac -version
mvn -version
git --version
```

Use a Java version supported by your application's framework and build configuration. The examples below use Java 17.

---

## 🌐 Infrastructure Setup

### 1. Choose the AWS Region

Set the deployment region and verify your active AWS identity.

```bash
export AWS_REGION=us-east-1
aws sts get-caller-identity
```

The commands in this guide use Bash syntax. In Windows PowerShell, set the variable using:

```powershell
$env:AWS_REGION = "us-east-1"
```

### 2. Create a VPC

```bash
aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --region "$AWS_REGION" \
  --tag-specifications \
  'ResourceType=vpc,Tags=[{Key=Name,Value=JavaAppVPC}]'
```

Save the returned VPC ID and use it in subsequent commands.

The VPC will contain:

- Two public subnets for the ALB.
- Two private application subnets.
- Two private database subnets.

Use non-overlapping CIDR blocks for each subnet and select subnets in different Availability Zones.

### 3. Create Subnets

Example subnet plan:

| Subnet | CIDR block | Availability Zone | Purpose |
|---|---|---|---|
| Public A | `10.0.1.0/24` | AZ A | ALB |
| Public B | `10.0.2.0/24` | AZ B | ALB |
| Application A | `10.0.11.0/24` | AZ A | EC2 |
| Application B | `10.0.12.0/24` | AZ B | EC2 |
| Database A | `10.0.21.0/24` | AZ A | RDS |
| Database B | `10.0.22.0/24` | AZ B | RDS |

Replace AZ placeholders with Availability Zones available in your selected region.

Example:

```bash
aws ec2 create-subnet \
  --vpc-id "$VPC_ID" \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications \
  'ResourceType=subnet,Tags=[{Key=Name,Value=PublicSubnetA}]'
```

Repeat for the remaining subnets with their respective CIDR blocks and Availability Zones.

### 4. Configure Internet Access

Create an Internet Gateway and attach it to the VPC.

```bash
aws ec2 create-internet-gateway \
  --tag-specifications \
  'ResourceType=internet-gateway,Tags=[{Key=Name,Value=JavaAppIGW}]'
```

Attach the returned gateway ID:

```bash
aws ec2 attach-internet-gateway \
  --internet-gateway-id "$IGW_ID" \
  --vpc-id "$VPC_ID"
```

Create a public route table and add a default route through the Internet Gateway. Associate it with both public subnets.

```bash
aws ec2 create-route-table \
  --vpc-id "$VPC_ID"
```

```bash
aws ec2 create-route \
  --route-table-id "$PUBLIC_ROUTE_TABLE_ID" \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id "$IGW_ID"
```

Associate the route table with each public subnet:

```bash
aws ec2 associate-route-table \
  --route-table-id "$PUBLIC_ROUTE_TABLE_ID" \
  --subnet-id "$PUBLIC_SUBNET_A_ID"
```

Repeat for the second public subnet.

Private application subnets should not have a direct route to an Internet Gateway. If outbound internet access is necessary, configure NAT Gateway routing or use appropriate VPC endpoints and Systems Manager.

### 5. Configure Security Groups

Create separate security groups for the ALB, application instances, and database.

| Security group | Inbound rule | Source |
|---|---|---|
| ALB security group | HTTPS, TCP 443 | Approved client networks or `0.0.0.0/0` for a public website |
| Application security group | Application port, e.g. TCP 8080 | ALB security group |
| Database security group | MySQL, TCP 3306 | Application security group |

Create the ALB security group:

```bash
aws ec2 create-security-group \
  --group-name JavaAppALBSG \
  --description "Security group for the application load balancer" \
  --vpc-id "$VPC_ID"
```

Create the application security group:

```bash
aws ec2 create-security-group \
  --group-name JavaAppAppSG \
  --description "Security group for Java application instances" \
  --vpc-id "$VPC_ID"
```

Create the database security group:

```bash
aws ec2 create-security-group \
  --group-name JavaAppDBSG \
  --description "Security group for RDS MySQL" \
  --vpc-id "$VPC_ID"
```

After creating the groups, configure the inbound rules using their returned IDs. For example, allow application traffic only from the ALB security group:

```bash
aws ec2 authorize-security-group-ingress \
  --group-id "$APP_SG_ID" \
  --protocol tcp \
  --port 8080 \
  --source-group "$ALB_SG_ID"
```

For the database, allow MySQL only from the application security group:

```bash
aws ec2 authorize-security-group-ingress \
  --group-id "$DB_SG_ID" \
  --protocol tcp \
  --port 3306 \
  --source-group "$APP_SG_ID"
```

Configure the ALB's inbound HTTPS rule and the appropriate outbound rules. For HTTP-to-HTTPS redirection, add an HTTP listener on port 80 and redirect requests to HTTPS.

Do not expose ports 8080 or 3306 directly to the internet.

---

## 🗄️ Database Configuration

### 1. Create a DB Subnet Group

First, create the private database subnets in two different Availability Zones. Then create a DB subnet group containing both subnet IDs.

```bash
aws rds create-db-subnet-group \
  --db-subnet-group-name javaapp-db-subnets \
  --db-subnet-group-description "Private subnets for JavaApp RDS" \
  --subnet-ids "$DB_SUBNET_A_ID" "$DB_SUBNET_B_ID"
```

### 2. Create an RDS MySQL Instance

Store the database password securely and supply it through a supported secret-management mechanism or an interactive deployment workflow. Do not hardcode production credentials in scripts or version control.

Example command:

```bash
aws rds create-db-instance \
  --db-instance-identifier javaapp-mysql \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --master-username dbadmin \
  --manage-master-user-password \
  --allocated-storage 20 \
  --storage-type gp3 \
  --db-name javaapp \
  --db-subnet-group-name javaapp-db-subnets \
  --vpc-security-group-ids "$DB_SG_ID" \
  --no-publicly-accessible \
  --backup-retention-period 7 \
  --region "$AWS_REGION"
```

Choose a supported database instance class for your region and account. For a production deployment, select capacity based on workload and use a Multi-AZ configuration.

Enable Multi-AZ when required:

```bash
aws rds modify-db-instance \
  --db-instance-identifier javaapp-mysql \
  --multi-az \
  --apply-immediately
```

Use `--apply-immediately` only when you accept the possibility of immediate changes and associated disruption. For planned production changes, schedule an appropriate maintenance window.

### 3. Initialize the Database

Connect to the RDS endpoint from an authorized machine or application environment with network access to the private database.

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Use application migrations, such as Flyway or Liquibase, for repeatable schema management.

**Security note:** Store password hashes produced by a suitable password-hashing algorithm such as Argon2id or bcrypt. Never store plaintext user passwords.

---

## ☕ Java Application Setup

### 1. Clone the Repository

Replace the example URL with your actual repository.

```bash
git clone https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
cd YOUR_REPOSITORY
```

### 2. Configure Maven

Ensure your `pom.xml` uses a compatible Java version and the required dependencies.

Example properties for a Spring Boot 3.x application:

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

Use a supported Spring Boot release and its corresponding dependency-management configuration. Avoid mixing incompatible Spring Boot, Java, and servlet-container versions.

### 3. Configure the Application

Externalize the database settings rather than committing credentials.

Example `application.properties`:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
server.port=8080
```

The database URL should use the private RDS endpoint, for example:

```text
jdbc:mysql://YOUR_RDS_ENDPOINT:3306/javaapp
```

Configure secrets through AWS Secrets Manager, Systems Manager Parameter Store, or another secure mechanism. Give the application role access only to the secrets it needs.

### 4. Build and Test

```bash
mvn clean test
mvn clean package
```

Check the generated artifact in the project's `target/` directory.

For an executable Spring Boot JAR:

```bash
java -jar target/YOUR_APPLICATION.jar
```

Use the actual filename generated by your Maven build. Do not skip tests in the standard build unless you have a specific reason.

---

## 🚀 Deployment on AWS

### 1. Prepare the EC2 Instances

Launch EC2 instances in the private application subnets, preferably through an Auto Scaling Group.

Recommended configuration:

- A supported Linux AMI.
- A compatible Java runtime.
- Application artifact downloaded from a controlled artifact repository or S3 bucket.
- An IAM instance profile with least-privilege permissions.
- CloudWatch Agent for logs and metrics.
- Systems Manager access for administration.
- A service manager such as `systemd` to run the application.

Avoid storing AWS access keys on the EC2 instances.

### 2. Run the Application as a Service

For a Linux EC2 instance running an executable JAR, create a systemd service.

Example `/etc/systemd/system/javaapp.service`:

```ini
[Unit]
Description=Java Web Application
After=network.target

[Service]
Type=simple
User=javaapp
WorkingDirectory=/opt/javaapp
EnvironmentFile=/etc/javaapp/javaapp.env
ExecStart=/usr/bin/java -jar /opt/javaapp/app.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Create a dedicated service account and configure the environment file with restricted permissions. Ensure the configured Java executable path matches the installed runtime.

Enable and start the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable javaapp
sudo systemctl start javaapp
sudo systemctl status javaapp
```

Check logs:

```bash
sudo journalctl -u javaapp -f
```

The application should expose a health endpoint, such as `/health`, that the load balancer can use to determine whether the application is ready to receive traffic.

### 3. Configure the Application Load Balancer

Create an ALB in the two public subnets.

Create a target group configured for the application's traffic port and protocol. Register the application instances through the Auto Scaling Group and configure a health-check path that returns a successful response when the application is healthy.

Configure listeners:

- Port 80: Redirect HTTP to HTTPS.
- Port 443: Terminate TLS using an ACM certificate and forward requests to the target group.

Verify that the ALB reports healthy targets before testing the application.

### 4. Verify the Deployment

Test the ALB DNS name:

```bash
curl -I https://YOUR_ALB_DNS_NAME
```

If you have a suitable health endpoint:

```bash
curl https://YOUR_ALB_DNS_NAME/health
```

A successful HTTP response confirms that the endpoint is responding. Also test actual application functionality and database connectivity.

---

## ⚖️ Load Balancing and Auto Scaling

Configure an EC2 Launch Template with:

- A supported AMI and instance type.
- The application security group.
- An IAM instance profile.
- Bootstrap configuration or an image containing the application.
- Appropriate metadata-service security settings.

Create an Auto Scaling Group using the Launch Template and place it in both private application subnets.

Example configuration values:

| Setting | Example |
|---|---|
| Minimum capacity | 2 instances |
| Desired capacity | 2 instances |
| Maximum capacity | 4 instances |
| Health check type | ELB |
| Health check grace period | 300 seconds |
| Scaling metric | Average CPU utilization |
| Example target CPU | 60% |

These are example values, not universal production recommendations.

Configure a target-tracking scaling policy through the AWS console or CLI. Confirm that the Auto Scaling Group is attached to the ALB target group.

For session-based applications, prefer stateless session management or an appropriate shared session store. Do not assume that local session state will be available when requests are routed to different instances.

---

## 📊 Monitoring and Logging

### Amazon CloudWatch

Monitor:

- EC2 CPU utilization and status checks.
- ALB request count, response time, and HTTP errors.
- Target health and unhealthy host counts.
- RDS CPU, database connections, storage, and latency.
- Application errors and service restarts.

Create CloudWatch alarms for significant failures and capacity issues.

### Application Logs

Collect application and system logs using the CloudWatch Agent or another centralized logging solution.

For a systemd-based application:

```bash
sudo journalctl -u javaapp --since "1 hour ago"
```

Configure log retention, access controls, and appropriate masking of sensitive information. Never log passwords, session tokens, or other secrets.

### CloudWatch Agent

Install and configure the CloudWatch Agent using the official AWS instructions for your operating system. Grant it the minimum permissions required to publish metrics and logs.

For memory and disk monitoring, configure the agent to collect operating-system metrics; EC2's default metrics do not include every memory and disk metric.

---

## 🔐 Security Best Practices

### Network Security

- Keep application and database instances in private subnets.
- Restrict inbound access through security groups.
- Use Systems Manager instead of exposing SSH or RDP unnecessarily.
- Use VPC Flow Logs where useful for network diagnostics.
- Use HTTPS and valid TLS certificates for public endpoints.

### Identity and Secrets

- Follow least-privilege IAM policies.
- Use IAM roles instead of long-lived access keys.
- Store database credentials in Secrets Manager or Parameter Store.
- Rotate secrets according to your security requirements.
- Avoid committing `.env` files, passwords, certificates, or private keys.

### Data Protection

- Enable RDS encryption at rest and use TLS for database connections.
- Configure automated backups and test restoration.
- Use appropriate EBS encryption and S3 bucket protections.
- Apply operating-system and application security updates.
- Enable relevant AWS security monitoring services where appropriate.

### Production Readiness

- Use multiple Availability Zones.
- Define backup retention and recovery objectives.
- Test failover and recovery procedures.
- Review CloudWatch alarms and operational logs.
- Use infrastructure as code for repeatable provisioning.

---

## 🛠️ Troubleshooting

| Issue | Possible cause | What to check |
|---|---|---|
| ALB returns 502 or 503 | Application unavailable or targets unhealthy | Target health, service status, port, and logs |
| ALB health checks fail | Wrong path, port, or response code | Health-check configuration and application endpoint |
| Application cannot connect to RDS | Incorrect endpoint or security group rules | DB endpoint, VPC routing, port 3306, credentials |
| Application fails to start | Java version mismatch or missing configuration | `java -version`, systemd logs, environment variables |
| EC2 cannot download artifacts | Missing egress or repository access | NAT Gateway, VPC endpoints, IAM permissions |
| HTTPS certificate errors | Invalid certificate or listener configuration | ACM certificate status, domain name, listener |
| Auto Scaling does not add instances | Missing policy or incorrect metrics | Scaling policy, CloudWatch metrics, ASG activity |
| High database latency | Insufficient capacity or inefficient queries | RDS metrics, query plans, indexes, connection pool |

Useful diagnostic commands:

```bash
# Application service status
sudo systemctl status javaapp

# Recent application logs
sudo journalctl -u javaapp -n 100 --no-pager

# Check listening ports
sudo ss -lntp

# Check disk space
df -h

# Check memory
free -m
```

AWS CLI health-check example:

```bash
aws elbv2 describe-target-health \
  --target-group-arn "$TARGET_GROUP_ARN" \
  --region "$AWS_REGION"
```

For database connectivity tests, use an authorized host within the VPC. A failed connection should be diagnosed by checking routing, security groups, endpoint configuration, and database availability.

---

## 💰 Cost Optimization

AWS resources can incur charges even when an application is not receiving traffic.

- Select EC2 and RDS sizes appropriate for the workload.
- Stop or delete unused development resources.
- Review NAT Gateway and data-transfer costs.
- Set AWS Budgets and billing alerts.
- Use S3 lifecycle policies for eligible stored artifacts.
- Avoid deploying optional services until they are needed.
- Review current AWS pricing and free-tier eligibility before deployment.

**Important:** Multi-AZ RDS, multiple EC2 instances, NAT Gateways, load balancers, and CloudWatch usage may incur charges. Check regional pricing before creating resources.

---

## 🚀 Future Enhancements

Possible extensions include:

- [ ] Jenkins-based CI/CD pipeline.
- [ ] Static code analysis using SonarQube or SonarCloud.
- [ ] Artifact management using JFrog Artifactory.
- [ ] Infrastructure provisioning using Terraform.
- [ ] Containerization with Docker.
- [ ] Container deployment using Amazon ECS or Amazon EKS.
- [ ] CloudFront distribution for static content.
- [ ] Route 53 custom domain configuration.
- [ ] Centralized logs, dashboards, and alerting.
- [ ] Automated backup restoration and disaster-recovery testing.
- [ ] Deployment strategies such as rolling or blue-green deployments.

Implement these incrementally and update the README when each feature has been tested.

---

## 🤝 Contributing

Contributions, bug reports, and suggestions are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Make your changes and run relevant tests.
4. Commit your changes with a descriptive message.
5. Push the branch and open a Pull Request.

Example:

```bash
git checkout -b feature/improve-deployment
git add .
git commit -m "Improve AWS deployment documentation"
git push origin feature/improve-deployment
```

Do not include AWS credentials, private keys, database passwords, or other sensitive configuration in Pull Requests.

---

## 👨‍💻 Author

**Mukul Sharma**

Building future-ready cloud and AI platforms, with a focus on AWS, DevOps, backend development, and AI/ML.

- **GitHub:** [MukulSha-afk](https://github.com/MukulSha-afk)
- **LinkedIn:** [Connect on LinkedIn](https://www.linkedin.com/)

Explore the repository, share feedback, or contribute improvements.

---

## ⭐ Support the Project

If you find this project useful:

- ⭐ Star the repository.
- 🍴 Fork it and experiment with the architecture.
- 🐛 Report issues and suggest improvements.
- 🤝 Contribute to the project.

**Disclaimer:** This repository provides an implementation guide. Actual high availability, security, scalability, and recovery depend on the resources deployed, their configuration, testing, and operational practices.
