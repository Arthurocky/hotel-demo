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
import javax.xml.bind.annotation.XmlTransient;
import java.io.IOException;

import static cn.itcast.hotel.constants.HotelIndexConstants.MAPPING_TEMPLATE;

@SpringBootTest
class HotelIndexTest {

    private RestHighLevelClient client;

    //创建索引和映射
    @Test
    void testCreateIndex() throws IOException {
        //1.
        CreateIndexRequest hotel = new CreateIndexRequest("hotel");

        //2.
        hotel.source(MAPPING_TEMPLATE,XContentType.JSON);

        //3.
        client.indices().create(hotel,RequestOptions.DEFAULT);
    }

    @Test
    void testExistsIndex() throws IOException {

    }


    @Test
    void testDeleteIndex() throws IOException {

    }



    @BeforeEach
    void setUp() {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.211.128:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }




}
