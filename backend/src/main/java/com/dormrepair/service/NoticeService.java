package com.dormrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dormrepair.dto.Requests;
import com.dormrepair.entity.Notice;
import com.dormrepair.mapper.NoticeMapper;
import com.dormrepair.support.AuthContext;
import com.dormrepair.support.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeService {
    private final NoticeMapper noticeMapper;
    private final OperationLogService operationLogService;

    public NoticeService(NoticeMapper noticeMapper, OperationLogService operationLogService) {
        this.noticeMapper = noticeMapper;
        this.operationLogService = operationLogService;
    }

    public List<Notice> publicNotices() {
        return noticeMapper.selectList(new QueryWrapper<Notice>()
                .eq("status", "PUBLISHED").orderByDesc("publish_time").last("limit 5"));
    }

    public IPage<Notice> page(long current, long size, String keyword, String status) {
        QueryWrapper<Notice> wrapper = new QueryWrapper<Notice>().orderByDesc("publish_time");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("title", keyword);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        return noticeMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Transactional
    public Notice save(Long id, Requests.NoticeRequest request) {
        Notice notice = id == null ? new Notice() : requireNotice(id);
        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setStatus(request.status() == null || request.status().isBlank() ? "PUBLISHED" : request.status());
        notice.setPublisherId(AuthContext.user().getId());
        if (notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        notice.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            noticeMapper.insert(notice);
            operationLogService.log("CREATE_NOTICE", "NOTICE", notice.getId(), "发布公告：" + notice.getTitle());
        } else {
            noticeMapper.updateById(notice);
            operationLogService.log("UPDATE_NOTICE", "NOTICE", notice.getId(), "编辑公告：" + notice.getTitle());
        }
        return notice;
    }

    @Transactional
    public void delete(Long id) {
        Notice notice = requireNotice(id);
        noticeMapper.deleteById(id);
        operationLogService.log("DELETE_NOTICE", "NOTICE", id, "删除公告：" + notice.getTitle());
    }

    private Notice requireNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }
        return notice;
    }
}
