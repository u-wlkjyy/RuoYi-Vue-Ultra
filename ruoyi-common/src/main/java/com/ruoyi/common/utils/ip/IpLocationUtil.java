package com.ruoyi.common.utils.ip;

import net.cz88.czdb.DbSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * IP地理位置查询工具类
 * 基于纯真IP数据库
 * 
 * @author ruoyi
 */
@Component
public class IpLocationUtil
{
    private static final Logger log = LoggerFactory.getLogger(IpLocationUtil.class);

    @Autowired
    @Qualifier("ipv4DbSearcher")
    private DbSearcher ipv4DbSearcher;

    @Autowired
    @Qualifier("ipv6DbSearcher")
    private DbSearcher ipv6DbSearcher;

    /**
     * 根据IP地址查询地理位置
     * 自动判断IPv4或IPv6
     * 
     * @param ip IP地址
     * @return 地理位置字符串，格式：国家-省份-城市-区域 ISP，查询失败返回null
     */
    public String searchIp(String ip)
    {
        if (ip == null || ip.trim().isEmpty()) {
            return null;
        }

        try {
            // 判断是IPv4还是IPv6
            if (ip.contains(":")) {
                // IPv6
                return ipv6DbSearcher.search(ip);
            } else {
                // IPv4
                return ipv4DbSearcher.search(ip);
            }
        } catch (Exception e) {
            log.error("查询IP地理位置失败: ip={}, error={}", ip, e.getMessage());
            return null;
        }
    }

    /**
     * 查询IPv4地址的地理位置
     * 
     * @param ip IPv4地址
     * @return 地理位置字符串
     */
    public String searchIpv4(String ip)
    {
        if (ip == null || ip.trim().isEmpty()) {
            return null;
        }

        try {
            return ipv4DbSearcher.search(ip);
        } catch (Exception e) {
            log.error("查询IPv4地理位置失败: ip={}, error={}", ip, e.getMessage());
            return null;
        }
    }

    /**
     * 查询IPv6地址的地理位置
     * 
     * @param ip IPv6地址
     * @return 地理位置字符串
     */
    public String searchIpv6(String ip)
    {
        if (ip == null || ip.trim().isEmpty()) {
            return null;
        }

        try {
            return ipv6DbSearcher.search(ip);
        } catch (Exception e) {
            log.error("查询IPv6地理位置失败: ip={}, error={}", ip, e.getMessage());
            return null;
        }
    }

    /**
     * 查询IP地址并解析为结构化数据
     * 返回格式：国家-省份-城市-区域 ISP
     * 
     * @param ip IP地址
     * @return Map包含: country(国家), province(省份), city(城市), area(区域), isp(ISP)
     */
    public Map<String, String> searchIpDetailed(String ip)
    {
        Map<String, String> result = new HashMap<>();
        String location = searchIp(ip);
        
        if (location == null || location.trim().isEmpty()) {
            return result;
        }

        try {
            // 解析格式：国家-省份-城市-区域 ISP
            String[] parts = location.split("\\s+", 2);
            String address = parts[0];
            String isp = parts.length > 1 ? parts[1] : "";

            String[] addressParts = address.split("-");
            
            result.put("country", addressParts.length > 0 ? addressParts[0] : "");
            result.put("province", addressParts.length > 1 ? addressParts[1] : "");
            result.put("city", addressParts.length > 2 ? addressParts[2] : "");
            result.put("area", addressParts.length > 3 ? addressParts[3] : "");
            result.put("isp", isp);
            result.put("fullLocation", location);
            
        } catch (Exception e) {
            log.error("解析IP地理位置失败: ip={}, location={}, error={}", ip, location, e.getMessage());
            result.put("fullLocation", location);
        }

        return result;
    }

    /**
     * 获取国家
     * 
     * @param ip IP地址
     * @return 国家名称
     */
    public String getCountry(String ip)
    {
        Map<String, String> detail = searchIpDetailed(ip);
        return detail.getOrDefault("country", "");
    }

    /**
     * 获取省份
     * 
     * @param ip IP地址
     * @return 省份名称
     */
    public String getProvince(String ip)
    {
        Map<String, String> detail = searchIpDetailed(ip);
        return detail.getOrDefault("province", "");
    }

    /**
     * 获取城市
     * 
     * @param ip IP地址
     * @return 城市名称
     */
    public String getCity(String ip)
    {
        Map<String, String> detail = searchIpDetailed(ip);
        return detail.getOrDefault("city", "");
    }

    /**
     * 获取ISP
     * 
     * @param ip IP地址
     * @return ISP名称
     */
    public String getIsp(String ip)
    {
        Map<String, String> detail = searchIpDetailed(ip);
        return detail.getOrDefault("isp", "");
    }
}

