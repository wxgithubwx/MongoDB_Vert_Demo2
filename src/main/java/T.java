import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;


public class T {

    public static Router router;  //定义web路由器
    public static MongoClient mongoClient;//定义客户端
    public static final String COL_NAME = "users";//数据库集合

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        router = Router.router(vertx);
        JsonObject config = new JsonObject()
                .put("host", "localhost")
                .put("port", 27017)
                .put("db_name", "test");

        mongoClient = MongoClient.createNonShared(vertx, config);

        // 增加一个处理器，将请求的上下文信息，放到RoutingContext中
        router.route().handler(BodyHandler.create());  //todo 可以解决post参数问题

        //处理注册
        router.post("/register").handler(
                T::register
        );

        //处理登录  todo 3.避免使用get route
        router.post("/login").handler(
                T::login
        );

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }

    //处理登录请求 /login?username=admin&password=admin
    public static void login(RoutingContext context) {
        String username = context.request().getParam("username");
        String password = context.request().getParam("password");
        if (isBlank(username) || isBlank(password)) {
            context.response().setStatusCode(400).end("Filed does not be null");
            return;
        }
        JsonObject document = new JsonObject();
        document.put("name", username)
                .put("password", password);
        mongoClient.find(COL_NAME, document, res -> {
            if (res.succeeded()) {
                if (res.result().size() == 0) {
                    context.response().setStatusCode(404).end("login Failure ,please check your username or password");
                } else {
                   /* for (JsonObject json : res.result()) {
                        //context.response().end(json.encodePrettily());
                        context.response().end("login Successfully ," + json.getString("name"));
                    }*/
                   //todo 5.解决不必要的for循环
                    context.response().setStatusCode(200).end("login Successfully ," + res.result().get(0).getString("name"));
                }
            } else {
                // res.cause().printStackTrace();
                //todo 1.解决发生异常客户端未响应
                context.response().setStatusCode(500).end("unexpected error");
            }
        });
    }


    //处理注册请求 /register?username=admin&password=admin
    public static void register(RoutingContext context) {
        String username = context.request().getParam("username");
        String password = context.request().getParam("password");
        //todo 4.判定空字符串
        if (isBlank(username) || isBlank(password)) {
            context.response().setStatusCode(400).end("Filed does not be null");
            return;
        }
        JsonObject document = new JsonObject();
        document.put("name", username);
        //用户名是否已存在
        mongoClient.find(COL_NAME, document, res -> {
            if (res.result().size() == 0) {
                document.put("password", password);
                mongoClient.insert(COL_NAME, document, res2 -> {
                            if (res2.succeeded()) {
                                String id = res2.result();
                                context.response().setStatusCode(200).end("Successfully,Inserted user with id " + id);
                            } else {
                                //res2.cause().printStackTrace();
                                context.response().setStatusCode(500).end("unexpected error");
                            }
                        }
                );
            } else {
                context.response().setStatusCode(404).end("Failure, username having exists");
            }
        });
    }

    public static boolean isBlank(String str) {
        if (str == null || "".equals(str))
            return true;
        return false;
    }
}


/**
 * router.route("/register").handler(
 * context -> {
 * String param1 = context.request().getParam("param1");
 * String param2 = context.request().getParam("param2");
 * JsonObject obj = new JsonObject();
 * obj.put("method", "post").put("param1", param1).put("param2", param2);
 * <p>
 * // 申明response类型为json格式，结束response并且输出json字符串
 * context.response().putHeader("content-type", "application/json")
 * .end(obj.encodePrettily());
 * }
 * );
 */