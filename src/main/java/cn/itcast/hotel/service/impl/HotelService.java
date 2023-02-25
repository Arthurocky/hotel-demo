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
     * 进行查询
     *
     * @param requestParams
     * @return
     */
    @Override
    public PageResult search(RequestParams requestParams) throws IOException
    {
        //1. 创建searchRequest对象, 指定索引名称
        SearchRequest searchRequest = new SearchRequest("hotel");
        //2. 构建查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //if (requestParams.getKey()!=null)
        if (StringUtils.isNotEmpty(requestParams.getKey())) {
            //当搜索框内有内容时
            BoolQueryBuilder queryBuilder = boolQuery.must(QueryBuilders.matchQuery("all", requestParams.getKey()));
        } else {
            //当搜索框内无内容时，搜索全部酒店
            boolQuery.must(QueryBuilders.matchAllQuery());
        }
        //对酒店结果进行过滤
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
        //对搜寻到的数据按距离进行排序
        if (StringUtils.isNotEmpty(requestParams.getLocation())) {
            //GeoDistanceSortBuilder location = SortBuilders.geoDistanceSort("location", new GeoPoint(requestParams.getLocation()));
            //GeoDistanceSortBuilder sortLocation = location.order(SortOrder.ASC).unit(DistanceUnit.KILOMETERS);
            GeoDistanceSortBuilder sortBuilder = SortBuilders.geoDistanceSort("location", requestParams.getLocation()).unit(DistanceUnit.KILOMETERS).order(SortOrder.ASC);
            searchRequest.source().sort(sortBuilder);
        }
        //分页显示
        Integer page = requestParams.getPage();
        Integer size = requestParams.getSize();
        searchRequest.source().from((page - 1) * size).size(size);

        //3.获取条件搜索请求
        searchRequest.source().query(boolQuery);

        //4. 根据请求执行查询
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        //5.定义对象，用于返回
        PageResult pageResult = new PageResult();

        //6.根据条件获取结果数据
        SearchHits hits = searchResponse.getHits();

        //7.获取到符合
        long total = hits.getTotalHits().value;

        pageResult.setTotal(total);

        for (SearchHit hit : hits.getHits()) {
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            //解析高亮
            if (null != hit.getHighlightFields()) {
                HighlightField fieldName = hit.getHighlightFields().get("name");
                if (null != fieldName) {
                    Text[] fragments = fieldName.getFragments();
                    String highName = Arrays.stream(fragments).map(Text::string).collect(Collectors.joining(","));
                    hotelDoc.setName(highName);
                }
            }
            //解析距离
            Object[] sortValues = hit.getSortValues();
            if (null != sortValues && sortValues.length > 0){
                hotelDoc.setDistance(sortValues[0]);
            }
            //System.out.println(hotelDoc);
            pageResult.getHotels().add(hotelDoc);
        }
        return pageResult;
    }
}
