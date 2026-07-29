# Google Login YeYamo

## Architecture

Le mobile obtient un ID Token avec le module natif Google Sign-In, puis l’envoie à `POST /api/v1/auth/oauth/google` via l’API Gateway. `auth-service` vérifie la signature Google, l’issuer, l’audience WEB, l’expiration, `email_verified` et `sub`. Il lie ou crée ensuite le compte via `oauth_accounts`, puis émet les mêmes access/refresh tokens YeYamo que le login classique.

Le Google ID Token n’est jamais persisté. SecureStore contient uniquement les tokens YeYamo.

## Variables

Mobile :

- `EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID`
- `EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID`
- `EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID`

Backend :

- `GOOGLE_CLIENT_ID` : identique au WEB Client ID utilisé comme `webClientId` mobile.

Aucun client secret n’est nécessaire pour valider un ID Token.

## Configuration native

- Android : package `com.yeyamo.mobile`, Client ID Android restreint au package et au SHA-1 du Development Build.
- iOS : bundle ID `com.yeyamo.mobile`; le config plugin dérive le URL scheme inversé depuis `EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID`.
- Le module est natif et nécessite un Development Build ; Expo Go n’est pas supporté.

## Account linking

L’identité stable est `(provider, sub)`. Un email Google vérifié peut être lié transactionnellement à un compte YeYamo existant portant le même email. La contrainte unique de `oauth_accounts(provider, provider_user_id)` empêche les doubles identités. Les comptes bannis, verrouillés ou supprimés restent refusés.

## Test manuel

1. Définir les quatre variables dans les secrets EAS et l’environnement backend.
2. Lancer `auth-service` et l’API Gateway.
3. Construire `eas build --platform android --profile development`.
4. Tester nouveau compte, compte email existant, annulation, redémarrage, refresh et logout.
5. Répéter avec un Development Build iOS et vérifier le retour par URL scheme.

## Erreurs courantes

- `DEVELOPER_ERROR` Android : package/SHA-1/Client ID incohérents.
- audience invalide : `GOOGLE_CLIENT_ID` backend différent du WEB Client ID mobile.
- iOS sans retour : URL scheme inversé absent ou Client ID iOS incorrect.
- Play Services indisponible : mettre à jour Google Play Services sur l’appareil.
