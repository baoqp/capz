package example;

import com.capz.core.Capz;
import com.capz.core.http.HttpServer;
import com.capz.core.impl.CapzImpl;

/**
 * @author Bao Qingping
 */
public class HttpExample {
    public static void main(String[] args) {
        Capz capz = new CapzImpl();
        new HttpExample().example1(capz);
    }

    public void example1(Capz capz) {
        HttpServer server = capz.createHttpServer();

        server.requestHandler(request -> {
            request.response().end("Hello world");
        });

        server.listen(8080, "127.0.0.1",  res -> {
            if (res.succeeded()) {
                System.out.println("Server is now listening!");
            } else {
                System.out.println("Failed to bind!");
            }
        });



    }

}
