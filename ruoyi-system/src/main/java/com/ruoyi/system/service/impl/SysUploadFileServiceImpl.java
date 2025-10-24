package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.SysUploadFile;
import com.ruoyi.system.mapper.SysUploadFileMapper;
import com.ruoyi.system.service.ISysUploadFileService;

/**
 * 文件上传记录 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class SysUploadFileServiceImpl implements ISysUploadFileService
{
    @Autowired
    private SysUploadFileMapper uploadFileMapper;

    /**
     * 查询文件上传记录
     * 
     * @param id 文件ID
     * @return 文件上传记录
     */
    @Override
    public SysUploadFile selectUploadFileById(Integer id)
    {
        return uploadFileMapper.selectUploadFileById(id);
    }

    /**
     * 查询文件上传记录列表
     * 
     * @param uploadFile 文件上传记录
     * @return 文件上传记录集合
     */
    @Override
    public List<SysUploadFile> selectUploadFileList(SysUploadFile uploadFile)
    {
        return uploadFileMapper.selectUploadFileList(uploadFile);
    }

    /**
     * 新增文件上传记录
     * 
     * @param uploadFile 文件上传记录
     * @return 结果
     */
    @Override
    public int insertUploadFile(SysUploadFile uploadFile)
    {
        return uploadFileMapper.insertUploadFile(uploadFile);
    }

    /**
     * 修改文件上传记录
     * 
     * @param uploadFile 文件上传记录
     * @return 结果
     */
    @Override
    public int updateUploadFile(SysUploadFile uploadFile)
    {
        return uploadFileMapper.updateUploadFile(uploadFile);
    }

    /**
     * 批量删除文件上传记录
     * 
     * @param ids 需要删除的文件ID
     * @return 结果
     */
    @Override
    public int deleteUploadFileByIds(Integer[] ids)
    {
        return uploadFileMapper.deleteUploadFileByIds(ids);
    }

    /**
     * 删除文件上传记录信息
     * 
     * @param id 文件ID
     * @return 结果
     */
    @Override
    public int deleteUploadFileById(Integer id)
    {
        return uploadFileMapper.deleteUploadFileById(id);
    }
}

