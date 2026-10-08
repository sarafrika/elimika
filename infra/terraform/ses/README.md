# Amazon SES (Terraform)

Creates the SES side of outbound mail for Elimika: the `sarafrika.com` domain identity with Easy DKIM, a custom MAIL FROM domain, account-level bounce/complaint suppression, and an IAM user whose access key becomes the SMTP password.

DNS for `sarafrika.com` is hosted in cPanel, not Route 53, so Terraform prints the records instead of creating them.

## Flow

```
Spring Boot (JavaMailSender) ─┐
                              ├─ SMTP :587 STARTTLS ─▶ email-smtp.<region>.amazonaws.com ─▶ recipient
Keycloak realm email ─────────┘        (IAM SMTP user, scoped to no-reply@sarafrika.com)
DNS (cPanel): DKIM CNAMEs + MAIL FROM MX/SPF + DMARC ─▶ SES verifies the domain and signs mail
```

## Apply

Terraform uses the `sarafrika` AWS CLI profile. Create it once with `aws configure --profile sarafrika` (region `eu-west-1`).
The profile's IAM user needs `deployer-policy.json` attached; it covers SES and only the `elimika-ses-smtp` IAM user.

```bash
cd infra/terraform/ses
cp terraform.tfvars.example terraform.tfvars   # adjust region if needed
terraform init
terraform apply
terraform output dns_records                    # add these in cPanel
terraform output -raw smtp_password             # shown once here; store it as a secret
```

The state file contains the SMTP password. It stays local and is gitignored, so keep it somewhere safe or move it to a remote backend.

## After apply

1. Add the `dns_records` in cPanel. If `sarafrika.com` already has an SPF record, keep one record and add `include:amazonses.com` to it. The MAIL FROM SPF is on `mail.sarafrika.com`, so it doesn't conflict with the root SPF.
2. Wait until SES shows the identity as **Verified**.
3. Request production access from the SES console (Account dashboard). Terraform doesn't do this step.
4. Set the backend env vars and restart the `elimika` container:
   ```
   MAIL_SERVER=<smtp_host>
   MAIL_SERVER_PORT=587
   MAIL_USERNAME=<smtp_username>
   MAIL_PASSWORD=<smtp_password>
   ```
5. In Keycloak, set the realm email (Realm settings → Email) to the same host, port 587, StartTLS and credentials, with From `no-reply@sarafrika.com`.

## Rotating the SMTP password

Run `terraform apply -replace=aws_iam_access_key.smtp`, then update `MAIL_PASSWORD` and the Keycloak credentials.
