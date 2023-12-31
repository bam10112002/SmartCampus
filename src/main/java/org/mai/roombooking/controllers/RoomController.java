package org.mai.roombooking.controllers;

import org.mai.roombooking.dtos.RoomDTO;
import org.mai.roombooking.services.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * /api/room (UPDATE CREATE)
 * /api/room/{id} (GET DELETE)
 * реализованно /api/room/all (GET)
 * /api/room/available?startTime&endTime&capacity&hasProjector&hasComputers (GET)
 * фильтрации
 */
@RestController
@RequestMapping("/api/room")
public class RoomController {

    private final RoomService roomService;

    @Autowired
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/all")
    public List<RoomDTO> getAllRooms() {
        return roomService.getAllRooms();
    }

    @GetMapping("/get/cathedral")
    public List<RoomDTO> getCathedralRooms() {
        return roomService.getCathedralRooms();
    }

    @PostMapping("/update")
    public RoomDTO updateRoom(@NonNull @RequestBody RoomDTO dto) {
        return roomService.update(dto);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteRoom(@NonNull @PathVariable Long id) {
        roomService.delete(id);
    }
}
