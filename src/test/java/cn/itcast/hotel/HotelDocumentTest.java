package cn.itcast.hotel;

import cn.itcast.hotel.service.IHotelService;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class HotelDocumentTest {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private IHotelService hotelService;

    @Test
    void testAddDocument()
    {

    }

    @Test
    void testGetDocumentById()
    {

    }

    @Test
    void testDeleteDocumentById()
    {

    }

    @Test
    void testUpdateById()
    {

    }


    //批量添加数据到es中
    @Test
    void testBulkRequest()
    {


    }

    @BeforeEach
    void setUp()
    {
        client = new RestHighLevelClient
                (RestClient.builder
                        (HttpHost.create("http://192.168.188.188:9200")));
    }

    @AfterEach
    void tearDown() throws IOException
    {
        client.close();
    }

}
