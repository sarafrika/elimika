variable "aws_region" {
  description = "SES region; SMTP credentials only work against this region's endpoint."
  type        = string
  default     = "eu-west-1"
}

variable "aws_profile" {
  description = "AWS CLI profile for the Sarafrika account; pinned so a stray AWS_PROFILE can't target another client."
  type        = string
  default     = "sarafrika"
}

variable "domain" {
  description = "Sending domain verified in SES."
  type        = string
  default     = "sarafrika.com"
}

variable "mail_from_subdomain" {
  description = "Custom MAIL FROM subdomain, so SPF aligns with the sending domain."
  type        = string
  default     = "mail"
}

variable "from_addresses" {
  description = "Addresses the SMTP user may send as."
  type        = list(string)
  default     = ["no-reply@sarafrika.com"]
}

variable "smtp_user_name" {
  description = "IAM user that backs the SMTP credentials."
  type        = string
  default     = "elimika-ses-smtp"
}
