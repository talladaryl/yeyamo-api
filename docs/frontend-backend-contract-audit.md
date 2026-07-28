# Audit des contrats backend → mobile YeYamo

Date de l'audit : 2026-07-26. Sources vérifiées : contrôleurs Spring, DTO/entités
sérialisées, sécurité Spring, configuration Gateway et annotations OpenAPI. Aucun
endpoint n'est déduit de l'UI.

## Constat Gateway bloquant

Le Gateway ne route actuellement que `/api/v1/analytics/**` et
`/api/v1/partners/**` parmi les nouveaux domaines. Il ne possède aucune route pour
`/api/v1/campaigns/**`, `/api/v1/ads/**`, `/api/v1/tickets/**` ou
`/api/v1/commerce/**`. De plus, `/api/v1/admin/**` est capturé par
`admin-service`; les endpoints `/api/v1/admin/campaigns/**` de campaign-service
ne sont donc pas joignables via le Gateway. Les lignes ci-dessous marquées
« non routé » existent dans le service, mais pas dans l'API mobile publique.

Tous les endpoints applicatifs utilisent `Authorization: Bearer <JWT>` sauf les
quatre endpoints Ads, dont le service n'a actuellement pas de chaîne de sécurité.
`X-Correlation-ID` est facultatif uniquement sur les mutations Campaign.
`Idempotency-Key` est obligatoire uniquement là où indiqué.

## Tableau des endpoints

| Feature | Méthode | Endpoint exact | Request | Response / succès | Permission | UI cible |
|---|---|---|---|---|---|---|
| Campaign liste | GET | `/api/v1/campaigns?status=&page=&size=&sort=` | `status?: CampaignStatus`; pagination Spring | `200 Page<CampaignResponse>` | `SCOPE_campaign:read`; JWT `partner_id` | Liste campagnes (non routé Gateway) |
| Campaign détail | GET | `/api/v1/campaigns/{id}` | `id: UUID` | `200 CampaignResponse` | `SCOPE_campaign:read`; même `partner_id` | Détail campagne (non routé) |
| Campaign création | POST | `/api/v1/campaigns` | `CreateCampaignRequest`; `X-Correlation-ID?` | `201 CampaignResponse` | `SCOPE_campaign:create` | Création (non routé) |
| Campaign modification | PUT | `/api/v1/campaigns/{id}` | `UpdateCampaignRequest`; `X-Correlation-ID?` | `200 CampaignResponse` | `SCOPE_campaign:update`; DRAFT seulement | Édition (non routé) |
| Campaign soumission | POST | `/api/v1/campaigns/{id}/submit` | aucun body; `X-Correlation-ID?` | `200 CampaignResponse` | `SCOPE_campaign:submit` | Soumission (non routé) |
| Campaign activation | POST | `/api/v1/campaigns/{id}/activate` | aucun body; `X-Correlation-ID?` | `200 CampaignResponse` | `SCOPE_campaign:update` | Activation (non routé) |
| Campaign pause | POST | `/api/v1/campaigns/{id}/pause` | aucun body; `X-Correlation-ID?` | `200 CampaignResponse` | `SCOPE_campaign:pause` | Pause (non routé) |
| Campaign reprise | POST | `/api/v1/campaigns/{id}/resume` | aucun body; `X-Correlation-ID?` | `200 CampaignResponse` | `SCOPE_campaign:update` | Reprise (non routé) |
| Campaign annulation | POST | `/api/v1/campaigns/{id}/cancel` | aucun body; `X-Correlation-ID?` | `200 CampaignResponse` | `SCOPE_campaign:update` | Annulation (non routé) |
| Campaign analytics | GET | `/api/v1/analytics/partners/{partnerId}/campaigns/{campaignId}` | `from`, `to` ISO date obligatoires; `timezone=UTC`, `page=0`, `size=50` (max 200) | `200 Page<Metrics>` | contextual `partner:analytics-view`, ADMIN/SUPER_ADMIN | Analytics campagne |
| Campaign admin liste | GET | `/api/v1/admin/campaigns?status=&page=&size=&sort=` | pagination Spring | `200 Page<CampaignResponse>` | `SCOPE_campaign:approve` ou `SCOPE_campaign:reject` | Admin (mal routé vers admin-service) |
| Campaign admin détail | GET | `/api/v1/admin/campaigns/{id}` | `id: UUID` | `200 CampaignResponse` | approve ou reject | Admin (mal routé) |
| Campaign approbation | POST | `/api/v1/admin/campaigns/{id}/approve` | aucun body; `X-Correlation-ID?` | `200 CampaignResponse` | `SCOPE_campaign:approve` | Admin (mal routé) |
| Campaign rejet | POST | `/api/v1/admin/campaigns/{id}/reject` | `{rejectionReason:string}` (10–1000) | `200 CampaignResponse` | `SCOPE_campaign:reject` | Admin (mal routé) |
| Ads select | POST | `/api/v1/ads/select` | `AdSelectionRequest` | `200 SponsoredPlacementResponse[]` | aucune auth service | Feed/discovery (non routé) |
| Ads impression | POST | `/api/v1/ads/impressions` | `{impressionToken,viewedAt,viewDurationMs?}` | `201`, body vide | token signé dans body | Tracking (non routé) |
| Ads clic | POST | `/api/v1/ads/clicks` | `{clickToken,clickedAt}` | `201`, body vide | token signé dans body | Tracking (non routé) |
| Ads conversion | POST | `/api/v1/ads/conversions` | `{deliveryId,convertedAt,conversionType,conversionValue?}` | `201`, body vide | aucune auth/token signé | Conversion (non routé) |
| Ticket hold | POST | `/api/v1/tickets/hold` | `CreateHoldRequest`; `Idempotency-Key` obligatoire | `200 HoldResponse` | authentifié | Réservation stock (non routé) |
| Ticket libération hold | DELETE | `/api/v1/tickets/hold/{holdId}` | `holdId: UUID` | `204` | authentifié; contrôle de propriété absent | Abandon panier (non routé) |
| Ticket commande | POST | `/api/v1/tickets/orders` | `{holdId:UUID,promotionCode?:string}` | `200 OrderResponse` | authentifié; pas d'Idempotency-Key HTTP | Achat (non routé) |
| Ticket commande détail | GET | `/api/v1/tickets/orders/{orderId}` | `orderId: UUID` | `200 OrderResponse` | propriétaire authentifié | Détail commande (non routé) |
| Ticket mes commandes | GET | `/api/v1/tickets/my-orders` | aucun | `200 OrderResponse[]` (non paginé) | authentifié | Commandes (non routé) |
| Ticket mes billets | GET | `/api/v1/tickets/my-tickets` | aucun | `200 TicketSummary[]` (non paginé) | authentifié | Mes billets (non routé) |
| Ticket QR | GET | `/api/v1/tickets/{ticketId}/qr` | `ticketId: UUID` | `200 TicketQrResponse` | propriétaire authentifié | QR (non routé) |
| Ticket scan | POST | `/api/v1/tickets/scan` | `ScanRequestDto` | `200 ScanResponseDto`, y compris refus métier | `SCOPE_ticket:scan` + affectation eventId | Scanner (non routé) |
| Ticket analytics scan | GET | `/api/v1/tickets/scans/stats/{eventId}` | `eventId` | `200 ScanStatisticsDto` | `SCOPE_ticket:scan` + affectation eventId | Stats scanner (non routé) |
| Ticket configuration | PUT | `/api/v1/partners/{partnerId}/tickets/configuration` | `SaleRequest`; header Authorization explicite | `200 TicketSaleConfigurationEntity` | contextual `partner:ticket-manage` | Configuration vente |
| Ticket type création | POST | `/api/v1/partners/{partnerId}/tickets/configurations/{id}/types` | `TypeRequest` | `200 TicketTypeEntity` | contextual `partner:ticket-manage` | Création Standard/VIP |
| Ticket affectation staff | POST | `/api/v1/partners/{partnerId}/tickets/staff` | `StaffRequest` | `200 EventStaffAssignmentEntity` | contextual `partner:staff-manage` | Affectation événement |
| Ticket retrait staff | DELETE | `/api/v1/partners/{partnerId}/tickets/staff/{id}` | IDs | `200`, body vide | contextual `partner:staff-manage` | Retrait événement |
| Ticket analytics métier | GET | `/api/v1/analytics/partners/{partnerId}/ticket-events/{eventId}` | `from`, `to`, `timezone`, `page`, `size` | `200 Page<Metrics>` | contextual `partner:analytics-view` | Dashboard billetterie |
| Ticket pic d'entrée | GET | `/api/v1/analytics/partners/{partnerId}/ticket-events/{eventId}/peak-entry` | `from`, `to`, `timezone` | `200 Metrics` ou JSON vide pour `Optional.empty` | contextual analytics-view | Pic d'entrée |
| Staff rôle création | POST | `/api/v1/partners/{partnerId}/staff/roles` | `{code,name,permissions:string[]}` | `200 PartnerRoleEntity` | service vérifie `partner:staff-manage` | Gestion rôles |
| Staff invitation | POST | `/api/v1/partners/{partnerId}/staff/invitations` | `{contact,roleId:UUID}` | `200 InvitationResult` | `partner:staff-manage` | Invitation |
| Staff acceptation | POST | `/api/v1/partners/{partnerId}/staff/invitations/accept` | `{token}` | `200 PartnerMembershipEntity` | JWT utilisateur | Acceptation |
| Staff rôle modification | PUT | `/api/v1/partners/{partnerId}/staff/{userId}/role/{roleId}` | path params | `200`, body vide | `partner:staff-manage` | Modification rôle |
| Staff révocation | DELETE | `/api/v1/partners/{partnerId}/staff/{userId}` | path params | `200`, body vide | `partner:staff-manage`; propriétaire protégé | Révocation |
| Staff audit | GET | `/api/v1/partners/{partnerId}/staff/audit` | aucun | `200 PartnerStaffAuditEntity[]` | `partner:staff-manage` | Audit |
| Permission contextuelle | GET | `/api/v1/partners/{partnerId}/staff/permissions/{permission}` | permission dans le path | `200 {"allowed":boolean}` | JWT | Guards frontend/services |
| Promotion création | POST | `/api/v1/commerce/admin/promotions` | `PromotionRequest` | `200 Promotion` | `ROLE_ADMIN` | Admin promotion (non routé) |
| Commission création | POST | `/api/v1/commerce/admin/commissions` | `CommissionRequest` | `200 CommissionRule` | `ROLE_ADMIN` | Admin commissions (non routé) |
| Commerce commande | POST | `/api/v1/commerce/orders` | `CreateRequest`; `Idempotency-Key` obligatoire | `200 CommerceOrder` | authentifié | Orchestration achat (non routé) |
| Commerce mes commandes | GET | `/api/v1/commerce/orders/me` | aucun | `200 CommerceOrder[]` | authentifié | Historique commerce (non routé) |
| Commerce remboursement | POST | `/api/v1/commerce/orders/{orderId}/refunds` | `{amount,reason}`; `Idempotency-Key` | `200 CommerceRefund` | authentifié/propriétaire | Remboursement (non routé) |
| Finance ledger | GET | `/api/v1/commerce/admin/ledger/{partnerId}` | partnerId | `200 LedgerEntry[]` | `ROLE_ADMIN` | Transactions admin (non routé) |
| Finance solde | GET | `/api/v1/commerce/admin/ledger/{partnerId}/balance/{currency}` | partnerId, currency | `200 {partnerId,currency,balance}` | `ROLE_ADMIN` | Résumé finance (non routé) |
| Finance correction | POST | `/api/v1/commerce/admin/ledger/{partnerId}/adjustments` | `AdjustmentRequest`; `Idempotency-Key` | `200 LedgerEntry` | `ROLE_ADMIN` | Correction auditée (non routé) |
| Finance mouvement | POST | `/api/v1/commerce/admin/ledger/{partnerId}/movements` | `MovementRequest`; `Idempotency-Key` | `200 LedgerEntry` | `ROLE_ADMIN` | Payout/chargeback (non routé) |
| Analytics admin | GET | `/api/v1/analytics/admin/{scopeType}/{scopeId}` | scope `CAMPAIGN|TICKET_EVENT`; from/to/timezone/page/size | `200 Page<Metrics>` | `ROLE_ADMIN` ou `ROLE_SUPER_ADMIN` | Analytics admin |
| Analytics rebuild | POST | `/api/v1/analytics/admin/rebuild` | aucun | `200 {"eventsReplayed":number}` | ADMIN/SUPER_ADMIN | Maintenance admin |

## Schémas exacts principaux

- `CreateCampaignRequest`: `name`, `objective`, `promotedEntityType`,
  `promotedEntityId`, `billingModel`, `totalBudget`, `dailyBudget`, `currency`,
  `startAt`, `endAt`, `targetConfiguration`, `creativeConfiguration`.
  `UpdateCampaignRequest` omet uniquement `promotedEntityType`,
  `promotedEntityId` et `currency`.
- `CampaignResponse`: champs de création plus `id`, `partnerId`, `status`,
  `spentAmount`, `createdBy`, `approvedBy?`, `rejectionReason?`, `createdAt`,
  `updatedAt`, `version`.
- `AdSelectionRequest`: `userId?`, `anonymousSessionId?`, `placement`,
  `countryCode`, `regionId?`, `cityId?`, `districtId?`, `latitude?`,
  `longitude?`, `interestIds?`, `ageBand?`, `language`, `deviceType`,
  `requestTimestamp`, `contextEntityType?`, `contextEntityId?`,
  `excludedCampaignIds?`, `limit?` (1–20).
- `SaleRequest`: `eventId`, `salesStartAt`, `salesEndAt`,
  `maxTicketsPerBuyer >= 1`, `currency` (trois majuscules).
- `TypeRequest`: `code`, `name`, `description?`, `price >= 0`, `quantity >= 0`,
  `salesStartAt?`, `salesEndAt?`, `accessZone?`, `gateInstructions?`.
- `Metrics`: date/dimension, impressions qualifiées/uniques, clics,
  conversions, tickets vendus, scans/rejets, spend/budget/revenue/commission/
  refunds/remainingBudget, CTR/conversionRate/CPM/CPC/CPA/attendanceRate et
  `suppressed`.
- La forme `Page<T>` est la sérialisation Spring (`content`, `pageable`,
  `totalPages`, `totalElements`, `last`, `size`, `number`, `sort`,
  `numberOfElements`, `first`, `empty`), pas le `PaginatedResponse<T>` cursor
  existant du mobile.

## Codes et erreurs observés

- Campaign utilise RFC 9457 `ProblemDetail`: `VALIDATION_ERROR`,
  `INVALID_ARGUMENT`, `INVALID_STATE` et les codes de `CampaignServiceException`
  (status porté par l'exception). Spring Security ajoute 401/403.
- Partner renvoie `{code,message,details,timestamp,correlationId}` avec
  `VALIDATION_ERROR`, codes `PartnerException`, ou `INTERNAL_ERROR`.
- Ads, Ticket et Commerce n'ont pas de contrat global d'erreur métier homogène
  dans leurs contrôleurs inspectés : validation Spring produit 400; auth 401/403;
  les exceptions non mappées peuvent produire 500. Le scan encode normalement
  les refus métier dans `200 ScanResponseDto.result/reasonCode`.
- Aucun contrôleur inspecté ne documente explicitement toutes ces réponses via
  `@ApiResponse`; le Swagger généré est donc incomplet pour les erreurs.

## ENDPOINTS MANQUANTS POUR LE FRONT

- Toutes les routes Gateway Campaign, Ads, Ticket et Commerce, plus un routage
  spécifique des Campaign Admin avant le catch-all Admin.
- Campaign UI actuelle appelle `/partners/me/campaigns/**`, qui n'existe pas.
- Liste et détail publics des ticket types; liste partenaire des ticket types;
  liste partenaire des commandes d'un événement.
- Création directe de commande `{ticketTypeId,quantity}` : le backend impose
  d'abord `/hold`, puis `/orders` avec `holdId`.
- Liste staff/memberships et rôles; détail staff; recherche utilisateur;
  renvoi/annulation/liste des invitations. L'UI utilise aujourd'hui des paths
  `/partners/me/events/{eventId}/staff` inexistants.
- Promotions : liste, détail, modification, désactivation; aucun endpoint
  partenaire. L'UI utilise `/partners/me/promotions`, inexistant.
- Finance partenaire : résumé, transactions paginées, détail transaction,
  commissions consultables et settlements dédiés. Les endpoints actuels sont
  uniquement admin et non routés.
- Analytics partenaire global/événement générique : seuls campaign et
  ticket-event existent dans la nouvelle API métier.
- Idempotence HTTP manque sur création de ticket order, tracking Ads et mutations
  Campaign.

## MAPPING BACKEND → ÉCRANS FRONTEND

| Écran/feature mobile | Contrat utilisable | Écart |
|---|---|---|
| Liste/détail campagne | CampaignController + AnalyticsController | adapter les paths et la réponse `Page`; Gateway absent |
| Création/édition campagne | Create/UpdateCampaignRequest | l'UI actuelle ne fournit pas objectif, ciblage, creative, billing, dailyBudget/currency |
| Feed/discovery sponsorisé | Ads select + tracking | Gateway absent; conversion insuffisamment protégée |
| Achat billet | hold → order → payment async → my-tickets | l'UI saute le hold et attend un autre DTO |
| Mes billets / QR | my-tickets et `/{ticketId}/qr` | DTO backend plus minimal que la carte UI; enrichissement ViewModel nécessaire |
| Scanner | `/tickets/scan` | compatible seulement avec scope et affectation événement |
| Dashboard ticketing | analytics ticket-event + peak-entry | ticket types/listes commandes partenaire manquants |
| Équipe partenaire | invitations/change/revoke | liste, profils UI, recherche et resend manquants |
| Promotions | création admin uniquement | écran partenaire non raccordable |
| Finance | ledger/balance admin uniquement | dashboard partenaire non raccordable |

## Types frontend

Les DTO API stricts ont été ajoutés aux six fichiers demandés et à
`src/features/ads/types.ts`. Les anciens types purement ViewModel restent
distincts lorsqu'ils sont déjà consommés par les écrans démo. Notamment
`CampaignApiStatus` et `PromotionApiStatus` représentent les enums backend sans
casser les filtres UI historiques `CampaignStatus`/`PromotionStatus`.
