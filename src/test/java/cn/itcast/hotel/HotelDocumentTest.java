package cn.itcast.hotel;



import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import javax.print.Doc;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class HotelDocumentTest {

    @Resource
    private RestHighLevelClient client;

    @Resource
    private IHotelService hotelService;

    /**
     * 添加es
     */
    @Test
    void testAddDocument() throws IOException {
        //1. 先从数据查询一条记录
        Hotel one = hotelService.getById(5555);
        //2. 创建酒店数据的doc文档
        HotelDoc hotelDoc = new HotelDoc(one);
        //3. 创建添加的请求对象
        IndexRequest indexRequest = new IndexRequest("hotel").id(hotelDoc.getId().toString());
        //4. 设置请求体内容，json
        indexRequest.source(JSON.toJSONString(hotelDoc),XContentType.JSON);
        //5. 执行请求
        client.index(indexRequest,RequestOptions.DEFAULT);

    }

    /**
     * 获取es数据
     */
    @Test
    void testGetDocumentById() throws IOException {
        //1. 创建GetRequest请求, 索引名称，文档id
        GetRequest getRequest = new GetRequest("hotel", "36934");
        //2. 执行请求,获取响应体
        GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);
        //3. 获取响应内容source里的数据 json
        String json = response.getSourceAsString();
        System.out.println(json);
        //4. 解析json成java对象
        HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
        System.out.println("doc:" + hotelDoc);
    }

    /**
     * 删除es数据
     */
    @Test
    void testDeleteDocumentById() throws IOException {
        //1. 创建DeleteRequest, 指定索引，id
        DeleteRequest deleteRequest = new DeleteRequest("hotel","3306");
        //2. 执行请求
        client.delete(deleteRequest, RequestOptions.DEFAULT);

    }

    /**
     * 更新es数据
     */
    @Test
    void testUpdateById() throws IOException {
        //1. 创建UpdateRequest, 指定索引，id
        UpdateRequest updateRequest = new UpdateRequest("hotel", "3306");
        //2. 设置doc内容，更新字段
        // 如何使用map时，map中的value必须为Object
        HashMap<String, Object> map = new HashMap<>();
        ////更新字段
        map.put("price", 550);
        map.put("score", 49);
        updateRequest.doc(map);
        /*updateRequest.doc(
            "price", 500,
            "score", 50);*/
        //3. 执行更新
        client.update(updateRequest,RequestOptions.DEFAULT);
    }


    //批量添加数据到es中
    @Test
    void testBulkRequest() throws IOException {
        //1. 查询Mysql数据库中所有酒店数据 集合
        List<Hotel> list = hotelService.list();
        //2. 转成HotelDoc对象
        //3. 创建BulkRequest对象
        //4. 遍历酒店数据
        BulkRequest bulkRequest = new BulkRequest();
        /*for (Hotel hotel : list) {
            HotelDoc doc = new HotelDoc(hotel);
            //5. 创建IndexRequest, 添加BulkRequest里
            // 创建添加的请求对象
            IndexRequest indexRequest = new IndexRequest("hotel").id(doc.getId().toString());
            // 设置请求体内容，json
            indexRequest.source(JSON.toJSONString(doc), XContentType.JSON);
            bulkRequest.add(indexRequest);
        }*/

        list.stream()// 流中元素是Hotel
            .map(HotelDoc::new)//流中元素是HotelDoc
            //流中元素 IndexRequest
            .map(doc->new IndexRequest("hotel").id(doc.getId().toString()).source(JSON.toJSONString(doc), XContentType.JSON))
            .forEach(bulkRequest::add);


        //6. 执行请求
        client.bulk(bulkRequest, RequestOptions.DEFAULT);
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
