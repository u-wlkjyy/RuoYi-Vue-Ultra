package com.ruoyi.web.controller.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.system.domain.SysUploadFile;
import com.ruoyi.system.service.ISysUploadFileService;

/**
 * 文件系统控制器
 * 用于管理和查看已上传的文件
 * 
 * @author wlkjyy
 */
@RestController
@RequestMapping("/common/filesystem")
public class FileSystemController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(FileSystemController.class);

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private ISysUploadFileService uploadFileService;

    /**
     * 支持的图片格式
     */
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(
        Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg")
    );

    /**
     * 支持的附件格式（文档类）
     */
    private static final Set<String> ATTACHMENT_EXTENSIONS = new HashSet<>(
        Arrays.asList("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "pdf","zip", "rar", "7z", "tar", "gz", "bz2", "xz")
    );

    /**
     * 获取所有图片列表（基于数据库）
     * 管理员可以查看所有文件，普通用户只能查看自己上传的文件
     * 
     * @param keyword 关键词搜索（可选）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认20）
     * @return 图片列表
     */
    @GetMapping("/images")
    public AjaxResult listImages(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize)
    {
        try
        {
            LoginUser loginUser = getLoginUser();
            Long userId = loginUser.getUserId();
            boolean isAdmin = SysUser.isAdmin(userId);

            // 构建查询条件
            SysUploadFile query = new SysUploadFile();
            
            // 如果不是管理员，只查询自己的文件
            if (!isAdmin)
            {
                query.setUserId(userId.intValue());
            }
            
            // 关键词搜索
            if (keyword != null && !keyword.trim().isEmpty())
            {
                query.setPath(keyword);
            }

            // 查询数据库中的文件记录
            List<SysUploadFile> uploadFiles = uploadFileService.selectUploadFileList(query);

            // 过滤出图片文件并构建返回信息
            List<Map<String, Object>> imageList = uploadFiles.stream()
                .filter(file -> isImageFile(file.getPath()))
                .map(this::buildFileInfoFromDb)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // 按创建时间倒序排序（最新的在前面）
            imageList.sort((a, b) -> {
                Date dateA = (Date) a.get("createTime");
                Date dateB = (Date) b.get("createTime");
                return dateB.compareTo(dateA);
            });

            // 分页处理
            int total = imageList.size();
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);

            List<Map<String, Object>> pagedList;
            if (fromIndex < total)
            {
                pagedList = imageList.subList(fromIndex, toIndex);
            }
            else
            {
                pagedList = Collections.emptyList();
            }

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("total", total);
            result.put("rows", pagedList);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("isAdmin", isAdmin);

            return AjaxResult.success(result);
        }
        catch (Exception e)
        {
            log.error("获取图片列表失败", e);
            return AjaxResult.error("获取图片列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有附件列表（基于数据库）
     * 管理员可以查看所有文件，普通用户只能查看自己上传的文件
     * 
     * @param keyword 关键词搜索（可选）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认20）
     * @return 附件列表
     */
    @GetMapping("/attachments")
    public AjaxResult listAttachments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize)
    {
        try
        {
            LoginUser loginUser = getLoginUser();
            Long userId = loginUser.getUserId();
            boolean isAdmin = SysUser.isAdmin(userId);

            // 构建查询条件
            SysUploadFile query = new SysUploadFile();
            
            // 如果不是管理员，只查询自己的文件
            if (!isAdmin)
            {
                query.setUserId(userId.intValue());
            }
            
            // 关键词搜索
            if (keyword != null && !keyword.trim().isEmpty())
            {
                query.setPath(keyword);
            }

            // 查询数据库中的文件记录
            List<SysUploadFile> uploadFiles = uploadFileService.selectUploadFileList(query);

            // 过滤出附件文件并构建返回信息
            List<Map<String, Object>> attachmentList = uploadFiles.stream()
                .filter(file -> isAttachmentFile(file.getPath()))
                .map(this::buildFileInfoFromDb)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // 按创建时间倒序排序（最新的在前面）
            attachmentList.sort((a, b) -> {
                Date dateA = (Date) a.get("createTime");
                Date dateB = (Date) b.get("createTime");
                return dateB.compareTo(dateA);
            });

            // 分页处理
            int total = attachmentList.size();
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);

            List<Map<String, Object>> pagedList;
            if (fromIndex < total)
            {
                pagedList = attachmentList.subList(fromIndex, toIndex);
            }
            else
            {
                pagedList = Collections.emptyList();
            }

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("total", total);
            result.put("rows", pagedList);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("isAdmin", isAdmin);

            return AjaxResult.success(result);
        }
        catch (Exception e)
        {
            log.error("获取附件列表失败", e);
            return AjaxResult.error("获取附件列表失败: " + e.getMessage());
        }
    }

    /**
     * 递归扫描目录下的所有图片文件
     * 
     * @param directory 目录
     * @param keyword 关键词过滤
     * @return 图片文件列表
     */
    private List<Map<String, Object>> scanImages(File directory, String keyword)
    {
        List<Map<String, Object>> imageList = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(directory.getPath())))
        {
            paths.filter(Files::isRegularFile)
                .filter(path -> isImageFile(path.toString()))
                .filter(path -> {
                    // 关键词过滤
                    if (keyword == null || keyword.trim().isEmpty())
                    {
                        return true;
                    }
                    return path.getFileName().toString().toLowerCase()
                        .contains(keyword.toLowerCase());
                })
                .forEach(path -> {
                    try
                    {
                        Map<String, Object> imageInfo = buildImageInfo(path);
                        if (imageInfo != null)
                        {
                            imageList.add(imageInfo);
                        }
                    }
                    catch (Exception e)
                    {
                        log.warn("读取文件信息失败: {}", path, e);
                    }
                });
        }
        catch (IOException e)
        {
            log.error("扫描目录失败: {}", directory, e);
        }

        return imageList;
    }

    /**
     * 递归扫描目录下的所有附件文件
     * 
     * @param directory 目录
     * @param keyword 关键词过滤
     * @return 附件文件列表
     */
    private List<Map<String, Object>> scanAttachments(File directory, String keyword)
    {
        List<Map<String, Object>> attachmentList = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(directory.getPath())))
        {
            paths.filter(Files::isRegularFile)
                .filter(path -> isAttachmentFile(path.toString()))
                .filter(path -> {
                    // 关键词过滤
                    if (keyword == null || keyword.trim().isEmpty())
                    {
                        return true;
                    }
                    return path.getFileName().toString().toLowerCase()
                        .contains(keyword.toLowerCase());
                })
                .forEach(path -> {
                    try
                    {
                        Map<String, Object> attachmentInfo = buildFileInfo(path);
                        if (attachmentInfo != null)
                        {
                            attachmentList.add(attachmentInfo);
                        }
                    }
                    catch (Exception e)
                    {
                        log.warn("读取文件信息失败: {}", path, e);
                    }
                });
        }
        catch (IOException e)
        {
            log.error("扫描目录失败: {}", directory, e);
        }

        return attachmentList;
    }

    /**
     * 从数据库记录构建文件信息
     * 
     * @param uploadFile 上传文件记录
     * @return 文件信息
     */
    private Map<String, Object> buildFileInfoFromDb(SysUploadFile uploadFile)
    {
        try
        {
            String filePath = uploadFile.getPath();
            
            // 构建完整的文件系统路径
            String profilePath = RuoYiConfig.getProfile();
            String fullPath = profilePath + filePath.replace("/profile", "");
            File file = new File(fullPath);
            
            // 如果文件不存在，返回 null
            if (!file.exists())
            {
                log.warn("文件不存在: {}", fullPath);
                return null;
            }
            
            // 构建访问URL
            String url = serverConfig.getUrl() + filePath;

            Map<String, Object> info = new HashMap<>();
            info.put("id", uploadFile.getId());
            info.put("userId", uploadFile.getUserId());
            info.put("fileName", file.getName());
            info.put("filePath", filePath);
            info.put("url", url);
            info.put("size", file.length());
            info.put("sizeFormatted", formatFileSize(file.length()));
            info.put("createTime", uploadFile.getCreateTime());
            info.put("modifiedTime", file.lastModified());
            info.put("modifiedDate", new Date(file.lastModified()));
            info.put("extension", getFileExtension(file.getName()));

            return info;
        }
        catch (Exception e)
        {
            log.error("构建文件信息失败: {}", uploadFile.getPath(), e);
            return null;
        }
    }

    /**
     * 构建图片信息
     * 
     * @param path 文件路径
     * @return 图片信息
     */
    private Map<String, Object> buildImageInfo(Path path) throws IOException
    {
        File file = path.toFile();
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

        // 计算相对路径（相对于 profile 的父目录，保留 /profile 前缀）
        String profilePath = RuoYiConfig.getProfile();
        String absolutePath = file.getAbsolutePath().replace("\\", "/");
        
        // 确保 profilePath 使用统一的路径分隔符
        String normalizedProfilePath = profilePath.replace("\\", "/");
        
        // 获取 profile 目录名称（通常是 "uploadPath"）
        String profileDirName = new File(normalizedProfilePath).getName();
        
        // 截取相对路径，保留 /profile 前缀
        String relativePath;
        int profileIndex = absolutePath.indexOf("/" + profileDirName + "/");
        if (profileIndex != -1)
        {
            // 从 profile 目录名称开始截取，替换为 /profile
            String pathAfterProfile = absolutePath.substring(profileIndex + profileDirName.length() + 1);
            // 确保路径以 / 开头
            if (!pathAfterProfile.startsWith("/"))
            {
                relativePath = "/profile/" + pathAfterProfile;
            }
            else
            {
                relativePath = "/profile" + pathAfterProfile;
            }
        }
        else
        {
            // 降级方案：尝试从 /upload 开始截取
            int uploadIndex = absolutePath.indexOf("/upload/");
            if (uploadIndex != -1)
            {
                relativePath = "/profile" + absolutePath.substring(uploadIndex);
            }
            else
            {
                // 最后的降级方案：使用文件名
                relativePath = "/profile/upload/" + file.getName();
            }
        }

        // 构建访问URL
        String url = serverConfig.getUrl() + relativePath;

        Map<String, Object> info = new HashMap<>();
        info.put("fileName", file.getName());
        info.put("filePath", relativePath);
        info.put("url", url);
        info.put("size", file.length());
        info.put("sizeFormatted", formatFileSize(file.length()));
        info.put("modifiedTime", file.lastModified());
        info.put("modifiedDate", new Date(file.lastModified()));
        info.put("createdTime", attrs.creationTime().toMillis());
        info.put("createdDate", new Date(attrs.creationTime().toMillis()));
        info.put("extension", getFileExtension(file.getName()));

        return info;
    }

    /**
     * 构建文件信息（通用方法）
     * 
     * @param path 文件路径
     * @return 文件信息
     */
    private Map<String, Object> buildFileInfo(Path path) throws IOException
    {
        // 复用 buildImageInfo 的逻辑，适用于所有文件类型
        return buildImageInfo(path);
    }

    /**
     * 判断是否为图片文件
     * 
     * @param filePath 文件路径
     * @return true=图片文件
     */
    private boolean isImageFile(String filePath)
    {
        String extension = getFileExtension(filePath);
        return IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * 判断是否为附件文件
     * 
     * @param filePath 文件路径
     * @return true=附件文件
     */
    private boolean isAttachmentFile(String filePath)
    {
        String extension = getFileExtension(filePath);
        return ATTACHMENT_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * 获取文件扩展名
     * 
     * @param fileName 文件名
     * @return 扩展名（小写，不含点）
     */
    private String getFileExtension(String fileName)
    {
        if (fileName == null || fileName.isEmpty())
        {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1)
        {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 格式化文件大小
     * 
     * @param size 文件大小（字节）
     * @return 格式化后的大小
     */
    private String formatFileSize(long size)
    {
        if (size < 1024)
        {
            return size + " B";
        }
        else if (size < 1024 * 1024)
        {
            return String.format("%.2f KB", size / 1024.0);
        }
        else if (size < 1024 * 1024 * 1024)
        {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
        else
        {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 获取上传目录统计信息（基于数据库）
     * 管理员可以查看所有统计，普通用户只能查看自己的统计
     * 
     * @return 统计信息
     */
    @GetMapping("/stats")
    public AjaxResult getStats()
    {
        try
        {
            LoginUser loginUser = getLoginUser();
            Long userId = loginUser.getUserId();
            boolean isAdmin = SysUser.isAdmin(userId);

            // 构建查询条件
            SysUploadFile query = new SysUploadFile();
            
            // 如果不是管理员，只查询自己的文件
            if (!isAdmin)
            {
                query.setUserId(userId.intValue());
            }

            // 查询数据库中的文件记录
            List<SysUploadFile> uploadFiles = uploadFileService.selectUploadFileList(query);

            Map<String, Object> stats = new HashMap<>();
            
            long totalSize = 0;
            int imageCount = 0;
            int attachmentCount = 0;
            int totalFiles = uploadFiles.size();

            // 计算统计信息
            for (SysUploadFile uploadFile : uploadFiles)
            {
                String filePath = uploadFile.getPath();
                String fullPath = RuoYiConfig.getProfile() + filePath.replace("/profile", "");
                File file = new File(fullPath);
                
                if (file.exists())
                {
                    totalSize += file.length();
                    
                    if (isImageFile(filePath))
                    {
                        imageCount++;
                    }
                    else if (isAttachmentFile(filePath))
                    {
                        attachmentCount++;
                    }
                }
            }

            stats.put("totalFiles", totalFiles);
            stats.put("imageCount", imageCount);
            stats.put("attachmentCount", attachmentCount);
            stats.put("otherFiles", totalFiles - imageCount - attachmentCount);
            stats.put("totalSize", totalSize);
            stats.put("totalSizeFormatted", formatFileSize(totalSize));
            stats.put("isAdmin", isAdmin);

            return AjaxResult.success(stats);
        }
        catch (Exception e)
        {
            log.error("获取统计信息失败", e);
            return AjaxResult.error("获取统计信息失败: " + e.getMessage());
        }
    }
}

