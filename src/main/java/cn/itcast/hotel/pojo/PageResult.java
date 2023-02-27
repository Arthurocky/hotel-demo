package cn.itcast.hotel.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页结果对象，返回给前端
 */
@Data
public class PageResult {
    private long total;
    private List<HotelDoc> hotels = new ArrayList<>();
}