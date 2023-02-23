package cn.itcast.hotel;

import cn.itcast.hotel.constants.HotelIndexConstants;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;


@SpringBootTest
class HotelIndexTest {

    private RestHighLevelClient client;

    /**
     *创建索引和映射
     */
    @Test
    void testCreateIndex() throws IOException
    {
        //1. 创建请求对象
        CreateIndexRequest indexRequest = new CreateIndexRequest("hotel");
        //2. 设置请求体内容 dsl中的请求体内容
        indexRequest.source(HotelIndexConstants.MAPPING_TEMPLATE,XContentType.JSON);
        //3. 执行请求
        // 执行http请求 http协议(header, body), RequestOptions.DEFAULT 默认设置，包含请求头信息
        client.indices().create(indexRequest,RequestOptions.DEFAULT);
    }

    /**
     * 判断是否存在索引
     * @throws IOException
     */
    @Test
    void testExistsIndex() throws IOException {
        //1. 创建请求对象
        GetIndexRequest getIndexRequest = new GetIndexRequest("hotel");
        //2. 执行请求
        boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        System.out.println(exists);
    }

    /**
     * 删除索引
     * @throws IOException
     */
    @Test
    void testDeleteIndex() throws IOException {
        //1. 创建请求对象 Delete请求
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("hotel");
        //2. 执行请求
        client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
    }


    @BeforeEach
    void setUp() {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.188.188:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }



}
