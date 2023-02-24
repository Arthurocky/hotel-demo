package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SearchTest {
    private RestHighLevelClient client;

    @BeforeEach
    void setUp()
    {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.188.188:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException
    {
        client.close();
    }

    /**
     * 抽取方法，进行json转为对象(HotelDoc)
     */
    private void parseResponse(SearchResponse response)
    {
        //5. 解析结果
        SearchHits searchHits = response.getHits();
        // 获取总数量
        long total = searchHits.getTotalHits().value;
        System.out.println("total:" + total);
        // 获取结果集
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            // 解析成java bean对象
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            // 解析高亮
            parseHighlight(hit, hotelDoc);
            System.out.println(hotelDoc);
        }
    }

    /**
     * 抽取方法，进行高亮设置
     */
    private void parseHighlight(SearchHit hit, HotelDoc hotelDoc) {
        // 设置的高亮字段
        /**
         * "highlight" : {
         *   "name" : [
         *     "维也纳酒店(<font color='red'>深圳</font>国王店)"
         *   ]
         * }
         */
        // 有高亮返回才处理
        if(null != hit.getHighlightFields()) {
            HighlightField field = hit.getHighlightFields().get("name");
            // 高亮内容
            if(null != field) {
                Text[] fragments = field.getFragments();
                String highLights = Arrays.stream(fragments)
                        // Text -> String
                        .map(Text::string)
                        // joining 连接起来，
                        .collect(Collectors.joining(","));
                hotelDoc.setName(highLights);
            }
        }
    }

    /**
     * Match全文检索
     */
    @Test
    public void testMatchQuery() throws IOException
    {
        //1. 创建searchRequest对象, 指定索引名称
        SearchRequest searchRequest = new SearchRequest("hotel");
        //2. 构建查询条件
        MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("all", "如家");
        // QueryBuilders.matchAllQuery(); 查询所有
        //QueryBuilders.multiMatchQuery();// multi_match
        //3. 设置到searchRequest.source里
        SearchSourceBuilder query = searchRequest.source().query(matchQuery);
        //4. 执行查询
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        //5. 转化
        parseResponse(response);
    }

    /**
     * 精确查询
     */
    @Test
    public void testTermQuery() throws IOException
    {
        //1. 创建searchRequest对象, 指定索引名称
        SearchRequest searchRequest = new SearchRequest("hotel");
        //2. 构建查询条件
        TermQueryBuilder termQuery = QueryBuilders.termQuery("city", "深圳");
        searchRequest.source().query(termQuery);
        //4. 执行查询
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        parseResponse(response);
    }


    /**
     * range查询
     */
    @Test
    public void testRangeQuery() throws IOException
    {
        //1. 创建searchRequest对象, 指定索引名称
        SearchRequest searchRequest = new SearchRequest("hotel");
        //2. 构建查询条件
        RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price").gte(300).lte(500);
        searchRequest.source().query(rangeQuery);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        //4. 执行查询
        parseResponse(searchResponse);
    }

    /**
     * 复合查询
     */
    @Test
    public void testBoolQuery() throws IOException
    {
        //1. 创建searchRequest对象, 指定索引名称
        SearchRequest searchRequest = new SearchRequest("hotel");
        //2. 构建查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //添加Bool条件
        boolQuery.must(QueryBuilders.termQuery("city", "深圳"));
        boolQuery.filter(QueryBuilders.rangeQuery("price").lte(150));

        searchRequest.source().query(boolQuery);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        //4. 执行查询
        parseResponse(searchResponse);
    }


    /**
     * 分页排序
     */
    @Test
    public void testBoolQuerySortAndPaging() throws IOException
    {
        //1. 创建searchRequest对象, 指定索引名称
        SearchRequest searchRequest = new SearchRequest("hotel");
        //2. 构建查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //添加Bool条件
        boolQuery.must(QueryBuilders.termQuery("city", "深圳"));
        boolQuery.filter(QueryBuilders.rangeQuery("price").lte(150));

        //排序
        searchRequest.source().sort("price", SortOrder.DESC);


        // 分页 每页2条，查询第二页
        int page = 2;   // 深度分页 100>=page>1
        int size = 2; // <=50
        searchRequest.source().from((page - 1) * size).size(size);

        searchRequest.source().query(boolQuery);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        //4. 执行查询
        parseResponse(searchResponse);
    }


    /**
     * 高亮设置
     */
    @Test
    public void testHighLight() throws IOException
    {
        //1. 创建searchRequest对象, 指定索引名称
        SearchRequest searchRequest = new SearchRequest("hotel");
        //2. 构建查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 添加条件
        boolQuery.must(QueryBuilders.matchQuery("all", "深圳"));
        boolQuery.filter(QueryBuilders.rangeQuery("price").lte(200));

        searchRequest.source().query(boolQuery);

        // 排序
        searchRequest.source().sort("price", SortOrder.ASC);
        // 分页 每页2条，查询第二页
        int page = 2;   // 深度分页 100>=page>1
        int size = 2; // <=50

        searchRequest.source().from((page - 1) * size).size(size);
        //高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder()
                // 要高亮的字段
                .field("name")
                // 条件与高亮字段一致时才高亮，false:不一致也能高亮
                .requireFieldMatch(false);
        searchRequest.source().highlighter(highlightBuilder);

        //4. 执行查询
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        parseResponse(response);
    }


}

