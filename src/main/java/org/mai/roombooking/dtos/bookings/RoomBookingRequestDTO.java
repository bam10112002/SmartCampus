package org.mai.roombooking.dtos.bookings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mai.roombooking.entities.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomBookingRequestDTO {
    private Long id;
    private UUID bookingGroupId;

    @NotNull
    private Long roomId;
    @NotNull
    private Long ownerId;
    @NotNull
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime endTime;
    @NotNull
    @NotBlank
    private String description;

    private String title;
    private List<Long> staffId;
    private List<Long> groupsId;
    private Tag tag;
}
