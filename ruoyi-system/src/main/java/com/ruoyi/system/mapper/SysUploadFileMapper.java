package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysUploadFile;

/**
 * 文件上传记录 数据层
 * 
 * @author ruoyi
 */
public interface SysUploadFileMapper
{
    /**
     * 查询文件上传记录
     * 
     * @param id 文件ID
     * @return 文件上传记录
     */
    public SysUploadFile selectUploadFileById(Integer id);

    /**
     * 查询文件上传记录列表
     * 
     * @param uploadFile 文件上传记录
     * @return 文件上传记录集合
     */
    public List<SysUploadFile> selectUploadFileList(SysUploadFile uploadFile);

    /**
     * 新增文件上传记录
     * 
     * @param uploadFile 文件上传记录
     * @return 结果
     */
    public int insertUploadFile(SysUploadFile uploadFile);

    /**
     * 修改文件上传记录
     * 
     * @param uploadFile 文件上传记录
     * @return 结果
     */
    public int updateUploadFile(SysUploadFile uploadFile);

    /**
     * 删除文件上传记录
     * 
     * @param id 文件ID
     * @return 结果
     */
    public int deleteUploadFileById(Integer id);

    /**
     * 批量删除文件上传记录
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteUploadFileByIds(Integer[] ids);
}

