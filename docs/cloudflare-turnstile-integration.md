# Cloudflare Turnstile YeYamo

## Architecture

Le mobile ouvre `https://yeyamo.com/turnstile?action=...` dans une WebView restrictive. La page retourne un token éphémère avec `postMessage`. `auth-service` envoie ce token à Siteverify et exige simultanément `success=true`, `hostname=yeyamo.com` et l’action attendue.

La Site Key est publique. La Secret Key reste exclusivement dans l’environnement backend. Aucun token n’est persisté ou journalisé.

## Flux protégés

- inscription : `register`;
- renvoi OTP email : `resend_otp`;
- mot de passe oublié : `forgot_password`;
- login après trois échecs Redis : `login`.

La vérification OTP, Google Login et les API métier ne sont pas protégés. Le rate limiting Redis du gateway et le verrouillage login à cinq échecs restent actifs.

## Déploiement de la page

Le repository ne contient pas de landing web. Déployer `deploy/turnstile/index.html` sous `https://yeyamo.com/turnstile` en remplaçant `__TURNSTILE_SITE_KEY__` pendant le déploiement. Ne jamais injecter la Secret Key dans cette page.

Le vrai widget ne fonctionnera pas tant que cette URL HTTPS n’est pas publiée sur le hostname autorisé.

## Configuration

Mobile : `EXPO_PUBLIC_TURNSTILE_SITE_KEY`, `EXPO_PUBLIC_TURNSTILE_CHALLENGE_URL`.

Backend : `TURNSTILE_ENABLED`, `TURNSTILE_SECRET_KEY`, `TURNSTILE_VERIFY_URL`, `TURNSTILE_EXPECTED_HOSTNAME`, `LOGIN_TURNSTILE_THRESHOLD`.

En production, activer Turnstile et conserver `yeyamo.com`. En tests automatisés, désactiver l’adapter ou mocker Siteverify ; ne jamais appeler Cloudflare depuis les tests unitaires.

## Erreurs

Les erreurs publiques sont `TURNSTILE_REQUIRED`, `TURNSTILE_INVALID`, `TURNSTILE_EXPIRED`, `TURNSTILE_ACTION_MISMATCH`, `TURNSTILE_HOSTNAME_MISMATCH` et `TURNSTILE_PROVIDER_UNAVAILABLE`. Une indisponibilité Cloudflare échoue fermée sur les opérations protégées.
