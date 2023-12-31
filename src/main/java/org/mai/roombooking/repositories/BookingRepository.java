package org.mai.roombooking.repositories;

import org.mai.roombooking.entities.Booking;
import org.mai.roombooking.entities.Group;
import org.mai.roombooking.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b FROM Booking b JOIN FETCH b.room JOIN FETCH b.owner " +
            "WHERE b.endTime >= :startDateTime AND b.startTime <= :endDateTime")
    List<Booking> findBookingsInDateRange(@Param("startDateTime") LocalDateTime startDateTime,
                                          @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.endTime >= :startDateTime " +
            "   AND b.startTime <= :endDateTime" +
            "   AND b.room.roomId = :roomId")
    List<Booking> findBookingsInDateRangeForRoom(@Param("startDateTime") LocalDateTime startDateTime,
                                                 @Param("endDateTime") LocalDateTime endDateTime,
                                                 @Param("roomId") Long roomId);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.endTime >= :startDateTime " +
            "   AND b.startTime <= :endDateTime " +
            "   AND b.owner.userId = :userId")
    List<Booking> findBookingsInDateRangeByUser(@Param("startDateTime") LocalDateTime startDateTime,
                                                @Param("endDateTime") LocalDateTime endDateTime,
                                                @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Booking b WHERE b.bookingGroupId = :periodicBookingId")
    void deleteAllByPeriodicBookingId(@Param("periodicBookingId") UUID periodicBookingId);

    List<Booking> findAllByBookingGroupId(@Param("periodicBookingId") UUID periodicBookingId);
    List<Booking> findByGroupsContaining(Group group);
    List<Booking> findByStaffContaining(User staff);
}
