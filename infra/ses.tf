# SES identity for sending submission notifications. Verifying the whole
# domain (rather than one address) lets the backend send from any @mkgg.dev
# address and — because sandbox mode allows sending to verified identities —
# also send *to* hello@mkgg.dev without ever requesting production access.
#
# Verification requires the three DKIM CNAMEs from `terraform output
# ses_dkim_records` to be added at Porkbun (same manual step as the ACM
# validation record). Porkbun's email-forwarding MX records must stay as-is.
resource "aws_sesv2_email_identity" "mail" {
  email_identity = var.mail_domain

  tags = {
    Project = var.project
  }
}
