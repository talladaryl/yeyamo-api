# Secret cleanup report

- Historical files removed from `main`: `.env` and the prior `.env.example`.
- `.env` is ignored and not tracked.
- A new `.env.example` contains variable names and only public defaults.
- Historical secret detections addressed: SMTP/Brevo and Google/GCP key material
  previously present in environment files.
- `target` tracked: 0; JAR/WAR/class tracked: 0.
- Credentials requiring rotation: all prior Brevo/Sendinblue SMTP credentials,
  Google/GCP API credentials, and any credentials formerly stored in `.env`.
- `git diff --check`: OK before this report was added.
- Push deploy: pending final commit and remote authentication.

Verdict: NOT SAFE TO USE FOR DOKPLOY until the sanitized history is pushed to
`deploy` and GitHub Push Protection accepts it without a bypass.
