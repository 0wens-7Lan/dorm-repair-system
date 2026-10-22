package com.dormrepair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dormrepair.dto.Requests;
import com.dormrepair.entity.DormBuilding;
import com.dormrepair.entity.DormRoom;
import com.dormrepair.service.DormService;
import com.dormrepair.support.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import com.dormrepair.support.Roles;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dorm")
public class DormController {
    private final DormService dormService;

    public DormController(DormService dormService) {
        this.dormService = dormService;
    }

    @GetMapping("/buildings")
    public ApiResult<List<DormBuilding>> buildings() {
        return ApiResult.ok(dormService.buildings());
    }

    @PostMapping("/buildings")
    @PreAuthorize("hasAnyRole('DORM_ADMIN','SYS_ADMIN')")
    public ApiResult<DormBuilding> createBuilding(@Valid @RequestBody Requests.DormBuildingRequest request) {
        return ApiResult.ok(dormService.saveBuilding(null, request));
    }

    @PutMapping("/buildings/{id}")
    @PreAuthorize("hasAnyRole('DORM_ADMIN','SYS_ADMIN')")
    public ApiResult<DormBuilding> updateBuilding(@PathVariable Long id, @Valid @RequestBody Requests.DormBuildingRequest request) {
        return ApiResult.ok(dormService.saveBuilding(id, request));
    }

    @DeleteMapping("/buildings/{id}")
    @PreAuthorize("hasAnyRole('DORM_ADMIN','SYS_ADMIN')")
    public ApiResult<Void> deleteBuilding(@PathVariable Long id) {
        dormService.deleteBuilding(id);
        return ApiResult.ok();
    }

    @GetMapping("/rooms")
    public ApiResult<IPage<Map<String, Object>>> rooms(@RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       @RequestParam(required = false) Long buildingId,
                                                       @RequestParam(required = false) String roomNo) {
        return ApiResult.ok(dormService.roomPage(current, size, buildingId, roomNo));
    }

    @GetMapping("/rooms/options")
    public ApiResult<List<DormRoom>> roomOptions(@RequestParam(required = false) Long buildingId) {
        return ApiResult.ok(dormService.roomsByBuilding(buildingId));
    }

    @PostMapping("/rooms")
    @PreAuthorize("hasAnyRole('DORM_ADMIN','SYS_ADMIN')")
    public ApiResult<DormRoom> createRoom(@Valid @RequestBody Requests.DormRoomRequest request) {
        return ApiResult.ok(dormService.saveRoom(null, request));
    }

    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('DORM_ADMIN','SYS_ADMIN')")
    public ApiResult<DormRoom> updateRoom(@PathVariable Long id, @Valid @RequestBody Requests.DormRoomRequest request) {
        return ApiResult.ok(dormService.saveRoom(id, request));
    }

    @DeleteMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('DORM_ADMIN','SYS_ADMIN')")
    public ApiResult<Void> deleteRoom(@PathVariable Long id) {
        dormService.deleteRoom(id);
        return ApiResult.ok();
    }
}
