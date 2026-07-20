# Security Policy

## Reporting a vulnerability

If you discover a security issue in MediPro, please **do not** open a public GitHub issue with exploit details.

Contact:

| | |
|---|---|
| **Developer** | Bimal Lamichhane |
| **Email** | bimal.lamichhane@gmail.com |
| **Mobile** | 9855065327 |
| **Company** | Bimal Tech Solution |

Include: app version, device/Android version, steps to reproduce, and impact assessment.

We aim to acknowledge reports within **5 business days**.

## Scope

- MediPro Android app (`com.medipro.manager`)
- Firebase rules / Cloud Functions for this project (when deployed by Bimal Tech Solution)

Out of scope: third-party Firebase/Google infrastructure unless misconfiguration is in our repo.

## Safe disclosure

Please allow reasonable time to patch before public disclosure.

## Secrets

Never commit:

- `keystore.properties`, `*.jks`
- `google-services.json`
- Production API keys or OTP bypass credentials
