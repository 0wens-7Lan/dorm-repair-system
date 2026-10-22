package com.dormrepair.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public final class Requests {
    private Requests() {
    }

    public record LoginRequest(@NotBlank @Size(max=60) String username, @NotBlank @Size(max=72) String password) {
    }

    public record RegisterRequest(@NotBlank @Size(max=60) String username,
                                  @NotBlank @Size(min=8,max=72) @Pattern(regexp="[ -~]+",message="密码须为8至72位可打印ASCII字符") String password,
                                  @NotBlank String name,
                                  @NotBlank String studentNo,
                                  @NotBlank String phone,
                                  @NotNull Long dormRoomId) {
    }

    public record CreateRepairRequest(@NotNull Long categoryId,
                                      @NotNull Long dormRoomId,
                                      @NotBlank String description,
                                      @NotBlank String contactPhone,
                                      LocalDateTime expectedTime,
                                      String imageUrl) {
    }

    public record AuditRequest(@NotNull Boolean approved, String rejectReason) {
    }

    public record AssignRequest(@NotNull Long repairerId, String remark) {
    }

    public record FinishRepairRequest(@NotBlank String faultReason,
                                      @NotBlank String repairMethod,
                                      String replacedParts,
                                      @NotBlank String resultSummary,
                                      String resultImageUrl) {
    }

    public record FeedbackRequest(@Min(1) @Max(5) Integer speedScore,
                                  @Min(1) @Max(5) Integer attitudeScore,
                                  @Min(1) @Max(5) Integer qualityScore,
                                  @Min(1) @Max(5) Integer overallScore,
                                  String comment,
                                  Boolean unresolved) {
    }

    public record ReopenRequest(@NotBlank String reason, Long repairerId) {
    }

    public record UserCreateRequest(@NotBlank @Size(max=60) String username,
                                    @NotBlank @Size(min=8,max=72) @Pattern(regexp="[ -~]+",message="密码须为8至72位可打印ASCII字符") String password,
                                    @NotBlank String name,
                                    String studentNo,
                                    String phone,
                                    @NotNull Long roleId,
                                    Long dormRoomId,
                                    String status) {
    }

    public record UserUpdateRequest(@NotBlank String name,
                                    String studentNo,
                                    String phone,
                                    @NotNull Long roleId,
                                    Long dormRoomId,
                                    String status) {
    }

    public record PasswordResetRequest(@NotBlank @Size(min=8,max=72) @Pattern(regexp="[ -~]+",message="密码须为8至72位可打印ASCII字符") String newPassword) {
    }

    public record NoticeRequest(@NotBlank String title, @NotBlank String content, String status) {
    }

    public record DormBuildingRequest(@NotBlank String name, Integer floors, String address) {
    }

    public record DormRoomRequest(@NotNull Long buildingId,
                                  @NotNull Integer floor,
                                  @NotBlank String roomNo,
                                  Integer capacity,
                                  Integer occupied) {
    }
}
