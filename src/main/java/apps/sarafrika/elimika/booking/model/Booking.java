package apps.sarafrika.elimika.booking.model;

import apps.sarafrika.elimika.booking.util.converter.BookingStatusConverter;
import apps.sarafrika.elimika.shared.enums.BookingStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends BaseEntity {

    @Column(name = "student_uuid")
    private UUID studentUuid;

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "status")
    @Convert(converter = BookingStatusConverter.class)
    private BookingStatus status;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "price_amount")
    private BigDecimal priceAmount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "payment_session_id")
    private String paymentSessionId;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "payment_engine")
    private String paymentEngine;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "availability_block_uuid")
    private UUID availabilityBlockUuid;

    @Column(name = "scheduled_instance_uuid")
    private UUID scheduledInstanceUuid;

    @Column(name = "enrollment_uuid")
    private UUID enrollmentUuid;
}
