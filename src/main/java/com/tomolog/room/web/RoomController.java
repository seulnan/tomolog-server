package com.tomolog.room.web;

import com.tomolog.room.domain.Room;
import com.tomolog.room.domain.RoomStatus;
import com.tomolog.room.dto.CreateRoomRequest;
import com.tomolog.room.dto.RoomDetailResponse;
import com.tomolog.room.dto.RoomMemberResponse;
import com.tomolog.room.dto.RoomResponse;
import com.tomolog.room.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Room creation, listing, detail, join and leave (SPEC §5). */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

  private final RoomService roomService;

  public RoomController(RoomService roomService) {
    this.roomService = roomService;
  }

  /** Creates a room with the caller as host. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public RoomResponse create(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody CreateRoomRequest request) {
    return RoomResponse.from(roomService.createRoom(userId, request.name(), request.capacity()));
  }

  /** Lists rooms, optionally filtered by status. */
  @GetMapping
  public Page<RoomResponse> list(
      @RequestParam(required = false) RoomStatus status,
      @PageableDefault(size = 20) Pageable pageable) {
    return roomService.listRooms(status, pageable).map(RoomResponse::from);
  }

  /** Returns a room with its members. */
  @GetMapping("/{roomId}")
  public RoomDetailResponse detail(@PathVariable Long roomId) {
    Room room = roomService.getRoom(roomId);
    return RoomDetailResponse.of(room, roomService.getMembers(roomId));
  }

  /** Joins the room, guarded so capacity is never exceeded. */
  @PostMapping("/{roomId}/join")
  @ResponseStatus(HttpStatus.CREATED)
  public RoomMemberResponse join(@AuthenticationPrincipal Long userId, @PathVariable Long roomId) {
    return RoomMemberResponse.from(roomService.join(roomId, userId));
  }

  /** Leaves the room. */
  @DeleteMapping("/{roomId}/members/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void leave(@AuthenticationPrincipal Long userId, @PathVariable Long roomId) {
    roomService.leave(roomId, userId);
  }
}
