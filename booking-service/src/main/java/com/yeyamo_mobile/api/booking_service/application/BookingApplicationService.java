package com.yeyamo_mobile.api.booking_service.application;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.booking_service.application.BookingDtos.*;
import com.yeyamo_mobile.api.booking_service.application.port.BookingOutboxPort;
import com.yeyamo_mobile.api.booking_service.domain.*;
import com.yeyamo_mobile.api.booking_service.infrastructure.persistence.*;
import com.yeyamo_mobile.shared.country.CountryConfigClient;
import com.yeyamo_mobile.shared.country.CountryConfigClient.CountryFeature;

@Service
public class BookingApplicationService {

    public static final String BOOKING_EVENTS = "booking.events", PAYMENT_COMMANDS = "payment.commands";
    private static final Set<String> MOBILE_MONEY_OPERATORS = Set.of("mtn", "orange", "moov", "airtel", "mpesa", "wave", "free", "tmoney", "afrimoney");

    private final ActivitySlotRepository slots;
    private final BookingRepository bookings;
    private final PaymentSagaRepository sagas;
    private final CommandReceiptRepository receipts;
    private final BookingHistoryRepository history;
    private final BookingOutboxPort outbox;
    private final BookingReferenceGenerator references;
    private final CountryConfigClient countries;
    private final PlaceReadModelRepository placeReadModelRepository;

    public BookingApplicationService(
            ActivitySlotRepository s,
            BookingRepository b,
            PaymentSagaRepository g,
            CommandReceiptRepository r,
            BookingHistoryRepository h,
            BookingOutboxPort o,
            BookingReferenceGenerator ref
    ) {
        this(s, b, g, r, h, o, ref, null, null);
    }

    public BookingApplicationService(
            ActivitySlotRepository s,
            BookingRepository b,
            PaymentSagaRepository g,
            CommandReceiptRepository r,
            BookingHistoryRepository h,
            BookingOutboxPort o,
            BookingReferenceGenerator ref,
            PlaceReadModelRepository placeReadModelRepository
    ) {
        this(s, b, g, r, h, o, ref, null, placeReadModelRepository);
    }

    @Autowired
    public BookingApplicationService(
            ActivitySlotRepository s,
            BookingRepository b,
            PaymentSagaRepository g,
            CommandReceiptRepository r,
            BookingHistoryRepository h,
            BookingOutboxPort o,
            BookingReferenceGenerator ref,
            @Autowired(required = false) CountryConfigClient countries,
            @Autowired(required = false) PlaceReadModelRepository placeReadModelRepository
    ) {
        this.slots = s;
        this.bookings = b;
        this.sagas = g;
        this.receipts = r;
        this.history = h;
        this.outbox = o;
        this.references = ref;
        this.countries = countries;
        this.placeReadModelRepository = placeReadModelRepository;
    }

    @Transactional
    public SlotView createSlot(CreateSlot c, String actor, String correlation) {
        if (!c.startsAt().isAfter(Instant.now())) {
            throw new BookingException("INVALID_SLOT", "Slot must start in the future");
        }
        if (c.isPaid() && (c.amount() == null || c.amount().signum() <= 0 || c.currency() == null || c.currency().isBlank())) {
            throw new BookingException("INVALID_ACTIVITY_PRICING", "A paid activity requires a positive amount and currency");
        }
        if (c.placeId() != null) {
            if (placeReadModelRepository == null
                    || placeReadModelRepository.findByPlaceIdAndActiveTrue(c.placeId()).isEmpty()) {
                throw new BookingException("INVALID_PLACE", "Le lieu spécifié est introuvable ou inactif");
            }
        }
        validate(c.countryCode(), CountryFeature.BOOKING);
        var slot = slots.save(ActivitySlotEntity.create(
                c.activityId(),
                actor,
                c.startsAt(),
                c.endsAt(),
                c.capacity(),
                c.isPaid(),
                c.amount(),
                c.currency(),
                c.countryCode(),
                c.placeId()
        ));
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("slotId", slot.getId());
        p.put("activityId", slot.getActivityId());
        p.put("placeId", slot.getPlaceId());
        p.put("ownerUserId", actor);
        p.put("startsAt", slot.getStartsAt());
        p.put("countryCode", slot.getCountryCode());
        outbox.append(BOOKING_EVENTS, "booking.slot.created", slot.getId().toString(), correlation, p);
        return slotView(slot);
    }

    @Transactional
    public void closeSlot(UUID id, String actor, String correlation, boolean admin) {
        var slot = slots.findLocked(id).orElseThrow(() -> new NoSuchElementException("Slot not found"));
        requireSlotOwner(slot, actor, admin);
        slot.close();
        slots.save(slot);
        outbox.append(BOOKING_EVENTS, "booking.slot.closed", id.toString(), correlation, Map.of("slotId", id, "activityId", slot.getActivityId()));
    }

    @Transactional(readOnly = true)
    public List<SlotView> availability(String activity) {
        return slots.findByActivityIdAndStartsAtAfterOrderByStartsAtAsc(activity, Instant.now()).stream().map(this::slotView).toList();
    }

    @Transactional(readOnly = true)
    public Page<SlotView> listActivities(UUID placeId, Pageable pageable) {
        if (placeId != null) {
            return slots.findByPlaceIdAndStartsAtAfterOrderByStartsAtAsc(placeId, Instant.now(), pageable).map(this::slotView);
        }
        return slots.findByStartsAtAfterOrderByStartsAtAsc(Instant.now(), pageable).map(this::slotView);
    }

    @Transactional
    public BookingView create(String user, CreateBooking c, String key, String correlation) {
        return create(user, c, key, correlation, null);
    }

    @Transactional
    public BookingView create(String user, CreateBooking c, String key, String correlation, String accountCountryCode) {
        String operation = "CREATE:" + c.slotId();
        var replay = receipts.findByKeyAndUserIdAndOperation(key, user, operation);
        if (replay.isPresent()) return bookingView(replay.get().getBooking());
        var slot = slots.findLocked(c.slotId()).orElseThrow(() -> new NoSuchElementException("Slot not found"));
        validate(slot.getCountryCode(), CountryFeature.BOOKING);
        String operator = null;
        String phoneNumber = null;
        String paymentCountryCode = null;
        if (slot.isPaid()) {
            operator = validatedOperator(c.operator());
            phoneNumber = validatedPhoneNumber(c.phoneNumber());
            paymentCountryCode = validatedCountryCode(accountCountryCode);
        }
        slot.reserve(c.quantity());
        slots.save(slot);
        var booking = bookings.save(BookingEntity.pending(uniqueReference(), user, slot, c.quantity(), operator, phoneNumber, paymentCountryCode));
        if (booking.getPaymentStatus() != PaymentStatus.NOT_REQUIRED) validate(booking.getCountryCode(), CountryFeature.PAYMENTS);
        history.save(new BookingHistoryEntity(booking.getId(), user, "CREATED", "quantity=" + c.quantity()));
        outbox.append(BOOKING_EVENTS, "booking.created", booking.getId().toString(), correlation, payload(booking));
        if (booking.getPaymentStatus() == PaymentStatus.NOT_REQUIRED) {
            booking.confirm();
            bookings.save(booking);
            history.save(new BookingHistoryEntity(booking.getId(), user, "CONFIRMED", "Free booking"));
            domainEvent("booking.confirmed", booking, correlation);
        } else {
            var saga = sagas.save(PaymentSagaEntity.authorization(booking));
            Map<String, Object> paymentCommand = new LinkedHashMap<>();
            paymentCommand.put("sagaId", saga.getId());
            paymentCommand.put("bookingId", booking.getId());
            paymentCommand.put("userId", user);
            paymentCommand.put("partnerId", slot.getOwnerUserId());
            paymentCommand.put("amount", booking.getTotalAmount());
            paymentCommand.put("currency", booking.getCurrency());
            paymentCommand.put("operator", booking.getPaymentOperator());
            paymentCommand.put("phoneNumber", booking.getPaymentPhoneNumber());
            paymentCommand.put("country", booking.getPaymentCountryCode());
            paymentCommand.put("idempotencyKey", "booking:" + booking.getId() + ":authorize");
            outbox.append(PAYMENT_COMMANDS, "payment.authorization.requested", booking.getId().toString(), correlation, paymentCommand);
        }
        receipts.save(new CommandReceiptEntity(key, user, operation, booking));
        return bookingView(booking);
    }

    @Transactional
    public BookingView cancel(String actor, UUID id, String reason, String key, String correlation, boolean privileged) {
        String operation = "CANCEL:" + id;
        var replay = receipts.findByKeyAndUserIdAndOperation(key, actor, operation);
        if (replay.isPresent()) return bookingView(replay.get().getBooking());
        var booking = lockedOwned(actor, id, privileged);
        boolean paid = booking.getPaymentStatus() == PaymentStatus.AUTHORIZED;
        boolean authorizationPending = booking.getPaymentStatus() == PaymentStatus.AUTHORIZATION_PENDING;
        booking.cancel(reason);
        bookings.save(booking);
        var slot = slots.findLocked(booking.getSlot().getId()).orElseThrow();
        slot.release(booking.getQuantity());
        slots.save(slot);
        history.save(new BookingHistoryEntity(id, actor, "CANCELLED", reason));
        if (paid) {
            requestRefund(booking, correlation);
        } else if (authorizationPending) {
            var saga = sagas.findByBookingId(id).orElseThrow(() -> new BookingException("PAYMENT_SAGA_NOT_FOUND", "Payment saga not found"));
            saga.cancellationRequested();
            sagas.save(saga);
            outbox.append(PAYMENT_COMMANDS, "payment.authorization.cancel.requested", id.toString(), correlation, Map.of("sagaId", saga.getId(), "bookingId", id, "idempotencyKey", "booking:" + id + ":cancel-authorization"));
        }
        domainEvent("booking.cancelled", booking, correlation);
        receipts.save(new CommandReceiptEntity(key, actor, operation, booking));
        return bookingView(booking);
    }

    @Transactional
    public BookingView complete(String actor, UUID id, String key, String correlation, boolean admin) {
        String operation = "COMPLETE:" + id;
        var replay = receipts.findByKeyAndUserIdAndOperation(key, actor, operation);
        if (replay.isPresent()) return bookingView(replay.get().getBooking());
        var booking = bookings.findLocked(id).orElseThrow(() -> new NoSuchElementException("Booking not found"));
        requireSlotOwner(booking.getSlot(), actor, admin);
        if (Instant.now().isBefore(booking.getSlot().getEndsAt())) throw new BookingException("ACTIVITY_NOT_ENDED", "Activity has not ended yet");
        booking.complete();
        bookings.save(booking);
        history.save(new BookingHistoryEntity(id, actor, "COMPLETED", "Activity completed"));
        domainEvent("booking.completed", booking, correlation);
        receipts.save(new CommandReceiptEntity(key, actor, operation, booking));
        return bookingView(booking);
    }

    @Transactional
    public void paymentAuthorized(UUID eventId, UUID bookingId, String paymentId, String correlation) {
        var booking = bookings.findLocked(bookingId).orElseThrow(() -> new NoSuchElementException("Booking not found"));
        var saga = sagas.findByBookingId(bookingId).orElseThrow();
        if (booking.getStatus() == BookingStatus.CANCELLED && booking.getPaymentStatus() == PaymentStatus.AUTHORIZATION_PENDING) {
            saga.authorized(paymentId);
            booking.authorizationAfterCancellation();
            sagas.save(saga);
            bookings.save(booking);
            history.save(new BookingHistoryEntity(bookingId, "payment-service", "LATE_PAYMENT_AUTHORIZED", "Compensating refund requested"));
            requestRefund(booking, correlation);
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING) return;
        saga.authorized(paymentId);
        booking.confirm();
        sagas.save(saga);
        bookings.save(booking);
        history.save(new BookingHistoryEntity(bookingId, "payment-service", "PAYMENT_AUTHORIZED", "paymentId=" + paymentId));
        domainEvent("booking.confirmed", booking, correlation);
    }

    @Transactional
    public void paymentFailed(UUID eventId, UUID bookingId, String reason, String correlation) {
        var booking = bookings.findLocked(bookingId).orElseThrow(() -> new NoSuchElementException("Booking not found"));
        var saga = sagas.findByBookingId(bookingId).orElseThrow();
        if (booking.getStatus() == BookingStatus.CANCELLED && booking.getPaymentStatus() == PaymentStatus.AUTHORIZATION_PENDING) {
            saga.failed(reason);
            booking.paymentFailedAfterCancellation(reason);
            sagas.save(saga);
            bookings.save(booking);
            history.save(new BookingHistoryEntity(bookingId, "payment-service", "CANCELLED_AUTHORIZATION_FAILED", reason));
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING) return;
        saga.failed(reason);
        booking.paymentFailed(reason);
        var slot = slots.findLocked(booking.getSlot().getId()).orElseThrow();
        slot.release(booking.getQuantity());
        sagas.save(saga);
        bookings.save(booking);
        slots.save(slot);
        history.save(new BookingHistoryEntity(bookingId, "payment-service", "PAYMENT_FAILED", reason));
        domainEvent("booking.cancelled", booking, correlation);
    }

    @Transactional
    public void paymentRefunded(UUID eventId, UUID bookingId, String correlation) {
        var booking = bookings.findLocked(bookingId).orElseThrow(() -> new NoSuchElementException("Booking not found"));
        var saga = sagas.findByBookingId(bookingId).orElseThrow();
        if (saga.getStatus() == SagaStatus.REFUNDED) return;
        booking.refunded();
        saga.refunded();
        bookings.save(booking);
        sagas.save(saga);
        history.save(new BookingHistoryEntity(bookingId, "payment-service", "PAYMENT_REFUNDED", "Refund confirmed"));
        domainEvent("booking.refunded", booking, correlation);
    }

    @Transactional(readOnly = true)
    public List<BookingView> mine(String user) {
        return bookings.findByUserIdOrderByCreatedAtDesc(user).stream().map(this::bookingView).toList();
    }

    @Transactional(readOnly = true)
    public BookingView get(String actor, UUID id, boolean privileged) {
        return bookingView(owned(actor, id, privileged));
    }

    @Transactional(readOnly = true)
    public List<HistoryView> history(String actor, UUID id, boolean privileged) {
        owned(actor, id, privileged);
        return history.findByBookingIdOrderByOccurredAtAsc(id).stream().map(h -> new HistoryView(h.getId(), h.getAction(), h.getDetails(), h.getOccurredAt())).toList();
    }

    private BookingEntity lockedOwned(String actor, UUID id, boolean privileged) {
        var b = bookings.findLocked(id).orElseThrow(() -> new NoSuchElementException("Booking not found"));
        if (!privileged && !b.getUserId().equals(actor)) throw new BookingException("BOOKING_FORBIDDEN", "Booking is not accessible");
        return b;
    }

    private BookingEntity owned(String actor, UUID id, boolean privileged) {
        var b = bookings.findById(id).orElseThrow(() -> new NoSuchElementException("Booking not found"));
        if (!privileged && !b.getUserId().equals(actor)) throw new BookingException("BOOKING_FORBIDDEN", "Booking is not accessible");
        return b;
    }

    private void requireSlotOwner(ActivitySlotEntity slot, String actor, boolean admin) {
        if (!admin && !slot.getOwnerUserId().equals(actor)) throw new BookingException("BOOKING_FORBIDDEN", "Activity slot belongs to another partner");
    }

    private void requestRefund(BookingEntity booking, String correlation) {
        var saga = sagas.findByBookingId(booking.getId()).orElseThrow(() -> new BookingException("PAYMENT_SAGA_NOT_FOUND", "Payment saga not found"));
        saga.refundRequested();
        sagas.save(saga);
        outbox.append(PAYMENT_COMMANDS, "payment.refund.requested", booking.getId().toString(), correlation, Map.of("sagaId", saga.getId(), "bookingId", booking.getId(), "paymentId", saga.getPaymentId(), "amount", booking.getTotalAmount(), "currency", booking.getCurrency(), "idempotencyKey", "booking:" + booking.getId() + ":refund"));
    }

    private String uniqueReference() {
        for (int i = 0; i < 5; i++) {
            String ref = references.next();
            if (bookings.findByReference(ref).isEmpty()) return ref;
        }
        throw new IllegalStateException("Cannot generate booking reference");
    }

    private void domainEvent(String type, BookingEntity b, String correlation) {
        outbox.append(BOOKING_EVENTS, type, b.getId().toString(), correlation, payload(b));
    }

    private Map<String, Object> payload(BookingEntity b) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("bookingId", b.getId());
        p.put("userId", b.getUserId());
        p.put("activityId", b.getActivityId());
        p.put("slotId", b.getSlot().getId());
        p.put("quantity", b.getQuantity());
        p.put("amount", b.getTotalAmount());
        p.put("currency", b.getCurrency());
        p.put("countryCode", b.getCountryCode());
        p.put("status", b.getStatus().name());
        return p;
    }

    private void validate(String country, CountryFeature feature) {
        if (countries == null) return;
        if (country == null || country.isBlank()) throw new BookingException("COUNTRY_REQUIRED", "A country is required");
        try {
            countries.validateFeature(country, feature);
        } catch (CountryConfigClient.CountryConfigException e) {
            throw new BookingException("COUNTRY_CONFIGURATION_REJECTED", e.getMessage());
        }
    }

    private String validatedOperator(String operator) {
        if (operator == null || !MOBILE_MONEY_OPERATORS.contains(operator.trim().toLowerCase(Locale.ROOT))) {
            throw new BookingException("PAYMENT_OPERATOR_REQUIRED", "A supported mobile money operator is required for a paid activity");
        }
        return operator.trim().toLowerCase(Locale.ROOT);
    }

    private String validatedPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || !phoneNumber.matches("\\+[1-9]\\d{1,14}")) {
            throw new BookingException("PAYMENT_PHONE_REQUIRED", "A valid E.164 mobile money phone number is required for a paid activity");
        }
        return phoneNumber;
    }

    private String validatedCountryCode(String countryCode) {
        if (countryCode == null || !countryCode.matches("[A-Z]{2}")) {
            throw new BookingException("PAYMENT_COUNTRY_REQUIRED", "The account country is required for a paid activity");
        }
        return countryCode;
    }

    private SlotView slotView(ActivitySlotEntity s) {
        return new SlotView(
                s.getId(),
                s.getActivityId(),
                s.getPlaceId(),
                s.getStartsAt(),
                s.getEndsAt(),
                s.getCapacity(),
                s.getReservedCount(),
                s.available(),
                s.getUnitPrice(),
                s.getCurrency(),
                s.getCountryCode(),
                s.getStatus(),
                s.isPaid(),
                s.getAmount()
        );
    }

    private BookingView bookingView(BookingEntity b) {
        return new BookingView(b.getId(), b.getReference(), b.getUserId(), b.getActivityId(), b.getSlot().getId(), b.getQuantity(), b.getUnitPrice(), b.getTotalAmount(), b.getCurrency(), b.getCountryCode(), b.getStatus(), b.getPaymentStatus(), b.getCancellationReason(), b.getCreatedAt(), b.getConfirmedAt(), b.getCancelledAt(), b.getCompletedAt());
    }
}
