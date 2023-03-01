package cn.itcast.hotel.service;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IHotelService extends IService<Hotel> {
    PageResult search(RequestParams requestParams) throws IOException;

    /**
     * 品牌过滤
     */
    Map<String, List<String>> getFilter(RequestParams requestParams);

    /**
     * 搜索框 自动填充
     * @param key
     * @return
     */
    List<String> getSuggestion(String key);
}
