package cn.itcast.hotel;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class Aggregation {

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

    @Test
    public void testAggregation() throws IOException
    {

        //1.创建SearchRequest
        SearchRequest searchRequest = new SearchRequest("hotel");

        //2.做聚合-->按品牌，取十个
        AggregationBuilder size = AggregationBuilders.terms("brandAgg").field("brand").size(10);
        searchRequest.source().aggregation(size);

        //3.查询
        SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);

        //4.解析结果
        Aggregations aggregations = search.getAggregations();
        //聚合名称
        Terms brandAgg = aggregations.get("brandAgg");
        //聚合名称的数组
        List<? extends Terms.Bucket> list = brandAgg.getBuckets();
        for (Terms.Bucket bucket : list) {
            System.out.println(bucket.getKeyAsString());
        }

    }

}
