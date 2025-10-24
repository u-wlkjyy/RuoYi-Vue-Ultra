package com.ruoyi.common.utils.ip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;

import java.util.Map;

/**
 * 获取地址类
 * 基于纯真IP数据库
 *
 * @author ruoyi
 */
public class AddressUtils
{
    private static final Logger log = LoggerFactory.getLogger(AddressUtils.class);

    // 未知地址
    public static final String UNKNOWN = "XX XX";

    /**
     * 根据IP地址获取地理位置
     *
     * @param ip IP地址
     * @return 地理位置，格式：省份 城市
     */
    public static String getRealAddressByIP(String ip)
    {
        if (StringUtils.isEmpty(ip))
        {
            return UNKNOWN;
        }

        try
        {
            // 获取 IpLocationUtil Bean
            IpLocationUtil ipLocationUtil = SpringUtils.getBean(IpLocationUtil.class);

            // 查询IP详细信息
            Map<String, String> detail = ipLocationUtil.searchIpDetailed(ip);

            if (detail == null || detail.isEmpty())
            {
                log.warn("无法查询IP地理位置: {}", ip);
                return UNKNOWN;
            }
            return detail.getOrDefault("fullLocation",UNKNOWN);
        }
        catch (Exception e)
        {
            log.error("查询IP地理位置异常: ip={}, error={}", ip, e.getMessage());
            return UNKNOWN;
        }
    }

    /**
     * 获取完整的地理位置信息
     *
     * @param ip IP地址
     * @return 地理位置，格式：国家-省份-城市-区域 ISP
     */
    public static String getFullAddressByIP(String ip)
    {
        // 内网不查询
        if (IpUtils.internalIp(ip))
        {
            return "内网IP";
        }

        if (StringUtils.isEmpty(ip))
        {
            return UNKNOWN;
        }

        try
        {
            IpLocationUtil ipLocationUtil = SpringUtils.getBean(IpLocationUtil.class);
            String location = ipLocationUtil.searchIp(ip);

            if (StringUtils.isNotEmpty(location))
            {
                return location;
            }

            return UNKNOWN;
        }
        catch (Exception e)
        {
            log.error("查询IP完整地理位置异常: ip={}, error={}", ip, e.getMessage());
            return UNKNOWN;
        }
    }

    /**
     * 获取地理位置详细信息
     *
     * @param ip IP地址
     * @return Map包含: country(国家), province(省份), city(城市), area(区域), isp(ISP)
     */
    public static Map<String, String> getAddressDetail(String ip)
    {
        try
        {
            IpLocationUtil ipLocationUtil = SpringUtils.getBean(IpLocationUtil.class);
            return ipLocationUtil.searchIpDetailed(ip);
        }
        catch (Exception e)
        {
            log.error("查询IP地理位置详情异常: ip={}, error={}", ip, e.getMessage());
            return null;
        }
    }
}
