package cn.itcast.hotel;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class Suggestion {

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
    public void testSuggestion() throws IOException
    {
        //1. 创建SearchRequest
        SearchRequest searchRequest = new SearchRequest("hotel");
        //设置条件
/*        CompletionSuggestionBuilder builder = SuggestBuilders.completionSuggestion("suggestion").prefix("sj").skipDuplicates(true).size(10);
        SuggestBuilder suggestion = new SuggestBuilder().addSuggestion("mySugg", builder);
        searchRequest.source().suggest(suggestion);*/
        searchRequest.source().suggest(new SuggestBuilder().addSuggestion(
                "mySugg",
                SuggestBuilders.completionSuggestion("suggestion")
                        .prefix("sj")
                        .skipDuplicates(true).size(10)
        ));
        //2. 查询
        SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);
        //3. 解析
        Suggest suggest = search.getSuggest();
        //Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> sugg = suggest.getSuggestion("mySugg");
        CompletionSuggestion mySugg = suggest.getSuggestion("mySugg");
        // 获取options
        List<CompletionSuggestion.Entry.Option> options = mySugg.getOptions();
        options.stream().map(CompletionSuggestion.Entry.Option::getText)
                .map(Text::string).forEach(System.out::println);

    }

}
