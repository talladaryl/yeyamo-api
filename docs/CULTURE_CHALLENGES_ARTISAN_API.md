# Défis culturels et profils artisans

## Défis et transmission

`culture-service` conserve uniquement les références `postId`, `cultureContributionId` et `mediaIds`. Les posts restent dans `content-service` et les fichiers dans `media-service`.

Endpoints : `GET /api/v1/culture/challenges`, `GET /{id}`, `POST /{id}/join`, `POST /{id}/submissions`, `GET /{id}/submissions`, `GET /challenges/me` et `DELETE /api/v1/culture/challenge-submissions/{id}`.

La participation est unique par `(challengeId,userId)` et la soumission également, ce qui empêche une attribution XP répétée. `CultureChallengeCompleted` n’est émis qu’une fois pour une soumission acceptée sans modération. Une soumission en attente devra être complétée par le consumer de décision de modération lors du prochain incrément d’intégration avec `moderation-trust-service`.

Toute réponse enregistrant une autre personne exige `consentConfirmed=true`. L’identité est facultative et supprimée de la déclaration si `anonymized=true`. Les déclarations et retraits sont append-only dans `culture_consent_declarations`; le retrait masque logiquement la soumission sans supprimer l’audit.

Les indicateurs mineur, cérémonie sacrée, données personnelles, haine potentielle ou personne enregistrée imposent `PENDING_MODERATION`. Les événements Kafka contiennent uniquement les identifiants et motifs de routage nécessaires.

## Artisan

Le profil `artisan_profiles` est une extension 1:1 de `partners`; `owner_user_id` reste l’identité. Les anciens types `INDIVIDUAL`, `COMPANY`, `ASSOCIATION` et `PUBLIC_ORGANIZATION` sont conservés.

Endpoints propriétaire : `POST|GET|PUT /api/v1/partners/me/artisan-profile`. Endpoints publics : `GET /api/v1/artisans`, `GET /api/v1/artisans/{id}`, `GET /api/v1/artisans/{id}/profile`, `GET /api/v1/artisan-specialties`. Validation admin : `PATCH /api/v1/admin/artisans/{id}/verification`.

Les filtres publics couvrent pays, niveau administratif, ville, spécialité, vérification, commandes personnalisées, livraison internationale et recherche. Un profil public individuel n’est exposé que lorsqu’il est `VERIFIED`.

Les spécialités sont des lignes administrables, pas un enum Java. Les exigences KYC sont configurées dans `artisan_kyc_requirements` par `countryCode`, `partnerType` et `documentType`; une validation `VERIFIED` échoue si un document requis manque.
