import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestNewsAPI {
    public static void main(String[] args) {
        String apiKey = "68b40c8dea846dd356f2afbc0adfe0e5";
        String url = "https://newsapi.org/v2/top-headlines?country=us&apiKey=" + apiKey;
        
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();
                    
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("Status: " + response.statusCode());
            System.out.println("Body: " + response.body());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
