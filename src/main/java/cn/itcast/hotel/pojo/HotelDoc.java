package cn.itcast.hotel.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public class HotelDoc {
    private Long id;
    private String name;
    private String address;
    private Integer price;
    private Integer score;
    private String brand;
    private String city;
    private String starName;
    private String business;
    private String location;
    private String pic;

    /**
     * 排序时的 距离值
     */
    private Object distance;

    /**
     * 广告
     */
    private Boolean isAD;

    /**
     * 补充session字段内容--》通过name和business中来
     */
    private List<String> suggestion;


    public HotelDoc(Hotel hotel)
    {
        this.id = hotel.getId();
        this.name = hotel.getName();
        this.address = hotel.getAddress();
        this.price = hotel.getPrice();
        this.score = hotel.getScore();
        this.brand = hotel.getBrand();
        this.city = hotel.getCity();
        this.starName = hotel.getStarName();
        this.business = hotel.getBusiness();
        this.location = hotel.getLatitude() + ", " + hotel.getLongitude();
        this.pic = hotel.getPic();
        //补充session字段内容--》通过name和business中来
        //仅添加品牌补充
        suggestion = new ArrayList<>();
        suggestion.add(this.brand);
        if(StringUtils.isNotEmpty(this.business)){
            // 按 、/ 拆分
            String[] split = this.business.split("、|/");
            Collections.addAll(suggestion, split);
        }
    }


    /**
     * 测试拼音
     */
    public static void main(String[] args)
    {
        String[] split = "前门、崇文门商贸区、天安门/王府井地区".split("、|/");
        Arrays.stream(split).forEach(System.out::println);
    }
}
