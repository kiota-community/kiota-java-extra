import com.github.api.GitHubClient;
import com.microsoft.kiota.authentication.AnonymousAuthenticationProvider;
import com.microsoft.kiota.http.OkHttpRequestAdapter;
import org.junit.Test;

public class ExcludeCodegenTest {

    @Test
    public void test() throws Exception {
        var client =
                new GitHubClient(new OkHttpRequestAdapter(new AnonymousAuthenticationProvider()));
        client.repos().byOwner("owner").byRepo("repo");
    }
}
