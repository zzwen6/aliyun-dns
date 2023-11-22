package top.hting;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.alidns20150109.AsyncClient;
import com.aliyun.sdk.service.alidns20150109.models.UpdateDomainRecordRequest;
import com.aliyun.sdk.service.alidns20150109.models.UpdateDomainRecordResponse;
import com.google.gson.Gson;
import darabonba.core.client.ClientOverrideConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args ) throws Exception {
        // 二级域名
        String rr = "home";
        String ipPath = System.getProperty("ip");
        String recordId = System.getProperty("recordId");
        File file = new File(ipPath);
        if (!file.exists()) {
            file.createNewFile();
        }
        String oldIp = FileUtil.readString(file, "utf-8");
        // 获取IP地址
        String ip = getPublicIp();

        if (!StringUtils.equals(oldIp, ip)) {
            // 更新
            boolean update = update(ip, rr, recordId);
            if (update) {
                FileUtil.writeString(ip,file,"utf-8");
            }

        }

    }

    private static String getPublicIp() throws Exception {
        String url = "https://ip.900cha.com/";
        HttpResponse execute = HttpUtil.createGet(url).execute();
        String body = execute.body();
        String ipAddr = StringUtils.substringBetween(body, "准确归属地: ", "->");
        System.out.println(ipAddr);
        return ipAddr.trim();
    }

    private static boolean update(String ip, String rr, String recordId) throws InterruptedException, java.util.concurrent.ExecutionException {
        // HttpClient Configuration
        /*HttpClient httpClient = new ApacheAsyncHttpClientBuilder()
                .connectionTimeout(Duration.ofSeconds(10)) // Set the connection timeout time, the default is 10 seconds
                .responseTimeout(Duration.ofSeconds(10)) // Set the response timeout time, the default is 20 seconds
                .maxConnections(128) // Set the connection pool size
                .maxIdleTimeOut(Duration.ofSeconds(50)) // Set the connection pool timeout, the default is 30 seconds
                // Configure the proxy
                .proxy(new ProxyOptions(ProxyOptions.Type.HTTP, new InetSocketAddress("<your-proxy-hostname>", 9001))
                        .setCredentials("<your-proxy-username>", "<your-proxy-password>"))
                // If it is an https connection, you need to configure the certificate, or ignore the certificate(.ignoreSSL(true))
                .x509TrustManagers(new X509TrustManager[]{})
                .keyManagers(new KeyManager[]{})
                .ignoreSSL(false)
                .build();*/

        // Configure Credentials authentication information, including ak, secret, token
        StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
                // Please ensure that the environment variables ALIBABA_CLOUD_ACCESS_KEY_ID and ALIBABA_CLOUD_ACCESS_KEY_SECRET are set.
                .accessKeyId(System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID"))
                .accessKeySecret(System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET"))
                //.securityToken(System.getenv("ALIBABA_CLOUD_SECURITY_TOKEN")) // use STS token
                .build());

        // Configure the Client
        AsyncClient client = AsyncClient.builder()
                .region("cn-hangzhou") // Region ID
                //.httpClient(httpClient) // Use the configured HttpClient, otherwise use the default HttpClient (Apache HttpClient)
                .credentialsProvider(provider)
                //.serviceConfiguration(Configuration.create()) // Service-level configuration
                // Client-level configuration rewrite, can set Endpoint, Http request parameters, etc.
                .overrideConfiguration(
                        ClientOverrideConfiguration.create()
                                .setEndpointOverride("alidns.cn-hangzhou.aliyuncs.com")
                        //.setConnectTimeout(Duration.ofSeconds(30))
                )
                .build();

        // Parameter settings for API request
        UpdateDomainRecordRequest updateDomainRecordRequest = UpdateDomainRecordRequest.builder()
                .recordId(recordId)
                .rr(rr)
                .type("A")
                .value(ip)
                // Request-level configuration rewrite, can set Http request parameters, etc.
                // .requestConfiguration(RequestConfiguration.create().setHttpHeaders(new HttpHeaders()))
                .build();

        // Asynchronously get the return value of the API request
        CompletableFuture<UpdateDomainRecordResponse> response = client.updateDomainRecord(updateDomainRecordRequest);
        // Synchronously get the return value of the API request
        UpdateDomainRecordResponse resp = response.get();
        String json = new Gson().toJson(resp);
        System.out.println(json);
        // Asynchronous processing of return values
        /*response.thenAccept(resp -> {
            System.out.println(new Gson().toJson(resp));
        }).exceptionally(throwable -> { // Handling exceptions
            System.out.println(throwable.getMessage());
            return null;
        });*/

        // Finally, close the client
        client.close();
        return StringUtils.isNotBlank(resp.getBody().getRecordId());
    }


}
