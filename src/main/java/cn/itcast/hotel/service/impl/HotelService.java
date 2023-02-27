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
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Arthurocky
 */
@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Resource
    private RestHighLevelClient client;

    /**
     * 查询操作
     */
    @Override
    public PageResult search(RequestParams requestParams)
    {
        //1. 创建searchRequest对象, 指定索引名称

        SearchRequest searchRequest = new SearchRequest("hotel");

        //2. 根据搜索框构建查询条件,并根据所选的条件对酒店结果进行过滤
        BoolQueryBuilder boolQuery = filterUtilSet(requestParams);

        //3.获取所定义的条件搜索请求
        searchRequest.source().query(boolQuery);

        //添加算分函数
/*        FunctionScoreQueryBuilder.FilterFunctionBuilder[] functionBuilders = {
                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                        QueryBuilders.termQuery("isAD", true),
                        ScoreFunctionBuilders.weightFactorFunction(10)
                )
        };
        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(boolQuery, functionBuilders);
        searchRequest.source().query(functionScoreQuery);*/

        //算分
        // 2.算分控制
        FunctionScoreQueryBuilder functionScoreQuery =
                QueryBuilders.functionScoreQuery(
                        // 原始查询，相关性算分的查询
                        boolQuery,
                        // function score的数组
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                // 其中的一个function score 元素
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        // 过滤条件
                                        QueryBuilders.termQuery("isAD", true),
                                        // 算分函数
                                        ScoreFunctionBuilders.weightFactorFunction(10)
                                )
                        });
        searchRequest.source().query(functionScoreQuery);

        //添加高亮
        addHighLight(searchRequest);

        //4.对搜寻到的数据按距离进行排序
        sortDistanceSet(requestParams, searchRequest);


        //5.对结果进行分页显示
        pageSet(requestParams, searchRequest);

        //6. 根据请求执行查询
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //7.定义对象，用于返回结果
        PageResult pageResult = new PageResult();

        //8.根据条件获取结果数据
        SearchHits searchHits = searchResponse.getHits();

        //9.获取到符合的酒店个数并设置到对象中
        pageTotalSet(pageResult, searchHits);

        //10对每个符合条件的个数进行遍历
        parseResponse(pageResult, searchHits);

        //11.返回结果
        return pageResult;
    }


    /**
     * 对品牌过滤
     */
    @Override
    public Map<String, List<String>> getFilter(RequestParams requestParams)
    {

        //1.创建返回对象
        Map<String, List<String>> map = new HashMap<>();

        //2.创建SearchRequest
        SearchRequest request = new SearchRequest("hotel");

        //3.使用聚合前，需进行条件过滤
        request.source().query(filterUtilSet(requestParams));

        //4.做聚合, 按品牌聚合
        TermsAggregationBuilder agg = AggregationBuilders.terms("brandAgg").field("brand").size(10);
        request.source().aggregation(agg);

        //5.进行查询
        SearchResponse search = null;
        try {
            search = client.search(request, RequestOptions.DEFAULT);
            //6.解析结果
            Aggregations aggregations = search.getAggregations();
            Terms brandAgg = aggregations.get("brandAgg");
            List<? extends Terms.Bucket> buckets = brandAgg.getBuckets();
            //7.将结果注入到返回对象中
            List<String> list = new ArrayList<>();
            for (Terms.Bucket bucket : buckets) {
                list.add(bucket.getKeyAsString());
                System.out.println(bucket.getKey());
            }
            map.put("brand", list);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //8。返回结果
        return map;
    }

    private void addHighLight(SearchRequest searchRequest)
    {
        HighlightBuilder highlightBuilder = new HighlightBuilder().field("name").requireFieldMatch(false);
        searchRequest.source().highlighter(highlightBuilder);
    }


    /**
     * 遍历搜索结果并转化为hotelDoc，存入到pageResult
     *
     * @param pageResult
     */
    private void parseResponse(PageResult pageResult, SearchHits searchHits)
    {
        SearchHit[] hits = searchHits.getHits();

        for (SearchHit hit : hits) {
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
     *
     * @param pageResult
     * @param hits
     */
    private void pageTotalSet(PageResult pageResult, SearchHits hits)
    {
        long total = hits.getTotalHits().value;
        pageResult.setTotal(total);
    }

    /**
     * 解析距离-对单个对象设置显示距离
     *
     * @param hit
     * @param hotelDoc
     */
    private void distanceSet(SearchHit hit, HotelDoc hotelDoc)
    {
        Object[] sortValues = hit.getSortValues();
        if (null != sortValues && sortValues.length > 0) {
            hotelDoc.setDistance(sortValues[0]);
        }
    }

    /**
     * 高亮设置-判断是否有高亮对象，如果存在则设置高亮
     *
     * @param hit
     * @param hotelDoc
     */
    private void highLightSet(SearchHit hit, HotelDoc hotelDoc)
    {

        //如果存在高亮，则进行下列操作
        Map<String, HighlightField> highlightFieldMap = hit.getHighlightFields();
        //if(null !=highlightFieldMap && highlightFieldMap.size()>0)
        if (!CollectionUtils.isEmpty(highlightFieldMap)) {
            for (String key : highlightFieldMap.keySet()) {
                //key-->HotelDoc的属性名
                HighlightField field = highlightFieldMap.get(key);
                //存在高亮名称时
                if (null != field) {
                    Text[] fragments = field.getFragments();
                    String highName = Arrays.stream(fragments).map(Text::string).collect(Collectors.joining(","));
                    //获取到属性
                    PropertyDescriptor propertyDescriptor = BeanUtils.getPropertyDescriptor(hotelDoc.getClass(), key);
                    //propertyDescriptor.getReadMethod();//getName
                    //propertyDescriptor.getWriteMethod();//setName
                    //获取到要高亮的属性方法
                    Method setMethod = propertyDescriptor.getWriteMethod();
                    try {
                        //调用方法，将高亮设置到存在的属性上
                        setMethod.invoke(hotelDoc, highName);
                    } catch (IllegalAccessException e) {
                        //e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        //e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 设置显示的页数
     *
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
     *
     * @param requestParams
     * @param searchRequest
     */
    private void sortDistanceSet(RequestParams requestParams, SearchRequest searchRequest)
    {
        if (StringUtils.isNotEmpty(requestParams.getLocation())) {
            GeoDistanceSortBuilder sortBuilder = SortBuilders.geoDistanceSort("location", new GeoPoint(requestParams.getLocation())).order(SortOrder.ASC).unit(DistanceUnit.KILOMETERS);
            searchRequest.source().sort(sortBuilder);
        }
    }

    /**
     * 根据所选的内容进行条件过滤
     */
    private BoolQueryBuilder filterUtilSet(RequestParams params)
    {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //if (requestParams.getKey()!=null)
        if (StringUtils.isNotEmpty(params.getKey())) {
            //2.1当搜索框内有内容时,根据输入的内容进行查询
            boolQuery.must(QueryBuilders.matchQuery("all", params.getKey()));
        } else {
            //2.2当搜索框内无内容时，默认搜索全部酒店
            boolQuery.must(QueryBuilders.matchAllQuery());
        }
        //品牌
        if (StringUtils.isNotEmpty(params.getBrand())) {
            boolQuery.filter(QueryBuilders.termQuery("brand", params.getBrand()));
        }
        //城市
        if (StringUtils.isNotEmpty(params.getCity())) {
            boolQuery.filter(QueryBuilders.termQuery("city", params.getCity()));
        }
        //
        //星级
        if (StringUtils.isNotEmpty(params.getStarName())) {
            boolQuery.filter(QueryBuilders.termQuery("starName", params.getStarName()));
        }
        //最小价格
        if (params.getMinPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(params.getMinPrice()));
        }
        //最大价格
        if (params.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").lte(params.getMaxPrice()));
        }
        return boolQuery;
    }
}
