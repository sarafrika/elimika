locals {
  mail_from_domain = "${var.mail_from_subdomain}.${var.domain}"
}

# Domain identity with Easy DKIM (RSA 2048).
resource "aws_sesv2_email_identity" "domain" {
  email_identity = var.domain

  dkim_signing_attributes {
    next_signing_key_length = "RSA_2048_BIT"
  }
}

resource "aws_sesv2_email_identity_mail_from_attributes" "domain" {
  email_identity         = aws_sesv2_email_identity.domain.email_identity
  mail_from_domain       = local.mail_from_domain
  behavior_on_mx_failure = "USE_DEFAULT_VALUE"
}

# Stop sending to addresses that bounced or complained, protecting account reputation.
resource "aws_sesv2_account_suppression_attributes" "this" {
  suppressed_reasons = ["BOUNCE", "COMPLAINT"]
}

# SMTP credentials are an IAM access key converted to an SES SMTP password.
resource "aws_iam_user" "smtp" {
  name = var.smtp_user_name
}

data "aws_iam_policy_document" "smtp_send" {
  statement {
    actions   = ["ses:SendRawEmail"]
    resources = [aws_sesv2_email_identity.domain.arn]

    condition {
      test     = "StringLike"
      variable = "ses:FromAddress"
      values   = var.from_addresses
    }
  }
}

resource "aws_iam_user_policy" "smtp_send" {
  name   = "ses-send"
  user   = aws_iam_user.smtp.name
  policy = data.aws_iam_policy_document.smtp_send.json
}

resource "aws_iam_access_key" "smtp" {
  user = aws_iam_user.smtp.name
}
