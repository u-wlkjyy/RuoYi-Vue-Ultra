import request from '@/utils/request'

// 获取图片列表
export function getImageList(query) {
  return request({
    url: '/common/filesystem/images',
    method: 'get',
    params: query
  })
}

// 获取附件列表
export function getAttachmentList(query) {
  return request({
    url: '/common/filesystem/attachments',
    method: 'get',
    params: query
  })
}

// 获取统计信息
export function getFileStats() {
  return request({
    url: '/common/filesystem/stats',
    method: 'get'
  })
}

