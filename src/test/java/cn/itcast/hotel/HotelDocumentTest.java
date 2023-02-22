package cn.itcast.hotel;

import cn.itcast.hotel.service.IHotelService;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

    }

    @AfterEach
    void tearDown()
    {

    }

}
