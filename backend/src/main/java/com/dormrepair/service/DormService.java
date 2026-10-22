package com.dormrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dormrepair.dto.Requests;
import com.dormrepair.entity.DormBuilding;
import com.dormrepair.entity.DormRoom;
import com.dormrepair.mapper.DormBuildingMapper;
import com.dormrepair.mapper.DormRoomMapper;
import com.dormrepair.support.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DormService {
    private final DormBuildingMapper buildingMapper;
    private final DormRoomMapper roomMapper;
    private final OperationLogService operationLogService;

    public DormService(DormBuildingMapper buildingMapper, DormRoomMapper roomMapper, OperationLogService operationLogService) {
        this.buildingMapper = buildingMapper;
        this.roomMapper = roomMapper;
        this.operationLogService = operationLogService;
    }

    public List<DormBuilding> buildings() {
        return buildingMapper.selectList(new QueryWrapper<DormBuilding>().orderByAsc("name"));
    }

    public List<DormRoom> roomsByBuilding(Long buildingId) {
        QueryWrapper<DormRoom> wrapper = new QueryWrapper<DormRoom>().orderByAsc("floor", "room_no");
        if (buildingId != null) {
            wrapper.eq("building_id", buildingId);
        }
        return roomMapper.selectList(wrapper);
    }

    public IPage<Map<String, Object>> roomPage(long current, long size, Long buildingId, String roomNo) {
        return roomMapper.selectRoomPage(new Page<>(current, size), buildingId, roomNo);
    }

    @Transactional
    public DormBuilding saveBuilding(Long id, Requests.DormBuildingRequest request) {
        DormBuilding building = id == null ? new DormBuilding() : requireBuilding(id);
        building.setName(request.name());
        building.setFloors(request.floors());
        building.setAddress(request.address());
        if (id == null) {
            building.setCreateTime(LocalDateTime.now());
            buildingMapper.insert(building);
            operationLogService.log("CREATE_BUILDING", "DORM_BUILDING", building.getId(), "新增宿舍楼：" + building.getName());
        } else {
            buildingMapper.updateById(building);
            operationLogService.log("UPDATE_BUILDING", "DORM_BUILDING", building.getId(), "修改宿舍楼：" + building.getName());
        }
        return building;
    }

    @Transactional
    public DormRoom saveRoom(Long id, Requests.DormRoomRequest request) {
        if (buildingMapper.selectById(request.buildingId()) == null) {
            throw new BusinessException("宿舍楼不存在");
        }
        DormRoom room = id == null ? new DormRoom() : requireRoom(id);
        room.setBuildingId(request.buildingId());
        room.setFloor(request.floor());
        room.setRoomNo(request.roomNo());
        room.setCapacity(request.capacity() == null ? 4 : request.capacity());
        room.setOccupied(request.occupied() == null ? 0 : request.occupied());
        if (id == null) {
            room.setCreateTime(LocalDateTime.now());
            roomMapper.insert(room);
            operationLogService.log("CREATE_ROOM", "DORM_ROOM", room.getId(), "新增宿舍房间：" + room.getRoomNo());
        } else {
            roomMapper.updateById(room);
            operationLogService.log("UPDATE_ROOM", "DORM_ROOM", room.getId(), "修改宿舍房间：" + room.getRoomNo());
        }
        return room;
    }

    @Transactional
    public void deleteBuilding(Long id) {
        buildingMapper.deleteById(id);
        operationLogService.log("DELETE_BUILDING", "DORM_BUILDING", id, "删除宿舍楼");
    }

    @Transactional
    public void deleteRoom(Long id) {
        roomMapper.deleteById(id);
        operationLogService.log("DELETE_ROOM", "DORM_ROOM", id, "删除宿舍房间");
    }

    private DormBuilding requireBuilding(Long id) {
        DormBuilding building = buildingMapper.selectById(id);
        if (building == null) {
            throw new BusinessException("宿舍楼不存在");
        }
        return building;
    }

    private DormRoom requireRoom(Long id) {
        DormRoom room = roomMapper.selectById(id);
        if (room == null) {
            throw new BusinessException("宿舍房间不存在");
        }
        return room;
    }
}
