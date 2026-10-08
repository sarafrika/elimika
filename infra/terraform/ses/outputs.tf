output "dns_records" {
  description = "Records to add at the DNS host (cPanel). Merge the SPF include into any existing SPF record."
  value = concat(
    [for token in aws_sesv2_email_identity.domain.dkim_signing_attributes[0].tokens : {
      type  = "CNAME"
      name  = "${token}._domainkey.${var.domain}"
      value = "${token}.dkim.amazonses.com"
    }],
    [
      {
        type  = "MX"
        name  = local.mail_from_domain
        value = "10 feedback-smtp.${var.aws_region}.amazonses.com"
      },
      {
        type  = "TXT"
        name  = local.mail_from_domain
        value = "v=spf1 include:amazonses.com ~all"
      },
      {
        type  = "TXT"
        name  = "_dmarc.${var.domain}"
        value = "v=DMARC1; p=none; rua=mailto:dmarc@${var.domain}"
      },
    ],
  )
}

output "smtp_host" {
  value = "email-smtp.${var.aws_region}.amazonaws.com"
}

output "smtp_username" {
  value = aws_iam_access_key.smtp.id
}

output "smtp_password" {
  value     = aws_iam_access_key.smtp.ses_smtp_password_v4
  sensitive = true
}
