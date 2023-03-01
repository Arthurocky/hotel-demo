package cn.itcast.hotel.controller;


import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Arthurocky
 */
@Slf4j
@RequestMapping("/hotel")
@RestController
public class HotelController {

    @Resource
    private IHotelService hotelService;

    /**
     * 精确查询获取列表
     * 请求网址: http://localhost:8089/hotel/list
     * 请求方法: POST
     */
    @PostMapping("/list")
    public PageResult getList(@RequestBody RequestParams requestParams) throws IOException
    {
        PageResult pageResult = hotelService.search(requestParams);
        return pageResult;
    }

    @PostMapping("/filters")
    public Map<String, List<String>> filters(@RequestBody RequestParams requestParams)
    {
        Map<String, List<String>> resultFilter = hotelService.getFilter(requestParams);
        return resultFilter;

    }

    @GetMapping("/suggestion")
    public List<String> suggest(String key){
        List<String> list = hotelService.getSuggestion(key);
        return list;
    }

}
