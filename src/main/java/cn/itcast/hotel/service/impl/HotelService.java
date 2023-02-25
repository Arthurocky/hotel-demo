package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Resource
    private RestHighLevelClient client;

    /**
     * 查询操作
     */
    @Override
    public PageResult search(RequestParams requestParams) throws IOException
    {
        //1. 创建searchRequest对象, 指定索引名称
        SearchRequest searchRequest = new SearchRequest("hotel");
        //2. 根据搜索框构建查询条件,并根据所选的条件对酒店结果进行过滤
        BoolQueryBuilder boolQuery =filterUtilSet(requestParams);

        //3.对搜寻到的数据按距离进行排序
        sortDistanceSet(requestParams, searchRequest);

        //4.对结果进行分页显示
        pageSet(requestParams, searchRequest);

        //5.获取所定义的条件搜索请求
        searchRequest.source().query(boolQuery);

        //6. 根据请求执行查询
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        //7.定义对象，用于返回结果
        PageResult pageResult = new PageResult();

        //8.根据条件获取结果数据
        SearchHits hits = searchResponse.getHits();

        //9.获取到符合的酒店个数并设置到对象中
        pageTotalSet(pageResult, hits);

        //10对每个符合条件的个数进行遍历
        parseResponse(pageResult, hits);

        //11.返回结果
        return pageResult;
    }

    /**
     * 遍历搜索结果并转化为hotelDoc，存入到pageResult
     * @param pageResult
     * @param hits
     */
    private void parseResponse(PageResult pageResult, SearchHits hits)
    {
        for (SearchHit hit : hits.getHits()) {
            //8.1获取到单个数据(json格式)
            String json = hit.getSourceAsString();
            //8.2将json格式转化为HotelDoc形式
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            //8.3解析高亮操作
            highLightSet(hit, hotelDoc);
            //8.4解析距离
            distanceSet(hit, hotelDoc);
            //8.5将获取的单个HotelDoc对象存入pageResult内
            //System.out.println(hotelDoc);
            pageResult.getHotels().add(hotelDoc);
        }
    }

    /**
     * 获取总页数并存入到pageResult
     * @param pageResult
     * @param hits
     */
    private void pageTotalSet(PageResult pageResult, SearchHits hits)
    {
        long total = hits.getTotalHits().value;
        pageResult.setTotal(total);
    }

    /**
     * 距离设置-对单个对象设置距离
     * @param hit
     * @param hotelDoc
     */
    private void distanceSet(SearchHit hit, HotelDoc hotelDoc)
    {
        Object[] sortValues = hit.getSortValues();
        if (null != sortValues && sortValues.length > 0){
            hotelDoc.setDistance(sortValues[0]);
        }
    }

    /**
     * 高亮设置-判断是否有高亮对象，如果存在则设置高亮
     * @param hit
     * @param hotelDoc
     */
    private void highLightSet(SearchHit hit, HotelDoc hotelDoc)
    {
        if (null != hit.getHighlightFields()) {
            HighlightField fieldName = hit.getHighlightFields().get("name");
            //存在高亮名称时
            if (null != fieldName) {
                Text[] fragments = fieldName.getFragments();
                String highName = Arrays.stream(fragments).map(Text::string).collect(Collectors.joining(","));
                hotelDoc.setName(highName);
            }
        }
    }

    /**
     * 设置显示的页数
     * @param requestParams
     * @param searchRequest
     */
    private void pageSet(RequestParams requestParams, SearchRequest searchRequest)
    {
        Integer page = requestParams.getPage();
        Integer size = requestParams.getSize();
        searchRequest.source().from((page - 1) * size).size(size);
    }

    /**
     * 距离设置-对获取的距离按递增进行排序
     * @param requestParams
     * @param searchRequest
     */
    private void sortDistanceSet(RequestParams requestParams, SearchRequest searchRequest)
    {
        if (StringUtils.isNotEmpty(requestParams.getLocation())) {
            //GeoDistanceSortBuilder location = SortBuilders.geoDistanceSort("location", new GeoPoint(requestParams.getLocation()));
            //GeoDistanceSortBuilder sortLocation = location.order(SortOrder.ASC).unit(DistanceUnit.KILOMETERS);
            GeoDistanceSortBuilder sortBuilder = SortBuilders.geoDistanceSort("location", requestParams.getLocation()).unit(DistanceUnit.KILOMETERS).order(SortOrder.ASC);
            searchRequest.source().sort(sortBuilder);
        }
    }

    /**
     * 根据所选的内容进行条件过滤
     */
    private BoolQueryBuilder filterUtilSet(RequestParams requestParams)
    {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //if (requestParams.getKey()!=null)
        if (StringUtils.isNotEmpty(requestParams.getKey())) {
            //2.1当搜索框内有内容时,根据输入的内容进行查询
            boolQuery.must(QueryBuilders.matchQuery("all", requestParams.getKey()));
        } else {
            //2.2当搜索框内无内容时，默认搜索全部酒店
            boolQuery.must(QueryBuilders.matchAllQuery());
        }
        //品牌
        if (StringUtils.isNotEmpty(requestParams.getBrand())) {
            boolQuery.filter(QueryBuilders.matchQuery("all", requestParams.getBrand()));
        }
        //城市
        if (StringUtils.isNotEmpty(requestParams.getCity())) {
            boolQuery.filter(QueryBuilders.matchQuery("all", requestParams.getCity()));
        }
        //
        //星级
        if (StringUtils.isNotEmpty(requestParams.getStarName())) {
            boolQuery.filter(QueryBuilders.termQuery("starName", requestParams.getStarName()));
        }
        //最小价格
        if (requestParams.getMinPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(requestParams.getMinPrice()));
        }
        //最大价格
        if (requestParams.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").lte(requestParams.getMaxPrice()));
        }
        return boolQuery;
    }
}
