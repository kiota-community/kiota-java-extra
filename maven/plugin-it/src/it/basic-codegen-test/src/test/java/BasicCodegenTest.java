import com.apisdk.models.Widget;
import org.junit.Test;

public class BasicCodegenTest {

    @Test
    public void test() throws Exception {
        Class.forName("com.apisdk.models.Widget");

        Widget widget = new Widget();
        widget.setName("Test");
        widget.setDescription("A test widget.");
        widget.toString();
    }
}
