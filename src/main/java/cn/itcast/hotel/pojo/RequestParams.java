package cn.itcast.hotel.pojo;

import lombok.Data;

/**
 * 接收关键字搜索时前端提交过来的数据
 */
@Data
public class RequestParams {
    private String key;
    private Integer page;
    private Integer size;
    private String sortBy;

    /**
     * 新增的过滤条件参数
     */
    private String city;
    private String brand;
    private String starName;
    private Integer minPrice;
    private Integer maxPrice;

    /**
     * 添加当前的地理坐标: 纬度,经度
     */
    private String location;

}