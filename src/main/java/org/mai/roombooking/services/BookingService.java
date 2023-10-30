package org.mai.roombooking.services;

import lombok.NonNull;
import org.mai.roombooking.dtos.Pair;
import org.mai.roombooking.dtos.RoomBookingDTO;
import org.mai.roombooking.dtos.RoomBookingRequestDTO;
import org.mai.roombooking.entities.*;
import org.mai.roombooking.exceptions.BookingException;
import org.mai.roombooking.exceptions.BookingNotFoundException;
import org.mai.roombooking.exceptions.RoomNotFoundException;
import org.mai.roombooking.exceptions.UserNotFoundException;
import org.mai.roombooking.repositories.BookingRepository;
import org.mai.roombooking.repositories.RoomRepository;
import org.mai.roombooking.repositories.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервисный класс для управления бронированиями.
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    BookingService(BookingRepository bookingRepository, UserRepository userRepository, RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
    }

    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow(()->new BookingNotFoundException(bookingId));
    }


    /**
     * Получает все бронирования комнат в заданном временном диапазоне.
     *
     * @param startTime дата и время начала запроса
     * @param endTime   дата и время окончания запроса
     * @return список бронирований комнат в заданном временном диапазоне
     */
    public List<Pair> getBookingsInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<Booking> bookings = bookingRepository.findBookingsInDateRange(startTime,endTime);

        Map<String, List<RoomBookingDTO>> groupedBookings = bookings.stream().map((RoomBookingDTO::new)).collect(Collectors.groupingBy(RoomBookingDTO::getRoom, Collectors.toList()));
        var data = groupedBookings.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .sorted(Comparator.comparing(RoomBookingDTO::getStartTime))
                                .toList()
                ));

        List<Pair> res = new ArrayList<>();
        for (var entry : data.entrySet()) {
            res.add(new Pair(entry));
        }
        return res;
    }

    /**
     * Получает бронирования для конкретной комнаты в заданном временном диапазоне.
     *
     * @param roomId    идентификатор комнаты
     * @param startTime дата и время начала запроса
     * @param endTime   дата и время окончания запроса
     * @return список бронирований для указанной комнаты в заданном временном диапазоне
     */
    public List<RoomBookingDTO> getBookingsByRoomInTimeRange(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Booking> bookings = bookingRepository.findBookingsInDateRangeForRoom(startTime, endTime, roomId);
        return bookings.stream().map((RoomBookingDTO::new)).toList();
    }

    /**
     * Получает бронирования для конкретного пользователя в заданном временном диапазоне.
     *
     * @param userId    идентификатор пользователя
     * @param startTime дата и время начала запроса
     * @param endTime   дата и время окончания запроса
     * @return список бронирований для указанного пользователя в заданном временном диапазоне
     * @throws UsernameNotFoundException если пользователь не найден по идентификатору
     * @throws AccessDeniedException если пользователю недостаточно прав для выполнения запроса
     */

    public List<RoomBookingDTO> getBookingsByUserInTimeRange(Long userId,
                                                             LocalDateTime startTime,
                                                             LocalDateTime endTime)
                                                             throws UsernameNotFoundException{
        List<Booking> bookings = bookingRepository.findBookingsInDateRangeByUser(startTime, endTime, userId);
            if (bookings.isEmpty())
                userRepository.findById(userId).orElseThrow(() ->
                        new UsernameNotFoundException("User with id" + userId + "not found"));

        return bookings.stream().map((RoomBookingDTO::new)).toList();
    }

    /**
     * Обновляет периодическое бронирование на основе предоставленного запроса.
     *
     * @param request   запрос с информацией для обновления бронирования
     * @return обновленное бронирование
     */
    @Transactional
    public Booking updatePeriodicBooking(@NonNull RoomBookingRequestDTO request) {
        this.deletePeriodicBooking(request.getId());
        return this.createPrerodicBooking(request);
    }

    /**
     * Обновляет отдельное бронирование на основе предоставленного запроса.
     *
     * @param request   запрос с информацией для обновления бронирования
     * @throws UsernameNotFoundException пользователь с id, переданным клиентом, не найдена
     * @throws RoomNotFoundException аудитория с id, переданным клиентом, не найдена
     */
    public Booking updateBooking(RoomBookingRequestDTO request)
            throws UsernameNotFoundException, RoomNotFoundException {

        var booking = getBookingFromDTO(request);
        if (!validateBooking(request.getStartTime(), request.getEndTime(), request.getRoomId()))
            throw new BookingException();

        return bookingRepository.save(booking);
    }

    /**
     * Удаляет периодическое бронирование на основе предоставленного идентификатора.
     *
     * @param bookingId идентификатор бронирования для удаления
     * @throws BookingNotFoundException если бронирование не найдено по идентификатору
     */
    public void deletePeriodicBooking(Long bookingId) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        bookingRepository.deleteAllByPeriodicBookingId(booking.getPeriodicBookingId());
    }

    /**
     * Удаляет отдельное бронирование на основе предоставленного идентификатора.
     *
     * @param bookingId идентификатор бронирования для удаления
     * @throws BookingNotFoundException если бронирование не найдено по идентификатору
     */
    public void deleteBooking(Long bookingId) {
        bookingRepository.deleteById(bookingId);
    }


    /**
     * Создает новое бронирование на основе предоставленного запроса.
     *
     * @param request запрос с информацией для создания бронирования
     * @return созданное бронирование
     */
    @Transactional
    public Booking createPrerodicBooking(@NonNull RoomBookingRequestDTO request) {
        LocalDateTime end = request.getEndTime();
        LocalDateTime start = request.getEndTime();

        var booking = getBookingFromDTO(request);
        booking.setPeriodicBookingId(UUID.randomUUID());

        while (end.isBefore(request.getRRule().getUntilDate())) {
            if (validateBooking(start,end, request.getRoomId()))
                bookingRepository.save(booking);
            else
                throw new BookingException();

            end = RECURRING_RULES.get(request.getRRule().getFrequency())
                    .performOperation(end, request.getRRule().getInterval());
            start = RECURRING_RULES.get(request.getRRule().getFrequency())
                    .performOperation(start, request.getRRule().getInterval());

            booking.setStartTime(start);
            booking.setEndTime(end);
        }
        return booking;
    }

    /**
     *
     * @param dto - Дто запроса от клиента
     * @return объект для сохранения изменений в базу данных
     * @throws UsernameNotFoundException пользователь с указанным клиентом id не обнаружен
     * @throws RoomNotFoundException аудитория с указанным клиентом id не обнаружен
     */
    private Booking getBookingFromDTO(RoomBookingRequestDTO dto) throws UsernameNotFoundException, RoomNotFoundException{
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new UserNotFoundException(dto.getUserId()));

        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new RoomNotFoundException(dto.getRoomId()));

        return Booking.builder()
                .bookingPurpose(dto.getDescription())
                .user(user)
                .room(room)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .id(dto.getId())
                .periodicBookingId(dto.getPeriodicBookingId())
                .build();
    }

    private static final Map<RRule.Frequency, IncrementLocalDataTime> RECURRING_RULES = new EnumMap<>(RRule.Frequency.class);

    static {
        RECURRING_RULES.put(RRule.Frequency.DAILY, LocalDateTime::plusDays);
        RECURRING_RULES.put(RRule.Frequency.WEEKLY, LocalDateTime::plusWeeks);
        RECURRING_RULES.put(RRule.Frequency.MONTHLY, LocalDateTime::plusMonths);
        RECURRING_RULES.put(RRule.Frequency.YEARLY, LocalDateTime::plusYears);
    }

    @FunctionalInterface
    private interface IncrementLocalDataTime {
        LocalDateTime performOperation(LocalDateTime inputDateTime, int value);
    }

    private boolean validateBooking(LocalDateTime start, LocalDateTime end, Long roomId) {
        if (start.isAfter(end) || start.getDayOfYear() != end.getDayOfYear())
            return false;

        long count = bookingRepository.findBookingsInDateRange(start, end)
                .stream()
                .filter((bookingItem -> bookingItem.getRoom().getRoomId().equals(roomId)))
                .count();
        return count == 0;
    }
}