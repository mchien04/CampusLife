package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityPhoto;
import vn.campuslife.model.ActivityPhotoResponse;
import vn.campuslife.model.Response;
import vn.campuslife.repository.ActivityPhotoRepository;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.service.ActivityPhotoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityPhotoServiceImpl implements ActivityPhotoService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityPhotoServiceImpl.class);
    private static final int MAX_PHOTOS_PER_ACTIVITY = 10;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final ActivityPhotoRepository photoRepository;
    private final ActivityRepository activityRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public Response uploadPhotos(Long activityId, List<MultipartFile> files, List<String> captions, String uploadedBy) {
        try {
            // 1. Validate activity exists
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(activityId);
            if (activityOpt.isEmpty()) {
                return Response.error("Activity not found");
            }
            Activity activity = activityOpt.get();

            // 2. Check if activity has ended
            if (activity.getEndDate() == null || activity.getEndDate().isAfter(LocalDateTime.now())) {
                return Response.error("Cannot upload photos before activity ends");
            }

            // 3. Check current photo count
            Long currentCount = photoRepository.countByActivityIdAndIsDeletedFalse(activityId);
            if (currentCount + files.size() > MAX_PHOTOS_PER_ACTIVITY) {
                return Response
                        .error(String.format("Maximum %d photos allowed per activity. Current: %d, Trying to add: %d",
                                MAX_PHOTOS_PER_ACTIVITY, currentCount, files.size()));
            }

            // 4. Validate and upload files
            List<ActivityPhoto> photos = new ArrayList<>();
            Path activityUploadDir = Paths.get(uploadDir, "activities", activityId.toString());
            Files.createDirectories(activityUploadDir);

            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);

                // Validate file
                if (file.isEmpty()) {
                    continue;
                }

                // Validate file type
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return Response.error("Only image files are allowed");
                }

                // Validate file size
                if (file.getSize() > MAX_FILE_SIZE) {
                    return Response.error("File size must be less than 5MB");
                }

                // Generate unique filename
                String originalFilename = file.getOriginalFilename();
                String fileExtension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String fileName = UUID.randomUUID().toString() + fileExtension;

                // Save file
                Path filePath = activityUploadDir.resolve(fileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Create photo record
                ActivityPhoto photo = new ActivityPhoto();
                photo.setActivity(activity);
                photo.setImageUrl("/uploads/activities/" + activityId + "/" + fileName);
                photo.setUploadedBy(uploadedBy);
                photo.setDisplayOrder((int) (currentCount + i));

                // Set caption if provided
                if (captions != null && i < captions.size() && captions.get(i) != null
                        && !captions.get(i).trim().isEmpty()) {
                    photo.setCaption(captions.get(i).trim());
                }

                photos.add(photo);
            }

            // Save all photos
            List<ActivityPhoto> savedPhotos = photoRepository.saveAll(photos);
            logger.info("Uploaded {} photos for activity {}", savedPhotos.size(), activityId);

            // Convert to response
            List<ActivityPhotoResponse> responses = savedPhotos.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());

            return Response.success("Photos uploaded successfully", responses);
        } catch (IOException e) {
            logger.error("Failed to upload photos: {}", e.getMessage(), e);
            return Response.error("Failed to upload photos: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error uploading photos: {}", e.getMessage(), e);
            return Response.error("Error uploading photos: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getActivityPhotos(Long activityId) {
        try {
            // Validate activity exists
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(activityId);
            if (activityOpt.isEmpty()) {
                return Response.error("Activity not found");
            }

            List<ActivityPhoto> photos = photoRepository
                    .findByActivityIdAndIsDeletedFalseOrderByDisplayOrderAsc(activityId);
            List<ActivityPhotoResponse> responses = photos.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());

            return Response.success("Photos retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to get photos: {}", e.getMessage(), e);
            return Response.error("Failed to get photos: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response deletePhoto(Long photoId, String username) {
        try {
            Optional<ActivityPhoto> photoOpt = photoRepository.findById(photoId);
            if (photoOpt.isEmpty()) {
                return Response.error("Photo not found");
            }

            ActivityPhoto photo = photoOpt.get();
            if (photo.isDeleted()) {
                return Response.error("Photo already deleted");
            }

            // Soft delete
            photo.setDeleted(true);
            photoRepository.save(photo);

            logger.info("Photo {} deleted by {}", photoId, username);
            return Response.success("Photo deleted successfully", null);
        } catch (Exception e) {
            logger.error("Failed to delete photo: {}", e.getMessage(), e);
            return Response.error("Failed to delete photo: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Response updatePhotoOrder(Long photoId, Integer newOrder, String username) {
        try {
            Optional<ActivityPhoto> photoOpt = photoRepository.findById(photoId);
            if (photoOpt.isEmpty()) {
                return Response.error("Photo not found");
            }

            ActivityPhoto photo = photoOpt.get();
            if (photo.isDeleted()) {
                return Response.error("Cannot update order of deleted photo");
            }

            photo.setDisplayOrder(newOrder);
            photoRepository.save(photo);

            logger.info("Photo {} order updated to {} by {}", photoId, newOrder, username);
            return Response.success("Photo order updated successfully", toResponse(photo));
        } catch (Exception e) {
            logger.error("Failed to update photo order: {}", e.getMessage(), e);
            return Response.error("Failed to update photo order: " + e.getMessage());
        }
    }

    private ActivityPhotoResponse toResponse(ActivityPhoto photo) {
        return new ActivityPhotoResponse(
                photo.getId(),
                photo.getActivity().getId(),
                photo.getImageUrl(),
                photo.getCaption(),
                photo.getDisplayOrder(),
                photo.getUploadedBy(),
                photo.getCreatedAt());
    }
    //hien tat ca hinh anh
    @Override
    @Transactional(readOnly = true)
    public Response getAllPhotos() {
        try {
            List<ActivityPhoto> photos = photoRepository
                    .findByIsDeletedFalseOrderByCreatedAtDesc();

            List<ActivityPhotoResponse> responses = photos.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());

            return Response.success("All photos retrieved", responses);

        } catch (Exception e) {
            logger.error("Failed to get all photos: {}", e.getMessage(), e);
            return Response.error("Failed to get all photos: " + e.getMessage());
        }
    }
}
